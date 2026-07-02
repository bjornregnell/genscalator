@main def probe(): Unit =
  println(scan("abcdeZ", true))
  println(scan("abcdeZ", false))
