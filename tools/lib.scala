// (no version include: mainless helper — inherits it from its includer; see project.scala)

// Shared helpers for the Scala agent toolbox. PURE (no I/O effects except the explicitly-named file
// readers). Project-agnostic — destined for an open Codeberg repo. Uses only the JDK (no deps) so pure
// text tools compile fast. Effectful drivers (running sbt/pdflatex) live in separate files that add os-lib.
package agenttools

object Lib:
  // --- file readers (the only I/O here; named so impurity is explicit) ---
  /** Read a file lenient as Latin-1: every byte maps to a char, so it never throws on non-UTF-8 logs
    * (LaTeX logs aren't valid UTF-8). ASCII pattern matching ("! ", etc.) is unaffected. */
  def readLatin1(path: String): String =
    String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)), "ISO-8859-1")

  /** Read a file as UTF-8 (for prose/source where åäö etc. matter). */
  def readUtf8(path: String): String =
    String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)), "UTF-8")

  // --- paths ---
  /** Absolute in the POSIX sense ("/..."), the Windows drive sense ("C:\..." or "C:/"), or UNC
    * ("\\server\share"). PURE: it inspects the STRING, touching no filesystem.
    *
    * Deliberately NOT `java.nio.Paths.get(_).isAbsolute`, whose answer is host-dependent in BOTH
    * directions — on Windows "/x" is not absolute, on Linux "C:\x" is not — while the callers are pure
    * functions whose tests must give the same answer on every host.
    *
    * ⚠ It lives HERE, in one place, because it did not. `tt sbt` and `tt bloop` each had their own
    * `startsWith("/")`, so both refused every path a Windows user could type. `tt sbt` was fixed first
    * and `tt bloop` kept failing, because a fix applied to one instance of a duplicated predicate looks
    * complete from inside that file. Same reason `toolsDir` below is shared.
    */
  def isAbsolutePath(p: String): Boolean =
    p.startsWith("/") || p.startsWith("""\\""") || p.matches("""^[A-Za-z]:[/\\].*""")

  /** Map an os/arch pair to the platform token in a release asset name (`genscalator-<token>.zip`), or
    * None when this project publishes no binary for that combination.
    *
    * Takes its inputs as PARAMETERS rather than reading `os.name`/`os.arch`, so the whole distribution
    * matrix is unit-testable from any host — the same reason `isAbsolutePath` is a string predicate.
    *
    * ⚠ None is a real answer and must not be turned into a guess. It encodes BR's distribution decision
    * of 2026-07-27: assets for the four PROVEN platforms, source build documented for the rest. Intel
    * macOS and Windows-on-ARM deliberately return None — the first has never produced an artifact, and
    * the second is published EXPERIMENTAL and currently fails because VirtusLab ships no
    * `aarch64-pc-win32` scala-cli build. Guessing a nearby token there would download a binary that
    * cannot run, which is a worse outcome than being told to build from source.
    */
  def releasePlatform(osName: String, osArch: String): Option[String] =
    val os = osName.toLowerCase
    val arch = osArch.toLowerCase match
      case "amd64" | "x86_64" | "x64"      => Some("x86_64")
      case "aarch64" | "arm64"             => Some("aarch64")
      case _                               => None
    val family =
      if os.contains("linux") then Some("linux")
      else if os.contains("mac") || os.contains("darwin") then Some("macos")
      else if os.contains("windows") then Some("windows")
      else None
    (family, arch) match
      case (Some("linux"), Some(a))           => Some(s"linux-$a")       // both linux arches are published
      case (Some("macos"), Some("aarch64"))   => Some("macos-aarch64")   // Apple Silicon
      case (Some("windows"), Some("x86_64"))  => Some("windows-x86_64")
      case _                                  => None                    // incl. Intel mac + win-aarch64

  /** Match a name against a `*`-only glob. `*` matches any run of characters; every other character is
    * literal, so a name containing regex metacharacters cannot smuggle a pattern in.
    *
    * ⚠ It lives HERE for the same reason `isAbsolutePath` does. It was written private inside `tt forge`
    * for `release-download --pattern`, and `tt zip extract --exec` needed exactly the same predicate
    * hours later — the moment at which a second copy usually appears and then drifts. Deliberately
    * `*`-only and documented as such: anything richer invites a caller to assume full shell globbing and
    * get a silently-empty result instead of an error.
    */
  def globMatches(glob: String, name: String): Boolean =
    name.matches(glob.split("\\*", -1).map(java.util.regex.Pattern.quote).mkString(".*"))

  // --- toolbox location ---
  /** Locate the tools dir (cwd-independent): the -Dtt.tools property the `tt` launcher passes, else walk up
    * from the cwd for a `tools/tt`. The ONE shared definition — doc / prd / skillcheck / skillgrants all use
    * this (previously each had an identical top-level copy, which collided at a whole-`tools` compile). */
  def toolsDir(): Option[java.nio.file.Path] =
    import java.nio.file.{Files, Path}
    sys.props.get("tt.tools").map(Path.of(_)).filter(d => Files.exists(d.resolve("tt")))
      .orElse:
        Iterator.iterate(Path.of("").toAbsolutePath)(p => p.getParent).takeWhile(_ != null).take(8)
          .find(d => Files.exists(d.resolve("tools").resolve("tt"))).map(_.resolve("tools"))

  /** Locate the genscalator ROOT — the directory holding `docs/`, `skills/`, `reqts/`. This, not
    * `toolsDir`, is what the runtime verbs actually want: `doc`, `prd`, `skillcheck`, `skillgrants`
    * and `update` each read a SIBLING of `tools/`, never `tools/` itself.
    *
    * Two installations must both resolve:
    *   - a CONTRIBUTOR's git clone, found via `-Dtt.tools` or the cwd walk-up (both land on `tools/`,
    *     whose parent is the root);
    *   - a USER's binary install under `GENSCALATOR_HOME` (default `~/.genscalator`), which has NO
    *     `tools/` at all — it ships the data those verbs read, not the sources.
    *
    * Order matters: an explicit `GENSCALATOR_HOME` wins, then the clone (so a contributor working in
    * a checkout gets THAT checkout, not a stale installed copy), then the default install location.
    * Ordering the default install ahead of the walk-up would silently shadow the repo you are editing. */
  def rootDir(): Option[java.nio.file.Path] =
    import java.nio.file.{Files, Path}
    def dir(p: Path): Option[Path] = Option.when(Files.isDirectory(p))(p)
    sys.env.get("GENSCALATOR_HOME").map(Path.of(_)).flatMap(dir)
      .orElse(toolsDir().map(_.getParent))
      .orElse(dir(Path.of(sys.props.getOrElse("user.home", "."), ".genscalator")))

  /** Directory NAMES the walker prunes by default alongside dot-names (issue-017): build output and
    * vendored trees that crowd out sources on any JVM/Node repo. Unlike the dot-name skip this
    * exclusion is always DISCLOSED via the PruneReport — a curated default may filter, never hide. */
  val curatedSkipDirNames: Set[String] = Set("target", "out", "build", "node_modules")

  /** Pruning policy for walkPruned. all=true disables BOTH dot-name and curated pruning (unchanged
    * meaning of --all: everything). excludeGlobs always apply, even under all — they are explicit
    * caller requests — matched in java.nio glob syntax against the ROOT-RELATIVE path of each entry.
    * A glob with a trailing double-star after a slash also prunes the subtree ROOT itself (an
    * exclude of dir-slash-star-star prunes `dir` too — Scala comments nest, so the literal form
    * lives in the tools' help texts, not here), and the walk never descends into an excluded tree. */
  final case class Prune(
      all: Boolean = false,
      curated: Boolean = true,
      excludeGlobs: Vector[String] = Vector.empty)

  /** What a walk suppressed, so tools can DISCLOSE exclusion on their count line (issue-017's hard
    * requirement: the tool never hides files without saying so). Counts are entries suppressed AT
    * THE WALK LEVEL: a pruned directory counts as ONE entry, because its subtree is never entered —
    * counting its contents would cost exactly the walk the pruning saved. Dot-name skips are not
    * reported; they are the documented default, as before. */
  final case class PruneReport(
      curatedCount: Int, curatedNames: Vector[String],
      excludeCount: Int, excludeGlobsHit: Vector[String]):
    def total: Int = curatedCount + excludeCount
    /** Suffix for a count line: "" when nothing was excluded (the plain old line, no noise), else
      * e.g. " (2 excluded: target, node_modules)". Shared here so find + files cannot drift. */
    def disclosure: String =
      if total == 0 then ""
      else s" ($total excluded: ${(curatedNames ++ excludeGlobsHit).mkString(", ")})"

  /** Walk the tree under root with PRUNING (issue-017): a dot-named directory is skipped as a WHOLE
    * subtree (so .git/.scala-build caches never crowd a scan) and dot-named files are dropped, unless
    * prune.all; directories named in curatedSkipDirNames are pruned the same way (disclosed via the
    * returned PruneReport); prune.excludeGlobs suppress entries by root-relative path, pruning early
    * so the walk never descends into an excluded subtree. The root itself is always entered, hidden
    * or not. Symlinks are not followed; unreadable entries are skipped. A bad glob throws
    * PatternSyntaxException BEFORE the walk starts (nothing partial is emitted). onEntry receives
    * every retained entry with its TRUE type — walkFileTree delivers directories at exactly maxDepth
    * to visitFile, so the type must travel with the path or boundary dirs masquerade as files
    * (issue-014). Shared by find + files, so the siblings' pruning can never drift apart again. */
  def walkPruned(root: java.nio.file.Path, prune: Prune = Prune(), maxDepth: Int = Int.MaxValue)(
      onEntry: (java.nio.file.Path, Boolean) => Unit): PruneReport =
    import java.nio.file.{Files, FileVisitResult, Path, PathMatcher, SimpleFileVisitor}
    import java.nio.file.attribute.BasicFileAttributes
    def hidden(p: Path): Boolean =
      val n = p.getFileName; n != null && n.toString.startsWith(".")
    def nameOf(p: Path): String = Option(p.getFileName).map(_.toString).getOrElse("")
    val fs = root.getFileSystem
    val excludeMatchers: Vector[(String, PathMatcher, Option[PathMatcher])] =
      prune.excludeGlobs.map: g =>
        (g, fs.getPathMatcher(s"glob:$g"),
          Option.when(g.endsWith("/**"))(fs.getPathMatcher(s"glob:${g.dropRight(3)}")))
    def excludedBy(rel: Path, isDir: Boolean): Option[String] =
      excludeMatchers.collectFirst:
        case (g, m, prefix) if m.matches(rel) || (isDir && prefix.exists(_.matches(rel))) => g
    def curatedSkip(name: String): Boolean =
      !prune.all && prune.curated && curatedSkipDirNames(name)
    var curatedCount, excludeCount = 0
    val curatedNames = scala.collection.mutable.SortedSet.empty[String]
    val globsHit = scala.collection.mutable.Set.empty[String]
    Files.walkFileTree(root, java.util.Collections.emptySet[java.nio.file.FileVisitOption](), maxDepth,
      new SimpleFileVisitor[Path] {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
          if dir.equals(root) then { onEntry(dir, true); FileVisitResult.CONTINUE }
          else if !prune.all && hidden(dir) then FileVisitResult.SKIP_SUBTREE
          else if curatedSkip(nameOf(dir)) then
            { curatedCount += 1; curatedNames += nameOf(dir); FileVisitResult.SKIP_SUBTREE }
          else excludedBy(root.relativize(dir), isDir = true) match
            case Some(g) => { excludeCount += 1; globsHit += g; FileVisitResult.SKIP_SUBTREE }
            case None    => { onEntry(dir, true); FileVisitResult.CONTINUE }
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
          // Boundary-depth dirs land here (issue-014) — suppress them by the same policies as above.
          if prune.all || !hidden(file) then
            if attrs.isDirectory && curatedSkip(nameOf(file)) then
              { curatedCount += 1; curatedNames += nameOf(file) }
            else excludedBy(root.relativize(file), attrs.isDirectory) match
              case Some(g) => { excludeCount += 1; globsHit += g }
              case None    => onEntry(file, attrs.isDirectory)
          FileVisitResult.CONTINUE
        override def visitFileFailed(file: Path, exc: java.io.IOException): FileVisitResult =
          FileVisitResult.CONTINUE
      })
    PruneReport(curatedCount, curatedNames.toVector,
      excludeCount, prune.excludeGlobs.distinct.filter(globsHit))

  /** Recovery text for a bare skillcheck/skillgrants failing on a NATIVE install (issue-015): the
    * install tree ships NO `skills/` by design (D4: the plugin owns the skills), so the default
    * resolution lands on a directory that does not exist and the session-start reflex dies at step 1.
    * Names the `--skills` escape hatch and PROBES the plugin cache for candidate dirs — a hint, never
    * a silent fallback: a stale cache yields a WRONG expected set, so the choice stays with the caller
    * (the version is visible in each candidate path). */
  def skillsRecoveryHint(): String =
    import java.nio.file.{Files, Path}
    import scala.jdk.CollectionConverters.*
    val cacheRoot = Path.of(sys.props.getOrElse("user.home", "."), ".claude", "plugins", "cache")
    val candidates =
      if !Files.isDirectory(cacheRoot) then Vector.empty
      else
        Files.list(cacheRoot).iterator.asScala.filter(Files.isDirectory(_))
          .map(_.resolve("genscalator")).filter(Files.isDirectory(_))
          .flatMap(g => Files.list(g).iterator.asScala)
          .map(_.resolve("skills")).filter(Files.isDirectory(_))
          .map(_.toString).toVector.sorted
    val found =
      if candidates.isEmpty then s"  (no plugin-cache skills/ found under $cacheRoot)"
      else candidates.map(p => s"  --skills $p").mkString("\n")
    s"""A native install ships NO skills/ by design (D4: the PLUGIN owns the skills) — point at the
       |plugin cache or a checkout via --skills <dir>. Plugin-cache candidates on this machine:
       |$found
       |Pick the one matching your installed version (see VERSION.txt) — a stale cache yields a
       |WRONG expected set.""".stripMargin

  // --- JSON ---
  /** Encode a string as a JSON string literal, quotes included, per RFC 8259. Pure, dependency-free.
    * Escapes the mandatory set (" \ and the C0 controls via \b \f \n \r \t or \uXXXX); passes other
    * characters (incl. UTF-8 åäö) through unchanged. Use to emit valid JSON from Scala tools without jq
    * (e.g. hook additionalContext) — in-process, so hot-path callers pay no external-process cost. */
  def jsonStr(s: String): String =
    val sb = StringBuilder("\"")
    s.foreach:
      case '"'  => sb ++= "\\\""
      case '\\' => sb ++= "\\\\"
      case '\b' => sb ++= "\\b"
      case '\f' => sb ++= "\\f"
      case '\n' => sb ++= "\\n"
      case '\r' => sb ++= "\\r"
      case '\t' => sb ++= "\\t"
      case c if c < 0x20 => sb ++= f"\\u${c.toInt}%04x"
      case c    => sb += c
    (sb += '"').toString

  // --- pure formatting/aggregation ---
  /** Frequency map → sorted bar histogram (descending), top N. */
  def histogram(counts: Map[String, Int], top: Int = 40): String =
    val sorted = counts.toVector.sortBy(-_._2).take(top)
    val w = sorted.map(_._1.length).maxOption.getOrElse(0)
    sorted.map((k, c) => s"  ${k.padTo(w, ' ')} ${"%6d".format(c)} ${"#" * math.min(50, c)}").mkString("\n")

  /** Levenshtein-ish: true iff a and b differ by exactly one edit (sub/ins/del). */
  def edit1(a: String, b: String): Boolean =
    if math.abs(a.length - b.length) > 1 then false
    else if a.length == b.length then a.zip(b).count(_ != _) == 1
    else
      val (s, l) = if a.length < b.length then (a, b) else (b, a)
      var i = 0; var j = 0; var diff = 0
      while i < s.length && diff <= 1 do
        if s(i) == l(j) then { i += 1; j += 1 } else { diff += 1; j += 1 }
      diff + (l.length - j) <= 1
