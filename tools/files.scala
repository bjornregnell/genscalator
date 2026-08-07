//> using file project.scala
//> using jvm 21
//> using file lib.scala

// files — typed `find`/`find|wc`/`grep -l` replacement (PURE). Walk a dir, filter by extension, and
// optionally by a content regex; print a count and (unless --count) the matching paths.
//   scala-cli run tools/files.scala -- <dir> <ext> [contentRegex] [--count]
import agenttools.Lib

// Top-level, so a UNIQUE name (the toolbox compiles as one unit; a generic `Help` would collide across files).
private val FilesHelp: String =
  """tt files — typed find / find|wc / grep -l replacement (pure)
    |
    |Walks a directory tree, filters files by extension and optionally by a content regex, then
    |prints a count plus the matching paths. Reach for it instead of find, find|wc -l, or grep -rl.
    |
    |Usage:
    |  files <dir> <ext>                    count + list files under <dir> ending <ext>   (find)
    |  files <dir> <ext> <contentRegex>     only files whose CONTENT matches the regex    (grep -l)
    |
    |Flags:
    |  --count                              print just the count line, not the paths      (find|wc)
    |  --all                                include hidden entries (default: skip dot-names)
    |
    |Notes:
    |  Hidden entries (names starting with '.', e.g. .git, .scala-build) are skipped by default,
    |  whole subtree and all — same pruning as tt find — so a scan is sources, not build caches.
    |
    |Examples:
    |  tt files src .scala 'TODO'               # source files containing TODO
    |  tt files src .scala --count              # just how many .scala files
    |  tt files docs .md 'deprecated' --count   # how many docs mention deprecated
    |
    |Full reference: tools/README.md""".stripMargin

@main def files(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(FilesHelp); sys.exit(0) }
  val countOnly = args.contains("--count")
  val all = args.contains("--all")
  args.filterNot(f => f == "--count" || f == "--all").toList match
    case dir :: ext :: rest =>
      val contentRe = rest.headOption.map(_.r)
      // Shared pruning walker (Lib.walkPruned, same as tt find): before issue-017 this was a raw
      // Files.walk, so .scala-build/.git internals dominated every scan (98% noise measured).
      val buf = Vector.newBuilder[String]
      Lib.walkPruned(java.nio.file.Path.of(dir), all) { (p, isDir) =>
        if !isDir && java.nio.file.Files.isRegularFile(p) && p.toString.endsWith(ext)
          && contentRe.forall(_.findFirstIn(Lib.readUtf8(p.toString)).isDefined)
        then buf += p.toString
      }
      val hits = buf.result()
      println(s"${hits.size} files")
      if !countOnly then hits.foreach(p => println(s"  $p"))
    case _ =>
      println("usage: files <dir> <ext> [contentRegex] [--all] [--count]")
