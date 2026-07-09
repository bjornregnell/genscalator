//> using scala 3.8.4

// =============================================================================
// mirror.sc  -  push a VERBATIM mirror of the genscalator master to all mirrors
// =============================================================================
// SM035 (thread: mirroring-for-EU-sovereignty).
//
// MASTER (EU-sovereign source of truth): https://codeberg.org/bjornregnell/genscalator
// Mirrored (verbatim, one-directional; master is authoritative, mirrors are
// disposable) to:
//   - github.com/bjornregnell/genscalator
//   - gitlab.com/bjornregnell/genscalator
//   - coursegit.cs.lth.se/bjorn.regnell/genscalator   (note: bjorn.regnell, with a dot)
//
// The "why this is a mirror" note lives in the MASTER README (a "Mirrors & EU
// sovereignty" section), so `git push --mirror` propagates it verbatim to every
// mirror - no per-mirror file, mirrors stay byte-identical to the master (design C).
//
// HOW IT WORKS: clone the master FRESH as a bare `--mirror` (so we replicate the
// master's exact refs, not blixten's local checkout), then `git push --mirror` that
// bare repo to each target. Reading the master is public (HTTPS); only the PUSH side
// needs auth.
//
// AUTH: SSH remote URLs. blixten needs an SSH key registered at each host
// (github.com / gitlab.com / coursegit.cs.lth.se). Until those keys exist, the pushes
// (and `--dry-run`, which still connects) will fail at the connect - that SSH setup is
// the deferred joint step.
//
// USAGE (from the genscalator root):
//   scala-cli run mirror.sc -- --dry-run   # clone the master + show what WOULD push
//   scala-cli run mirror.sc                # clone the master + push --mirror to all
//
// SAFETY: `git push --mirror` FORCE-updates each target to match the master and
// DELETES target refs not in the master. That is the intent (a verbatim mirror), so
// the mirrors are overwritten to match Codeberg on every run. One-directional only:
// this never pulls FROM a mirror.
// =============================================================================

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*

def die(msg: String): Nothing = { System.err.println(s"mirror: $msg"); sys.exit(2) }

val dryRun  = args.contains("--dry-run")
val master  = "https://codeberg.org/bjornregnell/genscalator.git"   // public read
val mirrors = Seq(
  "git@github.com:bjornregnell/genscalator.git",
  "git@gitlab.com:bjornregnell/genscalator.git",
  "git@coursegit.cs.lth.se:bjorn.regnell/genscalator.git",
)

// Run a git command with the terminal inherited (progress + errors visible); no secret
// ever crosses the command line (SSH keys, not passwords), so nothing to redact.
def git(argv: String*): Int =
  val pb = new ProcessBuilder(("git" +: argv.toVector)*)
  pb.inheritIO()
  pb.start().waitFor()

// A fresh bare mirror clone of the master, in the gitignored tmp/ (in-repo, not /tmp).
val bare = Paths.get("tmp", "genscalator-mirror.git").toAbsolutePath

def rmrf(p: Path): Unit =
  if Files.exists(p) then
    val all = Files.walk(p)
    try all.iterator.asScala.toList.reverse.foreach(Files.delete)   // children before parents
    finally all.close()

// ---- plan (printed up front, so the intent is visible even if a push fails on auth) ----
println(s"mirror: master (source of truth) = $master")
mirrors.foreach(m => println(s"mirror:   -> $m"))

// ---- clone the master exactly ----
println(s"mirror: cloning master --mirror -> $bare")
rmrf(bare)
Files.createDirectories(bare.getParent)
if git("clone", "--mirror", master, bare.toString) != 0 then
  die(s"could not clone the master ($master) - is it reachable?")

// ---- push --mirror to each target ----
var failures = 0
for m <- mirrors do
  println(s"mirror: ${if dryRun then "DRY-RUN " else ""}push --mirror -> $m")
  val cmd = (Seq("-C", bare.toString, "push", "--mirror") ++ (if dryRun then Seq("--dry-run") else Nil)) :+ m
  if git(cmd*) != 0 then
    failures += 1
    System.err.println(s"mirror: FAILED $m - is an SSH key set up for this host on blixten? (the deferred joint step)")

if failures == 0 then
  println(s"mirror: done${if dryRun then " (dry-run: nothing pushed)" else s", ${mirrors.size} mirror(s) synced to the master"}.")
else
  die(s"$failures mirror(s) failed (see above); most likely missing SSH auth - the deferred joint step.")
