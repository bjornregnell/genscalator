//> using file ../tools/statusline.scala
//> using file ../tools/project.scala
//> using file ../tools/minijson.scala
//> using file ../tools/boxstats.scala
//> using file ../tools/limitstore.scala
//> using file ../tools/sessionstore.scala

// previewstatusline — render the statusline's LINE 2 (the mode line) from source, without
// touching the live session, the native binary, or your own store.
//
// WHY IT EXISTS: line 2 is styled with SGR escapes, so "is the grammar right?" cannot be answered
// by reading the code — the labels are plain and the values are INVERTED, and you have to SEE it.
// Before this script the only ways to look were to rebuild the native binary (~2 minutes) or to
// edit tools/*.scala and let the live status line re-render, and that second one is a trap: the
// status line is a LIVE CONSUMER of these sources, so a file that does not compile blanks the
// human's line while you work (it happened on 2026-09-11, and it took seven CliSuite statusline
// tests down with it, because CliSuite spawns each tool as its own scala-cli subprocess).
//
// ⚠ THE INCLUDES ARE LISTED FLAT ON PURPOSE. `using file` does NOT chain, so naming only
// statusline.scala silently drops everything statusline.scala itself includes and the build fails
// with "Not found: MiniJson" and friends (the single-file-fallback gotcha, tools/project.scala:10-14).
//
// ⚠ NO `@main` HERE: scala-cli rejects it in a `.sc` script ("use .scala format instead"), and a
// `.scala` file would collide with statusline.scala's own `@main`. Top-level script code with the
// implicit `args`, exactly like payloadsync.sc.
//
// Usage (args all optional: <dir> <stamp> <subject>):
//   scala-cli run deploy/previewstatusline.sc
//   scala-cli run deploy/previewstatusline.sc -- introprog sep11@0930
//   scala-cli run deploy/previewstatusline.sc -- gs sep11@0930 "alpha prep"

import StatuslineTool.*

val chips = Vector("ColdStart", "TokSaving")

def show(caption: String, line: String): Unit =
  println()
  println(s"  $caption")
  println("  " + line)

println()
args.toList match
  case Nil =>
    show("no subject — the normal case, since the name is derived (issue 056):",
      renderModes(chips, Some(SessionLead("genscalator-work", "sep11@1408"))))
    show("with a subject set by `tt session alpha prep`:",
      renderModes(chips, Some(SessionLead("genscalator-work", "sep11@1408", Some("alpha prep")))))
    show("outside a harness session (no session id):", renderModes(chips))
    println()
    println("  labels render PLAIN, values INVERTED — pass <dir> <stamp> [subject] to try your own.")
  case dir :: rest =>
    val stamp   = rest.headOption.getOrElse("sep11@1408")
    val subject = rest.drop(1).headOption
    show(s"dir=$dir stamp=$stamp subject=${subject.getOrElse("(none)")}:",
      renderModes(chips, Some(SessionLead(dir, stamp, subject))))
println()
