//> using file project.scala
//> using jvm 21

// memory — keep the committed `memory/` snapshot in step with Claude Code's LIVE memory store.
//
// WHY THIS EXISTS (the failure it is built to catch). The live store lives OUTSIDE the repo, in a directory
// Claude Code names by SLUGIFYING the project's working-directory path. Move the project and the slug changes,
// so Claude Code silently starts a NEW live dir while any hardcoded copy command keeps reading the OLD one —
// copying nothing, exiting 0, looking healthy. That is how this snapshot once sat at 14 files against 148 live
// (see memory/README.md, reconciled 2026-07-26). A stale pointer fails silently and persuasively; a missing one
// fails loudly. So this tool DERIVES the live path from the repo path — never hardcodes it — and REFUSES to run
// when the source is missing or has implausibly collapsed.
//
//   tt memory where [--repo <dir>]    print the derived live path and stop
//   tt memory check [--repo <dir>]    read-only drift report; exit 1 if drifted, 0 if in step
//   tt memory sync  [--repo <dir>]    copy live -> <repo>/memory
//   ... --force                       proceed past the collapse guard (then say why in the commit message)
//
// ADDITIVE by design: a file present only in the snapshot is REPORTED, never deleted. memory/README.md
// documents the snapshot and is not itself a memory, so deleting snapshot-only files would eat it.
// --repo defaults to the cwd, so the tool is usable from a repo root without arguments.
import java.nio.file.{Files, Path}

private val MemoryHelp: String =
  """tt memory — keep the committed memory/ snapshot in step with the live Claude Code memory store
    |
    |The live store is OUTSIDE the repo, in a directory named after the project path. This tool DERIVES
    |that path from --repo instead of hardcoding it, because a hardcoded path goes stale silently when
    |the project moves: it copies nothing and still exits 0.
    |
    |Usage:
    |  memory where [--repo <dir>]    print the derived live path and stop
    |  memory check [--repo <dir>]    read-only drift report; exit 1 if drifted
    |  memory sync  [--repo <dir>]    copy live -> <repo>/memory (additive)
    |  memory ... --force             proceed past the collapse guard
    |
    |--repo defaults to the current directory.
    |
    |Guards (each one FAILS LOUDLY rather than doing nothing quietly):
    |  - <repo>/memory must exist            (else --repo is not a checkout with a snapshot)
    |  - the derived live dir must exist     (else the project moved and the slug changed)
    |  - the live dir must hold MEMORY.md    (else it is not a memory store)
    |  - the live file count must not have collapsed against the snapshot (the 148-vs-14 scream)
    |
    |Full reference: tools/README.md""".stripMargin

@main def memorySnapshotSync(args: String*): Unit = MemoryTool.dispatch(args)

object MemoryTool {

  type Relpath = String

  private def fail(msg: String): Nothing = { Console.err.println(s"memory: $msg"); sys.exit(2) }

  /** Claude Code names a project's state dir after the project path with every separator turned into '-',
    * so /home/u/git/proj becomes -home-u-git-proj. Derived, never hardcoded: that is the whole point of
    * this tool. If the slug rule ever changes, `memory where` shows what we computed and the existence
    * guard below turns a wrong guess into a loud failure instead of a silent no-op. */
  def slugOf(repo: Path): String = repo.toAbsolutePath.normalize.toString.replace('/', '-')

  def liveDirFor(repo: Path): Path =
    Path.of(sys.props.getOrElse("user.home", "."), ".claude", "projects", slugOf(repo), "memory")

  /** Every regular file under root, as sorted root-relative paths. */
  def listFiles(root: Path): Vector[Relpath] =
    if !Files.isDirectory(root) then Vector.empty
    else
      import scala.jdk.CollectionConverters.*
      val walk = Files.walk(root)
      try walk.iterator.asScala.filter(Files.isRegularFile(_)).map(root.relativize(_).toString).toVector.sorted
      finally walk.close()

  def sameBytes(a: Path, b: Path): Boolean =
    Files.size(a) == Files.size(b) && java.util.Arrays.equals(Files.readAllBytes(a), Files.readAllBytes(b))

