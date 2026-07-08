//> using scala 3.8.4
//> using jvm 21
//> using file lib.scala
//> using file mdparse.scala

// ssg — a hand-rolled static-site generator (SM019 step 2). It renders the GitHub-flavored-markdown subset we
// actually use to self-contained HTML, consuming the SAME `MdParse.parse` front-end that md-fmt reflows through
// (one parser, two renderers). EFFECTFUL: reads markdown, writes .html files (like svg/gvdot).
//   tt ssg <src> <out-dir> [--template <file>]
//     <src>          a .md file (rendered alone) OR a dir (every non-underscore .md rendered)
//     <out-dir>      output dir (created if missing); each <name>.md -> <out-dir>/<name>.html
//     --template F   HTML template with {{TITLE}} + {{CONTENT}} slots; else <srcdir>/_template.html; else builtin
//   A sibling `figures/` dir next to the source is copied to <out-dir>/figures so relative images resolve.
//
// Helpers live inside `object Ssg`; only the @main is top-level. The rendering is PURE (renderPage: md+template
// -> html) and unit-tested; the @main does the file I/O. Deferred: nested lists (rendered flat here), footnotes,
// reference links, syntax highlighting (code fences get a language class only). See skills/scala-style §1.
import agenttools.Lib
import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable.ArrayBuffer

