def scan(s: String, upper: Boolean): String =
  val sb = StringBuilder()
  var i = 0
  while i < s.length do
    val c = s(i)
    if upper then
      if c == 'a' then sb ++= "A"
      else if c == 'b' then sb ++= "B"
      else if c == 'c' then sb ++= "C"
      else if c == 'd' then sb ++= "D"
      else if c == 'e' then sb ++= "E"
      else if c == 'f' then sb ++= "F"
      else if c == 'g' then sb ++= "G"
      else if c == 'h' then sb ++= "H"
      else if c == 'i' then sb ++= "I"
      else if c == 'j' then sb ++= "J"
      else sb += c
      i += 1
    else
      sb += c
      i += 1
  sb.toString
