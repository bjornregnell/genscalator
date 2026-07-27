//> using file project.scala
//> using file lib.scala
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
// DELIBERATELY NO extract verb. Extraction WRITES, and a zip entry name may contain `../` or an
// absolute path (zip-slip), so a safe extractor needs a path-containment guard designed on purpose,
// with its own tests, rather than tacked onto an inspection tool. Flagged, not faked.
//
// STRUCTURE: the compute layer (`entriesOf`, `failures`) returns DATA and THROWS on bad input; only
// the drivers print and exit. That is not decoration — a `sys.exit` reached from the compute layer
// would kill the test JVM, so a tool that dies internally cannot be unit-tested at all.
import java.util.zip.{ZipEntry, ZipFile}
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}
import agenttools.Lib

object Zip {

  final case class Entry(name: String, size: Long, compressed: Long, method: String, crc: Long, isDir: Boolean)

  def methodName(m: Int): String = m match
    case ZipEntry.STORED   => "stored"
    case ZipEntry.DEFLATED => "deflated"
    case other             => s"method$other"

  /** Open a zip, or throw with a message that says which of the two failure modes happened —
    * "no such file" and "not a zip" are different problems and must not read the same.
    */
  private def open[A](file: Path)(use: ZipFile => A): A =
    if !Files.isRegularFile(file) then throw IllegalArgumentException(s"not a readable file: $file")
    Using.resource(
      Try(ZipFile(file.toFile)).getOrElse(throw IllegalArgumentException(s"not a readable zip archive: $file"))
    )(use)

  def entriesOf(file: Path): Vector[Entry] =
    open(file) { zf =>
      zf.entries.asScala.toVector.map { e =>
        Entry(e.getName, e.getSize, e.getCompressedSize, methodName(e.getMethod), e.getCrc, e.isDirectory)
      }
    }

  /** Decompress every file entry and return (name, error) for each that fails.
    *
    * Reading an entry TO COMPLETION is what makes java.util.zip validate its CRC32 — a stream that is
    * opened and never drained proves nothing, so this must not be "optimised" into a metadata check.
    * transferTo a null sink keeps a 40 MB entry off the heap.
    */
  def failures(file: Path): Vector[(String, String)] =
    open(file) { zf =>
      zf.entries.asScala.toVector.filterNot(_.isDirectory).flatMap { e =>
        Try(Using.resource(zf.getInputStream(e))(_.transferTo(java.io.OutputStream.nullOutputStream())))
          .toEither.left.toOption
          .map(t => (e.getName, s"${t.getClass.getSimpleName}: ${t.getMessage}"))
      }
    }

  // ---- extraction, and the containment guard that is its entire security content ----------------
  //
  // THE THREAT. A zip entry name is attacker-controlled text, and a naive extractor resolves it against
  // the destination and writes. `../../../../home/user/.bashrc` then lands nowhere near the destination.
  // That is zip-slip, and it is the reason this verb was NOT shipped alongside list/check.
  //
  // WHAT IS AND IS NOT DEFENDED, stated so nobody has to infer it:
  //
  //  1. ESCAPE via `..` or an absolute path — DEFENDED, by `resolveEntry` below, which is pure and
  //     therefore directly testable with hostile names and no filesystem.
  //  2. ABSOLUTE names, including a Windows drive letter and a UNC share — DEFENDED, via
  //     `Lib.isAbsolutePath`, deliberately reusing the ONE host-independent predicate rather than
  //     writing a third copy of it (see its own comment: two copies is exactly how `tt bloop` stayed
  //     broken after `tt sbt` was fixed).
  //  3. A SYMLINK ENTRY inside the archive — defanged BY CONSTRUCTION rather than by a check, and this
  //     distinction matters. `java.util.zip` exposes no unix mode, so this code CANNOT tell that an
  //     entry was a symlink. It does not need to: it only ever writes bytes, so a symlink entry becomes
  //     an ordinary file whose CONTENT is the link target. The classic two-step attack (extract
  //     `link -> /etc`, then write `link/passwd` through it) therefore has no first step. If this tool
  //     ever learns to restore symlinks, this paragraph stops being true and rule 3 needs a real check.
  //  4. A PRE-EXISTING symlink in the DESTINATION (`dest/sub` -> `/etc`, entry `sub/passwd`) — DEFENDED,
  //     but only by `realParentInside` at write time, because no amount of string arithmetic can see it:
  //     the entry name is entirely innocent and the escape lives in the filesystem, not the archive.
  //  5. A ZIP BOMB — bounded by a total-uncompressed-size cap, since a 14 MB archive can claim to hold
  //     terabytes and filling a disk is a real outcome even without an escape.
  //
  // PREVIEW BY DEFAULT, applying only with `--write`, the same contract as `tt sub` and
  // `tt forge release-delete`. This is the most destructive verb in the toolbox: it writes executables.

