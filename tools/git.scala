//> using file project.scala
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// git — typed, SAFE git helper for agents. It exposes ONLY add/commit/push plus fast-forward-only pull,
// fetch, and read-only show (never reset, rebase, merge, --force, rm, or clean — the destructive/interactive
// verbs stay off the tool entirely, so `Bash(tt git *)` cannot become a data-loss vector). Its whole reason to exist: the commit message is read from a FILE (`--message-file`),
// so message prose containing shell-glob metachars (backticks, `$`, `!`, `<->`, `{a,b}`, bare `*`) NEVER
// appears on the command line — which kills the recurring "commit-message metachar" allowlist tripwire at
// the source, and lets messages legitimately contain `code` spans again.
//   tt git commit --repo <dir> --message-file <path> [--add <pathspec>]... [--push]
//     --add PATHSPEC   stage this path before committing (repeatable). If none given, nothing is staged
//                      (you stage separately) — the tool never runs `git add -A` implicitly.
//     --push           push after a successful commit (current branch upstream by default)
//     --remote NAME    push to this remote instead of the upstream (repeatable, needs --push) —
//                      a mirror set (github + gitlab + coursegit) is one call, not one raw `git push` each
//     --tags           also push tags, to every named remote (needs --push). See pushTags for why this
//                      is --tags and not --follow-tags, and why tag CREATION stays out of scope.
//   tt git push --repo <dir> [--remote <name>]... [--tags]
//     push already-committed work without making a commit first.
//   tt git show --repo <dir> --ref <ref> --path <relpath> [--out <file>]
//     READ-ONLY: print the file content at <ref> (byte-exact) to stdout, or write it to <file> with
//     --out. This replaces the un-allowlistable shell pattern of redirecting `git show <ref>:<path>`
//     into a file (redirect + general git surface), e.g. for PR review of a file at a base ref.
//   tt git log --repo <dir> [--grep P] [--co-author P] [--author P] [--committer P] [--since D] [--limit N]
//     READ-ONLY commit-log search: capped + tab-formatted (<short-sha>\t<author-email>\t<subject>), so it
//     needs no `| head` and `Bash(tt git log *)` stays allowlist-safe. --co-author greps the Co-Authored-By
//     trailer (forge contributor attribution). Retires the raw `git log --grep … | head` reflex (SM217).
import scala.util.Try

// Helpers scoped in this object so top-level names (fail/usage/run) don't collide with the other tools when
// the toolbox compiles as one unit. Only the @main entry is top-level. See skills/scala-style.
object Git {
  private def fail(msg: String): Nothing = { System.err.println(s"git: $msg"); sys.exit(2) }

