// (no version include: mainless helper — inherits it from its includer; see project.scala)
//
// ⚠ NO `//> using dep`: JDK-only, like lib.scala. Pure — read a string, compute a list, render a
// string. Every effect (reading the workflow, rewriting the script) lives in deploy/payloadsync.sc.

// payloadlib — the ONE definition of which TOP-LEVEL entries a native release payload contains.
//
// WHY THIS FILE EXISTS (issue 040). `get-genscalator.sc`'s pre-manifest uninstall fallback held a
// hand-written copy of the payload layout, and it had drifted in BOTH directions:
//   - it MISSED `reqts`, so `reqts/PRD.md` survived an uninstall that then printed `kept:` and
//     blamed the user for a file the installer had written one command earlier;
//   - it CLAIMED `skills`, `tools` and `plugins`, which the staging step has never staged and the
//     installer never creates — and the fallback WALKS a listed directory and takes every regular
//     file under it, so a user's own `~/.genscalator/tools/` was destroyed two lines below a printed
//     promise that it would not be touched.
// The over-clean was reproduced on Linux 2026-08-26 (all three surplus entries took a user file);
// the leftover was reproduced on Windows 2026-08-21 (report 088). It is one stale literal with two
// opposite failure modes, which is why the fix is structural rather than a corrected list.
//
// DERIVE, NOT ASSERT — BR's decision on issue 040, 2026-08-25. The script's vector is a GENERATED
// region and the staging step is its source, so a new payload directory cannot be forgotten, only
// regenerated. Two independent gates keep that honest:
//   1. `PayloadLayoutSuite` regenerates from the workflow and diffs the committed region.
//   2. `native-release.yml` compares the committed region to the REAL `staging/` tree it just built.
// Gate 2 is why the parse below is allowed to be NARROW. A parser is a carrier too, and a clever one
// would just be a fourth place the layout is described; this one reads `staging/<entry>` references
// out of the workflow and nothing else. When it is wrong, gate 2 fails a RELEASE, loudly, at the one
// moment the actual payload exists on disk — which is a better failure than a subtle parse that
// keeps a wrong list looking generated.
//
// The `.sorted` is load-bearing for review, not for behaviour: a generated line whose order is
// stable produces an empty diff when nothing changed, so a regeneration is readable as a diff.
package agenttools

object PayloadLib:

  /** One top-level entry of the unpacked payload: a directory name (`bin`) or a file (`VERSION.txt`). */
  type Entry = String

  val Begin = "// >>> GENERATED payload layout — do not edit by hand >>>"
  val End   = "// <<< END GENERATED payload layout <<<"

  /** Top-level `staging/` entries the release workflow builds, in sorted order.
    *
    * Comments are stripped first: a `#` line mentioning `staging/whatever` is prose, and letting it
    * vote would make the generated list depend on how the workflow is documented. */
  def stagedTopLevel(workflowYaml: String): Vector[Entry] =
    val staged = """staging/([A-Za-z0-9._-]+)""".r
    workflowYaml.linesIterator
      .map(stripComment)
      .flatMap(line => staged.findAllMatchIn(line).map(_.group(1)))
      .toVector
      .distinct
      .sorted

  private def stripComment(line: String): String =
    line.indexOf('#') match
      case -1 => line
      case i  => line.substring(0, i)

  /** The generated block, markers included, exactly as it appears in `get-genscalator.sc`. */
  def render(entries: Vector[Entry]): String =
    val items = entries.map(e => s"\"$e\"").mkString(", ")
    s"""$Begin
       |// Source: .github/workflows/native-release.yml's staging step (issue 040). Regenerate with
       |//   scala-cli run deploy/payloadsync.sc -- --write
       |// Asserted two ways: against a regeneration by PayloadLayoutSuite, and against the real
       |// staging/ tree by native-release.yml. Editing this by hand re-creates the drift.
       |val PayloadTopLevel = Vector($items)
       |$End""".stripMargin

  /** The block currently committed in `script`, markers included. */
  def regionOf(script: String): Option[String] =
    val lines = script.linesIterator.toVector
    val b     = lines.indexWhere(_.trim == Begin)
    val e     = lines.indexWhere(_.trim == End)
    if b < 0 || e < b then None else Some(lines.slice(b, e + 1).mkString("\n"))

  /** Replace the committed block with `block`, or say why it cannot be found. */
  def splice(script: String, block: String): Either[String, String] =
    val lines = script.linesIterator.toVector
    val b     = lines.indexWhere(_.trim == Begin)
    val e     = lines.indexWhere(_.trim == End)
    if b < 0 then Left(s"no generated region: expected a line reading `$Begin`")
    else if e < b then Left(s"unterminated generated region: `$End` is missing after line ${b + 1}")
    else
      val out = lines.take(b) ++ block.linesIterator ++ lines.drop(e + 1)
      Right(out.mkString("", "\n", if script.endsWith("\n") then "\n" else ""))

  /** The entries a committed block declares, read back out of its `Vector(...)` line.
    *
    * Deliberately NOT `stagedTopLevel` applied to the script: the release-time gate compares the
    * script to the staged TREE, and routing that comparison through the workflow parser again would
    * make one parser answer for both sides of its own check. */
  def entriesOf(block: String): Vector[Entry] =
    val quoted = """"([^"]+)"""".r
    block.linesIterator
      .find(_.contains("val PayloadTopLevel"))
      .toVector
      .flatMap(line => quoted.findAllMatchIn(line).map(_.group(1)))

  /** `None` when the two agree; otherwise a human-readable account of the drift. */
  def drift(label: String, expected: Vector[Entry], actual: Vector[Entry]): Option[String] =
    if expected == actual then None
    else
      val missing = expected.filterNot(actual.contains)
      val surplus = actual.filterNot(expected.contains)
      Some(
        Vector(
          s"$label disagrees with the generated payload layout:",
          s"  expected: ${expected.mkString(", ")}",
          s"  actual:   ${actual.mkString(", ")}",
        ).++(
          Option.when(missing.nonEmpty)(s"  MISSING (would be left behind): ${missing.mkString(", ")}")
        ).++(
          Option.when(surplus.nonEmpty)(s"  SURPLUS (would be over-cleaned): ${surplus.mkString(", ")}")
        ).mkString("\n")
      )