  /** Decide where an entry may be written, or say why it may not.
    *
    * PURE — string and `Path` arithmetic only, no filesystem access — so its tests give the SAME answer
    * on every host, which is the property that makes a guard worth trusting.
    */
  def resolveEntry(destRoot: Path, entryName: String): Either[String, Path] =
    if entryName.isEmpty then Left("empty entry name")
    else if entryName.exists(_.isControl) then Left(s"control character in entry name: ${entryName.map(c => if c.isControl then '?' else c)}")
    else if Lib.isAbsolutePath(entryName) then Left(s"absolute path: $entryName")
    else
      // Normalise the entry's OWN separators FIRST. The zip spec says `/`, so a backslash in an entry
      // name is already non-conformant — and on POSIX `Path.of` would treat `..\..\etc` as one innocent
      // filename that `normalize` cannot collapse, while Windows would read it as an escape. Folding
      // them makes the verdict identical on both hosts, and errs toward rejecting.
      val root    = destRoot.toAbsolutePath.normalize
      val cleaned = entryName.replace('\\', '/')
      val target  = Try(root.resolve(cleaned).normalize).toOption
      target match
        case None                                 => Left(s"unusable entry name: $entryName")
        case Some(t) if t == root                 => Left(s"entry resolves to the destination itself: $entryName")
        case Some(t) if !t.startsWith(root)       => Left(s"escapes destination: $entryName")
        case Some(t)                              => Right(t)

  /** The check `resolveEntry` cannot make, because the escape is in the filesystem and not in the name:
    * if any component of the target's parent is a symlink pointing outside the destination, an innocent
    * entry still lands outside. Compares REAL paths, so it sees through links.
    */
  private def realParentInside(root: Path, target: Path): Boolean =
    val parent = target.getParent
    if parent == null then false
    else
      Try((parent.toRealPath(), root.toRealPath())).map((p, r) => p.startsWith(r)).getOrElse(false)

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

  /** Default cap on TOTAL uncompressed bytes. A 14 MB archive can legitimately claim to hold terabytes,
    * and filling a disk is a real outcome even with containment perfectly enforced. Raise with
    * `--max-bytes`; the number is a policy, not a law of zip.
    */
  private val DefaultMaxBytes: Long = 1024L * 1024 * 1024 // 1 GiB

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
    val es   = Try(entriesOf(src)).fold(t => die(t.getMessage), identity)
    val files = es.filterNot(_.isDir)
    if files.isEmpty then die(s"archive has no file entries to extract: $src")

    val total = files.map(_.size).sum
    if total > o.maxBytes then die(
      s"refusing: total uncompressed size $total B exceeds --max-bytes ${o.maxBytes} B.\n" +
        "  This is the zip-bomb guard. Raise it deliberately if the archive is genuinely this big.")

    // Adjudicate EVERY entry before writing ANY of them. A partially-extracted archive that then hits a
    // hostile entry is the worst outcome: the caller has files on disk and an error, and no clear state.
    val verdicts = files.map(e => (e, resolveEntry(root, e.name)))
    val rejected = verdicts.collect { case (e, Left(why)) => (e.name, why) }
    if rejected.nonEmpty then
      rejected.foreach((n, why) => println(s"REJECTED  $n  ($why)"))
      die(s"${rejected.size} of ${files.size} entries failed the containment guard; nothing was written")

    val planned = verdicts.collect { case (e, Right(t)) => (e, t) }
    val clashes = planned.filter((_, t) => Files.exists(t))
    if clashes.nonEmpty && !o.overwrite then
      clashes.foreach((_, t) => println(s"EXISTS    $t"))
      die(s"${clashes.size} target(s) already exist; pass --overwrite to replace them (nothing was written)")

    if !o.write then
      planned.foreach: (e, t) =>
        val marked = o.exec.exists(g => Lib.globMatches(g, e.name))
        println(f"  ${e.size}%12d  -> $t${if marked then "  [+x]" else ""}")
      if o.exec.isEmpty then
        println("  NOTE: no --exec glob given, so nothing will be executable — java.util.zip cannot")
        println("        restore permission bits, so a program extracted without --exec exits 126.")
      println(s"would extract ${planned.size} file(s), $total B, into $root")
      println(s"  guard: all entries contained${if clashes.isEmpty then "" else s", ${clashes.size} would be OVERWRITTEN"}")
      println("  re-run with --write to apply")
    else
      Using.resource(ZipFile(src.toFile)) { zf =>
        planned.foreach { (e, target) =>
          val parent = target.getParent
          Files.createDirectories(parent)
          // The check resolveEntry cannot make: a pre-existing symlink INSIDE the destination redirects
          // an innocent entry name outside it. Verified AFTER creating parents, because that is when the
          // real path exists to be resolved.
          if !realParentInside(root, target) then die(
            s"refusing: the real parent of '${e.name}' resolves outside $root\n" +
              "  (a symlink inside the destination redirects it). Nothing further was written.")
          val entry = Option(zf.getEntry(e.name)).getOrElse(die(s"entry vanished between plan and write: ${e.name}"))
          Using.resource(zf.getInputStream(entry))(in => Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING))
          val marked = o.exec.exists(g => Lib.globMatches(g, e.name))
          if marked then
            // Owner-execute only; a wider mode is the human's chmod. Report failure rather than swallow
            // it: a silently non-executable binary is exactly the 126 this flag exists to prevent.
            if !target.toFile.setExecutable(true, true) then
              println(s"WARNING   could not set the executable bit on $target")
          println(s"extracted ${e.name} -> $target${if marked then "  [+x]" else ""}")
        }
      }
      println(s"zip extract: ${planned.size} file(s), $total B, into $root")

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
