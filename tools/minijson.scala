//> using scala 3.8.4
//> using jvm 21

// minijson — a tiny, dependency-free JSON reader (SM112 de-dep). PURE; no @main (a shared helper like lib.scala,
// seqspec.scala). WHY it exists: it lets hot, JDK-light tools drop the ujson dependency and become PURE-JDK — the
// leanest Scala Native target (statusline runs on EVERY prompt render; see research/051 + blog 025). Scope is
// deliberately narrow: PARSE + navigate well-formed JSON (Claude Code's statusline object + the transcript JSONL);
// it does NOT serialize (we only ever READ this JSON). Recursive descent over standard JSON. Small, bounded, fully
// testable — exactly the hand-roll-it case (scala-style §1). Any malformed input yields None, never a throw.

/** A parsed JSON value with read-only navigation methods (so callers never pattern-match the ADT directly). */
enum Json:
  case JObj(fields: Map[String, Json])
  case JArr(items: Vector[Json])
  case JStr(value: String)
  case JNum(value: Double)
  case JBool(value: Boolean)
  case JNull

  /** This value as an object's field map, if it is an object. */
  def obj: Option[Map[String, Json]] = this match { case JObj(m) => Some(m); case _ => None }
  /** This value as an array, if it is one. */
  def arr: Option[Vector[Json]]      = this match { case JArr(a) => Some(a); case _ => None }
  /** This value as a string, if it is one. */
  def str: Option[String]            = this match { case JStr(v) => Some(v); case _ => None }
  /** This value as a number, if it is one. */
  def num: Option[Double]            = this match { case JNum(v) => Some(v); case _ => None }
  /** This value as a boolean, if it is one. */
  def bool: Option[Boolean]          = this match { case JBool(b) => Some(b); case _ => None }
  /** Look up a field by key (None if this is not an object or the key is absent). */
  def field(key: String): Option[Json] = obj.flatMap(_.get(key))
  /** True iff this value is a JSON array (used to distinguish a string prompt from a tool-result array). */
  def isArr: Boolean                 = this match { case JArr(_) => true; case _ => false }

object MiniJson:
  /** Parse ONE complete JSON value from `input`. None on any malformed input or trailing garbage. Never throws. */
  def parse(input: String): Option[Json] =
    val p = Parser(input)
    try
      p.skipWs()
      val v = p.parseValue()
      p.skipWs()
      if p.atEnd then Some(v) else None // reject trailing non-whitespace
    catch case _: Throwable => None

  /** A small mutable cursor parser. The `var` is fully ENCAPSULATED (never escapes this instance), which §2 of the
    * style skill explicitly allows for a bounded, clearer imperative scan. */
  private final class Parser(s: String):
    private var i = 0
    def atEnd: Boolean = i >= s.length

    def skipWs(): Unit =
      while i < s.length && { val c = s.charAt(i); c == ' ' || c == '\t' || c == '\n' || c == '\r' } do i += 1

    def parseValue(): Json =
      skipWs()
      s.charAt(i) match
        case '{' => parseObj()
        case '[' => parseArr()
        case '"' => Json.JStr(parseStr())
        case 't' => expect("true");  Json.JBool(true)
        case 'f' => expect("false"); Json.JBool(false)
        case 'n' => expect("null");  Json.JNull
        case _   => parseNum()

    private def expect(lit: String): Unit =
      if !s.regionMatches(i, lit, 0, lit.length) then throw RuntimeException(s"expected $lit")
      i += lit.length

    private def parseObj(): Json =
      i += 1 // consume '{'
      skipWs()
      if s.charAt(i) == '}' then { i += 1; return Json.JObj(Map.empty) }
      val m = scala.collection.mutable.LinkedHashMap[String, Json]()
      var more = true
      while more do
        skipWs()
        val key = parseStr()
        skipWs()
        if s.charAt(i) != ':' then throw RuntimeException("expected :")
        i += 1
        m(key) = parseValue()
        skipWs()
        s.charAt(i) match
          case ',' => i += 1
          case '}' => i += 1; more = false
          case _   => throw RuntimeException("expected , or }")
      Json.JObj(m.toMap)

    private def parseArr(): Json =
      i += 1 // consume '['
      skipWs()
      if s.charAt(i) == ']' then { i += 1; return Json.JArr(Vector.empty) }
      val b = Vector.newBuilder[Json]
      var more = true
      while more do
        b += parseValue()
        skipWs()
        s.charAt(i) match
          case ',' => i += 1
          case ']' => i += 1; more = false
          case _   => throw RuntimeException("expected , or ]")
      Json.JArr(b.result())

    private def parseStr(): String =
      if s.charAt(i) != '"' then throw RuntimeException("expected string")
      i += 1
      val sb = StringBuilder()
      var done = false
      while !done do
        val c = s.charAt(i); i += 1
        c match
          case '"'  => done = true
          case '\\' =>
            val e = s.charAt(i); i += 1
            e match
              case '"'  => sb.append('"')
              case '\\' => sb.append('\\')
              case '/'  => sb.append('/')
              case 'n'  => sb.append('\n')
              case 't'  => sb.append('\t')
              case 'r'  => sb.append('\r')
              case 'b'  => sb.append('\b')
              case 'f'  => sb.append('\f')
              case 'u'  => sb.append(Integer.parseInt(s.substring(i, i + 4), 16).toChar); i += 4
              case o    => sb.append(o)
          case o => sb.append(o)
      sb.toString

    private def parseNum(): Json =
      val start = i
      while i < s.length && { val c = s.charAt(i); c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || (c >= '0' && c <= '9') } do i += 1
      if i == start then throw RuntimeException("expected value")
      Json.JNum(s.substring(start, i).toDouble)
