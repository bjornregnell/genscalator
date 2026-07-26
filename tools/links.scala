//> using file project.scala
//> using jvm 21

// links — typed link + reference analysis across a repo (PURE reads, read-only output).
//
// WHY it exists: moving or renaming anything in a documentation-heavy repo needs two questions answered
// mechanically, not by eyeballing greps — "what is now broken?" and "what still points at this?". Both
// recur on every refactor, and both are far past hand-checking at this repo's size.
//
// THE DESIGN POINT, learned from a real sweep: references here come in THREE shapes, and only the first
// is a link.
//   1. markdown  [text](target)  and  ![alt](target)
//   2. html      href="target"   src="target"
//   3. a bare or backticked repo-relative PATH in prose — which is how most of the shipped skills cite
//      research files. A checker that parsed only shapes 1-2 would report "no references" for a file
//      that nine skills depend on.
// So `check` (the pass/fail gate) uses shapes 1-2 only, where a dangling target is unambiguous, while
// `to` and `reach` (the "may I move this?" questions) also count shape 3, because there a MISS is the
// expensive error and a false positive merely keeps a file.
//
// ⚠ KNOWN LIMIT, and it matters for exactly the migration this was built for: prose cites numbered notes
// by PREFIX (`research/052`), not by filename. Shape 3 therefore resolves a dir/prefix token to every
// file in that dir whose name starts with the prefix. That is deliberately generous: it can keep a file
// nobody meant, and it cannot silently drop one that is cited.
//
//   tt links check <absdir> [--ext .md,.html]
//   tt links to    <absdir> <repo-relative-path> [--ext ...]
//   tt links reach <absdir> --root <rel> [--root <rel>]... [--leaf <rel>]... [--ext ...] [--unreachable]
//
// ⚠ SECOND KNOWN LIMIT, the mirror of the one above and found the same way: reachability keeps whatever
// is MENTIONED, and an append-only archive mentions things historically rather than depending on them.
// So a frozen minion log naming a draft pinned that draft, and the draft pinned another — three hops
// from anything alive. `--leaf` separates the two relations a plain walk conflates: a leaf is KEPT when
// referenced, but does not itself keep others.
import java.nio.file.{Files, Path}

private val LinksHelp: String =
  """tt links — link + reference analysis across a repo (pure, read-only)
    |
    |Answers the two questions every move or rename raises: what is broken now, and what still points
    |at this? Reach for it instead of a pile of greps, and re-run it after the move.
    |
    |Usage:
    |  links check <absdir> [--ext <list>]            dangling markdown/html links; exit 1 if any
    |  links to <absdir> <path> [--ext <list>]        which files reference <path> (repo-relative)
    |  links reach <absdir> --root <rel> ...          files reachable from the roots, transitively
    |  links reach <absdir> --root <rel> --unreachable   the COMPLEMENT: what nothing public points at
    |  links reach <absdir> --root <rel> --leaf <rel>    ... treating <rel> as reachable-but-not-propagating
    |
    |Flags:
    |  --ext <list>    comma-separated extensions to scan (default: .md,.html)
    |  --root <rel>    a repo-relative file or directory to start reachability from; repeatable
    |  --leaf <rel>    a repo-relative file or directory that is KEPT when referenced but whose OWN
    |                  citations are not followed; repeatable. For append-only archives (raw research
    |                  logs, minion logs) that mention files historically rather than depend on them —
    |                  without this, anything such an archive ever named is pinned forever.
    |  --unreachable   with `reach`, print what is NOT reachable (the move candidates)
    |
    |What counts as a reference:
    |  check   markdown [text](target) and html href=/src= only — a dangling one of those is a real
    |          defect, so this is the gate you can put an exit code on.
    |  to/reach  additionally counts bare or backticked repo-relative paths in prose, because that is
    |          how most prose cites files. Here a MISSED reference is the expensive error, so the match
    |          is deliberately generous: a dir/prefix token like research/052 counts every file in that
    |          dir whose name starts with 052.
    |
    |Not counted: external urls (http, https, mailto), pure #anchors, and mailto-style targets.
    |Hidden dirs (.git, .scala-build, ...) are skipped entirely.
    |
    |Examples:
    |  tt links check /abs/repo                                  # is anything broken right now?
    |  tt links to /abs/repo research/METHODOLOGY.md             # who depends on this file?
    |  tt links reach /abs/repo --root README.md --root skills --unreachable   # safe-to-move candidates
    |
    |Full reference: tools/README.md""".stripMargin

