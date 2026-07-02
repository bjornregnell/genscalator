@main def probe(): Unit =
  println(scan("abXab", true))
  println(scan("abXab", false))
