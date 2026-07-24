//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// gitinfo — typed, READ-ONLY git status/overview for agents (retires raw `git -C status/log/ls-remote`).
// Prints branch, clean/dirty count, ahead/behind vs upstream, and the recent log; with `--remote <name>` it
// also checks whether the local HEAD is in sync with that remote's HEAD (via ls-remote). NON-MUTATING: only
// read-only git subcommands (rev-parse/status/rev-list/log/ls-remote) — no add/commit/checkout/fetch — so it
// is trivially safe to allowlist. Complements `tt git` (which owns the safe WRITE subset). Its own object +
// @main name keep it collision-free from git.scala in a whole-toolbox compile. See skills/scala-style.
//   tt gitinfo <repo> [--remote <name>]
import scala.util.Try

object GitInfo {
  private def fail(msg: String): Nothing = { System.err.println(s"gitinfo: $msg"); sys.exit(2) }

  /** Verdict comparing the local HEAD against a remote's HEAD. A plain equality test cannot tell a
    * deliberately-behind mirror (e.g. a batched release mirror) from a genuinely forked history, so
    * we classify by ancestry, not by hash-mismatch. */
  enum RemoteSync:
    case InSync       // same commit
    case RemoteBehind // remote HEAD is an ancestor of local HEAD (local is ahead; the mirror lags)
    case RemoteAhead  // local HEAD is an ancestor of remote HEAD (local is behind; fetch to catch up)
    case Diverged     // histories have forked (neither is an ancestor of the other)
    case Unresolved   // cannot decide: the remote HEAD object is not present locally (fetch to compare)

  /** Pure classifier. `remoteAncestorOfLocal` / `localAncestorOfRemote` carry the ancestry facts as
    * `Some(true|false)`, or `None` when git could not decide (typically the remote object is not in
    * the local store). Equality wins first; then a true ancestry; then a definite fork; else Unresolved. */
  def classify(
      localHead: String,
      remoteHead: String,
      remoteAncestorOfLocal: Option[Boolean],
      localAncestorOfRemote: Option[Boolean],
  ): RemoteSync =
    if remoteHead == localHead then RemoteSync.InSync
    else (remoteAncestorOfLocal, localAncestorOfRemote) match
      case (Some(true), _)            => RemoteSync.RemoteBehind
      case (_, Some(true))            => RemoteSync.RemoteAhead
      case (Some(false), Some(false)) => RemoteSync.Diverged
      case _                          => RemoteSync.Unresolved

  def verdictLine(rname: String, localHead: String, remoteHead: String, s: RemoteSync): String =
    val l = localHead.take(12)
    val r = remoteHead.take(12)
    val note = s match
      case RemoteSync.InSync       => "IN SYNC"
      case RemoteSync.RemoteBehind => "remote BEHIND (local is ahead; the mirror lags)"
      case RemoteSync.RemoteAhead  => "remote AHEAD (local is behind; fetch to catch up)"
      case RemoteSync.Diverged     => "DIVERGED (histories have forked)"
      case RemoteSync.Unresolved   => "differs (remote HEAD not present locally; fetch to compare)"
    s"remote $rname: $note (local $l vs remote $r)"

  private def run(repo: os.Path, args: String*): (Int, String) =
    Try(os.proc(("git" +: "-C" +: repo.toString +: args)).call(
      check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 60_000)) match
      case scala.util.Success(res) => (res.exitCode, (res.out.text() + res.err.text()).trim)
      case scala.util.Failure(e)   => (255, e.getMessage)

  private val Help: String =
    """tt gitinfo — read-only git repo overview (branch, state, sync, recent log)
      |
      |Prints one screen of repo status: current branch and HEAD, clean/dirty count,
      |ahead/behind vs upstream, and the 5 most recent commits. Strictly NON-MUTATING —
      |only read-only git subcommands run (rev-parse/status/rev-list/log/ls-remote),
      |so it is always safe to run, anywhere.
      |
      |Usage:
      |  gitinfo <repo>                    overview of the repo at <repo>
      |  gitinfo <repo> --remote <name>    also compare local HEAD against that remote's
      |                                    HEAD (via ls-remote), classified by ANCESTRY:
      |                                    IN SYNC / remote BEHIND / remote AHEAD / DIVERGED
      |                                    (a deliberately-lagging mirror reads BEHIND, not DIVERGED)
      |
      |Examples:
      |  tt gitinfo /abs/myrepo                   # branch, state, upstream sync, recent commits
      |  tt gitinfo /abs/myrepo --remote origin   # + local-vs-origin HEAD verdict
      |
      |Companion: `tt git` owns the safe WRITE subset (commit/push/pull/fetch).
      |Full reference: tools/README.md""".stripMargin

  private def parse(args: List[String]): (String, Option[String]) =
    args match
      case repo :: "--remote" :: name :: Nil => (repo, Some(name))
      case repo :: Nil                       => (repo, None)
      case _ => fail("usage: tt gitinfo <repo> [--remote <name>]")

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    val (repoStr, remote) = parse(args.toList)
    val repo = os.Path(repoStr, os.pwd)
    if !os.exists(repo / ".git") && run(repo, "rev-parse", "--git-dir")._1 != 0 then fail(s"not a git repo: $repo")

    val branch = run(repo, "rev-parse", "--abbrev-ref", "HEAD") match
      case (0, b) => b
      case _      => "(unknown)"
    val head = run(repo, "rev-parse", "--short", "HEAD")._2
    val state = run(repo, "status", "--porcelain") match
      case (0, "") => "clean"
      case (0, s)  => s"${s.linesIterator.size} uncommitted change(s)"
      case _       => "(status unavailable)"
    val sync = run(repo, "rev-list", "--left-right", "--count", "@{upstream}...HEAD") match
      case (0, s) =>
        s.split("\\s+").toList match
          case behind :: ahead :: Nil => s"$ahead ahead, $behind behind upstream"
          case _                      => "(no upstream tracking)"
      case _ => "(no upstream tracking)"
    val log = run(repo, "log", "--oneline", "-5")._2

    println(s"repo:    $repo")
    println(s"branch:  $branch @ $head")
    println(s"state:   $state")
    println(s"sync:    $sync")
    println("recent:")
    log.linesIterator.foreach(l => println(s"  $l"))

    remote.foreach: rname =>
      val localHead = run(repo, "rev-parse", "HEAD")._2
      run(repo, "ls-remote", rname, "HEAD") match
        case (0, out) if out.nonEmpty =>
          val remoteHead = out.split("\\s+").headOption.getOrElse("")
          // Ancestry over hash-equality: `merge-base --is-ancestor A B` exits 0 (A is ancestor of B),
          // 1 (not), or non-0/1 (git could not decide — typically the remote object is not present
          // locally). Map each to Some(true)/Some(false)/None so a lagging mirror reads BEHIND, not DIVERGED.
          def isAncestor(a: String, b: String): Option[Boolean] =
            run(repo, "merge-base", "--is-ancestor", a, b)._1 match
              case 0 => Some(true)
              case 1 => Some(false)
              case _ => None
          val sync = classify(
            localHead, remoteHead,
            remoteAncestorOfLocal = isAncestor(remoteHead, localHead),
            localAncestorOfRemote = isAncestor(localHead, remoteHead),
          )
          println(verdictLine(rname, localHead, remoteHead, sync))
        case (_, out) =>
          println(s"remote $rname: check failed ($out)")
}

@main def gitInfoOverview(args: String*): Unit = GitInfo.dispatch(args*)
