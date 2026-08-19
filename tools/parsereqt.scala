//> using file project.scala
//> using jvm 21
//> using file reqt-vendored

// tt parsereqt — parse / lint reqT-lang requirements (e.g. this repo's reqts/PRD.md).
//
// WORKING MODEL (BR 2026-07-01): reqT-lang is used by the reqT desktop tool, so changing it cascades
// release + docs work over there. So we IN-SOURCE it: `reqt-vendored/` is a PRISTINE copy of reqT-lang's
// `src/main/scala` (a clean base for a future, verified upstream PR). We iterate HERE as a tt tool, file
// ISSUES upstream (free — no release work; parser feedback = reqT/reqT-lang#15), and only later propose
// verified PRs. The strict/lint check below is a WRAPPER over the parser (we do NOT fork the parser logic),
// so the vendored copy stays diff-clean; the NATIVE in-parser strict mode is what issue #15 proposes to reqT.
// See research/015-reqt-lang-review.md.
import reqt.*

// Helpers (readFile/lint + the concept/relation patterns) scoped in this object so their generic names
// (readFile/lint) don't collide with other tools when the toolbox compiles together. Only the @main entry is
// top-level. See skills/scala-style.
object ParseReqt {
  def readFile(p: String): String =
    val src = scala.io.Source.fromFile(p)
    try src.mkString finally src.close()

  /** Wrapper strict/lint: a bullet whose leading `Word:` is not a known concept falls through to a Text attr
    * (parser ~line 188). We flag two kinds of silent fall-through so they surface instead of vanishing:
    *   (1) CONCEPT-like `Capitalized: ...` — typos (`Feautre:`) and un-mapped terms (`BadGoal:`);
    *   (2) a lowercase RELATION keyword written as a bullet (`requires: ...`, `verifies: ...`) — this happens
    *       when a relation is listed UNDER a `has` block instead of as its own top-level `ENT REL` clause; the
    *       relation is then LOST to Text. The trailing `:` requirement keeps prose like "is a…"/"has many…"
    *       from false-tripping. Both cases are evidence for the strict-mode ask in reqT/reqT-lang#15. */
  val conceptLike = "^[A-Z][A-Za-z0-9]*:".r
  val relLike = "^(binds|deprecates|excludes|has|helps|hurts|impacts|implements|interactsWith|is|precedes|relatesTo|requires|verifies):".r

  /** A lint hit, carrying the offending text alongside the message so the caller can decide whether it
    * came from somewhere the lint has no business judging (see `fencedLines`). */
  final case class Finding(text: String, message: String)

  def findings(m: Model): List[Finding] =
    def walk(elems: Vector[Elem]): Vector[Finding] =
      elems.flatMap:
        case StrAttr(Text, v) if conceptLike.findFirstIn(v.trim).isDefined =>
          Vector(Finding(v.trim, s"unknown concept '${v.trim.takeWhile(_ != ':')}' kept as Text: ${v.trim.take(70)}"))
        case StrAttr(Text, v) if relLike.findFirstIn(v.trim).isDefined =>
          Vector(Finding(v.trim, s"relation '${v.trim.takeWhile(_ != ':')}' LOST to Text — write it as a top-level 'ENT ${v.trim.takeWhile(_ != ':')} ENT' clause, not under has: ${v.trim.take(70)}"))
        case Rel(_, _, sub) => walk(sub.elems)
        case _ => Vector.empty
    walk(m.elems).toList

  def lint(m: Model): List[String] = findings(m).map(_.message)

  /** ISSUE 010. A grammar illustration is metasyntax, not requirements: `ENT: id` is a PLACEHOLDER, and
    * flagging it as an unknown concept is a false positive that can never be fixed by editing the model.
    * A check that reports the same 5 hits forever teaches its reader to ignore the number, which costs
    * more than the check is worth. So the lint skips what lives inside a fenced code block — the one
    * marker that already means "this is a specimen, not content".
    *
    * Deliberately LINT-ONLY: the parser is vendored pristine (see the header) and its handling of fences
    * is untouched. Fenced bullets still parse into the model exactly as before; they are merely not
    * judged. That keeps `reqt-vendored/` diff-clean and the skip auditable in one place.
    *
    * Strip list markers and surrounding space, because the parser keeps the bullet's CONTENT as the Text
    * value while the source line still carries its `* ` marker and indentation. */
  def normalizeBullet(s: String): String =
    val t = s.trim
    val stripped =
      if t.length > 1 && "-*+".contains(t.charAt(0)) && t.charAt(1) == ' ' then t.substring(2) else t
    stripped.trim

