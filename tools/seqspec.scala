//> using scala 3.8.4
//> using jvm 21

// seqspec — the shared spec model + parser for the sequence-diagram tools (svg, ascii). A shared helper with NO
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
  private val MsgRe   = """^(\S+)\s*(-->|->)\s*(\S+)\s*:\s*(.*)$""".r

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
    for raw <- spec.linesIterator do
      val line = raw.trim
      if line.isEmpty || line.startsWith("#") || line.startsWith("//") then () // comment / blank
      else line match
        case TitleRe(t)          => title = Some(t.trim)
        case DeclRe(id, label)   => decls(id) = Option(label).map(unquote).getOrElse(decls.getOrElse(id, id))
        case NoteRe(ids, text)   =>
          val over = ids.split(",").iterator.map(_.trim).filter(_.nonEmpty).toList
          over.foreach(ensure)
          events += Event.Note(over, text.trim)
        case MsgRe(from, arr, to, text) =>
          ensure(from); ensure(to)
          events += Event.Msg(from, to, text.trim, arr == "-->")
        case other => System.err.println(s"seqspec: ignoring unrecognized line: $other")
    Diagram(title, decls.iterator.map((id, lab) => Lifeline(id, lab)).toVector, events.result())
