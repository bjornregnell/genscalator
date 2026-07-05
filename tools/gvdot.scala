//> using scala 3.8.4
//> using jvm 21
//> using file seqspec.scala
//> using dep com.lihaoyi::os-lib:0.11.8

// gvdot — render a sequence-diagram spec (the SAME spec as `tt svg`/`tt ascii`, shared via seqspec.scala) to an
// image by generating **graphviz DOT** and shelling out to **`dot`**. EFFECTFUL driver (spawns `dot`, writes a file).
//   tt gvdot sequence <in.txt> [out.pdf]      (no out → prints the generated DOT source to stdout; needs no `dot`)
//   tt gvdot --sequence-diagram <in.txt> [out.pdf|.png|.svg]   (alias; format inferred from the out extension)
//
// Requires graphviz's `dot` on PATH for the render path (out given). If missing, it errors with an install hint.
// Graphviz docs (where to look when extending this): https://graphviz.org/ · `dot -h` (flags) · `man dot`.
//
// Safety: `dot` is invoked as **argv with no shell** (os.proc), the DOT source is fed on **stdin** (never
// interpolated into a shell string), so spec text can't inject a command. Effectful driver, not a pure tool.
import SeqSpec.*

object Gvdot:

  /** DOT string escaping: backslash first, then double-quote. */
  private def esc(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

  /** Generate graphviz DOT for a sequence diagram, using the standard rank=same lanes technique: actor headers on
    * one rank, a row of point nodes per event step (each row a rank), dashed vertical lifelines chaining the points,
    * and message edges drawn `constraint=false` so they don't distort the ranking. */
  def toDot(d: Diagram): String =
    val lls = d.lifelines
    val n = lls.length
    val idIndex = lls.iterator.map(_.id).zipWithIndex.toMap
    val steps = d.events.indices
    val sb = StringBuilder()
    sb ++= "digraph seq {\n"
    sb ++= "  rankdir=TB;\n  splines=false;\n  nodesep=0.5;\n"
    sb ++= "  node [fontname=\"Helvetica\", fontsize=11];\n"
    sb ++= "  edge [fontname=\"Helvetica\", fontsize=10];\n"
    d.title.foreach(t => sb ++= s"""  labelloc="t"; label="${esc(t)}"; fontname="Helvetica"; fontsize=14;\n""")

    // actor header boxes, on one rank, ordered left-to-right by invisible edges
    sb ++= "  { rank=same;\n"
    for i <- 0 until n do
      sb ++= s"""    h$i [shape=box, style="filled,rounded", fillcolor="#eef0fb", color="#9aa0d0", label="${esc(lls(i).label)}"];\n"""
    if n > 1 then sb ++= "    " + (0 until n).map(i => s"h$i").mkString(" -> ") + " [style=invis];\n"
    sb ++= "  }\n"

    // a row of (mostly invisible) point nodes per event step, each row on its own rank
    sb ++= "  node [shape=point, width=0.03, color=\"#888888\"];\n"
    for t <- steps do
      sb ++= "  { rank=same; "
      sb ++= (0 until n).map(i => s"p${i}_$t").mkString("; ")
      sb ++= ";\n"
      if n > 1 then sb ++= "    " + (0 until n).map(i => s"p${i}_$t").mkString(" -> ") + " [style=invis];\n"
      sb ++= "  }\n"

    // dashed vertical lifelines: header -> its point at each step (high weight keeps them straight)
    for i <- 0 until n do
      val chain = (s"h$i" +: steps.map(t => s"p${i}_$t")).mkString(" -> ")
      if steps.nonEmpty then sb ++= s"""  $chain [style=dashed, arrowhead=none, color="#c3c3d6", weight=100];\n"""
      else sb ++= s"""  h$i;\n"""

    // messages (constraint=false so they don't affect ranks); notes as a box node parked on that step's rank
    for (ev, t) <- d.events.zipWithIndex do ev match
      case Event.Msg(from, to, text, dashed) =>
        val a = idIndex(from); val b = idIndex(to)
        val style = if dashed then "dashed" else "solid"
        sb ++= s"""  p${a}_$t -> p${b}_$t [label="${esc(text)}", style=$style, color="#5a5f86", constraint=false];\n"""
      case Event.Note(over, text) =>
        val i = over.flatMap(idIndex.get).headOption.getOrElse(0)
        sb ++= s"""  note_$t [shape=note, style=filled, fillcolor="#fff6d8", color="#e0c86a", label="${esc(text)}"];\n"""
        sb ++= s"""  { rank=same; p${i}_$t; note_$t; }\n"""
        sb ++= s"""  p${i}_$t -> note_$t [style=invis];\n"""

    sb ++= "}\n"
    sb.toString

  /** Is graphviz `dot` on PATH? (Runs `dot -V`; false if the executable is missing or errors.) */
  def dotAvailable: Boolean =
    try os.proc("dot", "-V").call(check = false, stdout = os.Pipe, stderr = os.Pipe).exitCode == 0
    catch case _: Throwable => false

  private val OutFormats = Set("pdf", "png", "svg", "ps")
  private def formatOf(out: String): String =
    out.split('.').lastOption.map(_.toLowerCase).filter(OutFormats).getOrElse("pdf")

  private def installHint(): Unit =
    System.err.println(
      "gvdot: graphviz 'dot' not found on PATH. Install it, e.g.:  sudo apt install graphviz\n" +
      "       (docs: https://graphviz.org/  |  flags: dot -h  |  manual: man dot)")

  private def usage(): Unit =
    println(
      """usage: gvdot sequence <in.txt> [out.pdf|.png|.svg]   render a spec via graphviz `dot` (no out → prints DOT source)
        |       gvdot --sequence-diagram <in.txt> [out.…]     (alias; output format inferred from the out extension, default pdf)
        |
        |needs graphviz `dot` on PATH for the render path (sudo apt install graphviz). Docs: https://graphviz.org/ , dot -h , man dot
        |spec lines:  title: <t> | actor <Id> [as <label>] | <A> -> <B>: <msg> | <A> --> <B>: <msg> | note over <A>[,<B>]: <t>""".stripMargin)

  def dispatch(args: List[String]): Unit =
    args match
      case mode :: in :: rest if mode.toLowerCase == "sequence" || mode.toLowerCase == "--sequence-diagram" || mode == "-s" || mode.toLowerCase == "seq" =>
        val dot = toDot(parse(os.read(os.Path(in, os.pwd))))
        rest.headOption match
          case None => print(dot) // just the DOT source — no `dot` needed (inspect / test / pipe)
          case Some(out) =>
            if !dotAvailable then { installHint(); sys.exit(3) }
            val fmt = formatOf(out)
            val r = os.proc("dot", s"-T$fmt", "-o", out).call(stdin = dot, check = false, stderr = os.Pipe)
            if r.exitCode != 0 then
              System.err.println(s"gvdot: dot failed (exit ${r.exitCode}): ${r.err.text().trim}")
              sys.exit(r.exitCode)
            println(s"gvdot: wrote $fmt via graphviz dot to $out")
      case _ =>
        usage()
        sys.exit(2)

@main def renderGraphvizDiagram(args: String*): Unit = Gvdot.dispatch(args.toList)
