//> using file ../project.scala
//> using jvm 21
//> using dep org.scalameta::munit::1.3.4

// Shared helpers for the test suites. No @main, so this is not a tt verb and DispatchSuite does not
// count it; it lives in tools/test/ beside the suites that use it.
//
// Two things live here, and they are shared for the SAME reason: each was duplicated per suite until a
// single defect showed up as several unrelated failures. `removeAllForce` is that story on Windows
// cleanup; `ToolchainNoise` is it for build-tool chatter on stderr (issue 050).

object ToolchainNoise:

  /** Strip the lines the BUILD TOOL writes to a subprocess's stderr on an otherwise clean run.
    *
    * ⚠ WHY THIS EXISTS (issue 050). Every tool carries `//> using file project.scala` deliberately —
    * that include is what makes the launcher's single-file path agree with whole-directory builds — so
    * scala-cli answers with a "Using directives detected in multiple files" advisory on stderr, on
    * every per-file run, warm cache included. The advice cannot be taken. On a cold build unit it also
    * writes `Compiling project (...)` / `Compiled project (...)`, and on a cold cache `Downloading …`.
    *
    * None of that came from the tool under test, and it broke the suite's ONE exhaustive stderr
    * assertion — the only check across 46 verbs able to notice a verb that starts writing something
    * unexpected to stderr on a SUCCESS path. The cheap repair was to weaken that assertion to `._2`,
    * which would have deleted the check instead of fixing it. This is the other repair: filter the
    * toolchain out at the capture point so `stderr == ""` recovers its real meaning, *the tool wrote
    * nothing*.
    *
    * ⚠ IT FAILS TOWARD RED, deliberately. Every pattern is anchored and narrow. If scala-cli changes
    * its wording the lines stop matching, survive into `err`, and the exhaustive assertion goes red —
    * which is the safe direction, because a filter that silently widened would re-open the blind spot
    * it exists to close. Prefer extending the list over loosening a pattern.
    *
    * Verified 2026-09-19/23 that no tool in `tools/` prints a line of any of these shapes itself, so
    * nothing real is being swallowed. PURE. */
  def strip(err: String): String =
    val (_, kept) = err.split("\n", -1).foldLeft((false, Vector.empty[String])):
      case ((inAdvisory, acc), line) =>
        val plain = Ansi.replaceAllIn(line, "").trim
        if AdvisoryHead.matches(plain) then (true, acc)                 // block opens; drop
        else if inAdvisory && (plain.isEmpty || AdvisoryItem.matches(plain)) then (true, acc)
        else if inAdvisory && AdvisoryTail.matches(plain) then (false, acc)  // block closes; drop
        else if BuildProgress.matches(plain) then (false, acc)          // standalone, any position
        else (false, acc :+ line)                                       // anything unrecognised: KEEP
    kept.mkString("\n").trim

  /** scala-cli colours `[warn]` even into a pipe, so the patterns match on a de-escaped copy. */
  private val Ansi = """\e\[[0-9;]*m""".r

  // The advisory is a BLOCK: a head, a bullet per offending file, and a recommendation that closes it.
  // Anything not matching those shapes ends the block and is kept, so a malformed run cannot make this
  // swallow a tool's real output.
  private val AdvisoryHead = """\[warn\]\s+Using directives detected in multiple files:""".r
  private val AdvisoryItem = """-\s+\S+""".r
  private val AdvisoryTail = """It is recommended to keep them centralized in .*""".r

  /** Build progress, not tool output. `Downloading`/`Downloaded` matter on a fresh checkout, which is
    * exactly when someone runs the suite for the first time. */
  private val BuildProgress = """(Compiling|Compiled) project \(.*\)|(Downloading|Downloaded) \S+""".r