  private val Help: String =
    """tt git — safe git write helper for agents (commit message from a FILE)
      |
      |Exposes ONLY the safe, non-destructive git verbs: add/commit/push, fast-forward-only
      |pull, fetch, and read-only show. The commit message is read from a file, so prose
      |containing shell metacharacters (backticks, $, !, braces, bare *) never touches the
      |command line.
      |
      |Usage:
      |  git commit --repo <dir> --message-file <path> [--add <pathspec>]... [--push] [--remote <name>]... [--tags]
      |  git push  --repo <dir> [--remote <name>]... [--tags]
      |                                  push already-committed work; repeat --remote for a
      |                                  mirror set, omit it for the branch's upstream
      |  git pull  --repo <dir>          fast-forward only: either FFs or fails loudly
      |  git fetch --repo <dir>          update remote-tracking refs (never the working tree)
      |  git show  --repo <dir> --ref <ref> --path <relpath> [--out <file>]
      |                                  print the file content at <ref> (or write to <file>)
      |  git log   --repo <dir> [--grep P] [--co-author P] [--author P] [--committer P]
      |            [--since D] [--limit N]
      |                                  READ-ONLY commit-log search, capped + tab-formatted
      |                                  (so no `| head` is ever needed)
      |Flags (commit):
      |  --repo <dir>                    the git repository to operate on (required)
      |  --message-file <path>           file holding the commit message (required, non-empty)
      |  --add <pathspec>                stage this path before committing (repeatable);
      |                                  nothing is staged implicitly — never `git add -A`
      |  --push                          push after a successful commit
      |  --remote <name>                 push to this remote (repeatable, needs --push); with none
      |                                  given the push goes to the branch's upstream as before.
      |                                  Fails on the first remote that rejects, so a partially
      |                                  pushed mirror set is reported, never swallowed. A branch
      |                                  with no upstream in a single-remote repo is refused by git
      |                                  itself (push.default simple) — set the upstream once with
      |                                  git push -u; the tool never sets one behind your back.
      |  --tags                          also push tags, as a second push per remote (needs --push).
      |                                  --tags, NOT --follow-tags: --follow-tags sends only ANNOTATED
      |                                  tags, and this project's own tags are mixed (v0.8.0/v0.9.0/
      |                                  v0.9.1 lightweight, v0.9.2 annotated), so it would push some
      |                                  releases and silently skip others. Never --force: git refuses
      |                                  to MOVE an existing remote tag, so this can only ADD refs.
      |                                  Creating tags is out of scope — this sends tags you already
      |                                  made locally.
      |Flags (show):
      |  --repo <dir>                    the git repository to read from (required)
      |  --ref <ref>                     any commit-ish: HEAD, a branch, a tag, a SHA (required)
      |  --path <relpath>                the file's path relative to the repo root (required)
      |  --out <file>                    write the content to <file> instead of stdout
      |show is READ-ONLY (never mutates the repo) and byte-exact: content goes to stdout
      |untouched, so redirecting or --out reproduces the file at that ref precisely. On a
      |bad ref or path it exits non-zero with git's error — never a partial/empty success.
      |It replaces the un-allowlistable shell pattern of redirecting git show ref:path
      |output into a file (the redirect plus git's general surface blocked allowlisting).
      |Flags (log):
      |  --repo <dir>                    the git repository to read from (required)
      |  --grep P                        keep commits whose MESSAGE matches regex P
      |  --co-author P                   keep commits whose Co-Authored-By trailer matches P
      |                                  (what forges attribute contributors from)
      |  --author P / --committer P      filter by author / committer (name or email regex)
      |  --since D                       only commits more recent than D (e.g. 2026-07-01, "2 weeks ago")
      |  --limit N                       cap the output at N commits (default 50)
      |log is READ-ONLY. Output is one commit per line, <short-sha>TAB<author-email>TAB<subject>,
      |then a `=== N commit(s)` line that flags when the --limit cap was hit (no silent truncation).
      |Multiple message-patterns (--grep + --co-author) must ALL match. Because the tool caps and
      |formats, it needs no `| head` — so `Bash(tt git log *)` stays allowlist-safe (SM217).
      |
      |Not on the tool, by design: reset, rebase, merge, --force, rm, clean — the
      |destructive/interactive verbs stay off entirely, so allowlisting `tt git` is safe.
      |
      |Examples:
      |  tt git commit --repo /abs/repo --message-file tmp/msg.txt --add src/app.scala --push
      |  tt git commit --repo /abs/repo --message-file tmp/msg.txt --add tools --push \
      |    --remote origin --remote gitlab --remote coursegit      # one unit, three mirrors
      |  tt git push --repo /abs/repo --remote gitlab --remote coursegit   # sync, no new commit
      |  tt git pull --repo /abs/repo    # fast-forward to upstream, or fail (no merge commit)
      |  tt git fetch --repo /abs/repo   # refresh remote-tracking refs only
      |  tt git show --repo /abs/repo --ref main --path src/app.scala             # to stdout
      |  tt git show --repo /abs/repo --ref v1.2 --path README.md --out tmp/old-readme.md
      |
      |Full reference: tools/README.md""".stripMargin

  private def usage(): Nothing =
    System.err.println(
      """git: usage:
        |  tt git commit --repo <dir> --message-file <path> [--add <pathspec>]... [--push] [--remote <name>]...
        |  tt git push  --repo <dir> [--remote <name>]...
        |  tt git pull  --repo <dir>   (fast-forward only)
        |  tt git fetch --repo <dir>
        |  tt git show  --repo <dir> --ref <ref> --path <relpath> [--out <file>]   (read-only)
        |  tt git log   --repo <dir> [--grep P] [--co-author P] [--author P] [--committer P] [--since D] [--limit N]   (read-only search)
        |safe subset: add/commit/push/pull(--ff-only)/fetch/show/log only (no reset/rebase/force/rm/clean/merge); message read from file.""".stripMargin)
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
      case "push"   :: rest => push(rest)
      case "pull"   :: rest => pull(rest)
      case "fetch"  :: rest => fetch(rest)
      case "show"   :: rest => show(rest)
      case "log"    :: rest => log(rest)
      case _                => usage()

