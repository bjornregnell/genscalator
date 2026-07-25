//> using scala 3.9.0-RC4
//> using jvm 21
//> using file lib.scala

// sub — typed search-and-replace across files (EFFECTFUL, and deliberately NOT a verb on `text`).
// `text` documents itself as pure (reads, computes, prints), so a tool that REWRITES files lives here
// instead, per skills/scala-style: real side effects go in a clearly-marked driver.
//
// Why it exists (SM232): bumping the Scala version touched 78 files, and the toolbox had no typed shape for
// "rewrite this pattern across a tree". That is precisely the moment the sed/python3 reflex fires — the
// tripwire the guard-clean digest names. This is the typed shape, with the one safety property sed lacks:
//   PREVIEW IS THE DEFAULT. Nothing is written until --write is passed.
// So the destructive step is always a second, deliberate act, and the preview is the diff you approve.
import agenttools.Lib
import scala.util.matching.Regex

private val SubHelp: String =
  """tt sub — typed search-and-replace across files (EFFECTFUL; preview by default)
    |
    |Rewrites a Java-regex pattern in one file or across a tree. Reach for it instead of
    |sed -i / a python3 one-liner. NOTHING is written unless you pass --write.
    |
    |Usage:
    |  sub file <file> <regex> <replacement> [--write] [--literal]
    |  sub tree <dir> <ext[,ext2,...]> <regex> <replacement> [--write] [--literal]
    |
    |Flags:
    |  --write      actually rewrite the files (without it you get a preview and nothing changes)
    |  --literal    treat BOTH pattern and replacement as literal text, no regex, no $1 backrefs
    |
    |Notes:
    |  Patterns are Java regex (ERE), matched per LINE, so ^ and $ anchor to the line.
    |  In the replacement, $1 $2 are capture-group backrefs and \ escapes — pass --literal to
    |  turn that off when the text contains a literal $ or \.
    |  tree mode skips generated build dirs (.git .scala-build target node_modules) so a bulk
    |  rewrite never touches build caches, and needs an ABSOLUTE dir.
    |  Preview prints  path:line  with the old line then the new one, so you read the change
    |  before it happens; the summary always states how many lines in how many files.
    |
    |Examples:
    |  tt sub tree /abs/repo/tools .scala 'using scala 3\.8\.4' 'using scala 3.9.0-RC4'
    |  tt sub tree /abs/repo/tools .scala 'using scala 3\.8\.4' 'using scala 3.9.0-RC4' --write
    |  tt sub file notes.md 'colour' 'color' --write
    |  tt sub file build.txt 'v1.2 (old)' 'v1.3' --literal --write
    |
    |Full reference: tools/README.md""".stripMargin

/** Pure core, in an object so the co-located tests call it directly and the names stay off the
  * toolbox-wide top-level scope (the whole toolbox compiles as one unit). */
object SubTool:
  /** One rewritten line: its 1-based number, the text before, and the text after. */
  case class Change(line: Int, before: String, after: String)

  /** Dirs whose contents are generated, never hand-edited — a bulk rewrite that walks into a build
    * cache would "succeed" while corrupting derived state that is about to be regenerated anyway. */
  val SkipDirs: Set[String] = Set(".git", ".scala-build", "target", "node_modules", ".bloop", ".metals")

  def isSkipped(path: java.nio.file.Path): Boolean =
    import scala.jdk.CollectionConverters.*
    path.iterator.asScala.exists(p => SkipDirs.contains(p.toString))

  /** Rewrite every line of `text`, returning the new text and the changes. Line separators are preserved
    * exactly (including a missing final newline), so a rewrite never silently reformats a file's ending. */
  def rewrite(text: String, re: Regex, repl: String): (String, Vector[Change]) =
    val out = StringBuilder()
    var changes = Vector.empty[Change]
    var n = 0
    for chunk <- text.linesWithSeparators do
      n += 1
      val sepAt = chunk.indexWhere(c => c == '\n' || c == '\r')
      val (body, sep) = if sepAt < 0 then (chunk, "") else chunk.splitAt(sepAt)
      val replaced = re.replaceAllIn(body, repl)
      if replaced != body then changes = changes :+ Change(n, body, replaced)
      out.append(replaced).append(sep)
    (out.toString, changes)

  /** Build the pattern and replacement, honouring --literal (no regex, no backrefs on either side). */
  def compile(pattern: String, replacement: String, literal: Boolean): (Regex, String) =
    if literal then (java.util.regex.Pattern.quote(pattern).r, java.util.regex.Matcher.quoteReplacement(replacement))
    else (pattern.r, replacement)

@main def substituteText(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(SubHelp); sys.exit(0) }

  def fail(msg: String): Nothing = { System.err.println(s"sub: $msg"); sys.exit(2) }

  val write   = args.contains("--write")
  val literal = args.contains("--literal")
  val pos     = args.filterNot(a => a == "--write" || a == "--literal").toList

  // Report a file's changes, and (only with --write) persist them. Returns the number of changed lines.
  def handle(path: java.nio.file.Path, re: Regex, repl: String): Int =
    val before = Lib.readUtf8(path.toString)
    val (after, changes) = SubTool.rewrite(before, re, repl)
    if changes.nonEmpty then
      for c <- changes do
        println(s"$path:${c.line}:")
        println(s"  - ${c.before}")
        println(s"  + ${c.after}")
      if write then java.nio.file.Files.write(path, after.getBytes("UTF-8"))
    changes.size

  def summarise(lines: Int, files: Int): Unit =
    if lines == 0 then println("sub: no matches, nothing to change")
    else if write then println(s"sub: wrote $files file(s), $lines line(s) changed")
    else println(s"sub: $lines line(s) in $files file(s) would change — re-run with --write to apply")

  pos match
    case "file" :: file :: pattern :: replacement :: Nil =>
      val path = java.nio.file.Path.of(file)
      if !java.nio.file.Files.isRegularFile(path) then fail(s"not a file: $file")
      val (re, repl) = SubTool.compile(pattern, replacement, literal)
      val n = handle(path, re, repl)
      summarise(n, if n > 0 then 1 else 0)

    case "tree" :: dir :: ext :: pattern :: replacement :: Nil =>
      val root = java.nio.file.Path.of(dir)
      if !java.nio.file.Files.isDirectory(root) then
        fail(s"not a directory: $dir (resolved: ${root.toAbsolutePath}) — pass an absolute path")
      val exts = ext.split(",").iterator.map(_.trim).filter(_.nonEmpty).toVector
      if exts.isEmpty then fail("no extension given — use e.g. .scala or .scala,.java")
      val (re, repl) = SubTool.compile(pattern, replacement, literal)
      val stream = java.nio.file.Files.walk(root)
      try
        import scala.jdk.CollectionConverters.*
        var lines = 0
        var files = 0
        for p <- stream.iterator.asScala
            if java.nio.file.Files.isRegularFile(p) && exts.exists(p.toString.endsWith) && !SubTool.isSkipped(p)
        do
          val n = handle(p, re, repl)
          if n > 0 then { lines += n; files += 1 }
        summarise(lines, files)
      finally stream.close()

    case _ =>
      System.err.println(
        """sub: usage:
          |  tt sub file <file> <regex> <replacement> [--write] [--literal]
          |  tt sub tree <dir> <ext[,ext2,...]> <regex> <replacement> [--write] [--literal]
          |preview by default: nothing is written unless --write is given.""".stripMargin)
      sys.exit(2)
