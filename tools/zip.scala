//> using file project.scala
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
      |There is no extract verb on purpose: extraction writes, and zip entry names can contain `../`
      |(zip-slip), which needs a deliberate containment guard rather than a tacked-on one.""".stripMargin

  private def usage(): Nothing = die(
    "usage:\n" +
      "  zip list  <file.zip>    entries: uncompressed, compressed, method, crc32, name (READ-ONLY)\n" +
      "  zip check <file.zip>    read every entry so the JDK validates its CRC32"
  )

  private def pathOf(file: String): Path = Path.of(file).toAbsolutePath.normalize

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
      case _                   => usage()
}

@main def inspectZipArchive(args: String*): Unit = Zip.dispatch(args*)
