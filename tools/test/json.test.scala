//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Unit tests for json.scala's PURE core: path parsing, navigation, scalar extraction, rendering.
// No file is read here — MiniJson.parse supplies the values, as the driver does after reading disk.
// The navigation-failure cases are pinned deliberately: this tool exists so that "is this json ok /
// what is in it" stops reaching for a raw interpreter (SM228), and a tool that answers unclearly when
// a path is wrong just sends the reflex back where it came from.

class JsonToolSuite extends munit.FunSuite:

  private def parse(s: String): Json = MiniJson.parse(s).get

  private val doc = parse("""
    { "name": "gs", "version": 3, "ok": true, "nil": null,
      "permissions": { "allow": ["a", "b", "c"] },
      "hooks": [ { "matcher": "Bash" } ],
      "empty": {}, "none": [] }
  """)

  test("parsePath splits on dots; an empty path is the document root") {
    assertEquals(JsonTool.parsePath(""), Vector.empty)
    assertEquals(JsonTool.parsePath("a"), Vector("a"))
    assertEquals(JsonTool.parsePath("permissions.allow.3"), Vector("permissions", "allow", "3"))
  }

  test("navigate reaches a nested field and an array element") {
    assertEquals(JsonTool.navigate(doc, Vector("name")).flatMap(JsonTool.scalar(_).toRight("")), Right("gs"))
    assertEquals(
      JsonTool.navigate(doc, Vector("permissions", "allow", "1")).flatMap(JsonTool.scalar(_).toRight("")),
      Right("b"))
    assertEquals(
      JsonTool.navigate(doc, Vector("hooks", "0", "matcher")).flatMap(JsonTool.scalar(_).toRight("")),
      Right("Bash"))
  }

  test("an empty path returns the document unchanged") {
    assertEquals(JsonTool.navigate(doc, Vector.empty), Right(doc))
  }

  test("navigation failures say WHERE the walk died, not just that it did") {
    val missing = JsonTool.navigate(doc, Vector("nope")).left.getOrElse("")
    assert(missing.contains("no such key"), missing)

    val oob = JsonTool.navigate(doc, Vector("permissions", "allow", "9")).left.getOrElse("")
    assert(oob.contains("out of bounds") && oob.contains("3"), oob)

    val notIndex = JsonTool.navigate(doc, Vector("permissions", "allow", "x")).left.getOrElse("")
    assert(notIndex.contains("not an array index"), notIndex)

    val intoScalar = JsonTool.navigate(doc, Vector("name", "deeper")).left.getOrElse("")
    assert(intoScalar.contains("cannot descend into a string"), intoScalar)
  }

  test("scalar unquotes strings and leaves containers alone") {
    assertEquals(JsonTool.scalar(Json.JStr("hi")), Some("hi"))
    assertEquals(JsonTool.scalar(Json.JBool(true)), Some("true"))
    assertEquals(JsonTool.scalar(Json.JNull), Some("null"))
    assertEquals(JsonTool.scalar(Json.JObj(Map.empty)), None)
    assertEquals(JsonTool.scalar(Json.JArr(Vector.empty)), None)
  }

  test("whole numbers print without a decimal tail") {
    assertEquals(JsonTool.number(3.0), "3")
    assertEquals(JsonTool.number(-42.0), "-42")
    assertEquals(JsonTool.number(1.5), "1.5")
  }

  test("quote escapes what would otherwise break the rendering") {
    assertEquals(JsonTool.quote("a\"b"), "\"a\\\"b\"")
    assertEquals(JsonTool.quote("a\\b"), "\"a\\\\b\"")
    assertEquals(JsonTool.quote("a\nb"), "\"a\\nb\"")
  }

  test("render round-trips through the parser (the rendering stays valid json)") {
    val out = JsonTool.render(doc)
    assertEquals(MiniJson.parse(out).isDefined, true, out)
    assertEquals(MiniJson.parse(out), Some(doc))
  }

  test("render keeps empty containers on one line") {
    assertEquals(JsonTool.render(Json.JObj(Map.empty)), "{}")
    assertEquals(JsonTool.render(Json.JArr(Vector.empty)), "[]")
  }

  test("render sorts keys — a documented LOSS, so it is pinned, not assumed") {
    val out = JsonTool.render(parse("""{"b":1,"a":2}"""))
    assert(out.indexOf("\"a\"") < out.indexOf("\"b\""), out)
  }

  test("keyLines lists object keys sorted, reports array size, refuses scalars") {
    assertEquals(JsonTool.keyLines(parse("""{"b":1,"a":2}""")), Right(Vector("a", "b")))
    assertEquals(JsonTool.keyLines(parse("""[1,2,3]""")), Right(Vector("[array of 3]")))
    assert(JsonTool.keyLines(Json.JStr("x")).isLeft)
  }

  test("help documents the viewer-not-rewriter limit that makes key sorting safe") {
    assert(JsonTool.Help.contains("NEVER A REWRITER"), JsonTool.Help)
  }
