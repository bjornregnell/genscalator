//> using scala 3.8.4
//> using jvm 21

// Shared helpers for the Scala agent toolbox. PURE (no I/O effects except the explicitly-named file
// readers). Project-agnostic — destined for an open Codeberg repo. Uses only the JDK (no deps) so pure
// text tools compile fast. Effectful drivers (running sbt/pdflatex) live in separate files that add os-lib.
package agenttools

object Lib:
  // --- file readers (the only I/O here; named so impurity is explicit) ---
  /** Read a file lenient as Latin-1: every byte maps to a char, so it never throws on non-UTF-8 logs
    * (LaTeX logs aren't valid UTF-8). ASCII pattern matching ("! ", etc.) is unaffected. */
  def readLatin1(path: String): String =
    String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)), "ISO-8859-1")

  /** Read a file as UTF-8 (for prose/source where åäö etc. matter). */
  def readUtf8(path: String): String =
    String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)), "UTF-8")

  // --- pure formatting/aggregation ---
  /** Frequency map → sorted bar histogram (descending), top N. */
  def histogram(counts: Map[String, Int], top: Int = 40): String =
    val sorted = counts.toVector.sortBy(-_._2).take(top)
    val w = sorted.map(_._1.length).maxOption.getOrElse(0)
    sorted.map((k, c) => s"  ${k.padTo(w, ' ')} ${"%6d".format(c)} ${"#" * math.min(50, c)}").mkString("\n")

  /** Levenshtein-ish: true iff a and b differ by exactly one edit (sub/ins/del). */
  def edit1(a: String, b: String): Boolean =
    if math.abs(a.length - b.length) > 1 then false
    else if a.length == b.length then a.zip(b).count(_ != _) == 1
    else
      val (s, l) = if a.length < b.length then (a, b) else (b, a)
      var i = 0; var j = 0; var diff = 0
      while i < s.length && diff <= 1 do
        if s(i) == l(j) then { i += 1; j += 1 } else { diff += 1; j += 1 }
      diff + (l.length - j) <= 1
