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

  private def run(repo: os.Path, args: String*): (Int, String) =
    Try(os.proc(("git" +: "-C" +: repo.toString +: args)).call(
      check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 60_000)) match
      case scala.util.Success(res) => (res.exitCode, (res.out.text() + res.err.text()).trim)
      case scala.util.Failure(e)   => (255, e.getMessage)

  private def parse(args: List[String]): (String, Option[String]) =
    args match
      case repo :: "--remote" :: name :: Nil => (repo, Some(name))
      case repo :: Nil                       => (repo, None)
      case _ => fail("usage: tt gitinfo <repo> [--remote <name>]")

  def dispatch(args: String*): Unit =
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
          val verdict = if remoteHead == localHead then "IN SYNC" else "DIVERGED"
          println(s"remote $rname: $verdict (local ${localHead.take(12)} vs remote ${remoteHead.take(12)})")
        case (_, out) =>
          println(s"remote $rname: check failed ($out)")
}

@main def gitInfoOverview(args: String*): Unit = GitInfo.dispatch(args*)
