//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3
//> using dep com.lihaoyi::os-lib:0.11.8

// AN EXPERIMENT, not a regression test — and it must stay in the suite precisely because a DESIGN
// depends on its result remaining true (reqts/DESIGN.md D7b).
//
// THE QUESTION. `tt update --native` has to replace a binary that may be the one currently executing
// the update. Writing through it (`Files.copy(REPLACE_EXISTING)`) truncates the inode being executed,
// and Windows refuses to overwrite a running image outright. The proposed design is instead
// move-aside-then-move-in, two ATOMIC_MOVEs, which SHOULD be correct on both families for two
// different underlying reasons:
//
//   POSIX   — rename() is atomic and unlinks the old directory entry; the running process keeps its
//             open inode and carries on unharmed.
//   Windows — overwriting a running .exe is forbidden, but RENAMING one is permitted, so the same two
//             moves work. This is the half that was believed rather than known, and is why this file
//             exists: it is verified by the `windows-latest` CI leg, not by reasoning on a Linux box.
//
// If this test ever fails on Windows, D7b's single-code-path decision is void and `tt update --native`
// needs a platform branch. That is the whole point of pinning it here.
//
// WHY THE SUBJECT IS THE NATIVE `tt` BINARY. The experiment needs a genuinely self-contained
// executable that can be COPIED and then launched from its new location. The JDK's own `java.exe` is
// not usable: relocated, it can no longer find `jvm.dll` / its runtime image and fails to start, so a
// test built on it would prove nothing about rename and merely fail to launch. A GraalVM native image
// has no such dependency. The parity pass already hands us one via `-Dtt.native.bin`, which is exactly
// the CI stage where the Windows answer is wanted — so the test is GUARDED on that property and skips
// (loudly) in a plain JVM run where no native binary exists.

import java.nio.file.{Files, Path, StandardCopyOption}

class RunningBinaryRenameSuite extends munit.FunSuite:

  /** A free loopback port, found by binding 0 and closing. `serv` exits if its port is taken, and a
    * dead subject would make this test pass for the wrong reason. */
  private def freePort(): Int =
    val s = java.net.ServerSocket(0)
    try s.getLocalPort finally s.close()

  private def isWindows: Boolean =
    System.getProperty("os.name", "").toLowerCase.contains("windows")

  test("a RUNNING executable can be renamed, and survives it (D7b)") {
    val nativeBin = Option(System.getProperty("tt.native.bin")).filter(_.nonEmpty).map(Path.of(_))
    assume(
      nativeBin.exists(Files.isRegularFile(_)),
      "skipped: no -Dtt.native.bin, so there is no self-contained executable to experiment on. " +
        "This test is meaningful in the buildnative parity pass (notably the windows-latest CI leg)."
    )
    val src = nativeBin.get

    val dir = os.temp.dir(prefix = "ttrename-")
    try
      // Copy the native image out to a location we may freely rename.
      val exe = dir.toNIO.resolve(if isWindows then "ttcopy.exe" else "ttcopy")
      Files.copy(src, exe, StandardCopyOption.COPY_ATTRIBUTES)
      exe.toFile.setExecutable(true, true)

      // Serve the temp dir: `tt serv` runs until killed, which is what makes the process a live image
      // rather than something that has already exited by the time we rename it.
      val port = freePort()
      val proc = ProcessBuilder(exe.toString, "serv", dir.toString, "--port", port.toString)
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.to((dir / "serv.log").toIO))
        .start()
      try
        // Wait for the image to actually be mapped and listening. Polling the port beats a fixed sleep:
        // a rename that "succeeded" against a process that never started would be a false PASS.
        val deadline = System.nanoTime + 20_000_000_000L
        var up = false
        while !up && System.nanoTime < deadline && proc.isAlive do
          up = try { val s = java.net.Socket("127.0.0.1", port); s.close(); true } catch case _: Throwable => false
          if !up then Thread.sleep(100)
        assert(proc.isAlive, s"the subject process died before the experiment; see ${dir / "serv.log"}")
        assert(up, s"the subject never listened on $port; see ${dir / "serv.log"}")

        // THE EXPERIMENT.
        val aside = dir.toNIO.resolve(if isWindows then "ttcopy.exe.old" else "ttcopy.old")
        val moved =
          try
            Files.move(exe, aside, StandardCopyOption.ATOMIC_MOVE)
            Right(())
          catch case t: Throwable => Left(s"${t.getClass.getName}: ${t.getMessage}")

        assert(
          moved.isRight,
          s"RENAMING A RUNNING EXECUTABLE FAILED on ${System.getProperty("os.name")}: ${moved.left.getOrElse("")}\n" +
            "  => reqts/DESIGN.md D7b's single-code-path decision does NOT hold on this platform and\n" +
            "     tt update --native needs a different mechanism here."
        )
        assert(Files.exists(aside), "the renamed-aside file is missing, so the move did not do what it claimed")
        assert(!Files.exists(exe), "the original path still exists after an atomic move")

        // The process must SURVIVE its own image being renamed — the property the design relies on.
        assert(proc.isAlive, "the process died when its image was renamed; move-aside is not safe here")

        // And the vacated path must be reusable, which is the second half of move-aside-then-move-in.
        Files.copy(src, exe, StandardCopyOption.COPY_ATTRIBUTES)
        assert(Files.exists(exe), "could not put a new binary at the vacated path")
      finally
        proc.destroy()
        proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
        if proc.isAlive then proc.destroyForcibly()
    finally TestFs.removeAllForce(dir)
  }
