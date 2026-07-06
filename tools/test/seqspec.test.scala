//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3

// Unit tests for the shared SeqSpec parser/emitter (seqspec.scala). Like lib.scala it has NO @main, so it sits on the
// toolbox MAIN scope and is importable in-process here (fast + hermetic) — no subprocess needed. Covers the CD11
// Mermaid-subset tolerance (->>/-->> arrows, a leading `sequenceDiagram`, YAML front-matter title) and the
// parse<->emit round-trip.

class SeqSpecSuite extends munit.FunSuite:

  test("Mermaid arrows: ->> is solid, -->> is dashed — backward-tolerant with -> / -->") {
    val d = SeqSpec.parse("A ->> B: hi\nB -->> A: re\nC -> D: old\nD --> C: oldret\n")
    val msgs = d.events.collect { case m: SeqSpec.Event.Msg => m }
    assertEquals(msgs.map(_.dashed), Vector(false, true, false, true))
  }

  test("a leading `sequenceDiagram` opener line is accepted and ignored (no phantom lifeline)") {
    val d = SeqSpec.parse("sequenceDiagram\nA ->> B: hi\n")
    assertEquals(d.lifelines.map(_.id), Vector("A", "B"))
    assertEquals(d.events.size, 1)
  }

  test("YAML front-matter title is read and the --- block is stripped from the body") {
    val d = SeqSpec.parse("---\ntitle: My Flow\n---\nsequenceDiagram\nA ->> B: x\n")
    assertEquals(d.title, Some("My Flow"))
    assertEquals(d.lifelines.map(_.id), Vector("A", "B")) // no phantom "---" lifeline
  }

  test("the legacy `title:` body line form still works") {
    assertEquals(SeqSpec.parse("title: Legacy\nA -> B: x\n").title, Some("Legacy"))
  }

  test("leading-colon message text spawns no phantom lifeline (regression), also under ->>") {
    val d = SeqSpec.parse("A ->> B: :Z cue tired\n")
    assertEquals(d.lifelines.map(_.id), Vector("A", "B"))
    assertEquals(d.events.collect { case m: SeqSpec.Event.Msg => m.text }, Vector(":Z cue tired"))
  }

  test("emit produces Mermaid-subset text that round-trips through parse (model stable)") {
    val src = "---\ntitle: The compact dance\n---\nsequenceDiagram\n" +
      "participant BR\nparticipant CO4 as Claude Opus 4.8\n" +
      "BR ->> CO4: note: at a consolidation point\nCO4 -->> BR: pushed\n" +
      "note over BR,CO4: BR triggers /compact\n"
    val d  = SeqSpec.parse(src)
    val d2 = SeqSpec.parse(SeqSpec.emit(d))
    assertEquals(d2, d)
    val out = SeqSpec.emit(d)
    assert(clue(out).contains("sequenceDiagram"))
    assert(clue(out).contains("BR ->> CO4: note: at a consolidation point"))
    assert(clue(out).contains("CO4 -->> BR: pushed"))
    assert(clue(out).contains("participant CO4 as Claude Opus 4.8"))
  }

  test("emit renders dashed as -->> and solid as ->> even from legacy -> / --> input") {
    val out = SeqSpec.emit(SeqSpec.parse("A -> B: s\nB --> A: d\n"))
    assert(clue(out).contains("A ->> B: s"))
    assert(clue(out).contains("B -->> A: d"))
  }
