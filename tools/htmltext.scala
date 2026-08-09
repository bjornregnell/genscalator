//> using file project.scala
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
    |  htmltext <in.html> --cap <n>         print at most n lines to stdout
    |  htmltext <in.html> <out.file>        write the text to <out.file> (reports chars written)
    |
    |Flags:
    |  --cap <n>                            max lines printed to stdout (default: uncapped);
    |                                       when truncating, a non-silent notice reports the
    |                                       true total: === truncated: showing N of M lines
    |                                       (write-to-file mode is always uncapped)
    |
    |Examples:
    |  tt htmltext saved-page.html                  # read the whole page in the terminal
    |  tt htmltext saved-page.html --cap 40         # bounded peek: first 40 lines + notice
    |  tt htmltext guidelines.html guidelines.txt   # keep a plain-text copy
    |
    |Full reference: tools/README.md""".stripMargin

@main def htmltext(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(HtmltextHelp); sys.exit(0) }
  @annotation.tailrec
  def parse(rest: List[String], pos: List[String], cap: Option[Int]): Either[String, (List[String], Option[Int])] =
    rest match
      case Nil => Right((pos.reverse, cap))
      case "--cap" :: n :: t =>
        n.toIntOption match
          case Some(v) if v >= 0 => parse(t, pos, Some(v))
          case _ => Left(s"--cap needs a non-negative integer, got '$n'")
      case "--cap" :: Nil => Left("--cap is missing its argument")
      case other :: t => parse(t, other :: pos, cap)
  parse(args.toList, Nil, None) match
    case Left(msg) =>
      System.err.println(s"htmltext: $msg")
      sys.exit(2)
    case Right((in :: rest, cap)) =>
      val html = Files.readString(Path.of(in))
      val text = stripHtml(html)
      rest.headOption match
        case Some(out) => // write-to-file mode: always uncapped (--cap applies to stdout only)
          Files.writeString(Path.of(out), text)
          println(s"htmltext: wrote ${text.length} chars (from ${html.length}) to $out")
        case None =>
          val lines = text.linesIterator.toVector
          cap match
            case Some(n) if lines.size > n =>
              lines.take(n).foreach(println)
              println(s"=== truncated: showing $n of ${lines.size} lines")
            case _ => println(text)
    case Right((Nil, _)) =>
      println("usage: htmltext <in.html> [out.file] [--cap N]   strip a saved HTML page to readable text (no out → stdout)")
      sys.exit(2)
