def scan(s: String, upper: Boolean): String =
  val sb = StringBuilder()
  var i = 0
  while i < s.length do
    val c = s(i)
    if c == 'a' then sb ++= "A"
    else if c == 'b' then sb ++= "B"
    else if c == 'c' then sb ++= "C"
    else if c == 'd' then sb ++= "D"
    else if c == 'e' then sb ++= "E"
    else sb += c
    i += 1
  sb.toString
