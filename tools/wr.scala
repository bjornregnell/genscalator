//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8
//> using dep com.lihaoyi::ujson:4.4.3

// wr — Workflow-Research utilities (tooling for the WR corpus itself). One subcommand today:
//   tt wr stamp <project-dir> <regex> [--user] [--limit N]
// `stamp` retrofits the REAL date-time of an utterance or event from the session .jsonl transcripts (the
// evidence-timestamp enhancement, SM044). It scans every *.jsonl in <project-dir> for lines matching <regex>
// (Java regex, ERE - not grep BRE) and prints, sorted earliest-first, one row per match:
//   <timestamp>  [<type>]  <session8>:<line>  <snippet>
// so a fluent utterance recorded in a note can be dated to the second. --user keeps only human-typed entries
// (type == "user"), skipping tool_result echoes - prefer the EARLIEST such hit (the retrofit rule). --limit N
// caps rows (default 50). READ-ONLY; no writes. Replaces the 2-step grep+Read retrofit that pulled whole
// 16 KB transcript lines into context.
import scala.util.matching.Regex

object Wr:
  final case class Hit(ts: String, typ: String, session: String, line: Int, snippet: String)

  /** A one-line context window around the match position, whitespace-collapsed and ellipsized. */
  def snippet(raw: String, at: Int, radius: Int = 70): String =
    val start = math.max(0, at - radius)
    val end   = math.min(raw.length, at + radius)
    val core  = raw.substring(start, end).replaceAll("\\s+", " ").trim
    (if start > 0 then "..." else "") + core + (if end < raw.length then "..." else "")

  /** Top-level timestamp + type of a transcript line; ("","") if the line is not valid JSON. PURE. */
  def tsType(raw: String): (String, String) =
    try
      val o = ujson.read(raw).obj
      (o.get("timestamp").map(_.str).getOrElse(""), o.get("type").map(_.str).getOrElse(""))
    catch case _: Throwable => ("", "")

  def scanLines(lines: Iterator[String], session: String, rx: Regex, userOnly: Boolean): Seq[Hit] =
    lines.zipWithIndex.flatMap { (raw, i) =>
      rx.findFirstMatchIn(raw).flatMap { m =>
        val (ts, typ) = tsType(raw)
        if userOnly && typ != "user" then None
        else Some(Hit(ts, typ, session, i + 1, snippet(raw, m.start)))
      }
    }.toSeq

  def fmt(h: Hit): String =
    val sess = if h.session.length > 8 then h.session.take(8) else h.session
    s"${if h.ts.nonEmpty then h.ts else "(no-ts)"}  [${h.typ}]  $sess:${h.line}  ${h.snippet}"

  def stamp(dir: os.Path, pattern: String, userOnly: Boolean, limit: Int): Int =
    if !os.isDir(dir) then { System.err.println(s"wr stamp: not a directory: $dir"); return 2 }
    val rx = pattern.r
    val files = os.list(dir).filter(_.ext == "jsonl").sorted
    val hits = files.flatMap { f =>
      val src = scala.io.Source.fromFile(f.toIO, "UTF-8")
      try scanLines(src.getLines(), f.last.stripSuffix(".jsonl"), rx, userOnly) finally src.close()
    }.sortBy(_.ts)
    if hits.isEmpty then { println("wr stamp: no matches"); return 1 }
    hits.take(limit).foreach(h => println(fmt(h)))
    if hits.length > limit then println(s"... ${hits.length - limit} more match(es); raise --limit")
    0

  def usage(): Unit =
    println("""wr - Workflow-Research utilities
      |  tt wr stamp <project-dir> <regex> [--user] [--limit N]
      |    scan *.jsonl transcripts in <project-dir> for a Java-regex; print <timestamp> [<type>] <session>:<line> <snippet>,
      |    sorted earliest-first. --user = only human-typed (type==user) entries; --limit caps rows (default 50).
      |exit: 0 hits, 1 no matches, 2 usage/error""".stripMargin)

  def dispatch(args: List[String]): Int =
    args match
      case "stamp" :: rest =>
        var userOnly = false
        var limit = 50
        val pos = scala.collection.mutable.ArrayBuffer[String]()
        val a = rest.toVector
        var i = 0
        while i < a.length do
          a(i) match
            case "--user"                      => userOnly = true; i += 1
            case "--limit" if i + 1 < a.length => limit = a(i + 1).toIntOption.getOrElse(50); i += 2
            case other                         => pos += other; i += 1
        pos.toList match
          case dir :: pat :: Nil => stamp(os.Path(dir, os.pwd), pat, userOnly, limit)
          case _                 => usage(); 2
      case _ => usage(); 2

@main def workflowResearch(args: String*): Unit = sys.exit(Wr.dispatch(args.toList))
