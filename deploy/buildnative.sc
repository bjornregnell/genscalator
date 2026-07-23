//> using scala 3.8.4

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
val liveBin   = tmpDir.resolve("tt-native")
val nextBin   = tmpDir.resolve("tt-native.next")

// ---- step 0: free-memory floor (native-image measured peak 3.3 GB; floor 6 GB) ----
def availableGb: Long =
  val memLine = Files.readAllLines(Paths.get("/proc/meminfo")).stream()
    .filter(_.startsWith("MemAvailable:")).findFirst()
  if !memLine.isPresent then -1L  // non-Linux: unknown, proceed (the build will tell)
  else memLine.get.split("\\s+")(1).toLong / (1024L * 1024L)

val gb = availableGb
if gb >= 0 && gb < 6 then die(s"only $gb GB available (floor 6) - close things or retry later; nothing was changed")
println(s"buildnative: memory check ok (${if gb < 0 then "unknown, non-Linux" else s"$gb GB available"})")

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
Files.createDirectories(tmpDir)
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
println(s"  SWAPPED  : the live binary now IS the parity-proven candidate")
println(s"  reminder : plain tt <tool> uses it by default (TT_NATIVE=0 opts out); staleness re-arms on any tools/ edit")