  /** Pure comparison: what sync WOULD do. `check` and `sync` share it so they can never disagree. */
  def plan(live: Path, snap: Path): (added: Vector[Relpath], changed: Vector[Relpath], snapshotOnly: Vector[Relpath]) =
    val liveFiles = listFiles(live)
    val snapFiles = listFiles(snap).toSet
    val added     = liveFiles.filterNot(snapFiles)
    val changed   = liveFiles.filter(r => snapFiles(r) && !sameBytes(live.resolve(r), snap.resolve(r)))
    (added = added, changed = changed, snapshotOnly = listFiles(snap).filterNot(liveFiles.toSet))

  /** The guards. Each returns Unit or exits non-zero — never a quiet no-op, which is the bug being prevented. */
  def checkedDirs(repo: Path, force: Boolean): (live: Path, snap: Path) = {
    val snap = repo.resolve("memory")
    if !Files.isDirectory(snap) then
      fail(s"no snapshot dir at '$snap' — is --repo really the checkout that holds memory/?")

    val live = liveDirFor(repo)
    if !Files.isDirectory(live) then
      fail(
        s"""derived live store does not exist: $live
           |  repo: ${repo.toAbsolutePath.normalize}
           |  slug: ${slugOf(repo)}
           |This is the stale-pointer failure, caught loudly. Either the project moved (so Claude Code
           |started a new store under a new slug) or the slug rule changed. Do NOT hand-copy from a guess:
           |find the real dir under ~/.claude/projects/ and reconcile it deliberately.""".stripMargin
      )

    if !Files.isRegularFile(live.resolve("MEMORY.md")) then
      fail(s"'$live' has no MEMORY.md — that is not a memory store, refusing to copy from it")

    val liveN = listFiles(live).size
    val snapN = listFiles(snap).size
    if snapN >= 10 && liveN * 2 < snapN && !force then
      fail(
        s"""live store has COLLAPSED against the snapshot: $liveN live vs $snapN in the snapshot
           |  live: $live
           |That is the shape of a wrong source dir, not of normal editing. Investigate before syncing.
           |If the shrink is genuinely intended, re-run with --force and say why in the commit message.""".stripMargin
      )

    (live = live, snap = snap)
  }

  private def report(p: (added: Vector[Relpath], changed: Vector[Relpath], snapshotOnly: Vector[Relpath]), verb: String): Unit = {
    for r <- p.added   do println(s"  $verb (new)      $r")
    for r <- p.changed do println(s"  $verb (updated)  $r")
    for r <- p.snapshotOnly do println(s"  snapshot-only  $r   (kept: sync is additive)")
  }

  def dispatch(args: Seq[String]): Unit = {
    if args.contains("--help") || args.contains("-h") then { println(MemoryHelp); sys.exit(0) }
    val a       = args.toList
    val force   = a.contains("--force")
    val repoIdx = a.indexOf("--repo")
    val repo    = if repoIdx >= 0 && repoIdx + 1 < a.size then Path.of(a(repoIdx + 1)) else Path.of("")
    val consumed = if repoIdx >= 0 then Set(repoIdx, repoIdx + 1) else Set.empty[Int]
    val rest = a.zipWithIndex.collect { case (t, i) if !consumed(i) && t != "--force" => t }

    rest match {
      case "where" :: Nil =>
        println(liveDirFor(repo))

      case "check" :: Nil =>
        val d = checkedDirs(repo, force)
        val p = plan(d.live, d.snap)
        println(s"memory: check  ${d.live}  ->  ${d.snap}")
        report(p, "would copy")
        val drift = p.added.size + p.changed.size
        if drift == 0 then println(s"memory: in step (${listFiles(d.live).size} files)")
        else
          println(s"memory: DRIFTED — $drift file(s) would change (${p.added.size} new, ${p.changed.size} updated)")
          sys.exit(1)

      case "sync" :: Nil =>
        val d = checkedDirs(repo, force)
        val p = plan(d.live, d.snap)
        println(s"memory: sync  ${d.live}  ->  ${d.snap}")
        report(p, "copy")
        for r <- p.added ++ p.changed do
          val dst = d.snap.resolve(r)
          Option(dst.getParent).foreach(Files.createDirectories(_))
          Files.copy(d.live.resolve(r), dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        println(s"memory: done — ${p.added.size} new, ${p.changed.size} updated, ${p.snapshotOnly.size} snapshot-only kept")

      case _ =>
        Console.err.println("memory: usage: tt memory [ where | check | sync ] [--repo <dir>] [--force]")
        sys.exit(2)
    }
  }
}
