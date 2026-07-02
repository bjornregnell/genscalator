def scan(s: String, upper: Boolean): String =
  val sb = StringBuilder()
  var i = 0
  while i < s.length do
    val c = s(i)
    if upper then
      if c == 'a' then sb ++= "A"
      else if c == 'b' then sb ++= "B"
      else sb += c
      i += 1
    else
      sb += c
      i += 1
  sb.toString
