//> using file project.scala
//> using jvm 21
//> using file lib.scala

// find — typed, SAFE file enumeration (PURE, read-only): the allowlistable read-half of `find`. Walks the tree
// under <root> and prints the matching paths (stable-sorted), filtered by name-glob / extension / type / depth.
// It exposes ONLY name/ext/type/depth — no -exec, no arbitrary predicates, no -delete — which is exactly what
// makes it safe to blanket-allow where raw `find` (a general file-executor) cannot be. The guarded write-half
// (`--prune`, confined + dry-run-by-default) is a separate, later step (SM031).
//   scala-cli run tools/find.scala -- <root> [--name <glob>] [--ext <e>] [--type f|d] [--max-depth N] [--exclude <glob>]... [--count]
import java.nio.file.{Files, Path, FileSystems}
import agenttools.Lib

// Top-level, so a UNIQUE name (the toolbox compiles as one unit; a generic `Help` would collide across files).
private val FindHelp: String =
  """tt find — typed, safe file enumeration (pure, read-only)
    |
    |Walks the tree under <root> and prints the matching paths (stable-sorted). The allowlistable read-half of
    |`find`: it exposes only name/ext/type/depth — no -exec, no predicates, no -delete — so it can be trusted
    |where raw find cannot. Reach for it instead of `find <root> -name ...`.
    |
    |Usage:
    |  find <root>                          list regular files under <root>
    |  find <root> --name '<glob>'          filter by filename glob (e.g. '*.scala', 'test*')
    |  find <root> --ext <e>                filter by extension suffix (e.g. .md)
    |  find <root> --type f|d               regular files (f, the default) or directories (d)
    |  find <root> --max-depth N            descend at most N levels below <root> (root = 0)
    |  find <root> --exclude '<glob>'       drop entries whose path RELATIVE TO <root> matches the
    |                                       glob (java.nio glob syntax; repeatable); a glob ending
    |                                       in /** prunes that whole subtree, e.g. 'target/**'
    |  find <root> --all                    include EVERYTHING: hidden entries AND the curated skips
    |  find <root> ... --count              print just the count line, not the paths
    |
    |Notes:
    |  Hidden entries (names starting with '.', e.g. .git, .scala-build) are skipped by default —
    |  whole subtree and all — so a repo scan doesn't drown in build caches; pass --all to include them.
    |  Directories named target, out, build, or node_modules are ALSO skipped by default; unlike the
    |  dot-name skip this is DISCLOSED on the matches line together with --exclude suppressions, e.g.
    |  `5 matches (2 excluded: target, node_modules)` — a pruned subtree counts as one entry. When
    |  nothing was excluded the plain matches line is printed.
    |  Symlinks are NOT followed (a symlinked dir cannot smuggle the walk outside <root>).
    |  Filters combine (AND). Output is a count line plus indented paths, sorted for determinism.
    |
    |Examples:
    |  tt find src --ext .scala                 # every .scala file under src
    |  tt find docs --name 'SM*.md'             # docs whose name matches SM*.md
    |  tt find . --type d --max-depth 1         # immediate sub-directories
    |  tt find . --ext .json --exclude 'seeds/**'   # .json files, minus the whole seeds subtree
    |
    |Full reference: tools/README.md""".stripMargin

@main def find(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(FindHelp); sys.exit(0) }
  val countOnly = args.contains("--count")
  val all = args.contains("--all")
  val a = args.filterNot(f => f == "--count" || f == "--all").toList
  def optOf(flag: String): Option[String] =
    val i = a.indexOf(flag)
    if i >= 0 && i + 1 < a.size then Some(a(i + 1)) else None
  val nameGlob = optOf("--name")
  val ext = optOf("--ext")
  val typ = optOf("--type").getOrElse("f")
  val maxDepth: Option[Int] = optOf("--max-depth").map { s =>
    s.toIntOption match
      case Some(n) if n >= 0 => n
      case _ => Console.err.println(s"find: --max-depth needs a non-negative integer, got '$s'"); sys.exit(2)
  }
  if a.lastOption.contains("--exclude") then
    Console.err.println("find: --exclude needs a glob"); sys.exit(2)
  val excludes = a.zipWithIndex.collect { case (t, i) if t == "--exclude" => a(i + 1) }.toVector
  // Positional root = the first token that is neither a flag nor a flag's value.
  val flagsWithVal = Set("--name", "--ext", "--type", "--max-depth", "--exclude")
  val consumed = scala.collection.mutable.Set.empty[Int]
  a.zipWithIndex.foreach { case (t, i) => if flagsWithVal(t) then { consumed += i; consumed += i + 1 } }
  val positionals = a.zipWithIndex.collect { case (t, i) if !consumed(i) && !t.startsWith("--") => t }
  positionals.headOption match
    case None =>
      println("usage: find <root> [--name <glob>] [--ext <e>] [--type f|d] [--max-depth N] [--exclude <glob>]... [--all] [--count]")
    case Some(rootStr) =>
      val root = Path.of(rootStr)
      if !Files.exists(root) then { Console.err.println(s"find: no such path: $rootStr"); sys.exit(2) }
      val matcher = nameGlob.map(g => FileSystems.getDefault.getPathMatcher(s"glob:$g"))
      def matches(p: Path): Boolean =
        ext.forall(e => p.toString.endsWith(e)) && matcher.forall(m => Option(p.getFileName).exists(m.matches))
      val hits = scala.collection.mutable.ArrayBuffer.empty[String]
      val depth = maxDepth.getOrElse(Int.MaxValue)
      // Shared pruning walker (Lib.walkPruned, also behind tt files): hidden subtrees pruned whole,
      // symlinks not followed, boundary-depth dirs delivered with their TRUE type (issue-014).
      val report =
        try
          Lib.walkPruned(root, Lib.Prune(all = all, excludeGlobs = excludes), depth) { (p, isDir) =>
            if isDir then { if typ == "d" && matches(p) then hits += p.toString }
            else if typ == "f" && matches(p) then hits += p.toString
          }
        catch case e: java.util.regex.PatternSyntaxException =>
          Console.err.println(s"find: bad --exclude glob: ${e.getMessage}"); sys.exit(2)
      val sorted = hits.toVector.sorted
      println(s"${sorted.size} matches${report.disclosure}")
      if !countOnly then sorted.foreach(p => println(s"  $p"))
