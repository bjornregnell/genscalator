package reqt

object StringUtils:
  val q: String = '\"'.toString
  val q3: String = q*3
  val nl = "\n"
  val bs = '\\'
  def nlLiteral = """\n"""
  def indent(n: Int): String = " " * (n * Settings.indentSpacing)

  extension (c: Char) 
    def toUnicode: String = "\\u" + Integer.toHexString(c | 0x10000).substring(1)
    def escaped: String = c match 
      case '\b' => "\\b"
      case '\t' => "\\t"
      case '\n' => "\\n"
      case '\f' => "\\f"
      case '\r' => "\\r"
      case '\"' => "\\\""
      case '\\' => "\\\\"
      case _ if c < ' ' => c.toUnicode
      case _ => c.toString

  extension (s: String)
    def escaped: String = s.flatMap(_.escaped)
    def quotedEscaped: String = s"\"${s.escaped}\"" 
    def noQuotes: String = s.filterNot(_ == '"')

    def toLines: Array[String] = s.split("\n")
    def spaceSplit: Array[String] = s.split(" ").map(_.trim).filter(_.nonEmpty)
    def words: Array[String] = s.split("([^a-zA-Z']+)'*\\1*").filter(_.nonEmpty)

    /** 
     * Indent and trim each line with spaces and add trailing newline if missing.
     * From Java 17 there is indent on String; this is here to run on Java 8, 11. 
     */
    def trimIndent(nbrSpaces: Int): String = 
      s.trim.toLines.map(line => (" " * nbrSpaces) + line.trim).mkString("", "\n", "\n")

    /**
     * Strip leading whitespace. 
     * From Java 17 there is stripLeading on String; this is here to run on Java 8, 11.
     */
    def stripLeadingWhitespace: String = s.dropWhile(_.isWhitespace)

    def initLetters: String = s.takeWhile(_.isLetter)

    def deCapitalize: String = s.take(1).toLowerCase ++ s.drop(1)

    def skipIndent: String     = s.dropWhile(ch => ch.isSpaceChar || ch == '\t')
    def skipFirstWord: String  = s.dropWhile(ch => !(ch.isSpaceChar || ch == '\t'))
    def skipFirstToken: String = s.skipIndent.skipFirstWord.trim

    def dropQuotes: String = 
      s.stripPrefix("\"").stripPrefix("\"\"").stripSuffix("\"").stripSuffix("\"\"")

    def splitEscaped(c: Char, esc: Char): Array[String] = 
      if s.isEmpty then Array(s) else
        val result = collection.mutable.ArrayBuffer.empty[String]
        var prev = 0
        var i = 0
        var isInsideEscape = false
        while i < s.length do 
          if s(i) == esc then 
            isInsideEscape = !isInsideEscape
          else if s(i) == c && !isInsideEscape then 
            result.append(s.substring(prev, i))
            prev = i + 1
          i += 1            
        result.append(s.substring(prev, i))
        result.toArray

    def partitionByCharEscaped(c: Char, esc: Char): (String, String) =
      if s.isEmpty then ("", "") else
        var i = 0
        var isInsideEscape = false
        var continue = true
        while continue && i < s.length do 
          if s(i) == esc then 
            isInsideEscape = !isInsideEscape
            i += 1
          else if s(i) == c && !isInsideEscape then 
            continue = false
          else 
            i += 1            
        (s.substring(0, i), s.substring(i, s.length))


    def level(base: Int): Int = 
      val initSpace = s.takeWhile(ch => ch.isSpaceChar || ch == '\t')
      initSpace.replace("\\t", "  ").length + base

    def hasNewline: Boolean = s.contains("\n")

    def wrapLongLineAtWords(n: Int): String = 
      val words = s.split(" ").iterator
      val sb = StringBuilder()
      var i = 0
      while words.hasNext do
        val w: String = words.next
        i += w.length
        if i > n then 
          if w.length > n then 
            sb.append(w)
            sb.append('\n')
            i = 0
          else
            sb.append('\n')
            sb.append(w)
            i = w.length 
        else sb.append(w)
        if words.hasNext then sb.append(' ')
      end while
      sb.toString

    def wrap(n: Int): String = s.split("\n").map(_.wrapLongLineAtWords(n)).mkString("\n")

    def editDistanceTo(t: String): Int = 
      //https://github.com/scala/scala/blob/4e03eb5a1c7dc2cb5274a453dbff38fef12f12f4/src/compiler/scala/tools/nsc/util/EditDistance.scala#L26
      val insertCost: Int = 1
      val deleteCost: Int = 1
      val subCost: Int = 1
      val matchCost: Int = 0
      val caseCost: Int = 1
      val transpositions: Boolean = false
      val n = s.length
      val m = t.length
      if (n == 0) return m
      if (m == 0) return n

      val d = Array.ofDim[Int](n + 1, m + 1)
      0 to n foreach (x => d(x)(0) = x)
      0 to m foreach (x => d(0)(x) = x)

      for 
        i <- 1 to n
        s_i = s(i - 1)
        j <- 1 to m
      do
        val t_j = t(j - 1)
        val cost = 
          if s_i == t_j then matchCost 
          else if s_i.toLower == t_j.toLower then caseCost 
          else subCost

        val c1 = d(i - 1)(j) + deleteCost
        val c2 = d(i)(j - 1) + insertCost
        val c3 = d(i - 1)(j - 1) + cost

        d(i)(j) = c1 min c2 min c3

        if transpositions then
          if i > 1 && j > 1 && s(i - 1) == t(j - 2) && s(i - 2) == t(j - 1) then
            d(i)(j) = d(i)(j) min (d(i - 2)(j - 2) + cost)
        end if
        
      end for

      d(n)(m)
    end editDistanceTo

  end extension
