//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8
//> using file lib.scala

// update — check whether the installed genscalator is BEHIND its git marketplace remote, and SUGGEST the
// manual update steps. Two facts from Anthropic's docs shape this (see SM-K / blog 026):
//   (1) a third-party plugin marketplace does NOT auto-update by default, and there is no per-plugin update
//       command — the human runs `/plugin marketplace update` + `/reload-plugins`; a tool cannot run those.
//   (2) plugin authors get no programmatic update-check / notify API — but genscalator IS a git repo, so git
//       is our mechanism: fetch the remote-tracking refs and compare.
// So this tool CHECKS (read-only: fetch never touches the working tree) and TELLS THE HUMAN what to do. It
// degrades gracefully when offline, when there is no upstream, or when genscalator is not a git checkout.
//   tt update [--repo <dir>] [--brief]
//     --brief   speak ONLY when a newer release is available (silent otherwise) — for `gs warm` to call
//               behind a throttle, so warm gains update-awareness without becoming chatty.

import agenttools.Lib
import scala.util.Try

object Update:
  // The steps the human runs in Claude Code (the tool cannot drive the harness itself).
  private val ManualSteps =
    """  To update, run these in Claude Code:
      |    /plugin marketplace update genscalator
      |    /reload-plugins
      |  (Third-party marketplaces do not auto-update by default, so this is a manual step.)""".stripMargin

  private def git(repo: os.Path, args: String*): (Int, String) =
    Try(os.proc(("git" +: "-C" +: repo.toString +: args)).call(
      check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 120_000)) match
      case scala.util.Success(res) => (res.exitCode, (res.out.text() + res.err.text()).trim)
      case scala.util.Failure(e)   => (255, e.getMessage)

  /** Parse `git rev-list --left-right --count HEAD...@{u}` ("<ahead>\t<behind>") into (ahead, behind).
    * Pure, so it is unit-tested; defaults to (0, 0) on any unexpected shape. */
  def parseAheadBehind(counts: String): (ahead: Int, behind: Int) =
    counts.split("\\s+").filter(_.nonEmpty).toList match
      case a :: b :: _ => (a.toIntOption.getOrElse(0), b.toIntOption.getOrElse(0))
      case _           => (0, 0)

  /** Locate the genscalator repo root: an explicit `--repo`, else the parent of the tools dir. */
  private def resolveRepo(args: List[String]): Option[os.Path] =
    args match
      case "--repo" :: v :: _ => Some(os.Path(v, os.pwd))
      case _                  => Lib.toolsDir().map(td => os.Path(td.toAbsolutePath.normalize) / os.up)

  def dispatch(args: List[String]): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    val brief = args.contains("--brief")
    // In --brief mode, only an actionable "you are behind" notice is printed; everything else stays silent,
    // so `gs warm` can call this without nagging when up to date, offline, or not a git checkout.
    def say(s: String): Unit = if !brief then println(s)

    val repo = resolveRepo(args).getOrElse:
      say("update: could not locate the genscalator repo — pass --repo <dir>.")
      sys.exit(0) // informational check, never a hard error

    // Graceful degrade: not a git checkout -> cannot self-check; still tell the human how to update.
    val isGit = os.exists(repo / ".git") || git(repo, "rev-parse", "--git-dir")._1 == 0
    if !isGit then
      say(s"genscalator at $repo is not a git checkout, so the version cannot be self-checked.")
      say(ManualSteps)
      sys.exit(0)

    say(s"genscalator: $repo")
    val version = git(repo, "describe", "--tags", "--always", "--dirty")._2
    if version.nonEmpty then say(s"  installed: $version")

    // Fetch remote-tracking refs (read-only; never the working tree).
    val (fc, fout) = git(repo, "fetch", "--quiet")
    if fc != 0 then
      say(s"  could not fetch the remote (offline?): ${fout.take(200)}")
      say(ManualSteps)
      sys.exit(0)

    val (uc, upstream) = git(repo, "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}")
    if uc != 0 then
      say("  the current branch has no upstream to compare against.")
      say(ManualSteps)
      sys.exit(0)

    val (rc, counts) = git(repo, "rev-list", "--left-right", "--count", "HEAD...@{u}")
    val ab = parseAheadBehind(counts)
    if rc != 0 then
      say(s"  could not compare against $upstream.")
      say(ManualSteps)
    else if ab.behind == 0 then
      val aheadNote = if ab.ahead > 0 then s" (${ab.ahead} local commit(s) not yet pushed)" else ""
      say(s"  up to date with $upstream$aheadNote.")
    else
      // The one message --brief DOES print: a newer release is available.
      println(s"📦 genscalator is BEHIND $upstream by ${ab.behind} commit(s) — a newer release is available.")
      val (_, log) = git(repo, "log", "--oneline", "--no-decorate", "-n", "10", "HEAD..@{u}")
      if log.nonEmpty then
        println("  incoming:")
        log.linesIterator.foreach(l => println(s"    $l"))
      println(ManualSteps)

  private val Help: String =
    """tt update — check whether genscalator is behind its git marketplace remote
      |
      |Fetches remote-tracking refs (READ-ONLY, never the working tree) and compares your installed
      |genscalator against the remote, then SUGGESTS the manual update steps. It updates nothing itself:
      |Claude Code plugins update via the /plugin commands, which only the human can run, and plugin
      |authors get no update API — so git is the mechanism and the human is the actuator.
      |
      |Usage:
      |  tt update [--repo <dir>] [--brief]
      |  --repo <dir>   the genscalator repo to check (default: self-locate via the tools dir)
      |  --brief        print ONLY an actionable "newer release available" notice; silent otherwise
      |                 (for `gs warm` to call behind a throttle, so warm gains update-awareness quietly)
      |
      |Exit is 0 in all normal cases (an informational check). Degrades gracefully when offline, when
      |there is no upstream branch, or when genscalator is not a git checkout.""".stripMargin

@main def checkGenscalatorUpdate(args: String*): Unit = Update.dispatch(args.toList)
