//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8
//> using dep com.lihaoyi::ujson:4.4.3

// wr — Workflow-Research utilities (tooling for the WR corpus itself). One subcommand today:
//   tt wr stamp <project-dir> <regex> [--user | --human] [--limit N]
// `stamp` retrofits the REAL date-time of an utterance or event from the session .jsonl transcripts (the
// evidence-timestamp enhancement, SM044). It scans every *.jsonl in <project-dir> for lines matching <regex>
// (Java regex, ERE - not grep BRE) and prints, sorted earliest-first, one row per match:
//   <timestamp>  [<type>]  <session8>:<line>  <snippet>
// so a fluent utterance recorded in a note can be dated to the second. READ-ONLY; no writes. Replaces the
// 2-step grep+Read retrofit that pulled whole 16 KB transcript lines into context.
//
// Filter flags (the retrofit rule = prefer the EARLIEST genuine-human hit):
//   --user   keep type=="user" entries. NB Claude Code records TOOL RESULTS as type=="user" too, so this
//            still admits ~4.7k tool_result echoes in a real transcript - a coarse filter (kept for compat).
//   --human  keep only genuinely HUMAN-TYPED prose (SM044a). Empirically (probe on a 31k-line transcript) a
//            human line is type=="user" AND not isMeta AND has NO toolUseResult field AND its message.content
//            is either a non-command string or an array containing a text block. This drops the tool_result
//            echoes, the isMeta chrome (/context pastes flagged meta, heartbeats, caveats) and the
//            <command-name> slash-command wrappers - so the earliest hit is the human's ACTUAL utterance.
//            Known residual: a pasted /context block or the compact-summary continuation can still read as
//            prose; a specific-phrase regex won't match that boilerplate, so it's low-risk for retrofits.
import scala.util.matching.Regex

