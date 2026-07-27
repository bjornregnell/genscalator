//> using scala 3.9.0-RC4

// =============================================================================
// buildnative.sc  -  the tt-graalify REBUILD RITUAL: build -> parity -> swap
// =============================================================================
// The one sanctioned way to refresh the native-image tt binary (docs/native.md).
// The ritual exists so a rebuilt binary can NEVER silently replace a behaving one:
// the swap happens only after the golden CLI-contract suite passes THROUGH the
// candidate binary. Until this script has run green, the launcher's staleness
// check keeps routing TT_NATIVE=1 calls back to scala-cli — so a source edit
// plus a forgotten rebuild degrades to slow, never to wrong.
//
// WHAT IT DOES (in order; stops at the first failure, current binary untouched)
//   0. Refuses to start below a 6 GB free-memory floor (native-image is a hog;
//      measured peak 3.3 GB, the floor leaves headroom for the box's day job).
//   1. Builds tools/ via `scala-cli --power package --native-image` into
//      tmp/tt-native.next  (never directly onto the live binary).
//   2. Runs the FULL test suite with -Dtt.native.bin=tt-native.next, so every
//      CLI-contract test execs the CANDIDATE (parity mode, cli.test.scala).
//   3. Atomically swaps tt-native.next -> tmp/tt-native (same-filesystem move).
//   4. Prints an enumerated verdict: sizes, durations, suite exit, SWAPPED line.
//
// ON FAILURE
//   - build fails  -> no .next, nothing changed.
//   - parity fails -> tt-native.next is KEPT for inspection, live binary
//     untouched; delete the .next by hand after diagnosing.
//
// USAGE (run from the genscalator root; BR-present, it is a many-minute build)
//   scala-cli run deploy/buildnative.sc                  # full ritual
//   scala-cli run deploy/buildnative.sc -- --root <abs>  # explicit checkout root
//   scala-cli run deploy/buildnative.sc -- --out dist/bin/tt.exe   # name the output (CI, per platform)
//   scala-cli run deploy/buildnative.sc -- --mem-floor off         # skip the free-memory floor (CI)
//   (--out is relative to --root; the candidate is written beside it so the swap stays atomic)
//
// Expected magnitude: build ~1m40s + suite ~2-4 min; binary ~40 MB.
// =============================================================================

import java.nio.file.{Files, Path, Paths, StandardCopyOption}

def die(msg: String): Nothing = { System.err.println(s"buildnative: $msg"); sys.exit(2) }

def optVal(name: String): Option[String] =
  val i = args.indexOf(name); if i >= 0 && i + 1 < args.length then Some(args(i + 1)) else None

val root: Path =
  val r = optVal("--root").map(Paths.get(_)).getOrElse(Paths.get("").toAbsolutePath)
  if !Files.isRegularFile(r.resolve("tools/tt")) then
    die(s"'$r' is not a genscalator root (no tools/tt) - run from the root or pass --root <abs>")
  r.toAbsolutePath

val toolsDir  = root.resolve("tools")
val tmpDir    = root.resolve("tmp")

// --out <path>: where the PROVEN binary lands. Default tmp/tt-native, which is the path the launcher
// looks at, so a local rebuild refreshes the binary `tt` actually runs. CI passes a per-platform name
// (and Windows needs the .exe suffix). A relative path resolves against --root, never the cwd, so the
// meaning does not change with where you happened to invoke this from.
val liveBin   =
  optVal("--out").map(o => root.resolve(o).toAbsolutePath.normalize).getOrElse(tmpDir.resolve("tt-native"))

// The candidate is always written BESIDE its target, so step 3 stays a same-filesystem ATOMIC_MOVE.
// Deriving it (rather than hardcoding tmp/) is what keeps that guarantee true for any --out.
val nextBin   = liveBin.resolveSibling(liveBin.getFileName.toString + ".next")

// Fail fast if the resolved tools dir is a propagated SUBSET (no dispatcher) rather than the
// canonical toolbox — else the native build dies late with a cryptic "Main entry point class
// 'dispatchTypedTools' not found" (SM203 cross-repo resolution: a work-repo tools/ carries `tt`
// but not dispatch.scala, so the root check above passes yet the build cannot start). Name the fix.
val dispatchSrc = toolsDir.resolve("dispatch.scala")
if !Files.isRegularFile(dispatchSrc) || !Files.readString(dispatchSrc).contains("dispatchTypedTools") then
  die(s"'$toolsDir' has no dispatcher (dispatch.scala with @main dispatchTypedTools) - it looks like a " +
      "propagated tools SUBSET, not the canonical genscalator toolbox. Pass --root <genscalator-root>.")

