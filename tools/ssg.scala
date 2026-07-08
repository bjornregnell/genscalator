//> using scala 3.8.4
//> using jvm 21
//> using file lib.scala
//> using file mdparse.scala

// ssg — a hand-rolled static-site generator (SM019 step 2). It renders the GitHub-flavored-markdown subset we
// actually use to self-contained HTML, consuming the SAME `MdParse.parse` front-end that md-fmt reflows through
// (one parser, two renderers). EFFECTFUL: reads markdown, writes .html files (like svg/gvdot).
//   tt ssg <src> <out-dir> [--template <file>]
//   tt ssg --out <out-dir> <file.md>... [--template <file>]     render a CHOSEN SET of files in one pass
//   tt ssg --status <s[,s]> --out <dir> <blog-dir>              render posts whose CURRENT status is in the set
//                                                               (+ index.md always), one pass -- SM032
//   tt ssg --status-update <from>:<to> [--date <d>] <dir|files>      append a status transition to matching posts -- SM032
//     <src>          a .md file (rendered alone) OR a dir (every non-underscore .md rendered)
//     <out-dir>      output dir (created if missing); each <name>.md -> <out-dir>/<name>.html
//     --out D + list render exactly the listed files together into D (so the figure-prune below sees the
//                    UNION of all their references, not one page at a time) -- SM030
//     --template F   HTML template with {{TITLE}} + {{CONTENT}} + optional {{TOC}} slots; else <srcdir>/_template.html; else builtin
//   Only the `figures/` a rendered page actually links are copied to <out-dir>/figures, and stale ones are pruned
//   (reference-aware, self-cleaning) so the output never carries unrelated posts' assets.
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

  /** GitHub-style heading id: lowercase, drop punctuation, spaces -> hyphens. Matches the anchors authors
    * hand-write in the prose (e.g. blog 002's `#55-could-this-just-be-chance`). */
  def slugify(headingText: String): String =
    plainTitle(headingText).toLowerCase.replaceAll("[^a-z0-9\\s-]", "").trim.replaceAll("\\s+", "-")

  /** One id per Heading block, in document order, de-duplicated GitHub-style (append -1, -2, ...). */
  def headingSlugs(blocks: Vector[MdParse.Block]): Vector[String] =
    import MdParse.Block.*
    val seen = scala.collection.mutable.Map[String, Int]()
    blocks.collect { case Heading(raw) => headingParts(raw)._2 }.map { txt =>
      val base = { val s = slugify(txt); if s.isEmpty then "section" else s }
      seen.get(base) match
        case None    => seen(base) = 0; base
        case Some(n) => seen(base) = n + 1; s"$base-${n + 1}"
    }

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
    val ids = headingSlugs(blocks).iterator
    def flush(): Unit = if pending.nonEmpty then { sb ++= renderList(pending.toVector); pending.clear() }
    blocks.foreach {
      case it: Item      => pending += it
      case Blank         => flush()
      case Heading(raw)  =>
        flush(); val (lvl, txt) = headingParts(raw); val id = ids.next()
        val idAttr = if lvl >= 2 then s""" id="$id"""" else ""
        sb ++= s"<h$lvl$idAttr>${renderInline(txt)}</h$lvl>\n"
      case Rule(_)       => flush(); sb ++= "<hr>\n"
      case Para(_, _, t) =>
        flush()
        // Drop HTML comments (<!-- ... -->), including whole-paragraph author notes like AGENT-DRAFT scaffolding,
        // so they never render as visible text. Code fences are separate blocks, so this never touches code.
        val visible = "(?s)<!--.*?-->".r.replaceAllIn(t, _ => "").trim
        if visible.nonEmpty then sb ++= s"<p>${renderInline(visible)}</p>\n"
      case Quote(t)      =>
        flush()
        // Strip the internal **Status: ...** bookkeeping span so it never reaches readers (SM032 trim-at-publish);
        // keep any other preamble prose the author wrote (Audience / Author / Sources).
        val cleaned = StatusRe.replaceAllIn(t, _ => "").replaceAll("^\\s+", "")
        if cleaned.nonEmpty then sb ++= s"<blockquote>\n<p>${renderInline(cleaned)}</p>\n</blockquote>\n"
      case Fence(lines)  => flush(); sb ++= renderFence(lines)
      case Table(rows)   => flush(); sb ++= renderTable(rows)
    }
    flush()
    sb.toString

  /** A right-side "On this page" list from the h2/h3 headings (same-page anchor links); "" if fewer than 2. */
  def buildToc(blocks: Vector[MdParse.Block]): String =
    import MdParse.Block.*
    val ids = headingSlugs(blocks)
    val entries = blocks.collect { case Heading(raw) => raw }.zip(ids).map { (raw, id) =>
      val (lvl, txt) = headingParts(raw); (lvl, id, plainTitle(txt))
    }.filter { (lvl, _, _) => lvl == 2 || lvl == 3 }
    if entries.size < 2 then return ""
    val sb = StringBuilder("<aside class=\"toc\" aria-label=\"On this page\">\n")
    sb ++= "<div class=\"toc-title\">On this page</div>\n<nav>\n<ul>\n"
    entries.foreach { (lvl, id, text) =>
      val cls = if lvl == 3 then " class=\"toc-sub\"" else ""
      sb ++= s"""<li$cls><a href="#$id">${escape(text)}</a></li>\n"""
    }
    sb ++= "</ul>\n</nav>\n</aside>\n"
    sb.toString

  /** PURE: markdown + template -> a full HTML page. Title = the first level-1 heading (plain), else "Untitled". */
  def renderPage(md: String, template: String): String =
    import MdParse.Block.*
    val blocks = MdParse.parse(md)
    val title = blocks.collectFirst { case Heading(raw) if headingParts(raw)._1 == 1 => plainTitle(headingParts(raw)._2) }
      .getOrElse("Untitled")
    template.replace("{{TITLE}}", escape(title))
      .replace("{{TOC}}", buildToc(blocks))
      .replace("{{CONTENT}}", renderBlocks(blocks))

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

  // ---------- status preamble (SM032) ----------
  // The blog "status preamble" is a single blockquote line, e.g.
  //   > **Status: initialized 2026-07-03; drafted 2026-07-07.** ...
  // i.e. a semicolon-separated history of `<verb> <date>` transitions inside one **Status: ...** span. The
  // CURRENT status is the LAST verb; a status update APPENDS a `; <to> <date>` transition (the preamble is trimmed at
  // publish, so this bookkeeping never reaches readers). Both functions are PURE + unit-tested.
  private val StatusRe = "(?s)\\*\\*Status:\\s*(.*?)\\*\\*".r

  /** The current status verb of a post (lowercased), or None if it has no status preamble. */
  def currentStatus(md: String): Option[String] =
    StatusRe.findFirstMatchIn(md).flatMap: m =>
      m.group(1).trim.stripSuffix(".").split(";").map(_.trim).filter(_.nonEmpty)
        .lastOption.map(_.split("\\s+").head.toLowerCase)

  /** Append a `; <to> <date>` transition to the status preamble IFF the post's current status equals `from`
    * (case-insensitive); returns the rewritten markdown, or None if it does not match (or has no preamble). PURE. */
  def updateStatus(md: String, from: String, to: String, date: String): Option[String] =
    if !currentStatus(md).contains(from.toLowerCase) then None
    else StatusRe.findFirstMatchIn(md).map: m =>
      val inner = m.group(1).trim.stripSuffix(".").trim
      md.substring(0, m.start) + s"**Status: $inner; $to $date.**" + md.substring(m.end)

  /** Parse a `from:to` status-transition spec (legacy `from->to` also accepted); None if malformed. */
  def parseTransition(spec: String): Option[(String, String)] =
    val parts = if spec.contains("->") then spec.split("->") else spec.split(":")
    parts.map(_.trim) match
      case Array(f, t) if f.nonEmpty && t.nonEmpty => Some((f, t))
      case _ => None

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
      |  ssg <src> <out-dir> [--template <file>]                 # src = a .md file OR a dir of .md files
      |  ssg --out <out-dir> <file.md>... [--template <file>]    # render a chosen SET of files in one pass
      |  ssg --status <s[,s]> --out <dir> <blog-dir>             # render posts whose current status is in the set
      |  ssg --status-update <from>:<to> [--date <d>] <dir|files>     # append a status transition to matching posts
      |    <out-dir> created if missing; only figures referenced by the rendered pages are copied""".stripMargin

  def dispatch(args: String*): Unit =
    val a = args.toVector
    def optVal(name: String): Option[String] =
      val i = a.indexOf(name); if i >= 0 && i + 1 < a.length then Some(a(i + 1)) else None
    val tmplOpt    = optVal("--template")
    val outOpt     = optVal("--out")
    val statusOpt  = optVal("--status")
    val statusUpdateOpt = optVal("--status-update")
    val dateOpt    = optVal("--date")
    val flags = Set("--template", "--out", "--status", "--status-update", "--date")
    val positionals =
      val b = scala.collection.mutable.ArrayBuffer[String]()
      var i = 0
      while i < a.length do
        if flags.contains(a(i)) then i += 2 else { b += a(i); i += 1 }
      b.toVector
    // resolve <dir-or-files> positionals to concrete post .md files (a dir expands to its non-underscore .md)
    def postFiles(): Vector[Path] =
      positionals.flatMap { p =>
        val path = Paths.get(p).toAbsolutePath.normalize
        if Files.isDirectory(path) then
          val st = Files.list(path)
          try { import scala.jdk.CollectionConverters.*; st.iterator.asScala.filter(isMarkdown).toVector.sorted } finally st.close()
        else Vector(path)
      }.distinct
    def resolveDate: String = dateOpt match
      case None | Some("today") => java.time.LocalDate.now.toString
      case Some(d)              => d
    // --status-update FROM->TO [--date D] <dir-or-files>: append a status transition to matching posts (SM032). No render.
    statusUpdateOpt match
      case Some(spec) =>
        val (from, to) = parseTransition(spec).getOrElse { System.err.println("ssg: --status-update needs FROM:TO (e.g. published:deployed)"); System.err.println(Usage); sys.exit(2) }
        val date = resolveDate
        val files = postFiles()
        if files.isEmpty then { System.err.println("ssg: --status-update needs a source dir or .md file(s)"); sys.exit(2) }
        var n = 0
        for f <- files do
          updateStatus(Lib.readUtf8(f.toString), from, to, date) match
            case Some(updated) => Files.write(f, updated.getBytes("UTF-8")); System.err.println(s"ssg: updated ${f.getFileName} ($from -> $to $date)"); n += 1
            case None          => ()
        println(s"ssg: --status-update $from -> $to  ($n post(s) updated, dated $date)")
        return
      case None => ()
    val (sources, outDir, srcDir): (Vector[Path], Path, Path) = statusOpt match
      case Some(sel) =>
        // status-selection (SM032): positional = the blog dir; render posts whose CURRENT status is in `sel`
        // (comma-separated), plus index.md always, one-pass into --out (so the figure-prune sees their union).
        val wanted = sel.split(",").map(_.trim.toLowerCase).filter(_.nonEmpty).toSet
        val out = outOpt.map(o => Paths.get(o).toAbsolutePath.normalize)
          .getOrElse { System.err.println("ssg: --status needs --out <dir>"); System.err.println(Usage); sys.exit(2) }
        val dir = positionals.headOption.map(p => Paths.get(p).toAbsolutePath.normalize)
          .getOrElse { System.err.println("ssg: --status needs a source directory"); System.err.println(Usage); sys.exit(2) }
        if !Files.isDirectory(dir) then { System.err.println(s"ssg: --status source is not a dir: $dir"); sys.exit(2) }
        val all =
          val st = Files.list(dir)
          try { import scala.jdk.CollectionConverters.*; st.iterator.asScala.filter(isMarkdown).toVector } finally st.close()
        val selected = all.filter(p => currentStatus(Lib.readUtf8(p.toString)).exists(wanted.contains))
        val index    = all.filter(_.getFileName.toString == "index.md")
        val srcs = (index ++ selected).distinct.sorted
        if srcs.isEmpty then { System.err.println(s"ssg: no posts with status in {${wanted.mkString(", ")}} under $dir"); sys.exit(2) }
        (srcs, out, dir)
      case None => outOpt match
        case Some(o) =>
          // list mode (SM030): every positional is a source .md file, rendered together into <o>
          if positionals.isEmpty then { System.err.println("ssg: --out needs one or more source .md files"); System.err.println(Usage); sys.exit(2) }
          val srcs = positionals.map(p => Paths.get(p).toAbsolutePath.normalize)
          for s <- srcs do if !Files.isRegularFile(s) then { System.err.println(s"ssg: no such source file: $s"); sys.exit(2) }
          (srcs, Paths.get(o).toAbsolutePath.normalize, srcs.head.getParent)
        case None =>
          // legacy mode: <src> <out-dir>, src is a .md file or a dir of .md files
          positionals match
            case Vector(s, od) =>
              val src = Paths.get(s).toAbsolutePath.normalize
              if !Files.exists(src) then { System.err.println(s"ssg: no such source: $src"); sys.exit(2) }
              val srcs =
                if Files.isDirectory(src) then
                  val st = Files.list(src)
                  try { import scala.jdk.CollectionConverters.*; st.iterator.asScala.filter(isMarkdown).toVector.sorted } finally st.close()
                else Vector(src)
              (srcs, Paths.get(od).toAbsolutePath.normalize, if Files.isDirectory(src) then src else src.getParent)
            case _ => System.err.println("ssg: bad arguments"); System.err.println(Usage); sys.exit(2)
    if sources.isEmpty then { System.err.println("ssg: no .md sources to render"); sys.exit(2) }
    val discovered = srcDir.resolve("_template.html")
    val template =
      tmplOpt.map(Lib.readUtf8)
        .orElse(if Files.isRegularFile(discovered) then Some(Lib.readUtf8(discovered.toString)) else None)
        .getOrElse(BuiltinTemplate)
    Files.createDirectories(outDir)
    val referenced = scala.collection.mutable.Set[String]()
    val figRef = "(?:src|href)=\"figures/([^\"]+)\"".r
    for md <- sources do
      val name = md.getFileName.toString.stripSuffix(".md")
      val html = renderPage(Lib.readUtf8(md.toString), template)
      figRef.findAllMatchIn(html).foreach(m => referenced += m.group(1))
      val out = outDir.resolve(s"$name.html")
      Files.write(out, html.getBytes("UTF-8"))
      System.err.println(s"ssg: wrote $out")
    // Figures: copy ONLY the ones the rendered pages actually reference, and prune anything else from the
    // output figures/ dir. So the deploy set never carries unrelated posts' assets (reference-aware, self-cleaning).
    val srcFigures = srcDir.resolve("figures")
    val outFigures = outDir.resolve("figures")
    if referenced.nonEmpty && Files.isDirectory(srcFigures) then
      import scala.jdk.CollectionConverters.*
      for rel <- referenced do
        val f = srcFigures.resolve(rel)
        if Files.isRegularFile(f) then
          val dst = outFigures.resolve(rel)
          Files.createDirectories(dst.getParent)
          Files.copy(f, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      if Files.isDirectory(outFigures) then
        val walk = Files.walk(outFigures)
        try walk.iterator.asScala.filter(p => Files.isRegularFile(p)).foreach { p =>
          if !referenced.contains(outFigures.relativize(p).toString) then Files.delete(p)
        } finally walk.close()
      System.err.println(s"ssg: ${referenced.size} referenced figure(s) -> $outFigures")

@main def staticSiteGen(args: String*): Unit = Ssg.dispatch(args*)