object Wr:
  final case class Hit(ts: String, typ: String, session: String, line: Int, snippet: String)

  enum Mode:
    case All, User, Human

  /** A one-line context window around the match position, whitespace-collapsed and ellipsized. */
  def snippet(raw: String, at: Int, radius: Int = 70): String =
    val start = math.max(0, at - radius)
    val end   = math.min(raw.length, at + radius)
    val core  = raw.substring(start, end).replaceAll("\\s+", " ").trim
    (if start > 0 then "..." else "") + core + (if end < raw.length then "..." else "")

  /** Parse a transcript line to a ujson value; None if it is not valid JSON. PURE. */
  def parseLine(raw: String): Option[ujson.Value] =
    try Some(ujson.read(raw)) catch case _: Throwable => None

  /** Top-level timestamp + type of an already-parsed transcript value. PURE. */
  def tsTypeOf(v: ujson.Value): (String, String) =
    val o = v.obj
    (o.get("timestamp").flatMap(_.strOpt).getOrElse(""), o.get("type").flatMap(_.strOpt).getOrElse(""))

  /** Top-level timestamp + type of a raw line; ("","") if not valid JSON. PURE. (Kept for callers/tests.) */
  def tsType(raw: String): (String, String) = parseLine(raw).map(tsTypeOf).getOrElse(("", ""))

  /** Is this parsed transcript value a genuinely HUMAN-TYPED user turn (not a tool_result echo / meta /
    * slash-command wrapper)? See the `--human` note in the header for the empirical basis. PURE. */
  def humanTyped(v: ujson.Value): Boolean =
    val o = v.obj
    val isUser = o.get("type").flatMap(_.strOpt).contains("user")
    val isMeta = o.get("isMeta").flatMap(_.boolOpt).getOrElse(false)
    val isToolResult = o.contains("toolUseResult")
    if !isUser || isMeta || isToolResult then false
    else
      o.get("message").flatMap(_.objOpt).flatMap(_.get("content")) match
        case Some(ujson.Str(s))   => !s.contains("<command-name>")
        case Some(ujson.Arr(its)) => its.exists(_.objOpt.flatMap(_.get("type")).flatMap(_.strOpt).contains("text"))
        case _                    => false

  def scanLines(lines: Iterator[String], session: String, rx: Regex, mode: Mode): Seq[Hit] =
    lines.zipWithIndex.flatMap { (raw, i) =>
      rx.findFirstMatchIn(raw).flatMap { m =>
        val parsed = parseLine(raw)
        val (ts, typ) = parsed.map(tsTypeOf).getOrElse(("", ""))
        val keep = mode match
          case Mode.All   => true
          case Mode.User  => typ == "user"
          case Mode.Human => parsed.exists(humanTyped)
        if !keep then None
        else Some(Hit(ts, typ, session, i + 1, snippet(raw, m.start)))
      }
    }.toSeq

  def fmt(h: Hit): String =
    val sess = if h.session.length > 8 then h.session.take(8) else h.session
    s"${if h.ts.nonEmpty then h.ts else "(no-ts)"}  [${h.typ}]  $sess:${h.line}  ${h.snippet}"

  def stamp(dir: os.Path, pattern: String, mode: Mode, limit: Int): Int =
    if !os.isDir(dir) then { System.err.println(s"wr stamp: not a directory: $dir"); return 2 }
    val rx = pattern.r
    val files = os.list(dir).filter(_.ext == "jsonl").sorted
    val hits = files.flatMap { f =>
      val src = scala.io.Source.fromFile(f.toIO, "UTF-8")
      try scanLines(src.getLines(), f.last.stripSuffix(".jsonl"), rx, mode) finally src.close()
    }.sortBy(_.ts)
    if hits.isEmpty then { println("wr stamp: no matches"); return 1 }
    hits.take(limit).foreach(h => println(fmt(h)))
    if hits.length > limit then println(s"... ${hits.length - limit} more match(es); raise --limit")
    0

  def usage(): Unit =
    println("""wr - Workflow-Research utilities
      |  tt wr stamp <project-dir> <regex> [--user | --human] [--limit N]
      |    scan *.jsonl transcripts in <project-dir> for a Java-regex; print <timestamp> [<type>] <session>:<line> <snippet>,
      |    sorted earliest-first. --user = type==user (NB includes tool_result echoes); --human = only human-typed prose
      |    (drops tool_result / meta / slash-command wrappers - prefer this for retrofits); --limit caps rows (default 50).
      |exit: 0 hits, 1 no matches, 2 usage/error""".stripMargin)

  private val Help: String =
    """tt wr — Workflow-Research utilities (tooling for the WR corpus itself)
      |
      |Retrofits the REAL date-time of an utterance or event from the Claude Code session .jsonl
      |transcripts, so a fluent quote recorded in a note can be dated to the second. READ-ONLY —
      |it never writes; it replaces the 2-step grep+Read retrofit that pulled whole 16 KB
      |transcript lines into context.
      |
      |Usage:
      |  wr stamp <project-dir> <regex> [flags]
      |      scan every *.jsonl in <project-dir> for lines matching <regex> (a JAVA regex,
      |      ERE-style — not grep BRE) and print, sorted earliest-first, one row per match:
      |      <timestamp>  [<type>]  <session8>:<line>  <snippet>
      |
      |Flags:
      |  --user             keep only type=="user" entries. NB Claude Code records TOOL RESULTS as
      |                     type=="user" too, so this coarse filter still admits tool_result echoes
      |  --human            keep only genuinely human-typed prose (drops tool_result echoes, meta
      |                     chrome, slash-command wrappers) — PREFER this for retrofits, where the
      |                     rule is: take the EARLIEST genuine-human hit
      |  --limit N          cap printed rows (default 50)
      |
      |Exit: 0 hits, 1 no matches, 2 usage/error.
      |
      |Examples:
      |  tt wr stamp /abs/proj-dir "the compact dance" --human    # when did the human FIRST say it?
      |  tt wr stamp /abs/proj-dir "guardcheck hook" --limit 10   # first 10 mentions, any entry type
      |
      |Full reference: tools/README.md (and the header comment in tools/wr.scala)""".stripMargin

  def dispatch(args: List[String]): Int =
    if args.contains("--help") || args.contains("-h") then
      println(Help)
      0
    else args match
      case "stamp" :: rest =>
        var mode = Mode.All
        var limit = 50
        val pos = scala.collection.mutable.ArrayBuffer[String]()
        val a = rest.toVector
        var i = 0
        while i < a.length do
          a(i) match
            case "--user"                      => mode = Mode.User; i += 1
            case "--human"                     => mode = Mode.Human; i += 1
            case "--limit" if i + 1 < a.length => limit = a(i + 1).toIntOption.getOrElse(50); i += 2
            case other                         => pos += other; i += 1
        pos.toList match
          case dir :: pat :: Nil => stamp(os.Path(dir, os.pwd), pat, mode, limit)
          case _                 => usage(); 2
      case _ => usage(); 2

@main def workflowResearch(args: String*): Unit = sys.exit(Wr.dispatch(args.toList))
