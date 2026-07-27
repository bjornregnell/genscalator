//> using file project.scala
//> using file lib.scala
//> using file ziplib.scala
//> using jvm 21

// zip — read-only inspection of a zip archive via the JDK's own java.util.zip: no dependency, no
// subprocess. Reads, computes, prints; never writes.
//
//   tt zip list  <file.zip>    entries with uncompressed / compressed / method / crc32 / name
//   tt zip check <file.zip>    decompress EVERY entry so the JDK validates each CRC32
//
// WHY IT EXISTS: built 2026-07-27 after a raw `unzip -l` reach while verifying a downloaded release
// asset. The toolbox could prove an asset's sha256 (`tt forge release-download --verify`) but had no
// way to look INSIDE it — and D3 in reqts/DESIGN.md picked zip as the release format precisely
// BECAUSE the JDK reads it unaided, so shelling out to `unzip` contradicted the format's own stated
// rationale. The missing verb was the signal.
//
// `list` and `check` answer DIFFERENT questions and a caller should not conflate them: a sha256
// proves the bytes arrived exactly as sent; `check` proves the archive is internally sound and every
// entry actually decompresses. An archive can pass either and fail the other — a truncated upload
// fails sha256 while its surviving entries still decompress, and a correctly-transferred but
// internally corrupt archive passes sha256 and fails CRC.
//
// EXTRACTION was deliberately absent at first, and the reason is worth keeping: extraction WRITES, and a
// zip entry name may contain parent-directory hops or an absolute path (zip-slip), so a safe extractor
// needs a path-containment guard designed on purpose with its own hostile-entry tests, rather than tacked
// onto an inspection tool. It was then built that way, guard first, and shipped 2026-07-27.
//
// ⚠ THE COMPUTE LAYER AND THE GUARD NOW LIVE IN ziplib.scala, not here. `tt update --native` has to unpack
// the release it just downloaded, and tools depend on shared libs rather than on each other — so a second
// copy of the containment guard would otherwise have appeared in `update`. What remains in this file is
// the CLI: flags, preview rendering, and exit codes. See ziplib.scala for the threat model.
//
// STRUCTURE: the compute layer returns DATA and THROWS on bad input; only the drivers print and exit. That
// is not decoration — a `sys.exit` reached from the compute layer would kill the test JVM, so a tool that
// dies internally cannot be unit-tested at all.
import java.nio.file.{Files, Path}
import scala.util.Try
import agenttools.{Lib, ZipLib}

object Zip {

  // Forwarders to the ONE definition in ZipLib, so this file's drivers and the ZipSuite tests keep
  // reading the way they did while `tt update --native` shares the same code. Same move `globMatches`
  // made to Lib, and `forge`'s download path made to ReleaseLib.
  type Entry = ZipLib.Entry
  val Entry  = ZipLib.Entry

  def methodName(m: Int): String                  = ZipLib.methodName(m)
  def entriesOf(file: Path): Vector[Entry]        = ZipLib.entriesOf(file)

  def failures(file: Path): Vector[(String, String)] = ZipLib.failures(file)

  // ---- extraction ---------------------------------------------------------------------------------
  //
  // The containment guard, the threat model it answers, and the all-or-nothing adjudication now live in
  // ziplib.scala — read that file before changing anything here, because the security content is there
  // and not in this CLI. `resolveEntry` is forwarded rather than re-implemented, which is what keeps the
  // ZipSuite hostile-entry tests pointed at the code `tt update --native` actually runs.
  //
  // PREVIEW BY DEFAULT, applying only with `--write`, the same contract as `tt sub` and
  // `tt forge release-delete`. This is the most destructive verb in the toolbox: it writes executables.
  def resolveEntry(destRoot: Path, entryName: String): Either[String, Path] =
    ZipLib.resolveEntry(destRoot, entryName)

  private def die(msg: String): Nothing = { System.err.println(s"zip: $msg"); sys.exit(2) }

