//> using scala 3.8.4
//> using jvm 21
//> using file lib.scala

// files — typed `find`/`find|wc`/`grep -l` replacement (PURE). Walk a dir, filter by extension, and
// optionally by a content regex; print a count and (unless --count) the matching paths.
//   scala-cli run tools/files.scala -- <dir> <ext> [contentRegex] [--count]
import agenttools.Lib
import scala.jdk.CollectionConverters.*

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
  args.filterNot(_ == "--count").toList match
    case dir :: ext :: rest =>
      val contentRe = rest.headOption.map(_.r)
      val stream = java.nio.file.Files.walk(java.nio.file.Path.of(dir))
      try
        val hits = stream.iterator.asScala
          .filter(p => java.nio.file.Files.isRegularFile(p) && p.toString.endsWith(ext))
          .filter(p => contentRe.forall(_.findFirstIn(Lib.readUtf8(p.toString)).isDefined))
          .toVector
        println(s"${hits.size} files")
        if !countOnly then hits.foreach(p => println(s"  $p"))
      finally stream.close()
    case _ =>
      println("usage: files <dir> <ext> [contentRegex] [--count]")
