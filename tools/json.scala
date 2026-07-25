//> using scala 3.9.0-RC4
//> using jvm 21
//> using file minijson.scala

// json — READ a JSON file from the toolbox: validate it, look inside it, pluck one value out of it.
// The verb the toolbox was missing: `minijson.scala` has parsed JSON since SM112 and is unit-tested,
// but it is a LIBRARY with no @main, so `tt` could not reach it — and the gap got reached AROUND with
// a raw `python3 -m json.tool` instead of being flagged (research/wr-data, SM228). This is the verb.
//
//   tt json check  <file>            parse-or-fail                (exit 0 = well-formed, 2 = not)
//   tt json pretty <file> [path]     re-render, indented, for READING
//   tt json get    <file> <path>     print ONE scalar, unquoted   (the shell-friendly one)
//   tt json keys   <file> [path]     an object's keys, or an array's length
//
// PATH is dot-separated; a numeric segment indexes an array: `permissions.allow.3`, `hooks.PreToolUse.0.matcher`.
// Omitted path = the whole document.
//
// A VIEWER, NEVER A REWRITER — the one thing to know before piping this anywhere. `minijson` decodes
// objects into an unordered Map, so rendering CANNOT preserve key order (it sorts keys instead), and
// comments/spacing/number spelling are lost. Reading `pretty` output is fine; writing it back over the
// source file would silently reshuffle a human's file. To EDIT json, use the Edit tool on the real text.
//
// The rendering lives HERE, in the tool, not in minijson: that library's stated scope is parse-and-
// navigate ("it does NOT serialize"), and it is the hot path for `tt statusline` on every prompt render.
// Keeping the serializer out of it preserves both that scope and its JDK-light weight.
//
// Allowlisting note: this is a READ primitive over an arbitrary path, so `Bash(tt json *)` grants
// reading any JSON file the user can read (non-JSON just fails to parse). That is no more than the
// existing `Read` grant, but it is a real surface — allow per-verb (`Bash(tt json check *)`) if narrower.
//
// Pure core (`parsePath`, `navigate`, `render`, `scalar`) is total and unit-tested; only `dispatch` touches disk.

object JsonTool {

  val Help: String =
    """tt json — read a JSON file: validate, inspect, pluck
      |
      |Usage:
      |  tt json check  <file>           parse-or-fail (exit 0 = well-formed, 2 = not)
      |  tt json pretty <file> [path]    re-render indented, for reading
      |  tt json get    <file> <path>    print one scalar value, unquoted
      |  tt json keys   <file> [path]    an object's keys (one per line), or an array's length
      |
      |Path syntax:
      |  dot-separated; a numeric segment indexes an array. Omit it for the whole document.
      |    permissions.allow.3
      |    hooks.PreToolUse.0.matcher
      |
      |A VIEWER, NEVER A REWRITER: key order is NOT preserved (keys are sorted) and comments and
      |spacing are lost, so never write `pretty` output back over the source. Edit json as text.
      |
      |Exit: 0 = ok; 2 = usage error, unreadable file, malformed json, or path not found.""".stripMargin

  /** PURE: split a dot path into segments. An empty/absent path is the document root. */
  def parsePath(path: String): Vector[String] =
    if path.isEmpty then Vector.empty else path.split("\\.", -1).toVector

  /** PURE: walk `segments` from `root`. Left describes exactly where the walk died. */
  def navigate(root: Json, segments: Vector[String]): Either[String, Json] =
    segments.foldLeft[Either[String, Json]](Right(root)): (acc, seg) =>
      acc.flatMap: current =>
        current match
          case Json.JArr(items) =>
            seg.toIntOption match
              case None => Left(s"'$seg' is not an array index (this is an array of ${items.size})")
              case Some(i) if i < 0 || i >= items.size =>
                Left(s"index $seg out of bounds (array has ${items.size} items)")
              case Some(i) => Right(items(i))
          case Json.JObj(fields) =>
            fields.get(seg).toRight(s"no such key '$seg'")
          case other => Left(s"cannot descend into a ${kindOf(other)} looking for '$seg'")

  /** PURE: the name of a value's kind, for error messages. */
  def kindOf(j: Json): String = j match
    case Json.JObj(_)  => "object"
    case Json.JArr(_)  => "array"
    case Json.JStr(_)  => "string"
    case Json.JNum(_)  => "number"
    case Json.JBool(_) => "boolean"
    case Json.JNull    => "null"

