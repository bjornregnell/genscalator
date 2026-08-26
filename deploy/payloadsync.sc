//> using file ../tools/project.scala
//> using file ../tools/payloadlib.scala

// =============================================================================
// payloadsync.sc  -  keep get-genscalator.sc's payload layout DERIVED, not typed
// =============================================================================
// Issue 040. The uninstaller's pre-manifest fallback needs to know which top-level entries a
// release payload contains. That list used to be a hand-written literal, and it had drifted in both
// directions at once: missing `reqts` (leftover, then blamed on the user) and claiming `skills`,
// `tools`, `plugins` (a user's own files walked and deleted). The list is now a GENERATED region in
// get-genscalator.sc, and this script is the generator.
//
// THE EFFECTFUL DRIVER for tools/payloadlib.scala, which is pure. Everything here is IO: read the
// workflow, read the script, in --write mode rewrite ONE marked region of it. Nothing else is
// touched, and --check (the default) writes nothing at all.
//
// THREE MODES
//   --check              regenerate from the workflow and diff against the committed region.
//                        Default, because a generator whose default writes is a footgun in CI.
//   --write              rewrite the committed region. The only writing path.
//   --staged <dir>       compare the committed region against a REAL staged tree's top-level
//                        entries. This is the release-time gate, and it is the strongest of the
//                        three: it cannot be fooled by a mistake in the workflow parser, because it
//                        never uses it. Run by native-release.yml right after staging.
//
// EXIT CODES: 0 agree / wrote, 2 drift or usage error. Nonzero on drift is the point — this runs as
// a gate, and a gate that reports drift on stdout with exit 0 is decoration.
//
// WHY A DEPLOY SCRIPT AND NOT A `tt` VERB. It would be a 47th verb whose three hand-maintained
// description carriers are exactly what issue 041 is about, added for a job no human runs
// interactively: it belongs to the release pipeline, next to buildnative.sc. The pure core is in
// tools/payloadlib.scala instead, so the toolbox's test suite covers it without a verb existing.

import java.nio.file.{Files, Path}
import agenttools.PayloadLib

def die(msg: String): Nothing =
  System.err.println(s"payloadsync: $msg")
  sys.exit(2)

val argv    = args.toList
val write   = argv.contains("--write")
val staged  = argv.sliding(2).collectFirst { case "--staged" :: d :: _ => Path.of(d) }
val root    = argv.sliding(2).collectFirst { case "--root" :: d :: _ => Path.of(d) }.getOrElse(Path.of("."))

val workflow = root.resolve(".github/workflows/native-release.yml")
val script   = root.resolve("get-genscalator.sc")
for p <- Vector(workflow, script) if !Files.isRegularFile(p) do
  die(s"not found: $p  (run from the repo root, or pass --root <dir>)")

val scriptText = Files.readString(script)
val committed  = PayloadLib.regionOf(scriptText).getOrElse(
  die(s"$script has no generated payload-layout region — expected a line reading:\n  ${PayloadLib.Begin}"))

staged match
  case Some(dir) =>
    // The release-time gate: the real tree, as built, versus what the shipped script will claim.
    if !Files.isDirectory(dir) then die(s"--staged $dir is not a directory")
    val actual = Files.list(dir).toArray.toVector
      .map(_.asInstanceOf[Path].getFileName.toString)
      .sorted
    // The TREE is the source of truth here and the script's list is what is being judged, so the tree
    // goes in the `expected` slot: an entry staged but not claimed is a LEFTOVER, and one claimed but
    // not staged is an OVER-CLEAN. Passing these the other way round reverses both labels while still
    // failing the gate, which is the kind of correct-exit-code-wrong-diagnosis that wastes a release.
    PayloadLib.drift(s"get-genscalator.sc, against the staged tree $dir", actual, PayloadLib.entriesOf(committed)) match
      case Some(report) =>
        System.err.println(report)
        die("the shipped uninstaller would not match the payload this release stages")
      case None =>
        println(s"payload layout OK: ${actual.mkString(", ")}  (staged tree == get-genscalator.sc)")

  case None =>
    val entries    = PayloadLib.stagedTopLevel(Files.readString(workflow))
    if entries.isEmpty then die(s"no staging/<entry> references found in $workflow — refusing to generate an empty layout")
    val regenerated = PayloadLib.render(entries)
    if regenerated == committed then
      println(s"payload layout OK: ${entries.mkString(", ")}  (committed region == regeneration)")
    else if write then
      PayloadLib.splice(scriptText, regenerated) match
        case Left(err)  => die(err)
        case Right(out) =>
          Files.writeString(script, out)
          println(s"payload layout WRITTEN to $script: ${entries.mkString(", ")}")
          println("  review the diff before committing — this changes what --uninstall removes")
    else
      System.err.println("the committed payload layout is not what the staging step produces.")
      System.err.println("--- committed ---")
      System.err.println(committed)
      System.err.println("--- regenerated ---")
      System.err.println(regenerated)
      die("run with --write to regenerate")
