//> using scala 3.8.4
//> using jvm 21

// htmltext — strip a saved HTML page down to readable body text: drop <head>/<script>/<style>/<noscript>/<svg>,
// turn block tags into newlines, remove the remaining tags, decode common entities, collapse whitespace.
// Handy for turning a "Save Page As" dump into plain text (e.g. journal guidelines) without the JS/CSS bloat.
// PURE: reads a file, computes, prints (or writes a file).
//   tt htmltext <in.html> [out.file]     (no out.file → prints to stdout)
import java.nio.file.{Files, Path}

/** Strip HTML markup to readable text. */
def stripHtml(html: String): String =
  val noBlocks = html
    .replaceAll("(?is)<head[^>]*>.*?</head>", " ")
    .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
    .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
    .replaceAll("(?is)<noscript[^>]*>.*?</noscript>", " ")
    .replaceAll("(?is)<svg[^>]*>.*?</svg>", " ")
  val blocked = noBlocks // block-level tags become line breaks so the text keeps its shape
    .replaceAll("(?i)<(?:/?)(?:p|div|br|li|h[1-6]|tr|section|article|ul|ol|header|footer|nav|dt|dd|dl|table)[^>]*>", "\n")
  val noTags = blocked.replaceAll("(?is)<[^>]+>", " ")
  val decoded = noTags
    .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
    .replace("&quot;", "\"").replace("&#39;", "'").replace("&rsquo;", "'").replace("&lsquo;", "'")
    .replace("&ldquo;", "\"").replace("&rdquo;", "\"").replace("&mdash;", "—").replace("&ndash;", "–")
    .replace("&hellip;", "…").replaceAll("&#\\d+;", "")
  decoded.replaceAll("[ \\t]+", " ").replaceAll(" *\\n *", "\n").replaceAll("\\n{3,}", "\n\n").trim

// Top-level, so a UNIQUE name (the toolbox compiles as one unit; a generic `Help` would collide across files).
private val HtmltextHelp: String =
  """tt htmltext — strip a saved HTML page to readable text (pure)
    |
    |Turns an HTML file (e.g. a browser "Save Page As" dump) into plain readable text: drops
    |head/script/style/noscript/svg, turns block tags into newlines, removes the remaining
    |tags, decodes common entities, and collapses whitespace. Handy for reading a saved page
    |(journal guidelines, docs) without the JS/CSS bloat.
    |
    |Usage:
    |  htmltext <in.html>                   print the extracted text to stdout
    |  htmltext <in.html> <out.file>        write the text to <out.file> (reports chars written)
    |
    |Examples:
    |  tt htmltext saved-page.html                  # read the page in the terminal
    |  tt htmltext guidelines.html guidelines.txt   # keep a plain-text copy
    |
    |Full reference: tools/README.md""".stripMargin

@main def htmltext(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(HtmltextHelp); sys.exit(0) }
  args.toList match
    case in :: rest =>
      val html = Files.readString(Path.of(in))
      val text = stripHtml(html)
      rest.headOption match
        case Some(out) =>
          Files.writeString(Path.of(out), text)
          println(s"htmltext: wrote ${text.length} chars (from ${html.length}) to $out")
        case None => println(text)
    case _ =>
      println("usage: htmltext <in.html> [out.file]   strip a saved HTML page to readable text (no out → stdout)")
      sys.exit(2)
