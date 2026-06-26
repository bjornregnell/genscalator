//> using scala 3.8.4
//> using jvm 21
//> using file lib.scala

// texttool — a typed, compiler-checked replacement for the grep/awk/cut/sort|uniq reflex.
// Off-the-shelf: pick a subcommand, give args. Pure (reads a file, computes, prints; no other effects).
//   scala-cli run tools/text.scala -- <cmd> <args>
import agenttools.Lib

@main def text(args: String*): Unit =
  args.toList match
    case "count" :: file :: pat :: Nil => // grep -c
      println(pat.r.findAllIn(Lib.readLatin1(file)).size)

    case "match" :: file :: pat :: Nil => // grep -n
      val re = pat.r
      for (line, i) <- Lib.readLatin1(file).linesIterator.zipWithIndex if re.findFirstIn(line).isDefined do
        println(f"${i + 1}%6d: $line")

    case "freq" :: file :: pat :: Nil => // histogram of the match (or capture group 1) — like sort|uniq -c|sort -rn
      val counts = pat.r.findAllMatchIn(Lib.readUtf8(file))
        .map(m => if m.groupCount >= 1 && m.group(1) != null then m.group(1) else m.matched)
        .toVector.groupMapReduce(identity)(_ => 1)(_ + _)
      println(Lib.histogram(counts))

    case "grepr" :: dir :: ext :: pat :: rest => // grep -r: recurse <dir>, files ending <ext>; --count = just the number
      val re = pat.r; val countOnly = rest.contains("--count")
      val stream = java.nio.file.Files.walk(java.nio.file.Path.of(dir))
      try
        import scala.jdk.CollectionConverters.*
        var n = 0
        for p <- stream.iterator.asScala
            if java.nio.file.Files.isRegularFile(p) && p.toString.endsWith(ext)
        do
          for (line, i) <- Lib.readUtf8(p.toString).linesIterator.zipWithIndex if re.findFirstIn(line).isDefined do
            n += 1
            if !countOnly then println(s"$p:${i + 1}: ${line.trim.take(140)}")
        if countOnly then println(n)
      finally stream.close()

    case "cols" :: file :: sep :: idxs if idxs.nonEmpty => // cut/awk: print 1-based fields, tab-joined
      val want = idxs.map(_.toInt)
      for line <- Lib.readLatin1(file).linesIterator do
        val f = line.split(java.util.regex.Pattern.quote(sep), -1)
        println(want.map(i => if i >= 1 && i <= f.length then f(i - 1) else "").mkString("\t"))

    case _ =>
      println("""texttool — typed grep/awk replacement (pure)
        |  text count <file> <regex>          count regex matches            (grep -c)
        |  text match <file> <regex>          print matching lines, numbered (grep -n)
        |  text freq  <file> <regex>          histogram of match / group(1)  (sort|uniq -c|sort -rn)
        |  text grepr <dir> <ext> <regex>     recursive search, file:line:match (grep -r --include)
        |  text cols  <file> <sep> <i...>     extract 1-based fields, tab-joined (cut/awk)""".stripMargin)
