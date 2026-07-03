//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8

// git — typed, SAFE git helper for agents. It exposes ONLY add/commit/push (never reset, rebase, --force,
// rm, or clean — the destructive verbs stay off the tool entirely, so `Bash(tt git *)` cannot become a
// data-loss vector). Its whole reason to exist: the commit message is read from a FILE (`--message-file`),
// so message prose containing shell-glob metachars (backticks, `$`, `!`, `<->`, `{a,b}`, bare `*`) NEVER
// appears on the command line — which kills the recurring "commit-message metachar" allowlist tripwire at
// the source, and lets messages legitimately contain `code` spans again.
//   tt git commit --repo <dir> --message-file <path> [--add <pathspec>]... [--push]
//     --add PATHSPEC   stage this path before committing (repeatable). If none given, nothing is staged
//                      (you stage separately) — the tool never runs `git add -A` implicitly.
//     --push           push after a successful commit (plain `git push`, current branch upstream)
import scala.util.Try

// Helpers scoped in this object so top-level names (fail/usage/run) don't collide with the other tools when
// the toolbox compiles as one unit. Only the @main entry is top-level. See skills/scala-style.
object Git {
  private def fail(msg: String): Nothing = { System.err.println(s"git: $msg"); sys.exit(2) }

  private def usage(): Nothing =
    System.err.println(
      """git: usage:
        |  tt git commit --repo <dir> --message-file <path> [--add <pathspec>]... [--push]
        |safe subset: add/commit/push only (no reset/rebase/force/rm/clean); message read from file.""".stripMargin)
    sys.exit(2)

  private def run(repo: os.Path, args: String*): (Int, String) =
    Try(os.proc(("git" +: "-C" +: repo.toString +: args)).call(
      check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 120_000)) match
      case scala.util.Success(res) => (res.exitCode, (res.out.text() + res.err.text()).trim)
      case scala.util.Failure(e)   => (255, e.getMessage)

  def dispatch(args: String*): Unit =
    args.toList match
      case "commit" :: rest => commit(rest)
      case _                => usage()

  private def commit(args: List[String]): Unit =
    @annotation.tailrec
    def parse(r: List[String], repo: Option[String], msg: Option[String], adds: Vector[String], push: Boolean)
        : (String, String, Vector[String], Boolean) =
      r match
        case Nil                              => (repo.getOrElse(fail("--repo required")), msg.getOrElse(fail("--message-file required")), adds, push)
        case "--repo" :: v :: t               => parse(t, Some(v), msg, adds, push)
        case "--message-file" :: v :: t       => parse(t, repo, Some(v), adds, push)
        case "--add" :: v :: t                => parse(t, repo, msg, adds :+ v, push)
        case "--push" :: t                    => parse(t, repo, msg, adds, true)
        case other :: _                       => fail(s"unexpected/incomplete argument '$other'")
    val (repoStr, msgStr, adds, push) = parse(args, None, None, Vector.empty, false)

    val repo = os.Path(repoStr, os.pwd)
    if !os.exists(repo / ".git") && run(repo, "rev-parse", "--git-dir")._1 != 0 then fail(s"not a git repo: $repo")
    val msgFile = os.Path(msgStr, os.pwd)
    if !os.exists(msgFile) then fail(s"message file not found: $msgFile")
    if os.read(msgFile).trim.isEmpty then fail(s"message file is empty: $msgFile")

    for a <- adds do
      val (c, out) = run(repo, "add", "--", a)
      if c != 0 then fail(s"git add '$a' failed:\n$out")

    val (cc, cout) = run(repo, "commit", "-F", msgFile.toString)
    if cc != 0 then fail(s"git commit failed:\n$cout")
    val sha = run(repo, "rev-parse", "--short", "HEAD")._2
    println(s"committed $sha")

    if push then
      val (pc, pout) = run(repo, "push")
      if pc != 0 then fail(s"git push failed:\n$pout")
      println(s"pushed $sha")
}

@main def gitCommitPush(args: String*): Unit = Git.dispatch(args*)