  /** PURE: a scalar's bare text (no quotes), or None for objects and arrays. */
  def scalar(j: Json): Option[String] = j match
    case Json.JStr(v)  => Some(v)
    case Json.JNum(v)  => Some(number(v))
    case Json.JBool(b) => Some(b.toString)
    case Json.JNull    => Some("null")
    case _             => None

  /** PURE: print a whole double as an integer, so 3.0 reads as 3. */
  def number(d: Double): String =
    if d.isWhole && d.abs < 1e15 then d.toLong.toString else d.toString

  /** PURE: JSON string escaping, enough to round-trip what minijson accepts. */
  def quote(s: String): String =
    val sb = StringBuilder("\"")
    s.foreach:
      case '"'  => sb.append("\\\"")
      case '\\' => sb.append("\\\\")
      case '\n' => sb.append("\\n")
      case '\t' => sb.append("\\t")
      case '\r' => sb.append("\\r")
      case '\b' => sb.append("\\b")
      case '\f' => sb.append("\\f")
      case c if c < ' ' => sb.append("\\u%04x".format(c.toInt))
      case c    => sb.append(c)
    sb.append("\"").toString

  /** PURE: indented rendering. Keys are SORTED — minijson's Map cannot carry source order. */
  def render(j: Json, depth: Int = 0): String =
    val pad = "  " * depth
    val inner = "  " * (depth + 1)
    j match
      case Json.JObj(fields) if fields.isEmpty => "{}"
      case Json.JObj(fields) =>
        fields.toVector.sortBy(_._1)
          .map((k, v) => s"$inner${quote(k)}: ${render(v, depth + 1)}")
          .mkString("{\n", ",\n", s"\n$pad}")
      case Json.JArr(items) if items.isEmpty => "[]"
      case Json.JArr(items) =>
        items.map(v => inner + render(v, depth + 1)).mkString("[\n", ",\n", s"\n$pad]")
      case Json.JStr(v)  => quote(v)
      case other         => scalar(other).getOrElse("null")

  /** PURE: the lines `keys` prints for a value. */
  def keyLines(j: Json): Either[String, Vector[String]] = j match
    case Json.JObj(fields) => Right(fields.keys.toVector.sorted)
    case Json.JArr(items)  => Right(Vector(s"[array of ${items.size}]"))
    case other             => Left(s"${kindOf(other)} has no keys")

  private def fail(msg: String): Nothing =
    System.err.println(s"tt json: $msg")
    sys.exit(2)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then
      println(Help); sys.exit(0)

    val (verb, file, path) = args.toList match
      case v :: f :: rest if Set("check", "pretty", "get", "keys").contains(v) =>
        if v == "get" && rest.isEmpty then fail("get needs a <path> (try: tt json keys <file>)")
        (v, f, rest.headOption.getOrElse(""))
      case v :: _ if Set("check", "pretty", "get", "keys").contains(v) => fail(s"$v needs a <file>")
      case v :: _ => fail(s"unknown verb '$v' (expected: check, get, keys, pretty)")
      case Nil    => fail("usage: tt json <check|pretty|get|keys> <file> [path]  (tt json --help)")

    val source =
      try scala.io.Source.fromFile(file)(using scala.io.Codec.UTF8)
      catch case e: Throwable => fail(s"cannot read $file: ${e.getMessage}")
    val text = try source.mkString finally source.close()

    val root = MiniJson.parse(text).getOrElse(fail(s"malformed json: $file"))

    if verb == "check" then
      println(s"ok: $file (${text.length} chars, ${kindOf(root)})")
      sys.exit(0)

    val target = navigate(root, parsePath(path)) match
      case Right(v) => v
      case Left(msg) => fail(if path.isEmpty then msg else s"$msg (at path '$path')")

    verb match
      case "pretty" => println(render(target))
      case "keys"   => keyLines(target) match
        case Right(lines) => lines.foreach(println)
        case Left(msg)    => fail(msg)
      case "get" => scalar(target) match
        case Some(v) => println(v)
        case None    => fail(s"'$path' is a ${kindOf(target)}, not a scalar (try: tt json keys, or tt json pretty)")
      case _ => fail(s"unhandled verb '$verb'")
}

@main def jsonRead(args: String*): Unit = JsonTool.dispatch(args*)