object TestFs:

  /** Recursive delete that also works on Windows.
    *
    * git creates its object files READ-ONLY. On POSIX that is irrelevant to deletion — permission to
    * unlink lives on the containing directory — but on Windows the read-only ATTRIBUTE blocks the
    * delete itself, so `os.remove.all` on any tree containing a `.git/` throws
    * AccessDeniedException on `.git\objects\..`. That threw from the `finally` of tests whose
    * assertions had already PASSED, which is why it read as 14 unrelated git failures rather than as
    * one cleanup bug (first seen when CI reached the suite on Windows, 2026-07-27).
    *
    * Clearing the attribute on every entry first is a no-op on POSIX, so the same path runs on all
    * three platforms and there is no OS branch to keep honest.
    */
  def removeAllForce(p: os.Path): Unit =
    if os.exists(p) then
      // walk BEFORE deleting; setWritable on a directory is what lets its entries be unlinked on Windows
      os.walk(p, includeTarget = true).foreach: q =>
        try q.toIO.setWritable(true) catch case _: Throwable => () // best effort: the delete below reports
      os.remove.all(p)

// The filter is the one piece of test support that can HIDE a failure if it is wrong, so it is the one
// piece that gets its own tests. Every input below is a real line captured from scala-cli 1.15.0 or
// 1.17.1 on this repo, not an invented shape.
class ToolchainNoiseSuite extends munit.FunSuite:

  test("the multiple-files advisory is stripped, colour codes and all") {
    // exactly what a warm `scala-cli run tools/json.scala` writes: 190 bytes, nothing else (issue 050)
    val err =
      "\u001b[33mwarn\u001b[0m]  Using directives detected in multiple files:\n" +
      "- tools/json.scala:1:1-3:30\n" +
      "It is recommended to keep them centralized in /home/x/genscalator/tools/project.scala file."
    assertEquals(ToolchainNoise.strip("[" + err), "")
  }

  test("a multi-file advisory drops every bullet, not just the first") {
    val err = "[\u001b[33mwarn\u001b[0m]  Using directives detected in multiple files:\n" +
      (1 to 40).map(i => s"- tools/t$i.scala:1:1-3:25").mkString("\n") +
      "\nIt is recommended to keep them centralized in /x/tools/project.scala file."
    assertEquals(ToolchainNoise.strip(err), "")
  }

  test("build progress is stripped wherever it appears") {
    assertEquals(ToolchainNoise.strip("Compiling project (Scala 3.9.0, JVM (21))"), "")
    assertEquals(ToolchainNoise.strip("Compiled project (Scala 3.9.0, JVM (21))"), "")
    // a fresh checkout — the case someone meets on their FIRST run of the suite
    assertEquals(ToolchainNoise.strip("Downloading https://repo1.maven.org/x.pom\nDownloaded https://repo1.maven.org/x.pom"), "")
  }

  test("the TOOL's own stderr survives, which is the whole point") {
    assertEquals(ToolchainNoise.strip("doc: no such doc 'nope'"), "doc: no such doc 'nope'")
    // interleaved with noise: the tool's line is kept and only the toolchain's goes
    val mixed = "Compiling project (Scala 3.9.0, JVM (21))\nharden: 2 candidates\nCompiled project (Scala 3.9.0, JVM (21))"
    assertEquals(ToolchainNoise.strip(mixed), "harden: 2 candidates")
  }

  test("an UNRECOGNISED line inside the advisory block ends it and is kept — fails toward red") {
    // if scala-cli ever changes the block's shape, the filter must not swallow what follows it
    val err = "[\u001b[33mwarn\u001b[0m]  Using directives detected in multiple files:\n" +
      "- tools/json.scala:1:1-3:30\n" +
      "json: something the TOOL said"
    assertEquals(ToolchainNoise.strip(err), "json: something the TOOL said")
  }

  test("a reworded advisory is NOT stripped, so the exhaustive assertion goes red rather than quiet") {
    val reworded = "[warn] Directives were found in several files:"
    assertEquals(ToolchainNoise.strip(reworded), reworded)
  }
