//> using scala 3.8.4
//> using jvm 21
//> using file lib.scala

// texttool — a typed, compiler-checked replacement for the grep/awk/cut/sort|uniq reflex.
// Off-the-shelf: pick a subcommand, give args. Pure (reads a file, computes, prints; no other effects).
//   scala-cli run tools/text.scala -- <cmd> <args>
import agenttools.Lib

/** Warn (stderr, non-fatal) when a pattern carries grep-BRE escapes that Java regex — this tool's engine (`String.r`)
  * — reads LITERALLY. The silent-empty footgun: grep's `\|` means alternation, but Java reads it as a literal pipe, so
  * the search matches nothing and the empty result masquerades as "absent". Prosthetic perception, never a block. */
private def warnGrepBre(pat: String): Unit =
  val hits = List("\\|", "\\(", "\\)", "\\{", "\\}", "\\+", "\\?").filter(pat.contains)
  if hits.nonEmpty then
    System.err.println(
      s"text: warning — pattern has grep-BRE escape(s) ${hits.mkString(" ")} — this tool uses Java regex (ERE), where " +
      "`\\|` is a LITERAL pipe (matches nothing), not alternation. Use `|` (and unescaped `( { + ?`).")

// Top-level, so a UNIQUE name (the toolbox compiles as one unit; a generic `Help` would collide across files).
private val TextHelp: String =
  """tt text — typed grep/awk/cut/uniq replacement (pure)
    |
    |Searches, counts, and slices plain-text files with Java regex. Reach for it instead of
    |grep/awk/cut/sort|uniq: one compiler-checked tool, no pipes, no shell surprises.
    |
    |Usage:
    |  text count <file> <regex>            count regex matches                    (grep -c)
    |  text match <file> <regex>            print matching lines, numbered         (grep -n)
    |  text context <file> <regex> [N]      matches with N context lines, default 2  (grep -C N)
    |  text freq <file> <regex>             histogram of the match, or of capture group 1
    |                                       if the pattern has one       (sort|uniq -c|sort -rn)
    |  text grepr <dir> <ext[,ext2,...]> <regex> [--count]
    |                                       recursive search -> file:line:match  (grep -r --include)
    |                                       pass an ABSOLUTE dir; --count prints just the total
    |  text cols <file> <sep> <i...>        extract 1-based fields, tab-joined     (cut/awk)
    |
    |Notes:
    |  Patterns are Java regex (ERE): alternation is a bare |, and ( { + ? are special unescaped.
    |  grep-BRE escapes like \| are read LITERALLY (match nothing); the tool warns when it sees one.
    |
    |Examples:
    |  tt text count build.log '^! '                      # count LaTeX errors in a build log
    |  tt text freq run.log '\[fallback\] ([a-z][^,]*)'   # histogram of capture group 1
    |  tt text grepr /abs/src .scala,.java 'TODO'         # recursive TODO hunt, two extensions
    |
    |Full reference: tools/README.md""".stripMargin

@main def text(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(TextHelp); sys.exit(0) }
  args.toList match
    case "count" :: file :: pat :: Nil => // grep -c
      warnGrepBre(pat)
      println(pat.r.findAllIn(Lib.readUtf8(file)).size)

    case "match" :: file :: pat :: Nil => // grep -n
      warnGrepBre(pat)
      val re = pat.r
      for (line, i) <- Lib.readUtf8(file).linesIterator.zipWithIndex if re.findFirstIn(line).isDefined do
        println(f"${i + 1}%6d: $line")

    case "context" :: file :: pat :: rest => // grep -C N: matching lines with N lines of context (default 2)
      val n = rest.headOption.flatMap(_.toIntOption).filter(_ >= 0).getOrElse(2)
      warnGrepBre(pat)
      val re = pat.r
      val lines = Lib.readUtf8(file).linesIterator.toVector
      val hits = lines.indices.iterator.filter(i => re.findFirstIn(lines(i)).isDefined).toSet
      // union each hit's ±n window, then print in order; ':' marks a match line, '-' context, '--' a gap
      val show = hits.flatMap(i => math.max(0, i - n) to math.min(lines.length - 1, i + n)).toVector.sorted
      var prev = -2
      for i <- show do
        if prev >= 0 && i != prev + 1 then println("--")
        println(f"${i + 1}%6d${if hits(i) then ":" else "-"} ${lines(i)}")
        prev = i

    case "freq" :: file :: pat :: Nil => // histogram of the match (or capture group 1) — like sort|uniq -c|sort -rn
      warnGrepBre(pat)
      val counts = pat.r.findAllMatchIn(Lib.readUtf8(file))
        .map(m => if m.groupCount >= 1 && m.group(1) != null then m.group(1) else m.matched)
        .toVector.groupMapReduce(identity)(_ => 1)(_ + _)
      println(Lib.histogram(counts))

    case "grepr" :: dir :: ext :: pat :: rest => // grep -r: recurse <dir>, files ending <ext1[,ext2,…]>; --count = just the number
      warnGrepBre(pat)
      val re = pat.r; val countOnly = rest.contains("--count")
      val exts = ext.split(",").iterator.map(_.trim).filter(_.nonEmpty).toVector // multi-ext: ".scala,.java"
      val path = java.nio.file.Path.of(dir)
      if exts.isEmpty then
        System.err.println(s"grepr: no extension given — use e.g. .scala or .scala,.java")
        sys.exit(2)
      else if !java.nio.file.Files.isDirectory(path) then
        // friendly one-liner instead of a raw NoSuchFileException stack trace; cwd can differ between
        // calls, so show the resolved absolute path and suggest passing an absolute dir.
        System.err.println(s"grepr: not a directory: $dir (resolved: ${path.toAbsolutePath}) — pass an absolute path")
        sys.exit(2)
      val stream = java.nio.file.Files.walk(path)
      try
        import scala.jdk.CollectionConverters.*
        var n = 0
        for p <- stream.iterator.asScala
            if java.nio.file.Files.isRegularFile(p) && exts.exists(p.toString.endsWith)
        do
          for (line, i) <- Lib.readUtf8(p.toString).linesIterator.zipWithIndex if re.findFirstIn(line).isDefined do
            n += 1
            if !countOnly then println(s"$p:${i + 1}: ${line.trim.take(140)}")
        if countOnly then println(n)
      finally stream.close()

    case "cols" :: file :: sep :: idxs if idxs.nonEmpty => // cut/awk: print 1-based fields, tab-joined
      val want = idxs.map(_.toInt)
      for line <- Lib.readUtf8(file).linesIterator do
        val f = line.split(java.util.regex.Pattern.quote(sep), -1)
        println(want.map(i => if i >= 1 && i <= f.length then f(i - 1) else "").mkString("\t"))

    case _ =>
      println("""texttool — typed grep/awk replacement (pure)
        |  text count <file> <regex>          count regex matches            (grep -c)
        |  text match <file> <regex>          print matching lines, numbered (grep -n)
        |  text context <file> <regex> [N]    matching lines with N context lines (grep -C N, default 2)
        |  text freq  <file> <regex>          histogram of match / group(1)  (sort|uniq -c|sort -rn)
        |  text grepr <dir> <ext[,ext2…]> <regex>  recursive search, file:line:match (grep -r --include)
        |  text cols  <file> <sep> <i...>     extract 1-based fields, tab-joined (cut/awk)""".stripMargin)