  private def commit(args: List[String]): Unit =
    @annotation.tailrec
    def parse(r: List[String], repo: Option[String], msg: Option[String], adds: Vector[String], push: Boolean, remotes: Vector[String], tags: Boolean)
        : (String, String, Vector[String], Boolean, Vector[String], Boolean) =
      r match
        case Nil                              => (repo.getOrElse(fail("--repo required")), msg.getOrElse(fail("--message-file required")), adds, push, remotes, tags)
        case "--repo" :: v :: t               => parse(t, Some(v), msg, adds, push, remotes, tags)
        case "--message-file" :: v :: t       => parse(t, repo, Some(v), adds, push, remotes, tags)
        case "--add" :: v :: t                => parse(t, repo, msg, adds :+ v, push, remotes, tags)
        case "--push" :: t                    => parse(t, repo, msg, adds, true, remotes, tags)
        case "--remote" :: v :: t             => parse(t, repo, msg, adds, push, remotes :+ v, tags)
        case "--tags" :: t                    => parse(t, repo, msg, adds, push, remotes, true)
        case other :: _                       => fail(s"unexpected/incomplete argument '$other'")
    val (repoStr, msgStr, adds, push, remotes, tags) = parse(args, None, None, Vector.empty, false, Vector.empty, false)
    if remotes.nonEmpty && !push then fail("--remote needs --push (it names where to push)")
    // Same rule as --remote, same reason: a flag that names HOW to push is meaningless without a push,
    // and accepting it silently would report success for tags that never left the machine.
    if tags && !push then fail("--tags needs --push (it names what else to push)")

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

    if push then pushTo(repo, remotes, Some(sha), tags)

  // Push to each named remote in turn, or to the branch's default upstream when none is named (the
  // pre-SM232 behaviour, kept so existing `--push` calls are unchanged). A multi-remote project — genscalator
  // mirrors to github + gitlab + coursegit — otherwise forces a bare `git -C <dir> push <remote>` per extra
  // remote, which is exactly the raw-git reflex this tool exists to retire. Fails on the FIRST bad remote so a
  // half-pushed set is reported, never silently swallowed. Still no --force: the safe subset is unchanged.
  private def pushTo(repo: os.Path, remotes: Vector[String], sha: Option[String], tags: Boolean = false): Unit =
    val what = sha.map(s => s" $s").getOrElse("")
    if remotes.isEmpty then
      val (pc, pout) = run(repo, "push")
      if pc != 0 then fail(s"git push failed:\n$pout")
      println(s"pushed$what")
      if tags then pushTags(repo, None)
    else
      for r <- remotes do
        val (pc, pout) = run(repo, "push", r)
        if pc != 0 then fail(s"git push $r failed:\n$pout")
        println(s"pushed$what to $r")
        if tags then pushTags(repo, Some(r))

  /** Push tags as a SEPARATE invocation, and say which ones went.
    *
    * WHY --tags and not --follow-tags. The instinct is --follow-tags, which sends only ANNOTATED tags
    * reachable from what was just pushed, so a caller cannot sling unrelated local scratch tags at a
    * remote. That instinct is wrong here, and checking beats reasoning: this repo's own tags are
    * MIXED — v0.1.0..v0.7.0 annotated, v0.8.0/v0.9.0/v0.9.1 lightweight, v0.9.2 annotated again
    * (`git for-each-ref --format='%(objecttype)' refs/tags`, 2026-07-27). --follow-tags would push
    * some releases and silently skip others, reporting success either way, and which ones it skipped
    * would depend on how that release happened to be tagged months ago. --tags is PREDICTABLE.
    *
    * WHY a second invocation. `git push <remote> --tags` pushes tags and NOT the branch, so it cannot
    * replace the branch push above; it has to follow it.
    *
    * Still no --force, under any flag combination. Without it git REFUSES to move an existing remote
    * tag, which is the property that keeps a tag push inside the safe subset: it can only ADD refs.
    * Tag CREATION (`git tag -a`) is deliberately NOT in scope here — this sends tags that already
    * exist locally, and inventing them is a separate decision with its own naming and signing
    * questions.
    */
  private def pushTags(repo: os.Path, remote: Option[String]): Unit =
    val args = remote.toList :+ "--tags"
    val (tc, tout) = run(repo, ("push" :: args)*)
    val where = remote.map(r => s" to $r").getOrElse("")
    if tc != 0 then fail(s"git push${where.replace(" to ", " ")} --tags failed:\n$tout")
    // Report what git actually did rather than a bare "ok": "Everything up-to-date" and "pushed two new
    // tags" are different outcomes, and a caller who tagged and saw silence would assume the first.
    val detail = tout.trim
    if detail.isEmpty || detail.contains("Everything up-to-date")
    then println(s"tags$where: already up to date")
    else println(s"tags$where:\n$detail")

