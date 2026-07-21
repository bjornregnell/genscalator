//> using scala 3.8.4

// =============================================================================
// deployttapi.sc  -  generate the scaladoc API docs for the whole tt toolbox
// =============================================================================
// SM188 step 1 of the api-docs pipeline: GENERATE + populate docs/generated/api/.
// A LATER step will deploy to bjornregnell.se (see deployblog.sc for the transport
// pattern); this script deliberately does NOT deploy anything.
//
// WHAT IT DOES
//   1. Runs `scala-cli doc` on tools/ (the tt toolbox, Scala 3.8.4) into a FRESH
//      build dir under the repo's gitignored tmp/ (in-repo tmp, never /tmp) --
//      doc generators like to clear their output dir, so they never touch docs/.
//   2. Syncs the generated site into docs/generated/api/: stale previously-
//      generated files are removed, fresh ones copied in.
//   3. Prints an enumerated verdict (pages written, output dir, deploy reminder).
//
// THE README GUARD (the one hard safety rule)
//   docs/generated/api/README.md is HUMAN-authored (BR's TODO notes) and must
//   survive every run byte-identical. Enforced structurally, three layers deep:
//     - the generator writes only into tmp/ttapi-build/ (never into docs/);
//     - the sync step never deletes or overwrites api/README.md (and skips any
//       README.md the generator might emit at the site root);
//     - README's bytes are snapshotted before the sync and verified after --
//       if anything differs the original bytes are restored and the run FAILS.
//   Idempotent: safe to re-run; each run rebuilds tmp/ttapi-build/ from scratch.
//
// USAGE (run from the genscalator root)
//   scala-cli run deploy/deployttapi.sc -- --dry-run   # generate into tmp/, show what WOULD change in docs/, change nothing there
//   scala-cli run deploy/deployttapi.sc                # generate + sync into docs/generated/api/
//   scala-cli run deploy/deployttapi.sc -- --root <abs>  # treat <abs> as the genscalator checkout (else cwd must be the root)
//
// NOTES
//   - Expected magnitude: ~245 html pages (grows as tools are added).
//   - tools/test/*.test.scala are test-scope, so `scala-cli doc` (no --test) skips them.
//   - The doc build takes a minute or two and may spin the scala-cli compile server.
//
// REFERENCES (moved here 2026-07-21 from docs/generated/api/README.md when BR handed that
// human-authored file over -- the generated dir now holds ONLY generated output; the README
// guard below stays as a defensive rail should a human file ever land there again):
//   scaladoc COMMENT style (feeds the scala-style skill TODO, SM182:5):
//     https://docs.scala-lang.org/style/scaladoc.html
//     https://docs.scala-lang.org/overviews/scaladoc/for-library-authors.html
//     https://nightly.scala-lang.org/docs/contributing/scaladoc.html   (the source of truth)
//     https://github.com/scala/scala3/tree/main/scaladoc
//   generation method (BR's sbt-vs-scala-cli question, RESOLVED: scala-cli doc, implemented below):
//     https://scala-cli.virtuslab.org/docs/commands/doc/
//   local serving: tt serv <root>/docs/generated/api --port 8138
// =============================================================================

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import scala.jdk.CollectionConverters.*

def die(msg: String): Nothing = { System.err.println(s"deployttapi: $msg"); sys.exit(2) }

// ---- args: --dry-run + the --root <dir> value flag ----
val dryRun = args.contains("--dry-run")
def optVal(name: String): Option[String] =
  val i = args.indexOf(name); if i >= 0 && i + 1 < args.length then Some(args(i + 1)) else None

// The genscalator checkout to work in (default = current dir; override with --root <abs>).
// We generate into <root>/tmp/ and sync into <root>/docs/, so VERIFY the root really is
// a genscalator checkout before touching anything.
val root = optVal("--root").map(Paths.get(_)).getOrElse(Paths.get("")).toAbsolutePath.normalize
if !Files.isDirectory(root.resolve("tools")) || !Files.exists(root.resolve("deploy").resolve("deployttapi.sc")) then
  die(s"'$root' is not a genscalator checkout (expected tools/ + deploy/deployttapi.sc there); run from the repo root, or pass --root <abs>")

val toolsDir = root.resolve("tools")
val buildDir = root.resolve("tmp").resolve("ttapi-build")          // fresh scratch site (gitignored, in-repo)
val apiDir   = root.resolve("docs").resolve("generated").resolve("api")
val readme   = apiDir.resolve("README.md")                          // HUMAN-authored: never deleted, never overwritten

