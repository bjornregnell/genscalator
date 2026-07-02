@main def probe(): Unit =
  println(scan("abcdefghijZ", true))
  println(scan("abcdefghijZ", false))
