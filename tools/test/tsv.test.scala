//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Unit tests for tsv.scala's PURE core: parsing, column resolution, and the row predicates.
// The predicate tests are the substance — `drop` deletes rows on their verdict, so a predicate that
// over-matches loses data. Every case below that asserts `false` is a case where a row must SURVIVE.

class TsvSuite extends munit.FunSuite:
  import TsvTool.*

  private val withHeader = "key\tvalue\tnote\na\ta\tx\nb\tc\ty\n"

  test("parse splits on tabs and separates the header") {
    val t = parse(withHeader, hasHeader = true)
    assertEquals(t.header, Some(Vector("key", "value", "note")))
    assertEquals(t.rows.size, 2)
    assertEquals(t.rows.head, Vector("a", "a", "x"))
  }

  test("parse with no header keeps every line as data") {
    val t = parse(withHeader, hasHeader = false)
    assertEquals(t.header, None)
    assertEquals(t.rows.size, 3)
  }

  test("a trailing newline does not produce a phantom empty row") {
    assertEquals(parse("a\tb\n", hasHeader = false).rows.size, 1)
    assertEquals(parse("a\tb", hasHeader = false).rows.size, 1)
  }

  test("empty fields are preserved, not collapsed") {
    assertEquals(parse("a\t\tc", hasHeader = false).rows.head, Vector("a", "", "c"))
  }

  test("columnIndex resolves by header name and by numeric index") {
    val h = Some(Vector("key", "value"))
    assertEquals(columnIndex("value", h), Right(1))
    assertEquals(columnIndex("0", h), Right(0))
    assertEquals(columnIndex("7", None), Right(7)) // no header: index only
  }

  test("columnIndex rejects an unknown name and names the available columns") {
    val err = columnIndex("nope", Some(Vector("key", "value"))).left.getOrElse("")
    assert(err.contains("no such column"), err)
    assert(err.contains("key, value"), err)
    assert(columnIndex("-1", None).isLeft)
  }

  test("Eq matches only an exact value") {
    val row = Vector("alpha", "beta")
    assert(holds(Pred.Eq(0, "alpha"), row))
    assert(!holds(Pred.Eq(0, "alph"), row))
    assert(!holds(Pred.Eq(1, "alpha"), row))
  }

  test("Matches is an unanchored regex over one column") {
    val row = Vector("hej så", "x")
    assert(holds(Pred.Matches(0, "[åäö]".r), row))
    assert(!holds(Pred.Matches(1, "[åäö]".r), row))
  }

  test("SameAs is the key==value shape") {
    assert(holds(Pred.SameAs(0, 1), Vector("same", "same")))
    assert(!holds(Pred.SameAs(0, 1), Vector("same", "other")))
  }

  test("a row too short for a column never matches, rather than matching an implicit empty") {
    // over-selecting here would delete rows in `drop`, so missing data must be a NON-match
    assert(!holds(Pred.Eq(5, ""), Vector("a", "b")))
    assert(!holds(Pred.SameAs(0, 5), Vector("a", "b")))
    assert(!holds(Pred.Matches(9, ".*".r), Vector("a", "b")))
  }

  test("matches ANDs every predicate; no predicates selects everything") {
    val row = Vector("dup", "dup")
    assert(matches(Vector.empty, row))
    assert(matches(Vector(Pred.SameAs(0, 1), Pred.Eq(0, "dup")), row))
    assert(!matches(Vector(Pred.SameAs(0, 1), Pred.Eq(0, "other")), row))
  }

  test("the key==value plus Swedish-letter combination selects only the intended rows") {
    // the introprog#960 shape: identical columns AND a Swedish letter
    val preds = Vector(Pred.SameAs(0, 1), Pred.Matches(1, "[åäöÅÄÖ]".r))
    assert(matches(preds, Vector("på rad", "på rad")))     // identical + Swedish -> selected
    assert(!matches(preds, Vector("program", "program")))  // identical but not Swedish -> survives
    assert(!matches(preds, Vector("på rad", "on line")))   // Swedish but translated -> survives
  }

  test("help states the never-in-place guarantee that makes drop safe") {
    assert(Help.contains("NEVER edits in place"), Help)
  }
