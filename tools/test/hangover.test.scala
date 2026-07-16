//> using dep org.scalameta::munit::1.3.3

// Unit tests for the hangover detector (SM121). Compiled together with tools/ by `scala-cli test tools`,
// so `HangoverTool` is in scope without a `using file`. Timestamps are derived via parseInstantMs so no
// epoch constants are hard-coded.

class HangoverSuite extends munit.FunSuite:
  import HangoverTool.*

  val t1 = "2026-07-16T10:00:00.000Z"
  val t2 = "2026-07-16T10:00:12.000Z" // 12s after t1
  def ms(iso: String): Long = parseInstantMs(iso).get

  test("parseInstantMs: ISO-8601 -> epoch-ms; junk -> None"):
    assert(parseInstantMs(t1).isDefined)
    assertEquals(parseInstantMs("not a date"), None)

  test("parse: keeps timestamped records in order, skips untimestamped, flags compact_boundary"):
    val lines = Seq(
      s"""{"type":"message","timestamp":"$t1"}""",
      """{"type":"queue-operation"}""",                                    // no timestamp -> skipped
      s"""{"type":"system","subtype":"compact_boundary","timestamp":"$t2"}"""
    )
    val recs = parse(lines)
    assertEquals(recs.size, 2)
    assertEquals(recs.map(_.epochMs), Vector(ms(t1), ms(t2)))
    assertEquals(recs.head.isCompactBoundary, false)
    assertEquals(recs.last.isCompactBoundary, true)

  test("detect: gap under threshold -> no hangover"):
    val recs = parse(Seq(s"""{"timestamp":"$t1"}"""))
    val v = detect(recs, ms(t1) + 12_000L, 900L).get
    assertEquals(v.gapSec, 12L)
    assertEquals(v.isHangover, false)

  test("detect: gap over threshold -> hangover; a boundary among recent records is flagged"):
    val recs = parse(Seq(
      s"""{"type":"system","subtype":"compact_boundary","timestamp":"$t1"}""",
      s"""{"type":"user","timestamp":"$t2"}"""
    ))
    val v = detect(recs, ms(t2) + 3_600_000L, 900L).get // 1h after last
    assertEquals(v.gapSec, 3600L)
    assertEquals(v.isHangover, true)
    assertEquals(v.atCompactBoundary, true)

  test("detect: no records -> None"):
    assertEquals(detect(Vector.empty, 1000L, 900L), None)

  test("formatGap: largest unit first"):
    assertEquals(formatGap(41865L), "11h 37m")
    assertEquals(formatGap(303L), "5m 3s")
    assertEquals(formatGap(42L), "42s")

  test("report: hangover names the gap; no-hangover under threshold; empty -> nothing-to-say"):
    val one = Seq(s"""{"timestamp":"$t1"}""")
    val hangover = report(one, ms(t1) + 3_600_000L, 900L)
    assert(clue(hangover).startsWith("hangover:"))
    assert(clue(hangover).contains("1h 0m"))
    assert(clue(report(one, ms(t1) + 60_000L, 900L)).startsWith("no hangover:"))
    assertEquals(report(Seq.empty, 1000L, 900L), "hangover: no timestamped records to compare (nothing to say)")