end StringUtils
  
object err:
  class ParseException(msg: String) extends Exception(msg) 
  def unknown(s: String)          = ParseException(s"Unknown constraint: $s")
  def missingParen(s: String)     = ParseException(s"Missing enclosing () in: $s")
  def missingEndParen(s: String)  = ParseException(s"Missing matching ) at end: $s")
  def badIdentifier(s: String)    = ParseException(s"Bad identifier: $s")
  def badParamType(s: String)     = ParseException(s"Bad param type: $s")
  def varExpected(s: String)      = ParseException(s"Var expected: $s")
  def identExpected(s: String)    = ParseException(s"Identifier expected: $s")
  def operatorExpected(s: String) = ParseException(s"Operator expected: $s")
  def unknownTrailing(s: String)  = ParseException(s"Unknown trailing chars: $s")
  def illegalPath(s: String)      = ParseException(s"Illegal path: $s")

object parseUtils:
  /** Returns true if non-empty and first chars is valid start of identifier; otherwise false. */
  def isIdStart(s: String): Boolean = s.nonEmpty && s(0).isUnicodeIdentifierStart
  
  /** Returns a pair of string, first substring of s inside (), second substring of s after ) until end.
   *  Assumes that s starts with ( and ends with ), if not throws ParseException. */
  def parseInsideParen(s: String): (String, String) = 
    if !s.trim.startsWith("(") then throw err.missingParen(s)
    var level = 1
    var i = 1
    var isInsideString = false 
    while i < s.length && (level >= 1 || isInsideString) do 
      if s(i) == '(' then level += 1 
      else if s(i) == ')' then level -= 1
      else if s(i) == '"' then isInsideString = !isInsideString
      i += 1
    if s(i - 1) != ')' then throw err.missingEndParen(s)
    (s.substring(1,i - 1), s.substring(i))

object PrintUtils:
  trait PrettyPrinter[T]:
    extension (x: T) def pp: Unit
  object PrettyPrinter:
    given anyToStringPrinter: PrettyPrinter[Any] with
      extension (x: Any) def pp: Unit = println(x.toString)

