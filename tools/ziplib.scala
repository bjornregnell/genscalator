//> using jvm 21
// (no version include: mainless helper — inherits it from its includer; see project.scala)
//
// NO `//> using dep`: this file is JDK-only (java.util.zip, java.nio), like lib.scala. That is not an
// accident of the current implementation — D3 in reqts/DESIGN.md picked zip as the release format BECAUSE
// the JDK reads it unaided, so a dependency here would undercut the format's own rationale.

// ziplib — archive inspection, the zip-slip containment guard, and extraction. Shared, because TWO tools
// need it: `tt zip` (a human inspecting or unpacking an archive) and `tt update --native` (the toolbox
// unpacking the release it just downloaded over itself).
//
// ⚠ WHY IT IS NOT IN zip.scala, where it was born. `tt update --native` cannot call `tt zip`: the toolbox's
// dependency graph is deliberately FLAT — tools depend on shared libs, never on each other (BR, 2026-07-27).
// The alternative was for `update` to carry its own extractor, which would put a SECOND copy of the
// containment guard in the tree. That is the SM247 sibling-miss trap on the most dangerous code here: a
// zip-slip fix applied to one copy looks complete from inside that file. One definition, two callers.
//
// ⚠ IT IS NOT IN lib.scala either, even though it is JDK-only and would compile there. lib.scala is
// included by every pure text tool, and extraction is the most destructive capability in the toolbox;
// putting it in the file that `tt text` includes would widen a lot of blast radii to save a file.
//
// STRUCTURE, and it is load-bearing rather than decorative: the compute layer returns DATA and THROWS on
// bad input; only the DRIVERS in the calling tool print and exit. A `sys.exit` reached from here would kill
// the test JVM, so a compute layer that dies internally cannot be unit-tested at all.
package agenttools

import java.util.zip.{ZipEntry, ZipFile}
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}

