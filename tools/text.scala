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
    |  text grepr <dir> <ext[,ext2,...]> --any <p1> <p2> ... [--count]
    |                                       recursive search -> file:line:match  (grep -r --include)
    |                                       pass an ABSOLUTE dir; --count prints just the total.
    |                                       --any: match a line if ANY pattern matches (OR), metachar-free
    |                                       (no regex |); see Notes for why that avoids a guard stall.
    |  text cols <file> <sep> <i...>        extract 1-based fields, tab-joined     (cut/awk)
    |
    |Notes:
    |  Patterns are Java regex (ERE): alternation is a bare |, and ( { + ? are special unescaped.
    |  grep-BRE escapes like \| are read LITERALLY (match nothing); the tool warns when it sees one.
    |  --any ORs several patterns WITHOUT typing a regex | : prefer it over 'TODO|FIXME', because a quoted
    |  | (or > ; ) trips the safety guardcheck (not quote-aware) into a needless confirmation stall. It is a
    |  typed flag: unambiguous, no metachar. (Full rationale + design in tools/README.md.)
    |
    |Examples:
    |  tt text count build.log '^! '                      # count LaTeX errors in a build log
    |  tt text freq run.log '\[fallback\] ([a-z][^,]*)'   # histogram of capture group 1
    |  tt text grepr /abs/src .scala,.java 'TODO'         # recursive TODO hunt, two extensions
    |  tt text grepr /abs/src .scala --any TODO FIXME XXX # OR three patterns, no regex | (no guard stall)
    |
    |Full reference: tools/README.md""".stripMargin

/** Pure helpers for the text tool, in an object so the co-located munit tests can call them directly
  * (test scope extends the main scope) and so the names don't pollute the toolbox-wide top-level scope. */
object TextTool:
  /** grepr pattern selection from the args AFTER `<dir> <ext>`.
    *  - `--any p1 p2 …`  -> every following pattern (matched as a logical OR): the metachar-free way to OR
    *    patterns, so the agent never types a regex `|` (which the not-yet-quote-aware guardcheck would read
    *    as a shell pipe and false-trip into a confirmation stall — SM114).
    *  - otherwise         -> the first positional arg is the single `<regex>` (back-compat).
    * `--count` is a flag, never a pattern. */
  def selectGreprPatterns(rest: List[String]): Vector[String] =
    val afterAny = rest.dropWhile(_ != "--any")
    if afterAny.nonEmpty then afterAny.tail.filterNot(_ == "--count").toVector
    else rest.filterNot(_ == "--count").take(1).toVector

  /** A line matches if ANY of the patterns matches (logical OR over Java regexes). */
  def anyMatch(pats: Vector[scala.util.matching.Regex], line: String): Boolean =
    pats.exists(_.findFirstIn(line).isDefined)

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

    case "grepr" :: dir :: ext :: rest if rest.nonEmpty => // grep -r: recurse <dir>, files ending <ext1[,ext2,…]>; --count = just the number
      val countOnly = rest.contains("--count")
      // Patterns: a single positional <regex>, or `--any p1 p2 …` = match a line if ANY pattern matches (OR).
      // --any is the metachar-free way to OR patterns (see TextTool.selectGreprPatterns / SM114).
      val pats = TextTool.selectGreprPatterns(rest)
      if pats.isEmpty then
        System.err.println("grepr: no pattern — give a <regex>, or `--any pat1 pat2 …` to OR several without a regex |")
        sys.exit(2)
      pats.foreach(warnGrepBre)
      val res = pats.map(_.r)
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
          for (line, i) <- Lib.readUtf8(p.toString).linesIterator.zipWithIndex if TextTool.anyMatch(res, line) do
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
