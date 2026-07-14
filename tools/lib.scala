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

  // --- toolbox location ---
  /** Locate the tools dir (cwd-independent): the -Dtt.tools property the `tt` launcher passes, else walk up
    * from the cwd for a `tools/tt`. The ONE shared definition — doc / prd / skillcheck / skillgrants all use
    * this (previously each had an identical top-level copy, which collided at a whole-`tools` compile). */
  def toolsDir(): Option[java.nio.file.Path] =
    import java.nio.file.{Files, Path}
    sys.props.get("tt.tools").map(Path.of(_)).filter(d => Files.exists(d.resolve("tt")))
      .orElse:
        Iterator.iterate(Path.of("").toAbsolutePath)(p => p.getParent).takeWhile(_ != null).take(8)
          .find(d => Files.exists(d.resolve("tools").resolve("tt"))).map(_.resolve("tools"))

  // --- JSON ---
  /** Encode a string as a JSON string literal, quotes included, per RFC 8259. Pure, dependency-free.
    * Escapes the mandatory set (" \ and the C0 controls via \b \f \n \r \t or \uXXXX); passes other
    * characters (incl. UTF-8 åäö) through unchanged. Use to emit valid JSON from Scala tools without jq
    * (e.g. hook additionalContext) — in-process, so hot-path callers pay no external-process cost. */
  def jsonStr(s: String): String =
    val sb = StringBuilder("\"")
    s.foreach:
      case '"'  => sb ++= "\\\""
      case '\\' => sb ++= "\\\\"
      case '\b' => sb ++= "\\b"
      case '\f' => sb ++= "\\f"
      case '\n' => sb ++= "\\n"
      case '\r' => sb ++= "\\r"
      case '\t' => sb ++= "\\t"
      case c if c < 0x20 => sb ++= f"\\u${c.toInt}%04x"
      case c    => sb += c
    (sb += '"').toString

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
