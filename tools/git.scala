//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// git — typed, SAFE git helper for agents. It exposes ONLY add/commit/push plus fast-forward-only pull and
// fetch (never reset, rebase, merge, --force, rm, or clean — the destructive/interactive verbs stay off the
// tool entirely, so `Bash(tt git *)` cannot become a data-loss vector). Its whole reason to exist: the commit message is read from a FILE (`--message-file`),
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

  private val Help: String =
    """tt git — safe git write helper for agents (commit message from a FILE)
      |
      |Exposes ONLY the safe, non-destructive git verbs: add/commit/push, fast-forward-only
      |pull, and fetch. The commit message is read from a file, so prose containing shell
      |metacharacters (backticks, $, !, braces, bare *) never touches the command line.
      |
      |Usage:
      |  git commit --repo <dir> --message-file <path> [--add <pathspec>]... [--push]
      |  git pull  --repo <dir>          fast-forward only: either FFs or fails loudly
      |  git fetch --repo <dir>          update remote-tracking refs (never the working tree)
      |Flags (commit):
      |  --repo <dir>                    the git repository to operate on (required)
      |  --message-file <path>           file holding the commit message (required, non-empty)
      |  --add <pathspec>                stage this path before committing (repeatable);
      |                                  nothing is staged implicitly — never `git add -A`
      |  --push                          push after a successful commit (plain `git push`)
      |
      |Not on the tool, by design: reset, rebase, merge, --force, rm, clean — the
      |destructive/interactive verbs stay off entirely, so allowlisting `tt git` is safe.
      |
      |Examples:
      |  tt git commit --repo /abs/repo --message-file tmp/msg.txt --add src/app.scala --push
      |  tt git pull --repo /abs/repo    # fast-forward to upstream, or fail (no merge commit)
      |  tt git fetch --repo /abs/repo   # refresh remote-tracking refs only
      |
      |Full reference: tools/README.md""".stripMargin

  private def usage(): Nothing =
    System.err.println(
      """git: usage:
        |  tt git commit --repo <dir> --message-file <path> [--add <pathspec>]... [--push]
        |  tt git pull  --repo <dir>   (fast-forward only)
        |  tt git fetch --repo <dir>
        |safe subset: add/commit/push/pull(--ff-only)/fetch only (no reset/rebase/force/rm/clean/merge); message read from file.""".stripMargin)
    sys.exit(2)

  private def run(repo: os.Path, args: String*): (Int, String) =
    Try(os.proc(("git" +: "-C" +: repo.toString +: args)).call(
      check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 120_000)) match
      case scala.util.Success(res) => (res.exitCode, (res.out.text() + res.err.text()).trim)
      case scala.util.Failure(e)   => (255, e.getMessage)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    args.toList match
      case "commit" :: rest => commit(rest)
      case "pull"   :: rest => pull(rest)
      case "fetch"  :: rest => fetch(rest)
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

  private def repoArg(args: List[String], cmd: String): os.Path =
    args match
      case "--repo" :: v :: Nil =>
        val r = os.Path(v, os.pwd)
        if !os.exists(r / ".git") && run(r, "rev-parse", "--git-dir")._1 != 0 then fail(s"not a git repo: $r")
        r
      case _ => fail(s"usage: tt git $cmd --repo <dir>")

  // pull is FF-ONLY: it never creates a merge commit, runs merge hooks, or leaves conflicts — it either
  // fast-forwards or fails loudly, so it stays inside the safe (non-destructive, non-interactive) subset.
  private def pull(args: List[String]): Unit =
    val repo = repoArg(args, "pull")
    val (c, out) = run(repo, "pull", "--ff-only")
    if c != 0 then fail(s"git pull --ff-only failed:\n$out")
    println(if out.nonEmpty then out else "pull: up to date")

  // fetch is read-only: it updates remote-tracking refs, never the working tree.
  private def fetch(args: List[String]): Unit =
    val repo = repoArg(args, "fetch")
    val (c, out) = run(repo, "fetch")
    if c != 0 then fail(s"git fetch failed:\n$out")
    println(if out.nonEmpty then out else "fetch: up to date")
}

@main def gitCommitPush(args: String*): Unit = Git.dispatch(args*)
