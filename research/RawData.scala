//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8
//> using dep com.lihaoyi::ujson:4.4.3

// RawData — typed miner for the research RAW-DATA log. Reads the CURRENT Claude Code session transcript
// (newest *.jsonl in the project dir) and extracts VERBATIM chat excerpts with their REAL timestamps, so
// research data survives the compact dance without the agent retyping from memory (which is lossy and, per
// METHODOLOGY.md §5, confabulation-prone). The agent attaches reflections as clearly-separated annotations.
//
// The raw transcript already persists in the jsonl (harness-written); what we risk losing at compaction is
// the CURATION — which moments mattered and why. This tool makes capturing that cheap + objective.
//
// Modes (read-only except --append):
//   --list [--grep <regex>]          index | ts | role | snippet   (find turn indices to capture)
//   --dump <from> [<to>]             verbatim Markdown of turns [from..to] (default to=from)
//   --append <from> [<to>] --note "<reflection>"   append the excerpt + reflection to RAW-DATA.md
//   [--jsonl <path>]                 mine a specific transcript instead of the newest
//   [--role user|assistant]          filter to one side
// Usage: scala-cli run research/RawData.scala -- --list --grep "WR data"
//        scala-cli run research/RawData.scala -- --append 120 124 --note "META-2: cat regression right after the lesson."

import scala.util.Try

val ProjectDir =
  "/home/bjornr/.claude/projects/-home-bjornr-git-berg-bjornregnell-muntabot-synch-introprog"
val RawDataMd = "/home/bjornr/git/berg/bjornregnell/genscalator/research/RAW-DATA.md"

final case class Turn(idx: Int, role: String, ts: String, text: String, tools: Vector[String])

/** Pull readable text out of a message.content (string OR array of blocks). Tool calls become ⟦tool:Name⟧
  * markers; tool_result blocks are summarized (they are machine output, not chat). */
def extract(content: ujson.Value): (String, Vector[String]) =
  content match
    case ujson.Str(s) => (s, Vector.empty)
    case ujson.Arr(blocks) =>
      val sb = new StringBuilder; val tools = Vector.newBuilder[String]
      for b <- blocks do
        Try(b("type").str).getOrElse("") match
          case "text"        => sb.append(Try(b("text").str).getOrElse("")).append("\n")
          case "tool_use"    => val nm = Try(b("name").str).getOrElse("?"); tools += nm; sb.append(s"⟦→ tool: $nm⟧\n")
          case "tool_result" => sb.append("⟦tool_result⟧\n")
          case "thinking"    => sb.append("⟦thinking⟧\n")
          case _             => ()
      (sb.toString.trim, tools.result())
    case _ => ("", Vector.empty)

def readTurns(jsonl: os.Path): Vector[Turn] =
  var i = -1
  os.read.lines(jsonl).flatMap { line =>
    Try {
      val j = ujson.read(line)
      val t = j("type").str
      if t == "user" || t == "assistant" then
        i += 1
        val (txt, tools) = extract(j("message")("content"))
        Some(Turn(i, t, Try(j("timestamp").str).getOrElse(""), txt, tools))
      else None
    }.toOption.flatten
  }.toVector

def newestJsonl(dir: os.Path): os.Path =
  val js = Try(os.list(dir).filter(_.ext == "jsonl")).getOrElse(Nil)
  require(js.nonEmpty, s"no .jsonl transcripts in $dir")
  js.maxBy(os.mtime)

def fmtTs(ts: String): String = // ISO → "HH:mm:ss" (date is the session's; keep it short)
  Try(ts.drop(11).take(8)).filter(_.nonEmpty).getOrElse(ts)

def snippet(s: String, n: Int): String =
  s.replace("\n", " ").take(n) + (if s.length > n then "…" else "")

def mdTurn(t: Turn): String =
  val who = if t.role == "user" then "🧑 BR" else "🤖 agent"
  s"**[$who · ${fmtTs(t.ts)} · #${t.idx}]**\n\n${t.text}\n"

@main def run(args: String*): Unit =
  val a = args.toList
  def flag(f: String): Boolean = a.contains(f)
  def valOf(f: String): Option[String] =
    val i = a.indexOf(f)
    if i >= 0 && i + 1 < a.size then Some(a(i + 1)) else None

  val jsonl = valOf("--jsonl").map(os.Path(_)).getOrElse(newestJsonl(os.Path(ProjectDir)))
  val all = readTurns(jsonl)
  val roleFilter = valOf("--role")
  val turns = roleFilter.fold(all)(r => all.filter(_.role == r))

  if flag("--list") then
    val re = valOf("--grep").map(_.r)
    val sel = re.fold(turns)(r => turns.filter(t => r.findFirstIn(t.text).isDefined))
    println(s"=== ${jsonl.last} — ${all.size} turns; showing ${sel.size}${re.map(r => s" matching /$r/").getOrElse("")} ===")
    for t <- sel do
      val who = if t.role == "user" then "BR " else "AGT"
      println(f"  #${t.idx}%-4d ${fmtTs(t.ts)}  $who  ${snippet(t.text, 96)}")
  else
    val from = valOf("--dump").orElse(valOf("--append")).flatMap(_.toIntOption)
    if from.isEmpty then
      println("usage: --list [--grep RE] | --dump <from> [<to>] | --append <from> [<to>] --note \"...\"")
      sys.exit(1)
    val f = from.get
    // <to> is the positional after <from> for dump/append
    val mode = if flag("--append") then "--append" else "--dump"
    val to = a.drop(a.indexOf(mode) + 2).headOption.flatMap(_.toIntOption).getOrElse(f)
    val excerpt = all.filter(t => t.idx >= f && t.idx <= to)
    val body = excerpt.map(mdTurn).mkString("\n")
    if flag("--append") then
      val note = valOf("--note").getOrElse("")
      val date = excerpt.iterator.map(_.ts).find(_.nonEmpty).map(_.take(10)).getOrElse("") // the excerpt's own date
      val entry =
        s"""
           |## Excerpt #$f–$to · $date
           |
           |$body
           |${if note.nonEmpty then s"\n> **Agent reflection:** $note\n" else ""}
           |---
           |""".stripMargin
      os.write.append(os.Path(RawDataMd), entry)
      println(s"appended turns #$f–$to (${excerpt.size}) to ${os.Path(RawDataMd).last}")
    else
      println(body)