object ZipLib:

  final case class Entry(name: String, size: Long, compressed: Long, method: String, crc: Long, isDir: Boolean)

  def methodName(m: Int): String = m match
    case ZipEntry.STORED   => "stored"
    case ZipEntry.DEFLATED => "deflated"
    case other             => s"method$other"

  /** Open a zip, or throw with a message that says which of the two failure modes happened —
    * "no such file" and "not a zip" are different problems and must not read the same. */
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

  // ---- the containment guard, which is the entire security content of extraction --------------------
  //
  // THE THREAT. A zip entry name is attacker-controlled text, and a naive extractor resolves it against the
  // destination and writes. A name full of parent-directory hops then lands nowhere near the destination.
  // That is zip-slip, and it is why extraction was NOT shipped alongside list/check.
  //
  // WHAT IS AND IS NOT DEFENDED, stated so nobody has to infer it:
  //  1. ESCAPE via parent-directory hops or an absolute path — DEFENDED, by `resolveEntry`, which is pure
  //     and therefore directly testable with hostile names and no filesystem.
  //  2. ABSOLUTE names, including a Windows drive letter and a UNC share — DEFENDED, via
  //     `Lib.isAbsolutePath`, deliberately reusing the ONE host-independent predicate rather than writing
  //     a third copy (see its own comment: two copies is exactly how `tt bloop` stayed broken after
  //     `tt sbt` was fixed).
  //  3. A SYMLINK ENTRY inside the archive — defanged BY CONSTRUCTION rather than by a check, and the
  //     distinction matters. `java.util.zip` exposes no unix mode, so this code CANNOT tell that an entry
  //     was a symlink. It does not need to: it only ever writes bytes, so a symlink entry becomes an
  //     ordinary file whose CONTENT is the link target. The classic two-step attack (extract a link to
  //     /etc, then write through it) therefore has no first step. If this code ever learns to restore
  //     symlinks, this paragraph stops being true and rule 3 needs a real check.
  //  4. A PRE-EXISTING symlink in the DESTINATION — DEFENDED, but only by `realParentInside` at write
  //     time, because no amount of string arithmetic can see it: the entry name is entirely innocent and
  //     the escape lives in the filesystem, not the archive.
  //  5. A ZIP BOMB — bounded by a total-uncompressed-size cap, since a small archive can claim to hold
  //     terabytes and filling a disk is a real outcome even without an escape.

  /** Decide where an entry may be written, or say why it may not.
    *
    * PURE — string and `Path` arithmetic only, no filesystem access — so its tests give the SAME answer on
    * every host, which is the property that makes a guard worth trusting. */
  def resolveEntry(destRoot: Path, entryName: String): Either[String, Path] =
    if entryName.isEmpty then Left("empty entry name")
    else if entryName.exists(_.isControl) then Left(s"control character in entry name: ${entryName.map(c => if c.isControl then '?' else c)}")
    else if Lib.isAbsolutePath(entryName) then Left(s"absolute path: $entryName")
    else
      // Normalise the entry's OWN separators FIRST. The zip spec says forward slash, so a backslash in an
      // entry name is already non-conformant — and on POSIX `Path.of` would treat a backslash-laden name as
      // one innocent filename that `normalize` cannot collapse, while Windows would read it as an escape.
      // Folding them makes the verdict identical on both hosts, and errs toward rejecting.
      val root    = destRoot.toAbsolutePath.normalize
      val cleaned = entryName.replace('\\', '/')
      val target  = Try(root.resolve(cleaned).normalize).toOption
      target match
        case None                           => Left(s"unusable entry name: $entryName")
        case Some(t) if t == root           => Left(s"entry resolves to the destination itself: $entryName")
        case Some(t) if !t.startsWith(root) => Left(s"escapes destination: $entryName")
        case Some(t)                        => Right(t)

  /** The check `resolveEntry` cannot make, because the escape is in the filesystem and not in the name: if
    * any component of the target's parent is a symlink pointing outside the destination, an innocent entry
    * still lands outside. Compares REAL paths, so it sees through links. */
  def realParentInside(root: Path, target: Path): Boolean =
    val parent = target.getParent
    if parent == null then false
    else Try((parent.toRealPath(), root.toRealPath())).map((p, r) => p.startsWith(r)).getOrElse(false)

  /** Default cap on TOTAL uncompressed bytes. A small archive can legitimately claim to hold terabytes, and
    * filling a disk is a real outcome even with containment perfectly enforced. The number is a policy, not
    * a law of zip. */
  val DefaultMaxBytes: Long = 1024L * 1024 * 1024 // 1 GiB

  /** Every entry adjudicated, BEFORE anything is written. */
  final case class Plan(planned: Vector[(Entry, Path)], clashes: Vector[Path], total: Long)

  /** Adjudicate EVERY entry before writing ANY of them, and THROW if the archive fails the guard.
    *
    * ⚠ The all-or-nothing property is the point, not the tidiness: a partially-extracted archive that then
    * hits a hostile entry is the worst outcome, because the caller has files on disk AND an error and no
    * clear state to reason about. */
  def planExtraction(src: Path, root: Path, maxBytes: Long = DefaultMaxBytes): Plan =
    val files = entriesOf(src).filterNot(_.isDir)
    if files.isEmpty then throw IllegalArgumentException(s"archive has no file entries to extract: $src")
    val total = files.map(_.size).sum
    if total > maxBytes then throw IllegalArgumentException(
      s"refusing: total uncompressed size $total B exceeds the cap of $maxBytes B.\n" +
        "  This is the zip-bomb guard. Raise it deliberately if the archive is genuinely this big.")
    val verdicts = files.map(e => (e, resolveEntry(root, e.name)))
    val rejected = verdicts.collect { case (e, Left(why)) => s"REJECTED  ${e.name}  ($why)" }
    if rejected.nonEmpty then throw IllegalArgumentException(
      rejected.mkString("\n") +
        s"\n${rejected.size} of ${files.size} entries failed the containment guard; nothing was written")
    val planned = verdicts.collect { case (e, Right(t)) => (e, t) }
    Plan(planned, planned.map(_._2).filter(Files.exists(_)), total)

  /** Write a plan. Returns each entry with its target and whether it was marked executable.
    *
    * ⚠ `execGlob` exists because `java.util.zip` does not restore unix permission bits. The zip FORMAT
    * carries them (in the central directory's external attributes) but `ZipEntry` exposes no API to read
    * them, so every file written here lands non-executable. Found end to end on 2026-07-27: extracting the
    * real release archive produced a launcher that answered `Permission denied` (exit 126) — the exact wall
    * a tester following the documented install path hits on their first command. The CALLER declares which
    * entries are programs, because the archive cannot say and guessing from a bin-prefix would be a silent
    * policy. Sets owner-execute only; anything wider is the human's `chmod`. */
  def extract(src: Path, root: Path, plan: Plan, execGlob: Option[String]): Vector[(Entry, Path, Boolean)] =
    Using.resource(ZipFile(src.toFile)) { zf =>
      plan.planned.map { (e, target) =>
        Files.createDirectories(target.getParent)
        // The check resolveEntry cannot make: a pre-existing symlink INSIDE the destination redirects an
        // innocent entry name outside it. Verified AFTER creating parents, because that is when the real
        // path exists to be resolved.
        if !realParentInside(root, target) then throw IllegalArgumentException(
          s"refusing: the real parent of '${e.name}' resolves outside $root\n" +
            "  (a symlink inside the destination redirects it). Nothing further was written.")
        val entry = Option(zf.getEntry(e.name))
          .getOrElse(throw IllegalArgumentException(s"entry vanished between plan and write: ${e.name}"))
        Using.resource(zf.getInputStream(entry))(
          in => Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING))
        val marked = execGlob.exists(g => Lib.globMatches(g, e.name))
        if marked && !target.toFile.setExecutable(true, true) then
          // Report rather than swallow: a silently non-executable binary is exactly the 126 this prevents.
          System.err.println(s"WARNING   could not set the executable bit on $target")
        (e, target, marked)
      }
    }
