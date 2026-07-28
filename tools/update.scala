//> using file project.scala
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8
//> using dep com.lihaoyi::requests:0.9.3
//> using dep com.lihaoyi::ujson:4.4.3
//> using file lib.scala
//> using file releaselib.scala
//> using file ziplib.scala

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

import agenttools.{Lib, ReleaseLib, ZipLib}
import scala.util.Try

object Update:

  private def die(msg: String): Nothing = { System.err.println(s"update: $msg"); sys.exit(2) }
  // The steps the human runs in Claude Code (the tool cannot drive the harness itself).
  // ⚠ `/plugin marketplace update` takes the MARKETPLACE name (`bjornregnell`, from
  // .claude-plugin/marketplace.json), NOT the plugin name (`genscalator`) - the v0.9.1 gotcha.
  // This line shipped with the plugin name for a while (SM255 audit finding H1); the docs were
  // fixed in c1fbcd5 and this, the copy every behind-version tester actually runs, was not.
  private val ManualSteps =
    """  To update, run these in Claude Code:
      |    /plugin marketplace update bjornregnell
      |    /reload-plugins
      |  (Third-party marketplaces do not auto-update by default, so this is a manual step.
      |   The name above is the MARKETPLACE name, not the plugin name.)""".stripMargin

  private def git(repo: os.Path, args: String*): (Int, String) = gitTimed(repo, 120_000, args*)

  private def gitTimed(repo: os.Path, timeoutMs: Long, args: String*): (Int, String) =
    Try(os.proc(("git" +: "-C" +: repo.toString +: args)).call(
      check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = timeoutMs)) match
      case scala.util.Success(res) => (res.exitCode, (res.out.text() + res.err.text()).trim)
      case scala.util.Failure(e)   => (255, e.getMessage)

  // Throttle stamp: a user-level cache file holding the epoch-millis of the last completed check, so a
  // `gs warm` call (`tt update --brief --throttle 24`) only actually fetches once per window.
  private def stampPath: os.Path =
    os.Path(System.getProperty("user.home")) / ".cache" / "genscalator" / "last-update-check"

  private def readStamp(): Option[Long] = Try(os.read(stampPath).trim.toLong).toOption

  private def writeStamp(nowMs: Long): Unit = Try(os.write.over(stampPath, nowMs.toString, createFolders = true))

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
      case _                  => Lib.rootDir().map(r => os.Path(r.toAbsolutePath.normalize))

  // ================================================================================================
  // --native — replace the INSTALLED native toolbox with the latest published release.
  //
  // This is the sharpest verb in the toolbox: the file it replaces may be the very binary executing it.
  // D7b settled how (reqts/DESIGN.md), and the answer needs no platform branch. Renaming a RUNNING
  // executable is permitted on both families — on POSIX because rename unlinks the old inode while the
  // running process keeps it, and on Windows because renaming a live image is allowed even though
  // OVERWRITING it is not. So the swap is two renames, never a write-through:
  //     move the current install aside, then move the staged one in.
  // That claim was not taken on reasoning alone: RunningBinaryRenameSuite verified it on real Windows
  // (CI run 30301424616, job 90095042530) with `0 ignored` read from the log, because a guarded test
  // that SKIPS leaves the job green and proves nothing.
  //
  // PREVIEW BY DEFAULT, applying only with --write — the same contract as `tt sub`, `tt zip extract` and
  // `tt forge release-delete`. Nothing is downloaded to a permanent place and nothing is moved without it.
  // ================================================================================================

  private val DefaultReleaseRepo = "bjornregnell/genscalator"

  /** The asset pair for a platform — payload AND its sibling checksum — as one `*`-only glob, so the
    * download cannot fetch the zip while silently missing the .sha256 that makes verification possible. */
  def assetPattern(platform: String): String = s"genscalator-$platform.zip*"

  /** Where the new tree is staged, and where the outgoing one is retired to.
    *
    * SIBLINGS of the install rather than children, and that is structural rather than stylistic: the swap
    * renames the install directory itself, and a staging dir INSIDE the directory being renamed would move
    * with it. Pure, so the naming is unit-testable without touching a filesystem. */
  def swapPaths(home: os.Path, nowMs: Long): (staging: os.Path, retired: os.Path) =
    val parent = home / os.up
    (staging = parent / s"${home.last}-new-$nowMs", retired = parent / s"${home.last}-old-$nowMs")

  /** The install to replace: an explicit --home, else GENSCALATOR_HOME, else ~/.genscalator.
    *
    * ⚠ Deliberately NOT `Lib.rootDir()`, which also falls back to a CONTRIBUTOR'S GIT CLONE via the cwd
    * walk-up. That is right for the read-only verbs that share it and catastrophic here: this verb would
    * cheerfully rename someone's working checkout aside and drop a release tree in its place. The guard
    * below refuses a git checkout outright, because resolving to one is a mistake no message can undo. */
  private def resolveHome(explicit: Option[String]): os.Path =
    val home = explicit.map(os.Path(_, os.pwd))
      .orElse(sys.env.get("GENSCALATOR_HOME").filter(_.trim.nonEmpty).map(s => os.Path(s.trim, os.pwd)))
      .getOrElse(os.Path(System.getProperty("user.home")) / ".genscalator")
    if os.exists(home / ".git") then die(
      s"refusing: $home is a git checkout, not a binary install.\n" +
        "  Update a checkout with git and rebuild from source; --native replaces an INSTALLED tree.\n" +
        "  Pass --home <dir> or set GENSCALATOR_HOME to target a real install.")
    home

  /** The tag recorded inside an installed tree, as CI wrote it (see native-release.yml). */
  private def installedVersion(home: os.Path): Option[String] =
    Try(os.read(home / "VERSION.txt").trim).toOption.filter(_.nonEmpty)

  private def nativeUpdate(args: List[String]): Unit =
    def flagVal(name: String): Option[String] =
      val i = args.indexOf(name)
      if i >= 0 && i + 1 < args.size then Some(args(i + 1)) else None

    val write = args.contains("--write")

    // None is a REAL answer here and must not become a guess: Intel macOS and Windows-on-ARM publish no
    // asset, and downloading a nearby platform's binary would install something that cannot run — worse
    // than being told to build from source. See Lib.releasePlatform.
    val platform = Lib
      .releasePlatform(sys.props.getOrElse("os.name", ""), sys.props.getOrElse("os.arch", ""))
      .getOrElse(die(
        s"no published binary for this platform (${sys.props.getOrElse("os.name", "?")} " +
          s"${sys.props.getOrElse("os.arch", "?")}).\n" +
          "  Build from source instead — that is the supported route for the unproven platforms,\n" +
          "  and it is documented rather than a workaround."))

    val home  = resolveHome(flagVal("--home"))
    val rl    = ReleaseLib.Client("update")
    val (owner, repo) = rl.splitRepo(flagVal("--repo").getOrElse(DefaultReleaseRepo))
    val dialect       = ReleaseLib.Dialect.GitHub
    val base          = "https://github.com"

    val (rel, tag) = flagVal("--tag") match
      case Some(t) => (rl.findRelease(owner, repo, t, dialect, base), t)
      case None    => rl.latestRelease(owner, repo, dialect, base)

    // Named-tag refusal, because "release carries no assets" from deep inside the download path does not
    // say WHICH release or why, and this is the FIRST wall a tester hits when the newest published release
    // predates the native build matrix. Found by the first live run, 2026-07-28: the only published
    // release was v0.9.2, cut before the matrix existed, so it carries no binaries at all.
    if ReleaseLib.assetsOf(rel).isEmpty then die(
      s"release '$tag' carries no downloadable assets, so there is nothing to install.\n" +
        "  The newest PUBLISHED release predates the native build matrix (or its assets were removed).\n" +
        "  Fix by cutting a release WITH platform binaries, or pass --tag <tag> naming one that has them.\n" +
        s"  Check what exists: tt forge releases $owner/$repo --gh")

    val installed = installedVersion(home)
    println(s"platform:  $platform")
    println(s"install:   $home  (${installed.getOrElse("no VERSION.txt — not a genscalator install?")})")
    println(s"available: $tag")
    if installed.contains(tag) && flagVal("--tag").isEmpty then
      println("already up to date; nothing to do.")
      sys.exit(0)

    // Staging lives beside the install so the swap is two renames. Everything below happens in staging
    // until the very last step, so a failure at any point leaves the running install untouched.
    val nowMs              = System.currentTimeMillis
    val (staging, retired) = swapPaths(home, nowMs)
    val dl                 = staging / "download"
    val tree               = staging / "tree"

    val got = rl.downloadAssets(rel, Some(assetPattern(platform)), dl, dialect, base, "update --native")
    // INSIST on verification rather than reporting it: this writes an executable that will run as the
    // user. verifyChecksums returns the count it actually checked, so "no sibling .sha256 was published"
    // cannot pass as "verified" — which is precisely the confusion the SM241 Class-B rule exists to stop.
    val verified = rl.verifyChecksums(got)
    if verified < 1 then die(
      "refusing: nothing was verified against a published .sha256, so the payload is unproven.\n" +
        "  --native will not install bytes it cannot check.")

    val zips = got.filter(_.last.endsWith(".zip"))
    val zip  = if zips.sizeIs == 1 then zips.head else die(
      s"expected exactly one .zip for $platform, got ${zips.size}: ${zips.map(_.last).mkString(", ")}")

    // Every CRC32 validated before a single byte is unpacked: a sha256 proves the bytes arrived as sent,
    // a CRC pass proves the archive is internally sound. Different questions, and a self-updater wants both.
    val bad = ZipLib.failures(zip.toNIO)
    if bad.nonEmpty then
      bad.foreach((n, err) => println(s"FAILED  $n  $err"))
      die(s"${bad.size} entry/entries failed to decompress; the archive is not sound")

    val plan = Try(ZipLib.planExtraction(zip.toNIO, tree.toNIO)).fold(t => die(t.getMessage), identity)

    if !write then
      println(s"would install $tag over $home")
      println(s"  ${plan.planned.size} file(s), ${plan.total} B, every CRC32 valid, $verified payload(s) sha256-verified")
      println(s"  swap: $home -> $retired, then the staged tree -> $home  (two renames, no write-through)")
      println("  re-run with --write to apply")
      Try(os.remove.all(staging))
      sys.exit(0)

    // ExecGlob: java.util.zip restores no permission bits, so without this the installed launcher exits
    // 126 on its first use — the exact wall a tester hits following the documented install path.
    ZipLib.extract(zip.toNIO, tree.toNIO, plan, Some("bin/*"))

    // THE SWAP. Two renames, in this order, so the window in which no install exists is one syscall wide.
    val hadInstall = os.exists(home)
    if hadInstall then os.move(home, retired, atomicMove = true)
    Try(os.move(tree, home, atomicMove = true)).fold(
      t =>
        // Put it back rather than leaving the user with nothing: this is the one failure that would
        // otherwise remove a working toolbox and replace it with an error message.
        if hadInstall then Try(os.move(retired, home, atomicMove = true))
        die(s"the swap failed (${t.getMessage}); the previous install was restored"),
      identity)

    println(s"installed $tag -> $home")
    if hadInstall then
      // Best-effort, and its failure is cosmetic: the tree is already replaced. On Windows a leftover
      // directory whose files are open cannot be removed until the next start, which is the only genuinely
      // Windows-shaped residue of this design.
      if Try(os.remove.all(retired)).isFailure then
        println(s"  note: could not remove the previous install at $retired — remove it when convenient")
    Try(os.remove.all(staging))
    println("  run `tt --version` (or any verb) to confirm; the binary you just replaced kept running.")

  def dispatch(args: List[String]): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    if args.contains("--native") then { nativeUpdate(args); sys.exit(0) }
    // --throttle <hours>: only actually check once per window (stamp-file gated); implies --brief and a short
    // fetch timeout, so `gs warm` gains update-awareness without ever hanging or nagging.
    val throttleHours: Option[Double] =
      val i = args.indexOf("--throttle")
      if i >= 0 && i + 1 < args.size then args(i + 1).toDoubleOption else None
    throttleHours.foreach: hours =>
      val now = System.currentTimeMillis
      if readStamp().exists(ts => now - ts < (hours * 3600 * 1000).toLong) then sys.exit(0) // fresh: silent skip
      else writeStamp(now)                                                                  // stale: open a new window
    val brief = args.contains("--brief") || throttleHours.isDefined
    // In --brief mode, only an actionable "you are behind" notice is printed; everything else stays silent.
    def say(s: String): Unit = if !brief then println(s)

    val repo = resolveRepo(args).getOrElse:
      say("update: could not locate the genscalator repo — pass --repo <dir>.")
      sys.exit(0) // informational check, never a hard error

    // Graceful degrade: not a git checkout -> cannot self-check; still tell the human how to update.
    // ⚠ NOT silent in --brief (Hans's alpha finding, 2026-07-28): --brief used to print NOTHING here,
    // indistinguishable from "up to date" — and since `gs warm` calls --brief, every plugin-cache
    // install silently lost update-awareness. Cannot-check is actionable, so brief says it in one
    // line (the throttle keeps it to once per window, never a nag).
    val isGit = os.exists(repo / ".git") || git(repo, "rev-parse", "--git-dir")._1 == 0
    if !isGit then
      if brief then
        println(s"genscalator: cannot self-check for updates ($repo is not a git checkout) — " +
          "binary installs update via `tt update --native`; plugin installs via /plugin marketplace update bjornregnell")
      else
        say(s"genscalator at $repo is not a git checkout, so the version cannot be self-checked.")
        say(ManualSteps)
      sys.exit(0)

    say(s"genscalator: $repo")
    val version = git(repo, "describe", "--tags", "--always", "--dirty")._2
    if version.nonEmpty then say(s"  installed: $version")

    // Fetch remote-tracking refs (read-only; never the working tree). Short timeout under --throttle so a
    // throttled `gs warm` call never hangs on a slow network.
    val fetchTimeout = if throttleHours.isDefined then 8_000L else 120_000L
    val (fc, fout) = gitTimed(repo, fetchTimeout, "fetch", "--quiet")
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
      |  tt update [--repo <dir>] [--brief] [--throttle <hours>]
      |  --repo <dir>        the genscalator repo to check (default: self-locate via the tools dir)
      |  --brief             print ONLY an actionable "newer release available" notice; silent otherwise
      |  --throttle <hours>  only actually check once per <hours> window (stamp-file gated); implies --brief
      |                      and a short fetch timeout — for `gs warm` to call so warm gains update-awareness
      |                      without hanging or nagging
      |
      |Exit is 0 in all normal cases (an informational check). Degrades gracefully when offline, when
      |there is no upstream branch, or when genscalator is not a git checkout.
      |
      |  tt update --native [--home <dir>] [--repo <owner/repo>] [--tag <tag>] [--write]
      |                      replace an INSTALLED native toolbox with the latest published release.
      |
      |PREVIEWS by default like `tt sub` and `tt zip extract`; --write applies. Downloads the asset for
      |THIS platform, verifies its published sha256, validates every CRC32 in the archive, unpacks to a
      |staging dir beside the install, and only then swaps — by renaming the old install aside and the new
      |one in. Two renames, never a write-through: replacing a RUNNING executable by overwriting it can
      |corrupt the live process on POSIX and is refused outright on Windows, while RENAMING one is
      |permitted on both. One code path, no platform branch (D7b, verified on Windows CI).
      |
      |It REFUSES rather than guesses in five places, each of which would otherwise brick an install:
      |a platform with no published binary (build from source — the documented route), a --home that is a
      |git checkout rather than a binary install, an archive whose payload had no published .sha256 to
      |check it against, a release carrying no downloadable assets at all, and an asset that does not
      |unpack to exactly one .zip payload.
      |
      |--home defaults to GENSCALATOR_HOME, then ~/.genscalator. Deliberately NOT the repo self-locate the
      |other verbs use, which can resolve to a contributor's git clone.""".stripMargin

@main def checkGenscalatorUpdate(args: String*): Unit = Update.dispatch(args.toList)