// Recursively delete -- but ONLY our own build dir, never anything else (name-pinned like mirror.sc's rmrf).
def rmrfBuild(p: Path): Unit =
  if p.getFileName == null || p.getFileName.toString != "ttapi-build" then
    die(s"refusing to recursively delete an unexpected path: $p")
  if Files.exists(p) then
    val all = Files.walk(p)
    try all.iterator.asScala.toList.reverse.foreach(Files.delete)   // children before parents
    finally all.close()

// True iff `d` is an empty directory (stream closed -- adjudication fix: the inline
// Files.list(...).findFirst() form leaked the directory stream's fd).
def isEmptyDir(d: Path): Boolean =
  val s = Files.list(d)
  try !s.findFirst().isPresent finally s.close()

// Every regular file under `dir`, as paths RELATIVE to it (sorted, stable).
def filesUnder(dir: Path): Vector[Path] =
  if !Files.isDirectory(dir) then Vector.empty
  else
    val s = Files.walk(dir)
    try s.iterator.asScala.filter(Files.isRegularFile(_)).map(dir.relativize).toVector.sorted
    finally s.close()

// ---- 1. generate the scaladoc site into the fresh build dir (loud, inherited terminal) ----
println(s"deployttapi: scaladoc  $toolsDir  ->  $buildDir")
rmrfBuild(buildDir)
Files.createDirectories(buildDir.getParent)
val cmd = Vector("scala-cli", "doc", toolsDir.toString, "-o", buildDir.toString, "-f")
val rc  = { val pb = new ProcessBuilder(cmd*); pb.inheritIO(); pb.start().waitFor() }
if rc != 0 then die(s"scaladoc generation failed (`${cmd.mkString(" ")}` exited $rc)")
if !Files.exists(buildDir.resolve("index.html")) then die(s"no index.html in $buildDir -- generation produced no site?")

val generated  = filesUnder(buildDir).filterNot(_.toString == "README.md")  // a root README from the generator would shadow BR's: skip it
val htmlPages  = generated.count(_.toString.endsWith(".html"))
println(s"deployttapi: generated ${generated.size} files ($htmlPages html pages) in $buildDir")

// ---- 2. sync build dir -> docs/generated/api/ (README.md untouchable) ----
// Snapshot the human-authored README BEFORE touching api/, verify byte-identity after.
val readmeBefore: Option[Array[Byte]] = if Files.isRegularFile(readme) then Some(Files.readAllBytes(readme)) else None
if readmeBefore.isEmpty then println(s"deployttapi: note -- no $readme yet (nothing to guard this run)")

val stale = filesUnder(apiDir).filterNot(_.toString == "README.md")
if dryRun then
  println(s"deployttapi: DRY-RUN  would remove ${stale.size} stale files from $apiDir and write ${generated.size} fresh ones (README.md untouched); docs/ unchanged")
else
  // Safety: only ever clear a dir that IS the expected .../docs/generated/api (path-pinned).
  if !apiDir.endsWith(Paths.get("docs", "generated", "api")) then die(s"refusing to clear an unexpected path: $apiDir")
  for rel <- stale do Files.delete(apiDir.resolve(rel))             // stale generated files: removal is wanted
  // drop now-empty subdirs (deepest first), never api/ itself
  if Files.isDirectory(apiDir) then
    val ds = Files.walk(apiDir)
    try
      for d <- ds.iterator.asScala.toVector.sortBy(-_.getNameCount)
          if d != apiDir && Files.isDirectory(d) && isEmptyDir(d)
      do Files.delete(d)
    finally ds.close()
  for rel <- generated do
    val dst = apiDir.resolve(rel)
    Files.createDirectories(dst.getParent)
    Files.copy(buildDir.resolve(rel), dst, StandardCopyOption.REPLACE_EXISTING)
  // ---- the README guard verdict: byte-identical or restore + fail ----
  readmeBefore.foreach { before =>
    val intact = Files.isRegularFile(readme) && java.util.Arrays.equals(Files.readAllBytes(readme), before)
    if !intact then
      Files.write(readme, before)                                   // restore the human's bytes, then fail loudly
      die("README guard TRIPPED: api/README.md was altered by the sync -- original bytes restored; fix the sync logic before re-running")
  }

// ---- 3. enumerated verdict ----
val where = if dryRun then buildDir else apiDir
println("deployttapi: verdict")
println(s"  1. html pages ${if dryRun then "generated (dry-run, docs/ untouched)" else "written"}: $htmlPages  (${generated.size} files total)")
println(s"  2. output dir: $where")
println(s"  3. README guard: ${if readmeBefore.isEmpty then "n/a (no README.md present)" else if dryRun then "nothing touched (dry-run)" else "README.md byte-identical, untouched"}")
println( "  4. deploy step: NOT yet implemented -- this script only generates locally (SM188 step 1)")