// ---- step 0: free-memory floor (native-image measured peak 3.3 GB; floor 6 GB) ----
def availableGb: Long =
  val memLine = Files.readAllLines(Paths.get("/proc/meminfo")).stream()
    .filter(_.startsWith("MemAvailable:")).findFirst()
  if !memLine.isPresent then -1L  // non-Linux: unknown, proceed (the build will tell)
  else memLine.get.split("\\s+")(1).toLong / (1024L * 1024L)

// --mem-floor <gb|off>: the 6 GB floor is RIGHT on a dev box, where an OOM takes the desktop with it,
// and WRONG on a CI runner, where MemAvailable is not a reliable proxy for what the job may use. So it
// is overridable, never deleted. NB the floor is Linux-only either way: availableGb reads /proc/meminfo
// and returns -1 elsewhere, so macOS and Windows runners never hit it.
val memFloorGb: Option[Long] = optVal("--mem-floor") match
  case None                                => Some(6L)
  case Some("off")                         => None
  case Some(v) if v.toLongOption.isDefined => Some(v.toLong)
  case Some(v)                             => die(s"--mem-floor expects GB as a number, or 'off' - got '$v'")

val gb = availableGb
memFloorGb.foreach: floor =>
  if gb >= 0 && gb < floor then
    die(s"only $gb GB available (floor $floor) - close things or retry later; nothing was changed")
val memNote = if gb < 0 then "unknown, non-Linux" else s"$gb GB available"
println(s"buildnative: memory check ${if memFloorGb.isEmpty then "SKIPPED (--mem-floor off)" else "ok"} ($memNote)")

def run(label: String, cmd: String*): Long =
  println(s"buildnative: [$label] ${cmd.mkString(" ")}")
  val t0 = System.nanoTime
  val p  = new ProcessBuilder(cmd*).directory(root.toFile).inheritIO().start()
  val rc = p.waitFor()
  val secs = (System.nanoTime - t0) / 1_000_000_000L
  if rc != 0 then die(s"[$label] exited $rc after ${secs}s - current binary untouched" +
    (if label == "parity" then s"; candidate kept for inspection at $nextBin" else ""))
  secs

// ---- step 1: build the CANDIDATE (never the live path) ----
Files.createDirectories(liveBin.getParent)
Files.deleteIfExists(nextBin)
val buildSecs = run("build",
  "scala-cli", "--power", "package", "--native-image", toolsDir.toString,
  "--main-class", "dispatchTypedTools", "-o", nextBin.toString,
  "--", "--no-fallback", "--enable-url-protocols=https,http", "-J-Xmx6g")
if !Files.isRegularFile(nextBin) then die(s"build reported success but $nextBin is missing")
val sizeMb = Files.size(nextBin) / (1024 * 1024)

// ---- step 2: the golden net, THROUGH the candidate (parity mode) ----
val paritySecs = run("parity",
  "scala-cli", "test", toolsDir.toString,
  "--java-prop", s"tt.tools=$toolsDir",
  "--java-prop", s"tt.native.bin=$nextBin")

// ---- step 3: atomic swap (same filesystem, REPLACE_EXISTING) ----
Files.move(nextBin, liveBin, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)

// ---- step 4: enumerated verdict ----
println("buildnative: VERDICT")
println(s"  binary   : $liveBin ($sizeMb MB)")
println(s"  build    : ${buildSecs}s   parity suite: ${paritySecs}s (exit 0 = 0 failures)")
// With --out pointing away from the launcher's path, calling this "the live binary" would be a lie:
// plain `tt` still runs whatever sits at tmp/tt-native. Say which of the two actually happened.
val launcherBin = tmpDir.resolve("tt-native")
println(s"  SWAPPED  : $liveBin now IS the parity-proven candidate")
if liveBin == launcherBin then
  println(s"  reminder : plain tt <tool> uses it by default (TT_NATIVE=0 opts out); staleness re-arms on any tools/ edit")
else
  println(s"  note     : --out wrote OUTSIDE the launcher path ($launcherBin), so plain `tt` is unaffected")
