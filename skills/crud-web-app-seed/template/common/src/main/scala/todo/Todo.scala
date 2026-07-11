package todo

// The shared datamodel + a tiny, dependency-free JSON codec. Lives in `common` (a cross-project) so the server needs
// ONLY the JDK and the client shares the exact same wire format. Pure Scala → compiles to both JVM and Scala.js.
// Direct common style.

case class Todo(id: Int, title: String, done: Boolean)

object Todo:
  def toJson(t: Todo): String =
    s"""{"id":${t.id},"title":${Json.quote(t.title)},"done":${t.done}}"""

  def listToJson(ts: Seq[Todo]): String = ts.map(toJson).mkString("[", ",", "]")

  def parse(json: String): Todo = fromJs(Json.parse(json))

  def parseList(json: String): List[Todo] = Json.parse(json).arr.map(fromJs).toList

  private def fromJs(v: Js): Todo =
    val o = v.obj
    Todo(o("id").num.toInt, o("title").str, o("done").bool)

// ---- a minimal JSON value + recursive-descent parser (no dependencies; JDK-only-friendly) ----

enum Js:
  case Str(s: String)
  case Num(n: Double)
  case Bool(b: Boolean)
  case Nul
  case Arr(items: Vector[Js])
  case Obj(fields: Map[String, Js])

  def str: String          = this match { case Str(s) => s; case _ => sys.error(s"not a string: $this") }
  def num: Double          = this match { case Num(n) => n; case _ => sys.error(s"not a number: $this") }
  def bool: Boolean        = this match { case Bool(b) => b; case _ => sys.error(s"not a bool: $this") }
  def arr: Vector[Js]      = this match { case Arr(a) => a; case _ => sys.error(s"not an array: $this") }
  def obj: Map[String, Js] = this match { case Obj(m) => m; case _ => sys.error(s"not an object: $this") }

object Json:
  def quote(s: String): String =
    val b = StringBuilder("\"")
    s.foreach:
      case '"'  => b ++= "\\\""
      case '\\' => b ++= "\\\\"
      case '\n' => b ++= "\\n"
      case '\r' => b ++= "\\r"
      case '\t' => b ++= "\\t"
      case c    => b += c
    b += '"'
    b.toString

  def parse(s: String): Js =
    val p = Parser(s)
    val v = p.value()
    p.ws()
    if !p.atEnd then sys.error(s"trailing content at ${p.pos}")
    v

  private class Parser(s: String):
    var pos = 0
    def atEnd: Boolean = pos >= s.length
    def peek: Char = s.charAt(pos)
    def ws(): Unit = while !atEnd && peek.isWhitespace do pos += 1

    def value(): Js =
      ws()
      peek match
        case '{' => obj()
        case '[' => arr()
        case '"' => Js.Str(string())
        case 't' => lit("true"); Js.Bool(true)
        case 'f' => lit("false"); Js.Bool(false)
        case 'n' => lit("null"); Js.Nul
        case _   => number()

    def lit(w: String): Unit =
      if s.regionMatches(pos, w, 0, w.length) then pos += w.length else sys.error(s"expected $w at $pos")

    def obj(): Js =
      pos += 1; ws()
      val m = scala.collection.mutable.LinkedHashMap[String, Js]()
      if peek == '}' then { pos += 1; return Js.Obj(m.toMap) }
      var more = true
      while more do
        ws()
        val k = string(); ws()
        if peek != ':' then sys.error(s"expected ':' at $pos")
        pos += 1
        m(k) = value(); ws()
        peek match
          case ',' => pos += 1
          case '}' => pos += 1; more = false
          case _   => sys.error(s"expected ',' or '}' at $pos")
      Js.Obj(m.toMap)

    def arr(): Js =
      pos += 1; ws()
      val b = Vector.newBuilder[Js]
      if peek == ']' then { pos += 1; return Js.Arr(b.result()) }
      var more = true
      while more do
        b += value(); ws()
        peek match
          case ',' => pos += 1
          case ']' => pos += 1; more = false
          case _   => sys.error(s"expected ',' or ']' at $pos")
      Js.Arr(b.result())

    def string(): String =
      if peek != '"' then sys.error(s"expected string at $pos")
      pos += 1
      val b = StringBuilder()
      var done = false
      while !done do
        val c = s.charAt(pos); pos += 1
        c match
          case '"' => done = true
          case '\\' =>
            val e = s.charAt(pos); pos += 1
            e match
              case '"' => b += '"'
              case '\\' => b += '\\'
              case '/' => b += '/'
              case 'n' => b += '\n'
              case 'r' => b += '\r'
              case 't' => b += '\t'
              case 'b' => b += '\b'
              case 'f' => b += '\f'
              case 'u' => val h = s.substring(pos, pos + 4); pos += 4; b += Integer.parseInt(h, 16).toChar
              case _   => sys.error(s"bad escape \\$e at $pos")
          case _ => b += c
      b.toString

    def number(): Js =
      val start = pos
      while !atEnd && (peek.isDigit || "+-.eE".contains(peek)) do pos += 1
      Js.Num(s.substring(start, pos).toDouble)
