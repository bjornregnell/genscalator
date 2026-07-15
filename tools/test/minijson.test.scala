//> using dep org.scalameta::munit::1.3.3

// Unit tests for the hand-rolled MiniJson reader (SM112). Compiled together with tools/ by `scala-cli test tools`,
// so `Json` / `MiniJson` are in scope without a `using file`.

class MiniJsonSuite extends munit.FunSuite:
  import MiniJson.parse

  test("flat object: navigate fields by type"):
    val j = parse("""{"a":"x","b":42,"c":true,"d":null}""").get
    assertEquals(j.field("a").flatMap(_.str), Some("x"))
    assertEquals(j.field("b").flatMap(_.num), Some(42.0))
    assertEquals(j.field("c").flatMap(_.bool), Some(true))
    assertEquals(j.field("d"), Some(Json.JNull))
    assertEquals(j.field("missing"), None)

  test("nested objects + arrays (the transcript usage shape)"):
    val j = parse("""{"m":{"usage":{"output_tokens":108}},"xs":[1,2,3]}""").get
    assertEquals(j.field("m").flatMap(_.field("usage")).flatMap(_.field("output_tokens")).flatMap(_.num), Some(108.0))
    assertEquals(j.field("xs").flatMap(_.arr).map(_.size), Some(3))
    assert(j.field("xs").exists(_.isArr))

  test("string escapes: quote, backslash, newline, \\uXXXX"):
    val j = parse(""""a\"b\\c\ndA"""").get // -> a"b\c<newline>dA
    assertEquals(j.str, Some("a\"b\\c\nd" + "A"))

  test("numbers: negative, decimal, exponent"):
    assertEquals(parse("-3.5").flatMap(_.num), Some(-3.5))
    assertEquals(parse("1e3").flatMap(_.num), Some(1000.0))
    assertEquals(parse("12.34").flatMap(_.num), Some(12.34))

  test("empty object + array"):
    assertEquals(parse("{}").flatMap(_.obj).map(_.size), Some(0))
    assertEquals(parse("[]").flatMap(_.arr).map(_.size), Some(0))

  test("whitespace tolerated around tokens"):
    val j = parse("""  {  "a" : 1 , "b" : [ 2 , 3 ] }  """).get
    assertEquals(j.field("a").flatMap(_.num), Some(1.0))
    assertEquals(j.field("b").flatMap(_.arr).map(_.size), Some(2))

  test("string vs array content distinction (human prompt vs tool-result)"):
    assert(parse(""""just text"""").exists(v => v.str.isDefined && !v.isArr))
    assert(parse("""[{"type":"tool_result"}]""").exists(_.isArr))

  test("malformed input -> None (never throws)"):
    assertEquals(parse("{bad"), None)
    assertEquals(parse("""{"a":}"""), None)
    assertEquals(parse(""), None)
    assertEquals(parse("[1,2"), None)

  test("trailing garbage -> None"):
    assertEquals(parse("""{"a":1} extra"""), None)
    assertEquals(parse("1 2"), None)
