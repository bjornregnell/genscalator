def scan(s: String, upper: Boolean): String = {
  val sb = StringBuilder()
  var i = 0
  while (i < s.length) {
    val c = s(i)
    if (c == 'a') sb ++= "A"
    else if (c == 'b') sb ++= "B"
    else sb += c
    i += 1
  }
  sb.toString
}
