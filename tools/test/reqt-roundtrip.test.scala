//> using file ../project.scala
//> using file ../reqt-vendored
//> using dep org.scalameta::munit::1.3.4
//> using dep com.lihaoyi::os-lib:0.11.8

// ISSUE 005 — reqT-lang's markdown round trip is not idempotent, and each pass destroys more.
//
// This file is the "failing property test" the issue names as the best FIRST move, because it is the one
// step with ZERO downstream cascade: it changes no behaviour, nothing under the reqT organisation depends
// on it, and it converts a claim in an issue text into something the build states out loud.
//
// The idempotence property is marked `.fail`, which in munit means "this body is EXPECTED to throw". So:
//   * while the defect exists      -> the property throws  -> the test PASSES -> the suite stays green
//   * when reqT-lang is fixed      -> the property holds   -> the test FAILS  -> loudly, with a message
//                                                                                telling you to promote it
// That is deliberate. A genuinely red test would break CI for a defect we are not fixing here (the fix
// lives upstream and must be timed with a reqT desktop release); a silent TODO would rot. This states the
// defect, in the build, at zero cost, and cannot be forgotten.
//
// See reqts/issues/open/issue-005-reqt-markdown-round-trip-degrades.md for the measured table and for the
// cascade ordering that makes this item 1 of 3.

import reqt.*

class ReqtRoundTripSuite extends munit.FunSuite:

  // Deliberately restates the locate logic (test independence over DRY, scala-style §5).
  private lazy val root: os.Path =
    sys.props.get("tt.tools").map(os.Path(_)).filter(d => os.exists(d / "tt")).map(_ / os.up).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(8)
        .find(d => os.exists(d / "tools" / "tt"))
        .getOrElse(throw IllegalStateException(s"cannot locate the repo root (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))

  /** The normaliser under test: parse, then render back. A formatter is only safe to re-run if this
    * reaches a FIXED POINT — identity (`norm(x) == x`) is the stronger property and is not claimed here. */
  private def norm(s: String): String = MarkdownParser.parseModel(s).toMarkdown

  private lazy val reqtFiles: Vector[os.Path] =
    os.list(root / "reqts").filter(p => os.isFile(p) && p.last.endsWith(".md")).toVector.sortBy(_.last)

  test("the reqts/ corpus this property is about is actually there") {
    assert(reqtFiles.nonEmpty, s"no .md files under ${root / "reqts"}")
  }

  // ---- The acceptance property, expected to fail until prose spans survive a round trip ----------

  test("PROPERTY (issue 005 acceptance): norm(norm(x)) == norm(x) for every file in reqts/".fail) {
    val offenders = reqtFiles.filter: p =>
      val once = norm(os.read(p))
      norm(once) != once
    assertEquals(
      offenders.map(_.last),
      Vector.empty[String],
      "reqT-lang's markdown round trip has reached a fixed point for these files.\n" +
        "  If this test FAILED, the defect may be FIXED: drop the `.fail` marker, re-measure, and\n" +
        "  update reqts/issues/open/issue-005-*.md (it is an upstream candidate — see its cascade order).",
    )
  }

  // ---- Characterisation: WHERE the corpus drifts, MEASURED rather than guessed -------------------
  //
  // ⚠ An earlier version of this test asserted a hand-built specimen — "a bold span does not survive a
  // round trip" — and it FAILED: `* **agent capabilities** are the point` round-trips intact. The
  // emphasis-eating in issue 005's table is real but CONTEXT-DEPENDENT, and a minimal specimen that
  // reproduces it has not been isolated. So this test no longer invents one. It reports the first place
  // the REAL corpus drifts, which is evidence a maintainer can act on and cannot be wrong about the
  // mechanism, because it does not claim one. Isolating the trigger belongs to issue 005 item 3, where
  // the actual fix lives.

  test("CHARACTERISATION: the corpus is not at a fixed point, and here is where it first drifts") {
    val drift = reqtFiles.flatMap: p =>
      val once  = norm(os.read(p))
      val twice = norm(once)
      if once == twice then None
      else
        val a = once.linesIterator.toVector
        val b = twice.linesIterator.toVector
        val i = a.indices.find(j => j >= b.size || a(j) != b(j)).getOrElse(a.size)
        Some((p.last, i, a.lift(i).getOrElse("<absent>"), b.lift(i).getOrElse("<absent>")))
    assert(
      drift.nonEmpty,
      "every file in reqts/ round-trips to a fixed point — issue 005 may be FIXED. Re-measure, then drop\n" +
        "  the `.fail` marker on the property above and update the issue.",
    )
    // Printed, not asserted: the exact bytes are the evidence half of the report, and pinning them would
    // make an ordinary PRD edit fail the suite for no gain.
    val (file, line, before, after) = drift.head
    println(s"[issue 005] first drift in $file at rendered line $line of pass 1:")
    println(s"  pass 1: ${before.take(140)}")
    println(s"  pass 2: ${after.take(140)}")
    println(s"[issue 005] files that do not reach a fixed point: ${drift.map(_._1).mkString(", ")}")
  }

  // NOTE for whoever fixes this: the issue's table shows `**agent capabilities**` losing one marker per
  // pass in reqts/PRD.md, but the same construct in isolation does NOT degrade (measured 2026-08-19).
  // So the trigger is contextual — surrounding prose, line length, or a second emphasis span on the same
  // line — and the issue text should not be read as claiming every bold span degrades.
  //
  // The issue's second acceptance criterion — "prose containing no reqT construct survives a round trip
  // BYTE FOR BYTE" — is deliberately NOT asserted here. It is a strictly stronger property than
  // idempotence, it is equally broken today, and pinning two red properties adds no information over
  // pinning one. It lands as its own test the day the fix above goes in, since preserving the original
  // span (rather than perfecting the prose renderer) is the cheaper route to both at once.
