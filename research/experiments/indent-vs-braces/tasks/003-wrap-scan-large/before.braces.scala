def scan(s: String, upper: Boolean): String = {
  val sb = StringBuilder()
  var i = 0
  while (i < s.length) {
    val c = s(i)
    if (c == 'a') sb ++= "A"
    else if (c == 'b') sb ++= "B"
    else if (c == 'c') sb ++= "C"
    else if (c == 'd') sb ++= "D"
    else if (c == 'e') sb ++= "E"
    else if (c == 'f') sb ++= "F"
    else if (c == 'g') sb ++= "G"
    else if (c == 'h') sb ++= "H"
    else if (c == 'i') sb ++= "I"
    else if (c == 'j') sb ++= "J"
    else sb += c
    i += 1
  }
  sb.toString
}