object Ssg:

  // ---------- inline rendering ----------
  def escape(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

  /** Rewrite a RELATIVE `*.md` link target to `*.html` so intra-site cross-links resolve on the generated site (a
    * trailing `#fragment` is preserved). Absolute URLs (any `scheme:`) and pure `#anchors` are left untouched. */
  def siteHref(url: String): String =
    if url.startsWith("#") || url.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*") then url
    else
      val h = url.indexOf('#')
      val (path, frag) = if h >= 0 then (url.take(h), url.drop(h)) else (url, "")
      (if path.endsWith(".md") then path.dropRight(3) + ".html" else path) + frag

  /** Render inline markdown (code, images, links, autolinks, bold, italic) in one line of text to HTML, escaping
    * all literal text. Code/image/link/autolink are stashed as `@@S<n>@@` placeholders first (a sentinel that
    * never occurs in prose) so bold/italic never rewrite their insides, then restored last so emphasis can still
    * wrap them. */
  def renderInline(raw: String): String =
    val store = ArrayBuffer[String]()
    def stash(html: String): String = { store += html; s"@@S${store.size - 1}@@" }
    def q(s: String): String = java.util.regex.Matcher.quoteReplacement(s)
    var s = raw
    s = "`([^`]+)`".r.replaceAllIn(s, m => q(stash(s"<code>${escape(m.group(1))}</code>")))
    s = "!\\[([^\\]]*)\\]\\(([^)]+)\\)".r.replaceAllIn(s, m =>
      q(stash(s"""<img src="${escape(m.group(2))}" alt="${escape(m.group(1))}">""")))
    s = "\\[([^\\]]+)\\]\\(([^)]+)\\)".r.replaceAllIn(s, m =>
      q(stash(s"""<a href="${escape(siteHref(m.group(2)))}">${escape(m.group(1))}</a>""")))
    s = "<(https?://[^>\\s]+)>".r.replaceAllIn(s, m =>
      q(stash(s"""<a href="${escape(m.group(1))}">${escape(m.group(1))}</a>""")))
    s = escape(s)                                                                   // remaining literal text
    s = "\\*\\*(.+?)\\*\\*".r.replaceAllIn(s, m => q(s"<strong>${m.group(1)}</strong>")) // non-greedy: allows *italic* inside
    s = "(?<![A-Za-z0-9])_([^_]+)_(?![A-Za-z0-9])".r.replaceAllIn(s, m => q(s"<em>${m.group(1)}</em>")) // no intraword _
    s = "\\*([^*]+)\\*".r.replaceAllIn(s, m => q(s"<em>${m.group(1)}</em>"))
    // restore stashed html, LOOPING so a placeholder nested inside a stashed span (e.g. [`code`](url), a code
    // span inside a link) is itself resolved; the guard bounds it against any pathological self-reference.
    val ph = "@@S(\\d+)@@".r
    var guard = 0
    while ph.findFirstIn(s).isDefined && guard < 1000 do
      s = ph.replaceAllIn(s, m => q(store(m.group(1).toInt))); guard += 1
    s

  /** Plain-text title from a heading's markdown text (drop links to their text, strip `* _ ` markers). */
  def plainTitle(text: String): String =
    val t = "\\[([^\\]]+)\\]\\([^)]+\\)".r.replaceAllIn(text, m => java.util.regex.Matcher.quoteReplacement(m.group(1)))
    t.replace("**", "").replace("*", "").replace("`", "").replace("_", "").trim

  // ---------- block rendering ----------
  def headingParts(raw: String): (Int, String) =
    val t = raw.trim
    val hashes = t.takeWhile(_ == '#').length
    (math.min(hashes, 6), t.drop(hashes).trim)

  private def splitRow(r: String): Vector[String] =
    r.trim.stripPrefix("|").stripSuffix("|").split("\\|", -1).map(_.trim).toVector
  private def isSep(r: String): Boolean =
    r.contains("-") && r.trim.replace("|", " ").trim.matches("[-: ]+")

  def renderTable(rows: Vector[String]): String =
    if rows.isEmpty then return ""
    val sb = StringBuilder("<table>\n")
    val header = rows.length >= 2 && isSep(rows(1))
    if header then
      sb ++= "<thead>\n<tr>"
      splitRow(rows(0)).foreach(c => sb ++= s"<th>${renderInline(c)}</th>")
      sb ++= "</tr>\n</thead>\n<tbody>\n"
      rows.drop(2).foreach { r => sb ++= "<tr>"; splitRow(r).foreach(c => sb ++= s"<td>${renderInline(c)}</td>"); sb ++= "</tr>\n" }
      sb ++= "</tbody>\n"
    else
      sb ++= "<tbody>\n"
      rows.foreach { r => sb ++= "<tr>"; splitRow(r).foreach(c => sb ++= s"<td>${renderInline(c)}</td>"); sb ++= "</tr>\n" }
      sb ++= "</tbody>\n"
    sb ++= "</table>\n"
    sb.toString

  private def renderFence(lines: Vector[String]): String =
    val lang = lines.head.trim.dropWhile(c => c == '`' || c == '~').trim
    val afterOpen = lines.drop(1)
    val body = if afterOpen.nonEmpty && MdParse.isFence(afterOpen.last) then afterOpen.dropRight(1) else afterOpen
    val cls = if lang.nonEmpty then s""" class="language-$lang"""" else ""
    s"<pre><code$cls>${escape(body.mkString("\n"))}</code></pre>\n"

  /** Render a run of list items as one or more lists (flat; a sub-run switches list type when `ordered` flips).
    * Nested lists are deferred (SM019 refinement) — items render at one level. */
  def renderList(items: Vector[MdParse.Block.Item]): String =
    val sb = StringBuilder()
    var i = 0
    while i < items.length do
      val ordered = items(i).ordered
      val tag = if ordered then "ol" else "ul"
      sb ++= s"<$tag>\n"
      while i < items.length && items(i).ordered == ordered do
        sb ++= s"<li>${renderInline(items(i).text)}</li>\n"; i += 1
      sb ++= s"</$tag>\n"
    sb.toString

  def renderBlocks(blocks: Vector[MdParse.Block]): String =
    import MdParse.Block.*
    val sb = StringBuilder()
    val pending = ArrayBuffer[Item]()
    def flush(): Unit = if pending.nonEmpty then { sb ++= renderList(pending.toVector); pending.clear() }
    blocks.foreach {
      case it: Item      => pending += it
      case Blank         => flush()
      case Heading(raw)  => flush(); val (lvl, txt) = headingParts(raw); sb ++= s"<h$lvl>${renderInline(txt)}</h$lvl>\n"
      case Rule(_)       => flush(); sb ++= "<hr>\n"
      case Para(_, _, t) => flush(); sb ++= s"<p>${renderInline(t)}</p>\n"
      case Quote(t)      => flush(); sb ++= s"<blockquote>\n<p>${renderInline(t)}</p>\n</blockquote>\n"
      case Fence(lines)  => flush(); sb ++= renderFence(lines)
      case Table(rows)   => flush(); sb ++= renderTable(rows)
    }
    flush()
    sb.toString

  /** PURE: markdown + template -> a full HTML page. Title = the first level-1 heading (plain), else "Untitled". */
  def renderPage(md: String, template: String): String =
    import MdParse.Block.*
    val blocks = MdParse.parse(md)
    val title = blocks.collectFirst { case Heading(raw) if headingParts(raw)._1 == 1 => plainTitle(headingParts(raw)._2) }
      .getOrElse("Untitled")
    template.replace("{{TITLE}}", escape(title)).replace("{{CONTENT}}", renderBlocks(blocks))

  val BuiltinTemplate: String =
    """<!doctype html>
      |<html lang="en"><head><meta charset="utf-8">
      |<meta name="viewport" content="width=device-width, initial-scale=1">
      |<title>{{TITLE}}</title>
      |<style>body{max-width:44rem;margin:2rem auto;padding:0 1rem;font-family:system-ui,sans-serif;line-height:1.6}
      |pre{overflow-x:auto;background:#f4f4f4;padding:1rem;border-radius:6px}code{font-family:ui-monospace,monospace}
      |table{border-collapse:collapse}th,td{border:1px solid #ccc;padding:.3rem .6rem}img{max-width:100%}</style>
      |</head><body>
      |{{CONTENT}}
      |</body></html>""".stripMargin

  // ---------- tool (effectful) ----------
  private def isMarkdown(p: Path): Boolean =
    val n = p.getFileName.toString
    n.endsWith(".md") && !n.startsWith("_")

  private def copyTree(src: Path, dst: Path): Unit =
    val stream = Files.walk(src)
    try
      import scala.jdk.CollectionConverters.*
      stream.iterator.asScala.foreach { p =>
        val target = dst.resolve(src.relativize(p))
        if Files.isDirectory(p) then Files.createDirectories(target)
        else { Files.createDirectories(target.getParent); Files.copy(p, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
      }
    finally stream.close()

  private val Usage =
    """ssg — hand-rolled markdown -> static HTML site generator
      |  ssg <src> <out-dir> [--template <file>]
      |    <src> = a .md file or a dir of .md files; <out-dir> created if missing""".stripMargin

  def dispatch(args: String*): Unit =
    val (pos, tmplOpt) = args.toList match
      case s :: o :: "--template" :: t :: Nil => (List(s, o), Some(t))
      case s :: o :: Nil                       => (List(s, o), None)
      case _ => System.err.println("ssg: bad arguments"); System.err.println(Usage); sys.exit(2)
    val src = Paths.get(pos(0)).toAbsolutePath.normalize
    val outDir = Paths.get(pos(1)).toAbsolutePath.normalize
    if !Files.exists(src) then { System.err.println(s"ssg: no such source: $src"); sys.exit(2) }
    val srcDir = if Files.isDirectory(src) then src else src.getParent
    val sources: Vector[Path] =
      if Files.isDirectory(src) then
        val st = Files.list(src)
        try { import scala.jdk.CollectionConverters.*; st.iterator.asScala.filter(isMarkdown).toVector.sorted } finally st.close()
      else Vector(src)
    if sources.isEmpty then { System.err.println(s"ssg: no .md files under $src"); sys.exit(2) }
    val discovered = srcDir.resolve("_template.html")
    val template =
      tmplOpt.map(Lib.readUtf8)
        .orElse(if Files.isRegularFile(discovered) then Some(Lib.readUtf8(discovered.toString)) else None)
        .getOrElse(BuiltinTemplate)
    Files.createDirectories(outDir)
    for md <- sources do
      val name = md.getFileName.toString.stripSuffix(".md")
      val html = renderPage(Lib.readUtf8(md.toString), template)
      val out = outDir.resolve(s"$name.html")
      Files.write(out, html.getBytes("UTF-8"))
      System.err.println(s"ssg: wrote $out")
    val figures = srcDir.resolve("figures")
    if Files.isDirectory(figures) then
      copyTree(figures, outDir.resolve("figures")); System.err.println(s"ssg: copied figures/ -> ${outDir.resolve("figures")}")

@main def staticSiteGen(args: String*): Unit = Ssg.dispatch(args*)
