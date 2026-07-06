//> using scala 3.8.4
//> using jvm 21

// seqspec — the shared spec model + parser for the sequence-diagram tools (svg, ascii, gvdot). A shared helper with NO
// @main (like lib.scala), so both tools `//> using file seqspec.scala` and reuse ONE grammar / parser — the spec is
// defined in exactly one place.
//
// Spec grammar (one statement per line; blank lines and `#` / `//` comments ignored):
//   title: <text>                     optional diagram title
//   actor <Id> [as <label>]           declare a lifeline (also: participant); label may be "quoted"
//   <A> -> <B>: <message>             solid arrow  (a call / synchronous message)
//   <A> --> <B>: <message>            dashed arrow  (a return / reply / async)
//   note over <A>[, <B>]: <text>      a note over one or two lifelines
// Lifelines not pre-declared are auto-created in first-seen order. A self-message is A -> A.

object SeqSpec:
  final case class Lifeline(id: String, label: String)
  enum Event:
    case Msg(from: String, to: String, text: String, dashed: Boolean)
    case Note(over: List[String], text: String)
  final case class Diagram(title: Option[String], lifelines: Vector[Lifeline], events: Vector[Event])

  private val TitleRe = """(?i)^title\s*:\s*(.*)$""".r
  private val DeclRe  = """(?i)^(?:actor|participant)\s+(\S+?)(?:\s+as\s+(.+))?\s*$""".r
  private val NoteRe  = """(?i)^note\s+over\s+(.+?)\s*:\s*(.*)$""".r
  // from/to ids use [^\s:]+ (not \S+) so they can't swallow the delimiter colon: without this, a message whose TEXT
  // begins with a colon (e.g. `A -> B: :Z cue`) made `to` capture "B:" — a phantom lifeline. Ids never contain ':'.
  // arrows: solid -> (Mermaid ->>) · dashed --> (Mermaid -->>). Backward-tolerant: accept the original -> / --> AND
  // the Mermaid sequenceDiagram ->> / -->> (CD11). Longest alternatives first so -->> wins over --> and ->> over ->.
  private val MsgRe   = """^([^\s:]+)\s*(-->>|->>|-->|->)\s*([^\s:]+)\s*:\s*(.*)$""".r
  private val SeqDiagRe = """(?i)^sequenceDiagram\s*$""".r // Mermaid opener line — accepted and ignored

  private def unquote(s: String): String =
    val t = s.trim
    if t.length >= 2 && ((t.head == '"' && t.last == '"') || (t.head == '\'' && t.last == '\''))
    then t.substring(1, t.length - 1) else t

  /** Parse a sequence-diagram spec. Lifelines are collected in first-seen order; unknown lines warn (stderr). */
  def parse(spec: String): Diagram =
    var title: Option[String] = None
    val decls  = scala.collection.mutable.LinkedHashMap.empty[String, String] // id -> label, insertion-ordered
    val events = Vector.newBuilder[Event]
    def ensure(id: String): Unit = if !decls.contains(id) then decls(id) = id
    // Mermaid YAML front-matter: a leading `---` … `---` block (CD11). Pull `title:` from it and drop the block;
    // a legacy `title:` body line (below) still works and overrides it.
    val all = spec.linesIterator.toVector
    val trimmed = all.map(_.trim)
    val fnb = trimmed.indexWhere(_.nonEmpty)
    val body =
      if fnb >= 0 && trimmed(fnb) == "---" then
        val close = trimmed.indexOf("---", fnb + 1)
        if close > fnb then
          title = (fnb + 1 until close).iterator.map(trimmed).collectFirst { case TitleRe(t) => t.trim }
          all.drop(close + 1)
        else all
      else all
    for raw <- body do
      val line = raw.trim
      if line.isEmpty || line.startsWith("#") || line.startsWith("//") then () // comment / blank
      else if SeqDiagRe.matches(line) then () // Mermaid `sequenceDiagram` opener — accepted and ignored
      else line match
        case TitleRe(t)          => title = Some(t.trim)
        case DeclRe(id, label)   => decls(id) = Option(label).map(unquote).getOrElse(decls.getOrElse(id, id))
        case NoteRe(ids, text)   =>
          val over = ids.split(",").iterator.map(_.trim).filter(_.nonEmpty).toList
          over.foreach(ensure)
          events += Event.Note(over, text.trim)
        case MsgRe(from, arr, to, text) =>
          ensure(from); ensure(to)
          events += Event.Msg(from, to, text.trim, arr.startsWith("--")) // --> and -->> are dashed
        case other => System.err.println(s"seqspec: ignoring unrecognized line: $other")
    Diagram(title, decls.iterator.map((id, lab) => Lifeline(id, lab)).toVector, events.result())

  /** Render a Diagram back to canonical Mermaid-subset spec text (the CD11 authored form): a `---`/`title:`/`---`
    * front-matter (only when titled), a `sequenceDiagram` opener, `participant` lifelines, and `->>`/`-->>` messages.
    * The model carries no actor-vs-participant kind, so every lifeline emits as `participant` (render-neutral).
    * Round-trips: `parse(emit(d)) == d`. */
  def emit(d: Diagram): String =
    val out = Vector.newBuilder[String]
    for t <- d.title do { out += "---"; out += s"title: $t"; out += "---" }
    out += "sequenceDiagram"
    for ll <- d.lifelines do
      out += (if ll.label == ll.id then s"participant ${ll.id}" else s"participant ${ll.id} as ${ll.label}")
    for e <- d.events do e match
      case Event.Msg(from, to, text, dashed) => out += s"$from ${if dashed then "-->>" else "->>"} $to: $text"
      case Event.Note(over, text)            => out += s"note over ${over.mkString(",")}: $text"
    out.result().mkString("\n") + "\n"
