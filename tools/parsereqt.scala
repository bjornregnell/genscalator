//> using scala 3.8.4
//> using jvm 21
//> using file reqt-vendored

// tt parsereqt — parse / lint reqT-lang requirements (e.g. this repo's PRD.md).
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
  def lint(m: Model): List[String] =
    def walk(elems: Vector[Elem]): Vector[String] =
      elems.flatMap:
        case StrAttr(Text, v) if conceptLike.findFirstIn(v.trim).isDefined =>
          Vector(s"unknown concept '${v.trim.takeWhile(_ != ':')}' kept as Text: ${v.trim.take(70)}")
        case StrAttr(Text, v) if relLike.findFirstIn(v.trim).isDefined =>
          Vector(s"relation '${v.trim.takeWhile(_ != ':')}' LOST to Text — write it as a top-level 'ENT ${v.trim.takeWhile(_ != ':')} ENT' clause, not under has: ${v.trim.take(70)}")
        case Rel(_, _, sub) => walk(sub.elems)
        case _ => Vector.empty
    walk(m.elems).toList

  def dispatch(args: String*): Unit =
    args.toList match
      case "parse" :: path :: _ =>
        val m = MarkdownParser.parseModel(readFile(path))
        println(m)
        println(s"reqt parse: ${m.elems.size} top-level elems in $path")
      case "lint" :: path :: _ =>
        val m = MarkdownParser.parseModel(readFile(path))
        val ws = lint(m)
        ws.foreach(w => println(s"  [lint] $w"))
        println(s"reqt lint: ${ws.size} unknown-concept fall-through(s) in $path  (real Swedish? typo? un-mapped term?)")
      case _ =>
        println("usage: tt parsereqt parse FILE | tt parsereqt lint FILE")
        sys.exit(2)
}

@main def requirementsMarkdownParser(args: String*): Unit = ParseReqt.dispatch(args*)