  private val Help: String =
    """tt zip — read-only zip inspection using the JDK's java.util.zip (no dependency, no subprocess)
      |
      |Usage:
      |  zip list  <file.zip>    list entries: uncompressed, compressed, method, crc32, name
      |  zip check <file.zip>    decompress every entry so the JDK validates each CRC32
      |
      |check vs a checksum: a sha256 proves the bytes arrived as sent; check proves the archive is
      |internally sound and every entry decompresses. Different questions — run both on a release.
      |
      |  zip extract <file.zip> --dir D [--write] [--overwrite] [--exec GLOB] [--max-bytes N]
      |                          extract into D. PREVIEWS by default like `tt sub`; --write applies.
      |
      |--exec GLOB marks matching entries owner-executable. It is needed because java.util.zip does not
      |restore permission bits: the zip format carries them, ZipEntry exposes no way to read them, so
      |every extracted file is non-executable and a program without --exec exits 126. The caller declares
      |which entries are programs, because the archive cannot say and guessing from `bin/*` would be a
      |silent policy. This makes the tool deliberately stricter than the OS `unzip`, which restores it.
      |
      |Extraction is guarded, and the guard is the point. Every entry is adjudicated BEFORE anything is
      |written, so a hostile archive cannot leave a half-extracted tree behind. Rejected: any entry whose
      |resolved target escapes D, any absolute path (including a Windows drive letter or a UNC share, via
      |the one host-independent predicate in lib.scala), any control character in a name, and any archive
      |whose total uncompressed size exceeds --max-bytes (the zip-bomb guard, 1 GiB by default). An
      |existing target is refused unless --overwrite. A symlink INSIDE D that would redirect an innocent
      |entry outside is caught at write time by comparing real paths.
      |
      |A symlink ENTRY in the archive needs no check: this tool only ever writes bytes, so such an entry
      |becomes an ordinary file containing the link target, and the extract-then-write-through attack has
      |no first step. That stops being true if this tool ever learns to restore symlinks.""".stripMargin

  private def usage(): Nothing = die(
    "usage:\n" +
      "  zip list  <file.zip>    entries: uncompressed, compressed, method, crc32, name (READ-ONLY)\n" +
      "  zip check <file.zip>    read every entry so the JDK validates its CRC32\n" +
      "  zip extract <file.zip> --dir D [--write] [--overwrite] [--exec GLOB] [--max-bytes N]\n" +
      "                          PREVIEWS by default; --write applies. Rejects any entry that escapes D.\n" +
      "                          --exec GLOB marks matching entries executable (java.util.zip cannot\n" +
      "                          restore permission bits, so a program needs this or it exits 126)."
  )

  private def pathOf(file: String): Path = Path.of(file).toAbsolutePath.normalize

  /** Default cap on TOTAL uncompressed bytes — one definition, in ZipLib, so the CLI default and the
    * default a self-updater gets cannot drift apart. Raise with `--max-bytes`; the number is a policy,
    * not a law of zip.
    */
  private val DefaultMaxBytes: Long = ZipLib.DefaultMaxBytes

  /** ⚠ WHY `--exec` has to exist, because it looks like a wart and is not.
    *
    * `java.util.zip` does not restore unix permission bits. The zip FORMAT carries them (in the external
    * attributes field of the central directory) but `ZipEntry` exposes no API to read them, so every file
    * this tool writes lands non-executable. Found end-to-end on 2026-07-27: extracting the real release
    * archive produced a `bin/tt` that answered `Permission denied` (exit 126) — the exact wall a tester
    * following the documented install path would hit on their first command.
    *
    * The honest fix is for the CALLER to declare which entries are programs, because the archive cannot
    * tell us and guessing from a `bin/` prefix would be a silent policy. (Careful writing a glob in a
    * comment here: Scala comments NEST, so a literal slash-star opens one and swallows the close.)
    * Note the consequence: this
    * tool is deliberately STRICTER than the OS `unzip`, which does restore the bit. `--exec` sets
    * owner-execute only; anything wider is the human's `chmod`.
    */
  final case class ExtractOpts(file: String, dest: String, write: Boolean, overwrite: Boolean,
      maxBytes: Long, exec: Option[String])

