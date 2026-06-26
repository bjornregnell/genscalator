//> using scala 3.8.4
//> using jvm 21
//> using file lib.scala

// files — typed `find`/`find|wc`/`grep -l` replacement (PURE). Walk a dir, filter by extension, and
// optionally by a content regex; print a count and (unless --count) the matching paths.
//   scala-cli run tools/files.scala -- <dir> <ext> [contentRegex] [--count]
import agenttools.Lib
import scala.jdk.CollectionConverters.*

@main def files(args: String*): Unit =
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
