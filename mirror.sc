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
//   scala-cli run mirror.sc -- --dry-run           # clone master + show what WOULD push (all mirrors)
//   scala-cli run mirror.sc                         # clone master + push --mirror to ALL mirrors
//   scala-cli run mirror.sc -- github               # only the named mirror: github | gitlab | coursegit
//   scala-cli run mirror.sc -- --dry-run github     # dry-run a single mirror
//   (pass one or more names to mirror just those - go one repo at a time as SSH keys land)
//   scala-cli run mirror.sc -- --root <abs> ...     # treat <abs> as the genscalator checkout (else cwd must be the root)
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

// ---- arg parsing: --dry-run, the --root <dir> value flag, and positional mirror names ----
def optVal(name: String): Option[String] =
  val i = args.indexOf(name); if i >= 0 && i + 1 < args.length then Some(args(i + 1)) else None
val valueFlags = Set("--root")               // flags that consume the next arg (so it is NOT read as a mirror name)
val positional =
  val b = scala.collection.mutable.ArrayBuffer[String]()
  var i = 0
  while i < args.length do
    if valueFlags.contains(args(i)) then i += 2
    else if args(i).startsWith("--") then i += 1
    else { b += args(i); i += 1 }
  b.toList

// The genscalator checkout to work in (default = current dir; override with --root <abs>).
// We clone into <root>/tmp/, so VERIFY the root really is a genscalator checkout - this
// refuses to run (and, below, refuses to rmrf) from an unexpected directory.
val root = optVal("--root").map(Paths.get(_)).getOrElse(Paths.get("")).toAbsolutePath.normalize
if !Files.exists(root.resolve("mirror.sc")) || !Files.isDirectory(root.resolve("tools")) then
  die(s"'$root' is not a genscalator checkout (expected mirror.sc + tools/ there); run from the repo root, or pass --root <abs>")

// All mirror targets (name -> SSH URL). Pass one or more NAMES as positional args to
// mirror ONLY those - so we can go one repo at a time as each host's SSH key gets set
// up. No name given = all of them.
val allMirrors = Seq(
  "github"    -> "git@github.com:bjornregnell/genscalator.git",
  "gitlab"    -> "git@gitlab.com:bjornregnell/genscalator.git",
  "coursegit" -> "git@coursegit.cs.lth.se:bjorn.regnell/genscalator.git",
)
val known     = allMirrors.map(_._1)
val selectors = positional
selectors.filterNot(known.contains).foreach(s => die(s"unknown mirror '$s' (known: ${known.mkString(", ")})"))
val mirrors   = if selectors.isEmpty then allMirrors else allMirrors.filter((n, _) => selectors.contains(n))

// Run a git command with the terminal inherited (progress + errors visible); no secret
// ever crosses the command line (SSH keys, not passwords), so nothing to redact.
def git(argv: String*): Int =
  val pb = new ProcessBuilder(("git" +: argv.toVector)*)
  pb.inheritIO()
  pb.start().waitFor()

// A fresh bare mirror clone of the master, in the gitignored <root>/tmp/ (in-repo, not /tmp).
val bare = root.resolve("tmp").resolve("genscalator-mirror.git")

def rmrf(p: Path): Unit =
  // Safety: only ever recursively delete OUR OWN bare-clone dir, never anything else.
  if p.getFileName == null || p.getFileName.toString != "genscalator-mirror.git" then
    die(s"refusing to recursively delete an unexpected path: $p")
  if Files.exists(p) then
    val all = Files.walk(p)
    try all.iterator.asScala.toList.reverse.foreach(Files.delete)   // children before parents
    finally all.close()

// ---- plan (printed up front, so the intent is visible even if a push fails on auth) ----
println(s"mirror: master (source of truth) = $master")
mirrors.foreach: (n, url) => 
  println(s"mirror:   -> $n  ($url)")

// ---- clone the master exactly ----
println(s"mirror: cloning master --mirror -> $bare")
rmrf(bare)
Files.createDirectories(bare.getParent)
if git("clone", "--mirror", master, bare.toString) != 0 then
  die(s"could not clone the master ($master) - is it reachable?")

// ---- push --mirror to each target ----
var failures = 0
for (name, url) <- mirrors do {  // the real action
  println(s"mirror: ${if dryRun then "DRY-RUN " else ""}push --mirror -> $name  ($url)")
  val cmd = (Seq("-C", bare.toString, "push", "--mirror") ++ (if dryRun then Seq("--dry-run") else Nil)) :+ url
  if git(cmd*) != 0 then
    failures += 1
    System.err.println(s"mirror: FAILED $name - is an SSH key set up for this host on blixten? (the deferred joint step)")
} 

if failures == 0 then
  println(s"mirror: done${if dryRun then " (dry-run: nothing pushed)" else s", ${mirrors.size} mirror(s) synced to the master"}.")
else
  die(s"$failures mirror(s) failed (see above); most likely missing SSH auth - the deferred joint step.")