  /** Pure: the normalized content lines sitting INSIDE fenced blocks (the fence markers themselves are
    * not content). Tracks which marker opened the block so a ``` inside a ~~~ block reads as content,
    * and allows the up-to-3-space indent CommonMark permits before a fence. */
  def fencedLines(src: String): Set[String] =
    def marker(line: String): Option[String] =
      val t = line.dropWhile(_ == ' ')
      if line.length - t.length > 3 then None
      else if t.startsWith("```") then Some("`")
      else if t.startsWith("~~~") then Some("~")
      else None
    val (inside, _) =
      src.linesIterator.foldLeft((Set.empty[String], Option.empty[String])):
        case ((set, open), line) =>
          (open, marker(line)) match
            case (None, Some(m))              => (set, Some(m)) // opening fence
            case (Some(o), Some(m)) if o == m => (set, None)    // matching closing fence
            case (Some(_), _)                 => (set + normalizeBullet(line), open) // fenced content
            case (None, None)                 => (set, None)
    inside - ""

  /** Pure: drop the findings whose offending bullet is a line inside a fenced block. */
  def dropFenced(fs: List[Finding], fenced: Set[String]): List[Finding] =
    fs.filterNot(f => fenced.contains(normalizeBullet(f.text)))

  private val Help: String =
    """tt parsereqt — parse / lint reqT-lang requirements models (e.g. this repo's reqts/PRD.md)
      |
      |Parses a Markdown file written in reqT-lang into a structured requirements model, or lints it
      |for silent fall-throughs where a mistyped concept or a misplaced relation is kept as plain Text.
      |
      |Usage:
      |  parsereqt parse FILE            parse FILE and print the structured model + a summary line
      |  parsereqt lint FILE             flag bullets that silently fell through to a Text attribute:
      |                                  (1) a Capitalized 'Word:' that is not a known reqT concept
      |                                      (a typo like 'Feautre:' or an un-mapped term like
      |                                      'BadGoal:')
      |                                  (2) a lowercase relation keyword written as a bullet under
      |                                      a 'has' block ('requires: ...') — the relation is LOST;
      |                                      write it as a top-level 'ENT requires ENT' clause
      |
      |Fenced code blocks are SKIPPED by the lint: a grammar illustration ('ENT: id') is
      |metasyntax, not a mistake, and a check that reports the same hits forever teaches its
      |reader to ignore the number. The skipped count is printed, never swallowed. This is a
      |lint-only rule — the parser's handling of fences is unchanged.
      |
      |Examples:
      |  tt parsereqt parse reqts/PRD.md # print the parsed model, then a top-level elem count
      |  tt parsereqt lint reqts/PRD.md  # list fall-throughs (real Swedish? typo? un-mapped term?)
      |
      |Full reference: tools/README.md""".stripMargin

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then
      println(Help)
      sys.exit(0)
    args.toList match
      case "parse" :: path :: _ =>
        val m = MarkdownParser.parseModel(readFile(path))
        println(m)
        println(s"reqt parse: ${m.elems.size} top-level elems in $path")
      case "lint" :: path :: _ =>
        val src = readFile(path)
        val m = MarkdownParser.parseModel(src)
        val all = findings(m)
        val kept = dropFenced(all, fencedLines(src))
        val skipped = all.size - kept.size
        kept.foreach(f => println(s"  [lint] ${f.message}"))
        // the skipped count is PRINTED rather than swallowed: a silent filter is how a lint starts lying
        val note = if skipped > 0 then s" ($skipped skipped inside fenced code block(s))" else ""
        println(s"reqt lint: ${kept.size} unknown-concept fall-through(s) in $path$note  (real Swedish? typo? un-mapped term?)")
      case _ =>
        println("usage: tt parsereqt parse FILE | tt parsereqt lint FILE")
        sys.exit(2)
}

@main def requirementsMarkdownParser(args: String*): Unit = ParseReqt.dispatch(args*)