object Links:

  /** A reference found in a file: the raw target text as written. */
  final case class Ref(from: String, line: Int, target: String)

  /** Targets of markdown inline links/images and html href/src attributes. A target must look like a
    * path — contain a dot or a slash — because documentation that SHOWS the syntax (this toolbox
    * documents `[text](target)` and `href="url"`) otherwise yields `target` and `url` as findings, and a
    * gate that cries wolf on its own manual will not be used. PURE. */
  def linkTargets(text: String): Vector[String] =
    val md = """\]\(\s*([^)\s]+)""".r.findAllMatchIn(text).map(_.group(1))
    val html = """(?:href|src)\s*=\s*["']([^"']+)["']""".r.findAllMatchIn(text).map(_.group(1))
    (md ++ html).filter(t => t.contains('.') || t.contains('/')).toVector

  /** Build caches and scratch, never sources: skipped when scanning AND when inventorying. Everything
    * else, including dot-directories like `.claude-plugin`, is real repo content — skipping those made
    * the checker report a live directory as missing. PURE. */
  val skipDirs: Set[String] =
    Set(".git", ".scala-build", ".bsp", ".bloop", ".metals", "node_modules", "target", "tmp")

  /** A link to `x.html` is NOT dangling when `x.md` sits beside it: the html is produced at render time
    * by the site generator, so the repo legitimately contains only the source. Without this rule every
    * page-to-page link on the site reads as broken. PURE. */
  def generatedFrom(target: String): Option[String] =
    if target.endsWith(".html") then Some(target.dropRight(5) + ".md") else None

  /** True for targets that do not name a file in THIS repo. A leading `/` counts: on the deployed site
    * `/genscalator/graphical-profile/design.css` is a site-absolute URL whose repo home is somewhere
    * else entirely, so validating it here would report a false break on every page. PURE. */
  def isExternal(target: String): Boolean =
    val t = target.trim
    t.isEmpty || t.startsWith("#") || t.startsWith("/") || t.startsWith("mailto:") || t.contains("://")

  /** Drop a #fragment / ?query and any trailing punctuation a sentence left behind — INCLUDING a trailing
    * slash, because prose writes a directory as `research/experiments/indent-vs-braces/` while the
    * inventory holds it without one. Missing that made a cited experiment directory read as unreferenced,
    * which is the expensive direction of error for a migration. PURE. */
  def normalizeTarget(target: String): String =
    val cut = target.takeWhile(c => c != '#' && c != '?')
    cut.reverse.dropWhile(c => c == '.' || c == ',' || c == ')' || c == ';' || c == ':' || c == '/').reverse

  /** Tokens in prose that LOOK like a path: either they contain a slash, or they carry an extension and
    * may name a SIBLING of the citing file. Split on every character a path cannot contain, so backticks,
    * quotes, parens and commas all delimit rather than corrupt.
    *
    * Accepting the slash-less form is deliberate and was a real miss: a research note citing its neighbour
    * as `031-references-summary-enum-design.md` would otherwise count as unreferenced and be listed as
    * safe to move. It costs nothing in precision because the caller only keeps tokens that RESOLVE to an
    * existing file — so `sys.exit`, `e.g.` and `3.9.0-RC4` fall out by themselves. PURE. */
  def pathTokens(text: String): Vector[String] =
    text.split("[\\s`\"'()\\[\\]{}<>,;:!?=|*]+").iterator
      .map(normalizeTarget)
      .filter(t => (t.contains('/') || t.contains('.')) && !isExternal(t) && t.length > 2)
      .map(t => if t.startsWith("./") then t.drop(2) else t)
      .toVector

  /** Resolve a target against the directory of the file it appears in; None if it escapes the repo.
    * Returns a repo-relative path string. PURE (string arithmetic only). */
  def resolve(repoRel: String, fromFile: String): Option[String] =
    val base = fromFile.split('/').dropRight(1).toVector
    val parts = repoRel.split('/').iterator.filter(p => p.nonEmpty && p != ".").toVector
    parts.foldLeft(Option(base)) {
      case (None, _)          => None                                            // already escaped
      case (Some(acc), "..")  => if acc.isEmpty then None else Some(acc.dropRight(1))
      case (Some(acc), p)     => Some(acc :+ p)
    }.map(_.mkString("/"))

  /** Which known repo paths a token refers to. Exact match first; then dir/prefix (research/052 ->
    * every file in research/ whose name starts with 052); then the token as a directory. PURE. */
  def referents(token: String, known: Set[String], dirs: Set[String]): Set[String] =
    if known(token) then Set(token)
    else if dirs(token) then
      // A cited DIRECTORY means one of two things, and DEPTH tells them apart (BR's ruling, 2026-07-26,
      // refined the same day against real counts).
      //   ARTIFACT dir, 3+ components — `research/experiments/indent-vs-braces/` is cited by blog 002 and
      //     the research-methods skill, and its probes and tasks ARE the thing cited. Citing it keeps them.
      //   GROUPING dir, 1-2 components — `research/`, `research/wr-data/`, `research/theory/`. These name
      //     a LOCATION. `HUMANS.md` links bare `research/` as a repo-map entry, and 27 files cite
      //     `research/wr-data/` as "the logs"; expanding either would keep hundreds of files on one
      //     generic mention and make a migration a no-op (measured: 271 movable files fell to ~22).
      // So expand at depth 3 and deeper only.
      if token.count(_ == '/') >= 2 then known.filter(_.startsWith(token + "/")) + token else Set(token)
    else
      val i = token.lastIndexOf('/')
      if i <= 0 then Set.empty
      else
        val (dir, prefix) = (token.substring(0, i), token.substring(i + 1))
        if prefix.isEmpty || !dirs(dir) then Set.empty
        else known.filter(k => k.startsWith(dir + "/") && k.substring(dir.length + 1).startsWith(prefix))

  /** True when `p` is at, or under, one of the leaf prefixes.
    *
    * A LEAF is REACHABLE-BUT-NON-PROPAGATING: something points at it, so it stays; but what IT cites is
    * not kept on its account. The distinction exists because "is referenced" and "keeps others alive"
    * are two different relations that a plain reachability walk conflates.
    *
    * Found 2026-07-26, tracing why an unpublished blog post survived a keep-set computation: a
    * meta-minion log — a FROZEN RECORD of a past audit — mentioned another draft in passing, and that
    * mention alone pinned both. Raw records cite things historically, not because anything depends on
    * them, so an append-only archive otherwise acts as a permanent anchor for whatever it ever named.
    * Marking such an archive a leaf keeps the archive and releases its mentions. PURE. */
  def isLeaf(p: String, leaves: Vector[String]): Boolean =
    leaves.exists(l => p == l || p.startsWith(l + "/"))

  // --- the effectful half: reads the tree. Kept INSIDE this object because a top-level `private def`
  // still lands in package scope, and `scanDir` / `inventory` are exactly the generic names that collide
  // when the whole toolbox compiles as one unit (scala-style §1).

  /** Read every file with a scanned extension: (repo-relative path, content). EFFECTFUL: reads files. */
  def scanDir(root: Path, exts: Vector[String]): Vector[(String, String)] =
    val out = scala.collection.mutable.ArrayBuffer.empty[(String, String)]
    Files.walkFileTree(root, java.util.Collections.emptySet[java.nio.file.FileVisitOption](), Int.MaxValue,
      new java.nio.file.SimpleFileVisitor[Path] {
        override def preVisitDirectory(dir: Path, a: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult =
          val n = Option(dir.getFileName).map(_.toString).getOrElse("")
          if skipDirs(n) && !dir.equals(root) then java.nio.file.FileVisitResult.SKIP_SUBTREE
          else java.nio.file.FileVisitResult.CONTINUE
        override def visitFile(f: Path, a: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult =
          val rel = root.relativize(f).toString
          if exts.exists(e => rel.endsWith(e)) then
            try out += ((rel, String(Files.readAllBytes(f), "UTF-8"))) catch case _: Throwable => ()
          java.nio.file.FileVisitResult.CONTINUE
        override def visitFileFailed(f: Path, e: java.io.IOException): java.nio.file.FileVisitResult =
          java.nio.file.FileVisitResult.CONTINUE
      })
    out.toVector

  /** Every file under root (any extension), repo-relative, plus every directory. EFFECTFUL: reads the tree. */
  def inventory(root: Path): (Set[String], Set[String]) =
    val files = scala.collection.mutable.Set.empty[String]
    val dirs = scala.collection.mutable.Set.empty[String]
    Files.walkFileTree(root, java.util.Collections.emptySet[java.nio.file.FileVisitOption](), Int.MaxValue,
      new java.nio.file.SimpleFileVisitor[Path] {
        override def preVisitDirectory(d: Path, a: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult =
          val n = Option(d.getFileName).map(_.toString).getOrElse("")
          if skipDirs(n) && !d.equals(root) then java.nio.file.FileVisitResult.SKIP_SUBTREE
          else { if !d.equals(root) then dirs += root.relativize(d).toString; java.nio.file.FileVisitResult.CONTINUE }
        override def visitFile(f: Path, a: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult =
          files += root.relativize(f).toString; java.nio.file.FileVisitResult.CONTINUE
        override def visitFileFailed(f: Path, e: java.io.IOException): java.nio.file.FileVisitResult =
          java.nio.file.FileVisitResult.CONTINUE
      })
    (files.toSet, dirs.toSet)

@main def resolveLinks(args: String*): Unit =
  if args.isEmpty || args.contains("--help") || args.contains("-h") then { println(LinksHelp); sys.exit(0) }
  val a = args.toList
  def optsOf(flag: String): Vector[String] =
    a.sliding(2).collect { case List(f, v) if f == flag => v }.toVector
  val exts = optsOf("--ext").headOption.map(_.split(",").toVector.map(_.trim)).getOrElse(Vector(".md", ".html"))
  val roots = optsOf("--root")
  val leaves = optsOf("--leaf")
  val unreachableOnly = a.contains("--unreachable")
  val flagged = Set("--ext", "--root", "--leaf")
  val consumed = scala.collection.mutable.Set.empty[Int]
  a.zipWithIndex.foreach { case (t, i) => if flagged(t) then { consumed += i; consumed += i + 1 } }
  val pos = a.zipWithIndex.collect { case (t, i) if !consumed(i) && !t.startsWith("--") => t }

  val verb = pos.headOption.getOrElse("")
  val dirStr = pos.lift(1).getOrElse("")
  if dirStr.isEmpty then { Console.err.println("links: need an absolute repo dir"); sys.exit(2) }
  val root = Path.of(dirStr)
  if !Files.isDirectory(root) then { Console.err.println(s"links: not a directory: $dirStr"); sys.exit(2) }

  val docs = Links.scanDir(root, exts)
  val (files, dirs) = Links.inventory(root)

  /** Strict links only, resolved: (from, line, target, resolved). */
  def strictRefs: Vector[(String, String, Option[String])] =
    for
      (rel, text) <- docs
      t <- Links.linkTargets(text) if !Links.isExternal(t)
      n = Links.normalizeTarget(t) if n.nonEmpty
    yield (rel, t, Links.resolve(n, rel))

  /** Every reference, strict + prose paths, as edges from file -> referenced repo paths. */
  def edges: Map[String, Set[String]] =
    docs.map { (rel, text) =>
      val strict = Links.linkTargets(text).filterNot(Links.isExternal).map(Links.normalizeTarget)
        .flatMap(n => Links.resolve(n, rel)).toSet
      // A prose token may be repo-relative (research/052) or a bare sibling name (031-foo.md); try the
      // repo-relative reading first, then resolve against the citing file's own directory.
      val prose = Links.pathTokens(text).flatMap { t =>
        val direct = Links.referents(t, files, dirs)
        if direct.nonEmpty then direct else Links.resolve(t, rel).filter(files).toSet
      }.toSet
      val strictHits = strict.flatMap(s => Links.referents(s, files, dirs))
      rel -> (strictHits ++ prose)
    }.toMap

  verb match
    case "check" =>
      def resolves(r: String): Boolean =
        files(r) || dirs(r) || Links.generatedFrom(r).exists(files)
      val bad = strictRefs.collect { case (from, raw, res) if !res.exists(resolves) => (from, raw) }
      bad.foreach((from, raw) => println(s"$from -> $raw"))
      println(s"links check: ${bad.size} dangling of ${strictRefs.size} local link(s) in ${docs.size} file(s)")
      if bad.nonEmpty then sys.exit(1)

    case "to" =>
      val target = pos.lift(2).getOrElse("")
      if target.isEmpty then { Console.err.println("links to: need a repo-relative path"); sys.exit(2) }
      val e = edges
      val hits = e.collect { case (from, tos) if tos(target) => from }.toVector.sorted
      hits.foreach(h => println(s"  $h"))
      println(s"links to: ${hits.size} file(s) reference $target")

    case "reach" =>
      if roots.isEmpty then { Console.err.println("links reach: need at least one --root"); sys.exit(2) }
      val e = edges
      // A root may be a file or a directory; a directory root seeds every file under it.
      val seed = roots.flatMap(r => if dirs(r) then files.filter(_.startsWith(r + "/")) else Set(r)).toSet
      val seen = scala.collection.mutable.Set.from(seed)
      val queue = scala.collection.mutable.Queue.from(seed)
      while queue.nonEmpty do
        val cur = queue.dequeue()
        // A leaf is kept (it is already in `seen`) but its own citations are not followed.
        if !Links.isLeaf(cur, leaves) then
          for next <- e.getOrElse(cur, Set.empty) if !seen(next) do { seen += next; queue.enqueue(next) }
      val reached = seen.toSet.filter(files)
      if unreachableOnly then
        val out = (files -- reached).toVector.sorted
        out.foreach(p => println(s"  $p"))
        println(s"links reach: ${out.size} file(s) NOT reachable from ${roots.size} root(s), of ${files.size}")
      else
        val out = reached.toVector.sorted
        out.foreach(p => println(s"  $p"))
        println(s"links reach: ${out.size} file(s) reachable from ${roots.size} root(s), of ${files.size}")

    case other =>
      Console.err.println(s"links: unknown verb '$other'")
      println(LinksHelp)
      sys.exit(2)
