//> using file project.scala
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// gitinfo — typed, READ-ONLY git status/overview for agents (retires raw `git -C status/log/ls-remote`).
// Prints branch, clean/dirty count, ahead/behind vs upstream, and the recent log; with `--remote <name>` it
// also checks whether the local HEAD is in sync with that remote's HEAD (via ls-remote). NON-MUTATING: only
// read-only git subcommands (rev-parse/status/rev-list/log/ls-remote/merge-base) — no add/commit/checkout/fetch — so it
// is trivially safe to allowlist. Complements `tt git` (which owns the safe WRITE subset). Its own object +
// @main name keep it collision-free from git.scala in a whole-toolbox compile. See skills/scala-style.
//   tt gitinfo <repo> [--remote <name>] [--files]
// `--files` answers WHICH paths changed (issue 004): the bare count invites a guess, and the guess is
// made at the one step that decides what enters history — staging for `tt git commit --add`.
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

  /** stdout ONLY and deliberately NOT trimmed, unlike `run`. Porcelain output is column-significant:
    * a leading space IS a status column (` M path` = modified in the worktree, not in the index), so a
    * trim would silently reclassify every unstaged change as a staged one. stderr is dropped rather than
    * merged so a git warning cannot be mistaken for a change entry. */
  private def runRaw(repo: os.Path, args: String*): (Int, String) =
    Try(os.proc(("git" +: "-C" +: repo.toString +: args)).call(
      check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 60_000)) match
      case scala.util.Success(res) => (res.exitCode, res.out.text())
      case scala.util.Failure(_)   => (255, "")

  /** How a changed path relates to the index, which is what decides whether a broad `add` is safe.
    * `Both` = staged AND further modified since; `Untracked` is the case a broad add damages most. */
  enum ChangeKind:
    case Conflicted, Staged, Both, Unstaged, Untracked

  final case class Change(kind: ChangeKind, code: String, path: String)

  def label(k: ChangeKind): String = k match
    case ChangeKind.Conflicted => "conflict"
    case ChangeKind.Staged     => "staged"
    case ChangeKind.Both       => "both"
    case ChangeKind.Unstaged   => "unstaged"
    case ChangeKind.Untracked  => "untracked"

  /** Pure parser for ONE `git status --porcelain` (v1) line: two status columns, a space, then the path.
    * `X` is the index column, `Y` the worktree column. Returns None for anything that is not an entry
    * (blank lines, `!!` ignored entries), so a malformed line is skipped rather than guessed at.
    * Rename/copy entries carry `old -> new`; the DESTINATION is reported, because that is the path a
    * caller would hand to `--add`, and the `R`/`C` code keeps the move visible. */
  def classifyEntry(line: String): Option[Change] =
    if line.length < 4 || line.charAt(2) != ' ' then None
    else
      val x    = line.charAt(0)
      val y    = line.charAt(1)
      val code = s"$x$y"
      val raw  = line.substring(3)
      val path = raw.split(" -> ", 2) match
        case Array(_, dst) if dst.nonEmpty => dst
        case _                             => raw
      val kind =
        if code == "??" then Some(ChangeKind.Untracked)
        else if code == "!!" then None // ignored entries only appear under --ignored; never a change
        else if x == 'U' || y == 'U' || code == "AA" || code == "DD" then Some(ChangeKind.Conflicted)
        else if x != ' ' && y != ' ' then Some(ChangeKind.Both)
        else if x != ' ' then Some(ChangeKind.Staged)
        else if y != ' ' then Some(ChangeKind.Unstaged)
        else None
      kind.map(k => Change(k, code, path))

  /** Pure: whole porcelain text -> the `files:` block body, one path per line with its marker.
    * Grouped by kind (conflicts loudest first, untracked last) and then by path, so the categories
    * read as blocks while the output stays flat enough to compose with `--add` without parsing. */
  def formatChanges(porcelain: String): Vector[String] =
    porcelain.linesIterator
      .flatMap(classifyEntry)
      .toVector
      .sortBy(c => (c.kind.ordinal, c.path))
      .map(c => f"  ${label(c.kind)}%-9s ${c.code}%-2s  ${c.path}")

  private val Help: String =
    """tt gitinfo — read-only git repo overview (branch, state, sync, recent log)
      |
      |Prints one screen of repo status: current branch and HEAD, clean/dirty count,
      |ahead/behind vs upstream, and the 5 most recent commits. Strictly NON-MUTATING —
      |only read-only git subcommands run (rev-parse/status/rev-list/log/ls-remote/merge-base),
      |so it is always safe to run, anywhere.
      |
      |Usage:
      |  gitinfo <repo>                    overview of the repo at <repo>
      |  gitinfo <repo> --remote <name>    also compare local HEAD against that remote's
      |                                    HEAD (via ls-remote), classified by ANCESTRY:
      |                                    IN SYNC / remote BEHIND / remote AHEAD / DIVERGED
      |                                    (a deliberately-lagging mirror reads BEHIND, not DIVERGED)
      |  gitinfo <repo> --files            also list WHICH paths changed, one per line, each
      |                                    labelled staged / unstaged / both / untracked /
      |                                    conflict plus its raw porcelain code
      |
      |Why --files: the `state:` line gives a COUNT, and `tt git commit --add` needs exact
      |paths. Guessing there is how a human's in-progress file gets committed under an agent's
      |message. Reach for --files instead of a bare `git status --short`; the labels are what
      |make "untracked" — where a broad add does the most damage — visible before staging.
      |A rename reports its DESTINATION path (the one you would --add); the R code shows the move.
      |
      |Examples:
      |  tt gitinfo /abs/myrepo                   # branch, state, upstream sync, recent commits
      |  tt gitinfo /abs/myrepo --remote origin   # + local-vs-origin HEAD verdict
      |  tt gitinfo /abs/myrepo --files           # + which paths changed, and how
      |
      |Companion: `tt git` owns the safe WRITE subset (commit/push/pull/fetch).
      |Full reference: tools/README.md""".stripMargin

  final case class Opts(repo: String, remote: Option[String], files: Boolean)

  private val Usage = "usage: tt gitinfo <repo> [--remote <name>] [--files]"

  /** Flag loop rather than a fixed shape, so --remote and --files compose in either order. */
  private def parse(args: List[String]): Opts =
    def loop(rest: List[String], repo: Option[String], remote: Option[String], files: Boolean): Opts =
      rest match
        case Nil => repo match
          case Some(r) => Opts(r, remote, files)
          case None    => fail(Usage)
        case "--remote" :: name :: t if !name.startsWith("--") => loop(t, repo, Some(name), files)
        case "--files" :: t                                    => loop(t, repo, remote, true)
        case a :: _ if a.startsWith("--")                      => fail(s"unknown/incomplete flag '$a'\n$Usage")
        case a :: t if repo.isEmpty                            => loop(t, Some(a), remote, files)
        case a :: _                                            => fail(s"unexpected extra argument '$a'\n$Usage")
    loop(args, None, None, false)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    val opts = parse(args.toList)
    val remote = opts.remote
    val repo = os.Path(opts.repo, os.pwd)
    if !os.exists(repo / ".git") && run(repo, "rev-parse", "--git-dir")._1 != 0 then fail(s"not a git repo: $repo")

    val branch = run(repo, "rev-parse", "--abbrev-ref", "HEAD") match
      case (0, b) => b
      case _      => "(unknown)"
    val head = run(repo, "rev-parse", "--short", "HEAD")._2
    // core.quotePath=false so non-ASCII paths arrive verbatim instead of octal-escaped and unusable as --add args
    val (statusExit, statusOut) = runRaw(repo, "-c", "core.quotePath=false", "status", "--porcelain")
    val state =
      if statusExit != 0 then "(status unavailable)"
      else if statusOut.trim.isEmpty then "clean"
      else s"${statusOut.linesIterator.size} uncommitted change(s)"
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
    if opts.files then
      println("files:")
      if statusExit != 0 then println("  (status unavailable)")
      else
        val rows = formatChanges(statusOut)
        if rows.isEmpty then println("  (no uncommitted changes)") else rows.foreach(println)

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