  private def repoArg(args: List[String], cmd: String): os.Path =
    args match
      case "--repo" :: v :: Nil =>
        val r = os.Path(v, os.pwd)
        if !os.exists(r / ".git") && run(r, "rev-parse", "--git-dir")._1 != 0 then fail(s"not a git repo: $r")
        r
      case _ => fail(s"usage: tt git $cmd --repo <dir>")

  /** Flag parsing for the `push` verb, PURE and public so the co-located tests can exercise it directly
    * (an argv path that silently drops a remote would push less than the caller believes). Returns the repo
    * (absent = the caller reports the missing --repo) and the remotes in the order given, duplicates kept:
    * naming a remote twice is harmless, and de-duplicating would hide a typo in the caller's mirror set. */
  /** Parsed `push` arguments. A case class, not a tuple: the flag set is open-ended now, and a tuple
    * that grew a slot would silently re-bind every existing destructuring rather than failing to
    * compile. */
  final case class PushArgs(repo: Option[String], remotes: Vector[String], tags: Boolean)

  def parsePushArgs(args: List[String]): PushArgs =
    @annotation.tailrec
    def go(r: List[String], repo: Option[String], remotes: Vector[String], tags: Boolean): PushArgs =
      r match
        case Nil                  => PushArgs(repo, remotes, tags)
        case "--repo" :: v :: t   => go(t, Some(v), remotes, tags)
        case "--remote" :: v :: t => go(t, repo, remotes :+ v, tags)
        case "--tags" :: t        => go(t, repo, remotes, true)
        case other :: _           => fail(s"unexpected/incomplete argument '$other' (usage: tt git push --repo <dir> [--remote <name>]... [--tags])")
    go(args, None, Vector.empty, false)

  // push as a standalone verb: send ALREADY-committed work to one or more remotes without making a commit
  // first. Before this the only way to reach a push was `tt git commit --push`, so syncing a second remote
  // meant raw git.
  private def push(args: List[String]): Unit =
    val parsed = parsePushArgs(args)
    val repoStr = parsed.repo.getOrElse(fail("--repo required"))
    val repo = os.Path(repoStr, os.pwd)
    if !os.exists(repo / ".git") && run(repo, "rev-parse", "--git-dir")._1 != 0 then fail(s"not a git repo: $repo")
    pushTo(repo, parsed.remotes, None, parsed.tags)

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

  // show is READ-ONLY: extract a file's content at a ref (the allowlist-clean replacement for
  // redirecting `git show ref:path` into a file). It does NOT use the shared run() helper because
  // that trims and merges stderr — file content must stay byte-exact, so stdout is captured raw
  // (bytes) and only checked-then-emitted on success (never a partial/empty success).
  private def show(args: List[String]): Unit =
    @annotation.tailrec
    def parse(r: List[String], repo: Option[String], ref: Option[String], path: Option[String], out: Option[String])
        : (String, String, String, Option[String]) =
      r match
        case Nil                 => (repo.getOrElse(fail("--repo required")), ref.getOrElse(fail("--ref required")),
                                     path.getOrElse(fail("--path required")), out)
        case "--repo" :: v :: t  => parse(t, Some(v), ref, path, out)
        case "--ref" :: v :: t   => parse(t, repo, Some(v), path, out)
        case "--path" :: v :: t  => parse(t, repo, ref, Some(v), out)
        case "--out" :: v :: t   => parse(t, repo, ref, path, Some(v))
        case other :: _          => fail(s"unexpected/incomplete argument '$other' (usage: tt git show --repo <dir> --ref <ref> --path <relpath> [--out <file>])")
    val (repoStr, ref, relpath, outOpt) = parse(args, None, None, None, None)

