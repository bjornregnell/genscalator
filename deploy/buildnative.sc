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
// ⚠ The "non-Linux: unknown, proceed" intent below was NEVER REACHED before 2026-07-27. Files
// .readAllLines THROWS NoSuchFileException on a missing path rather than returning empty, so on any
// box without /proc/meminfo this died instead of proceeding. It read as handled and was not, and it
// only ever ran on Linux, so nothing contradicted the comment. The first macOS runner crashed here,
// in the step whose entire job is to decide whether it is safe to START.
//
// LAYERED, and the layers are NOT interchangeable. Linux MemAvailable is the kernel's estimate of
// what a new process could actually get, INCLUDING reclaimable page cache. The JDK's figure is
// genuinely-free RAM, which on a box that has been up a while is far smaller, because the kernel
// deliberately fills RAM with cache.
//
// MEASURED on blixten 2026-07-27, same box, same second: MemAvailable 18 GB, JDK free 5 GB. The JDK
// number sits BELOW the 6 GB floor, so had the portable metric been made primary, that build would
// have been REFUSED while 18 GB was genuinely available. The ordering below is therefore evidence,
// not taste: best metric first, portable metric second, unknown only if neither answers.

/** Linux only: MemAvailable, the metric that actually predicts whether a big allocation succeeds. */
def memAvailableGbLinux: Option[Long] =
  scala.util.Try {
    val memLine = Files.readAllLines(Paths.get("/proc/meminfo")).stream()
      .filter(_.startsWith("MemAvailable:")).findFirst()
    Option.when(memLine.isPresent)(memLine.get.split("\\s+")(1).toLong / (1024L * 1024L))
  }.toOption.flatten   // Try guards the missing file; the inner Option guards a missing line

/** Cross-platform fallback: free physical RAM via the JDK's extended OS bean. Conservative against
  * MemAvailable, since it does not count reclaimable cache, which is why it is second and not first.
  * com.sun.management is a JDK extension rather than java.*, present on OpenJDK and GraalVM. */
def freeMemoryGbJdk: Option[Long] =
  scala.util.Try {
    java.lang.management.ManagementFactory.getOperatingSystemMXBean match
      case os: com.sun.management.OperatingSystemMXBean => Some(os.getFreeMemorySize / (1024L * 1024L * 1024L))
      case _                                            => None
  }.toOption.flatten

/** GB the build may plausibly use, with the source that produced it. -1 means nobody could answer. */
def availableGb: (gb: Long, source: String) =
  memAvailableGbLinux.map(g => (gb = g, source = "MemAvailable"))
    .orElse(freeMemoryGbJdk.map(g => (gb = g, source = "JDK free physical, conservative")))
    .getOrElse((gb = -1L, source = "unknown"))

// --mem-floor <gb|off>: the 6 GB floor is RIGHT on a dev box, where an OOM takes the desktop with it,
// and WRONG on a CI runner, where neither metric is a reliable proxy for what the job may use. So it
// is overridable, never deleted. Since 2026-07-27 the floor applies on macOS and Windows too, via the
// JDK fallback above; before that those platforms were not merely unchecked, they crashed.
val memFloorGb: Option[Long] = optVal("--mem-floor") match
  case None                                => Some(6L)
  case Some("off")                         => None
  case Some(v) if v.toLongOption.isDefined => Some(v.toLong)
  case Some(v)                             => die(s"--mem-floor expects GB as a number, or 'off' - got '$v'")

// Not merely unused when the floor is off: NOT EVALUATED. --mem-floor off must mean "do not consult
// the memory at all", or a probe that cannot run on this platform still gets to fail the build.
val mem = memFloorGb.fold((gb = -1L, source = "not consulted"))(_ => availableGb)
memFloorGb.foreach: floor =>
  if mem.gb >= 0 && mem.gb < floor then
    die(s"only ${mem.gb} GB available via ${mem.source} (floor $floor) - close things or retry " +
        "later; nothing was changed")
// Name the SOURCE, not just the number: the two metrics differ by a lot on Linux, so a refusal
// that reports 3 GB is only interpretable if you know whether that was MemAvailable or free RAM.
val memNote =
  if memFloorGb.isEmpty then "not consulted"
  else if mem.gb < 0 then "unknown: no /proc/meminfo, and no JDK OS bean either"
  else s"${mem.gb} GB via ${mem.source}"
println(s"buildnative: memory check ${if memFloorGb.isEmpty then "SKIPPED (--mem-floor off)" else "ok"} ($memNote)")

// ⚠ Windows needs the .bat name EXPLICITLY. PATHEXT resolution is a SHELL feature, and neither Git
// Bash nor Java's ProcessBuilder does it: the launcher installs scala-cli.bat, so a bare "scala-cli"
// dies with CreateProcess error=2. This is the SAME defect at two layers — the workflow step hit it
// first (exit 127, fixed by letting Windows use its default shell), and then THIS script hit it from
// inside, spawning its own subprocess. Fixing one layer just exposed the next.
val scalaCli =
  if System.getProperty("os.name", "").toLowerCase.contains("win") then "scala-cli.bat" else "scala-cli"

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
  scalaCli, "--power", "package", "--native-image", toolsDir.toString,
  "--main-class", "dispatchTypedTools", "-o", nextBin.toString,
  "--", "--no-fallback", "--enable-url-protocols=https,http", "-J-Xmx6g")
if !Files.isRegularFile(nextBin) then die(s"build reported success but $nextBin is missing")
val sizeMb = Files.size(nextBin) / (1024 * 1024)

// ---- step 2: the golden net, THROUGH the candidate (parity mode) ----
val paritySecs = run("parity",
  scalaCli, "test", toolsDir.toString,
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
