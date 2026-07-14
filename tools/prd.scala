//> using scala 3.8.4
//> using jvm 21
//> using file lib.scala

// prd — read + navigate the genscalator PRD.md (PURE, read-only). Complements `tt parsereqt` (which
// parses + lints the reqT-lang); this one is for a human/agent who wants to SEE what the PRD says without
// re-emitting the whole file token-by-token. Three verbs:
//   tt prd show               print the whole PRD.md verbatim (like `tt doc`, but the repo-root PRD)
//   tt prd summarize          one-screen structural summary of the FUTURE part: each release's Feature/Goal
//                             Gists, one line each — EXTRACTED from the markdown, never LLM-generated
//   tt prd find <what>        find where <what> appears in the PRD (case-insensitive), with its section
//   tt prd --prd <file> ...   override the PRD path (default <tools>/../PRD.md, via -Dtt.tools)
// Design (echt): show is a trivial cat; summarize walks `## FUTURE`..`## PAST`, pairs each
// `* Feature|Goal: <id> has` with its nested `* Gist:` line under the current `### Release` header; find is a
// deterministic case-insensitive line scan tagged with the nearest heading. No LLM, feed-efficient. Ties SM065.
import java.nio.file.{Files, Path}
import agenttools.Lib

private val PrdHelp: String =
  """tt prd — read + navigate the genscalator PRD.md (pure, read-only)
    |
    |Complements `tt parsereqt` (parse + lint the reqT-lang); this is for SEEING what the PRD says without
    |re-typing the whole file. The summary is structurally EXTRACTED from the markdown, never LLM-generated.
    |
    |Usage:
    |  prd show               print the whole PRD.md verbatim
    |  prd summarize          one-screen summary of FUTURE: each release's Feature/Goal Gists, one line each
    |  prd find <what>        find where <what> appears (case-insensitive), tagged with its section heading
    |  prd --prd <file> ...   override the PRD path (default: <tools>/../PRD.md, via -Dtt.tools or cwd walk-up)
    |
    |Examples:
    |  tt prd summarize            # what is on the roadmap, gist by gist
    |  tt prd find allowlist       # every PRD line mentioning allowlist, with its heading
    |
    |Full reference: tools/README.md""".stripMargin

/** The FUTURE block: lines from `## FUTURE` up to (not including) the next top-level `## ` heading. */
private def futureBlock(lines: Vector[String]): Vector[String] =
  val start = lines.indexWhere(_.trim == "## FUTURE")
  if start < 0 then Vector.empty
  else
    val rest = lines.drop(start + 1)
    val end = rest.indexWhere(l => l.startsWith("## "))
    if end < 0 then rest else rest.take(end)

/** Extract (releaseHeader, id, gist) triples from the FUTURE block, in document order. A gist is the first
  * nested `* Gist:` bullet after a top-level `* Feature|Goal: <id> has`, before the next top-level `* `. */
private def futureGists(lines: Vector[String]): Vector[(String, String, String)] =
  val FeatureHas = """^\* (?:Feature|Goal): (\w+) has\s*$""".r
  val Gist = """^\s+\* Gist:\s*(.*)$""".r
  val Release = """^### (.*)$""".r
  var release = "Roadmap"
  var pending: Option[String] = None
  val out = Vector.newBuilder[(String, String, String)]
  for line <- futureBlock(lines) do
    line match
      case Release(h)     => release = h.trim; pending = None
      case FeatureHas(id) => pending = Some(id)
      case Gist(g) if pending.isDefined =>
        out += ((release, pending.get, g.trim)); pending = None
      case l if l.startsWith("* ") => pending = None // a sibling top-level bullet with no gist
      case _              => // keep scanning (indented non-gist lines don't reset the pending feature)
  out.result()

/** Nearest heading at or above index i (any level), for `find` context. */
private def headingAt(lines: Vector[String], i: Int): String =
  (i to 0 by -1).iterator.map(lines).find(_.matches("^#{1,6} .*"))
    .map(_.replaceAll("^#+\\s*", "").trim).getOrElse("(top)")

@main def prd(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(PrdHelp); sys.exit(0) }
  val a = args.toList
  val prdIdx = a.indexOf("--prd")
  val consumed = if prdIdx >= 0 then Set(prdIdx, prdIdx + 1) else Set.empty[Int]
  val prdPath: Path =
    (if prdIdx >= 0 && prdIdx + 1 < a.size then Some(Path.of(a(prdIdx + 1))) else None)
      .orElse(Lib.toolsDir().map(_.getParent.resolve("PRD.md")))
      .getOrElse:
        Console.err.println("prd: cannot locate PRD.md (pass --prd <file>, or set -Dtt.tools=<dir>)")
        sys.exit(2)
  if !Files.isRegularFile(prdPath) then
    Console.err.println(s"prd: no such PRD file: $prdPath")
    sys.exit(2)
  val lines = String(Files.readAllBytes(prdPath), "UTF-8").linesIterator.toVector
  val positionals = a.zipWithIndex.collect { case (t, i) if !consumed(i) && !t.startsWith("--") => t }
  positionals match
    case Nil =>
      Console.err.println("prd: usage: tt prd [ show | summarize | find <what> ]")
      sys.exit(2)
    case "show" :: _ =>
      lines.foreach(println)
    case "summarize" :: _ =>
      val gists = futureGists(lines)
      if gists.isEmpty then
        println("prd: no FUTURE Feature/Goal gists found (is the PRD's ## FUTURE section present?)")
      else
        println(s"FUTURE roadmap — ${gists.size} gists from $prdPath:")
        var lastRelease = ""
        for (release, id, gist) <- gists do
          if release != lastRelease then { println(""); println(s"### $release"); lastRelease = release }
          println(s"  $id — $gist")
    case "find" :: rest =>
      val what = rest.mkString(" ").trim
      if what.isEmpty then
        Console.err.println("prd: find needs a search term: tt prd find <what>")
        sys.exit(2)
      val needle = what.toLowerCase
      val hits = lines.zipWithIndex.filter { case (l, _) => l.toLowerCase.contains(needle) }
      if hits.isEmpty then
        println(s"prd: no line matches '$what'")
        sys.exit(1)
      else
        println(s"${hits.size} line(s) matching '$what' in $prdPath:")
        for (l, i) <- hits do
          println(s"  [${headingAt(lines, i)}] ${l.trim}")
    case other :: _ =>
      Console.err.println(s"prd: unknown verb '$other' (use: show | summarize | find <what>)")
      sys.exit(2)