  private def parseExtract(args: List[String]): ExtractOpts =
    @annotation.tailrec
    def go(rest: List[String], o: ExtractOpts): ExtractOpts =
      rest match
        case Nil                       => o
        case "--dir" :: d :: t         => go(t, o.copy(dest = d))
        case "--write" :: t            => go(t, o.copy(write = true))
        case "--overwrite" :: t        => go(t, o.copy(overwrite = true))
        case "--exec" :: g :: t        => go(t, o.copy(exec = Some(g)))
        case "--max-bytes" :: n :: t   =>
          n.toLongOption match
            case Some(v) if v > 0 => go(t, o.copy(maxBytes = v))
            case _                => die(s"--max-bytes needs a positive integer, got '$n'")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case f :: t if o.file.isEmpty  => go(t, o.copy(file = f))
        case other :: _               => die(s"unexpected argument '$other'")
    val o = go(args, ExtractOpts("", "", false, false, DefaultMaxBytes, None))
    if o.file.isEmpty then usage()
    if o.dest.isEmpty then die("extract needs --dir <destination> (there is no implicit destination, on purpose)")
    o

  private def extractDriver(args: List[String]): Unit =
    val o    = parseExtract(args)
    val src  = pathOf(o.file)
    val root = pathOf(o.dest)
    // planExtraction THROWS on an unreadable archive, an empty one, the zip-bomb cap, or any entry that
    // fails containment — and it adjudicates EVERY entry before anything is written, so a hostile archive
    // cannot leave a half-extracted tree behind. The driver's job is to turn that into an exit code.
    val plan = Try(ZipLib.planExtraction(src, root, o.maxBytes)).fold(t => die(t.getMessage), identity)

    if plan.clashes.nonEmpty && !o.overwrite then
      plan.clashes.foreach(t => println(s"EXISTS    $t"))
      die(s"${plan.clashes.size} target(s) already exist; pass --overwrite to replace them (nothing was written)")

    if !o.write then
      plan.planned.foreach: (e, t) =>
        val marked = o.exec.exists(g => Lib.globMatches(g, e.name))
        println(f"  ${e.size}%12d  -> $t${if marked then "  [+x]" else ""}")
      if o.exec.isEmpty then
        println("  NOTE: no --exec glob given, so nothing will be executable — java.util.zip cannot")
        println("        restore permission bits, so a program extracted without --exec exits 126.")
      println(s"would extract ${plan.planned.size} file(s), ${plan.total} B, into $root")
      println(s"  guard: all entries contained${if plan.clashes.isEmpty then "" else s", ${plan.clashes.size} would be OVERWRITTEN"}")
      println("  re-run with --write to apply")
    else
      val written = Try(ZipLib.extract(src, root, plan, o.exec)).fold(t => die(t.getMessage), identity)
      written.foreach((e, target, marked) => println(s"extracted ${e.name} -> $target${if marked then "  [+x]" else ""}"))
      println(s"zip extract: ${written.size} file(s), ${plan.total} B, into $root")

  private def listDriver(file: String): Unit =
    val p  = pathOf(file)
    val es = Try(entriesOf(p)).fold(t => die(t.getMessage), identity)
    val fs = es.filterNot(_.isDir)
    println(s"zip: $p  (${Files.size(p)} B on disk, ${es.size} entries)")
    es.foreach { e =>
      val nm = if e.isDir then s"${e.name}   (dir)" else e.name
      println(f"  ${e.size}%12d  ${e.compressed}%12d  ${e.method}%-8s  ${e.crc}%10d  $nm")
    }
    println(s"  total uncompressed: ${fs.map(_.size).sum} B in ${fs.size} file(s)")

  private def checkDriver(file: String): Unit =
    val p  = pathOf(file)
    val es = Try(entriesOf(p)).fold(t => die(t.getMessage), identity)
    val fs = es.filterNot(_.isDir)
    if fs.isEmpty then die(s"archive has no file entries, so there is nothing to check: $p")
    val bad = Try(failures(p)).fold(t => die(t.getMessage), identity)
    bad.foreach((n, err) => println(s"FAILED  $n  $err"))
    if bad.nonEmpty then die(s"${bad.size} of ${fs.size} entries failed to decompress: $p")
    println(s"zip check: $p")
    println(s"  ${fs.size} file entries, every CRC32 valid, ${fs.map(_.size).sum} B decompressed")

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    args.toList match
      case "list" :: f :: Nil  => listDriver(f)
      case "check" :: f :: Nil => checkDriver(f)
      case "extract" :: rest   => extractDriver(rest)
      case _                   => usage()
}

@main def inspectZipArchive(args: String*): Unit = Zip.dispatch(args*)