    val repo = os.Path(repoStr, os.pwd)
    if !os.exists(repo / ".git") && run(repo, "rev-parse", "--git-dir")._1 != 0 then fail(s"not a git repo: $repo")

    val res = Try(os.proc("git", "-C", repo.toString, "show", s"$ref:$relpath").call(
      check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 120_000)) match
      case scala.util.Success(r) => r
      case scala.util.Failure(e) => fail(s"git show failed: ${e.getMessage}")
    if res.exitCode != 0 then fail(s"git show '$ref:$relpath' failed:\n${res.err.text().trim}")

    val bytes = res.out.bytes
    outOpt match
      case Some(o) =>
        val outFile = os.Path(o, os.pwd)
        os.write.over(outFile, bytes, createFolders = true)
        println(s"wrote ${bytes.length} bytes from $ref:$relpath to $outFile")
      case None =>
        System.out.write(bytes)
        System.out.flush()

  // log is READ-ONLY: search/scan the commit log with typed filters, CAPPED and tab-formatted so it never
  // needs a `| head` pipe (which trips guardcheck) — the raw-git reflex a missing typed shape used to force
  // (SM217). One commit per line: `<short-sha>\t<author-email>\t<subject>`; a trailing count line makes the
  // cap visible (no silent truncation). --co-author greps the Co-Authored-By trailer (what forges attribute
  // contributors from); multiple message-patterns (--grep/--co-author) must ALL match (git --all-match).
  private def log(args: List[String]): Unit =
    @annotation.tailrec
    def parse(r: List[String], repo: Option[String], greps: Vector[String], author: Option[String],
        committer: Option[String], since: Option[String], limit: Int)
        : (String, Vector[String], Option[String], Option[String], Option[String], Int) =
      r match
        case Nil                     => (repo.getOrElse(fail("--repo required")), greps, author, committer, since, limit)
        case "--repo" :: v :: t      => parse(t, Some(v), greps, author, committer, since, limit)
        case "--grep" :: v :: t      => parse(t, repo, greps :+ v, author, committer, since, limit)
        case "--co-author" :: v :: t => parse(t, repo, greps :+ s"[Cc]o-[Aa]uthored-[Bb]y:.*$v", author, committer, since, limit)
        case "--author" :: v :: t    => parse(t, repo, greps, Some(v), committer, since, limit)
        case "--committer" :: v :: t => parse(t, repo, greps, author, Some(v), since, limit)
        case "--since" :: v :: t     => parse(t, repo, greps, author, committer, Some(v), limit)
        case "--limit" :: v :: t     =>
          v.toIntOption match
            case Some(n) if n > 0 => parse(t, repo, greps, author, committer, since, n)
            case _                => fail(s"--limit needs a positive integer, got '$v'")
        case other :: _              => fail(s"unexpected/incomplete argument '$other' (usage: tt git log --repo <dir> [--grep P] [--co-author P] [--author P] [--committer P] [--since D] [--limit N])")
    val (repoStr, greps, author, committer, since, limit) = parse(args, None, Vector.empty, None, None, None, 50)

    val repo = os.Path(repoStr, os.pwd)
    if !os.exists(repo / ".git") && run(repo, "rev-parse", "--git-dir")._1 != 0 then fail(s"not a git repo: $repo")

    // %h short-sha, %x09 TAB, %ae author-email, %s subject — a stable machine-readable row per commit.
    val gitArgs = Vector("log", "--format=%h%x09%ae%x09%s", s"--max-count=$limit") ++
      (if greps.size > 1 then Vector("--all-match") else Vector.empty) ++
      greps.map(g => s"--grep=$g") ++
      author.map(a => s"--author=$a").toVector ++
      committer.map(c => s"--committer=$c").toVector ++
      since.map(s => s"--since=$s").toVector
    val (c, out) = run(repo, gitArgs*)
    if c != 0 then fail(s"git log failed:\n$out")
    if out.isEmpty then println("(no matching commits)")
    else
      println(out)
      val n = out.linesIterator.size
      println(s"=== $n commit(s)" + (if n >= limit then s" (hit --limit $limit; there may be more)" else ""))
}

@main def gitCommitPush(args: String*): Unit = Git.dispatch(args*)
