//> using file project.scala
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// issue — typed verb for the IN-REPO issue workflow (issue-032): the rules reqts/issues/README.md
// states in prose, made executable. `next` and `list` are PURE reads; `close` is the one effectful
// verb (a status-line rewrite + a file move, as ONE operation so directory and preamble can never
// disagree) and PREVIEWS by default with `--yes` to actuate, following `tt forge release-delete`.
// LOCAL clone only — no forge/network calls. Staging + committing stay with the caller: stage BOTH
// paths (`tt git commit --add` twice) so git records the rename from content similarity — which is
// also why a standalone move verb stayed OUT (issue-032's design question: verb sprawl for a shape
// only this workflow uses; the rename detection survives the bare move once both paths are staged).
//   tt issue next  [--repo <dir>]
//   tt issue list  [--state open|closed|all] [--repo <dir>]
//   tt issue close <NNN> (--fixed-by <ref> | --as <text>) [--date YYYY-MM-DD] [--yes] [--repo <dir>]
import scala.annotation.tailrec

// Helpers live INSIDE this object so top-level names don't collide with the other tools when the
// toolbox compiles as one unit. Only the @main entry is top-level. See skills/scala-style §1.
object Issue {
  private def fail(msg: String): Nothing = { System.err.println(s"issue: $msg"); sys.exit(2) }
  private def refuse(msg: String): Nothing = { System.err.println(s"issue: refusing: $msg"); sys.exit(1) }

  // --- pure core: names, numbers, preambles, the status rewrite (unit-tested without a filesystem) ---

  /** The file-name convention of reqts/issues/README.md: `issue-NNN-short-snake-case-name.md`.
    * Lenient upward (4+ digits) so the scheme survives issue 1000; READMEs and strays parse to None. */
  private val FileName = "issue-(\\d{3,})-[a-z0-9][a-z0-9-]*\\.md".r

  /** An issue file name -> its number, or None for non-issue files. Pure. */
  def issueNum(name: String): Option[Int] = name match
    case FileName(d) => d.toIntOption
    case _           => None

  /** Zero-padded 3-digit rendering (widens naturally past 999). Pure. */
  def nnn(n: Int): String = f"$n%03d"

  /** Next free number per the README: highest existing (across open AND closed) plus one, never
    * reused; 0 on an empty tracker. Takes NAMES so the rule is testable without a filesystem. Pure. */
  def nextNumber(names: Seq[String]): Int = names.flatMap(issueNum).maxOption.fold(0)(_ + 1)

  /** The block-quote preamble after the `#` heading: the FIRST contiguous run of `>` lines, joined
    * to one string with the `>` markers stripped (the preamble wraps freely across lines). Pure. */
  def preambleOf(text: String): Option[String] =
    val quoted = text.linesIterator.dropWhile(l => !l.startsWith(">")).takeWhile(_.startsWith(">")).toList
    Option.when(quoted.nonEmpty)(quoted.map(_.stripPrefix(">").trim).mkString(" ").trim)

  /** The finer-state text of a joined preamble: the first ·-segment, which must carry `status:`. Pure. */
  def statusField(pre: String): Option[String] =
    pre.split('·').headOption.map(_.trim).filter(_.startsWith("status:")).map(_.stripPrefix("status:").trim)

  /** The `labels:` segment of a joined preamble, comma-list as written. Pure. */
  def labelsField(pre: String): Option[String] =
    pre.split('·').map(_.trim).find(_.startsWith("labels:")).map(_.stripPrefix("labels:").trim)

  /** Everything after `summary:` in the joined preamble — NOT segment-split, because a summary may
    * span wrapped lines (and could itself contain a ·). Pure. */
  def summaryField(pre: String): Option[String] =
    val i = pre.indexOf("summary:")
    Option.when(i >= 0)(pre.drop(i + "summary:".length).trim)

  /** First word of a status field: "open, GATED" -> "open"; "closed 2026-08-13, fixed by ..." ->
    * "closed"; "done (2026-07-21)" -> "done". Pure. */
  def statusHead(status: String): String = status.takeWhile(_.isLetter).toLowerCase

  /** The finer states that mean "this ended" (closed/README.md: done, wontfix, duplicate, ...) —
    * used to detect a preamble that DISAGREES with its directory, the invariant the README says
    * must never break. */
  val closedStates: Set[String] = Set("closed", "done", "wontfix", "duplicate", "rejected")

  /** The status text a close writes: `closed <date>, <how it ended>`. The standing decision: closure
    * tracks the defect fixed on MAIN (a commit ref), not the release tag. Pure. */
  def closedStatus(date: String, end: String): String = s"closed $date, $end"

  /** Rewrite the FIRST `> status:` line's finer-state field (the text between `status:` and the
    * first `·`, or the line end when no `·` follows) to newStatus, leaving labels, summary, and
    * every other byte of the file untouched. Pure. */
  def rewriteStatus(text: String, newStatus: String): Either[String, String] =
    val lines = text.split("\n", -1).toVector
    lines.indexWhere(_.trim.startsWith("> status:")) match
      case -1 => Left("no `> status:` line found in the preamble")
      case i =>
        val line = lines(i)
        val afterKey = line.indexOf("status:") + "status:".length
        val sep = line.indexOf('·', afterKey)
        val rebuilt =
          if sep >= 0 then s"${line.take(afterKey)} $newStatus ${line.drop(sep)}"
          else s"${line.take(afterKey)} $newStatus"
        Right(lines.updated(i, rebuilt).mkString("\n"))

  // --- pure close-argument parsing (bloop-planClean style: Either, so refusals are unit-testable) ---

  private val DateRe = "\\d{4}-\\d{2}-\\d{2}".r
  // A ref (hash, tag, or ref path) that stays sane inside backticked markdown: no spaces/backticks.
  private val RefRe = "[A-Za-z0-9._/-]+".r

  final case class CloseOpts(num: Int, end: String, date: Option[String], yes: Boolean, repo: Option[String])

  /** Parse `close` arguments. Exactly one of --fixed-by (the normal case: the fixing commit on main)
    * or --as (the README's other endings: wontfix, duplicate, ...) is required; preview is the
    * DEFAULT (yes = false). Pure. */
  def parseClose(args: List[String]): Either[String, CloseOpts] =
    @tailrec
    def go(rest: List[String], num: Option[Int], fixed: Option[String], as: Option[String],
           date: Option[String], yes: Boolean, repo: Option[String]): Either[String, CloseOpts] =
      rest match
        case Nil =>
          val end = (fixed, as) match
            case (Some(r), None) => Right(s"fixed by `$r`")
            case (None, Some(t)) => Right(t)
            case (None, None)    => Left("close needs exactly one of --fixed-by <ref> or --as <text>")
            case _               => Left("close takes --fixed-by OR --as, not both")
          for
            n <- num.toRight("close needs an issue number NNN")
            e <- end
          yield CloseOpts(n, e, date, yes, repo)
        case "--fixed-by" :: r :: t =>
          if RefRe.matches(r) then go(t, num, Some(r), as, date, yes, repo)
          else Left(s"--fixed-by ref looks wrong: '$r' (letters, digits, . _ / - only)")
        case "--as" :: v :: t =>
          if v.trim.isEmpty then Left("--as text is empty")
          else if v.exists(c => c == '`' || c == '\n' || c == '·') then
            Left("--as text may not contain backticks, newlines, or the · separator")
          else go(t, num, fixed, Some(v.trim), date, yes, repo)
        case "--date" :: d :: t =>
          if DateRe.matches(d) then go(t, num, fixed, as, Some(d), yes, repo)
          else Left(s"--date must be YYYY-MM-DD, got '$d'")
        case "--yes" :: t         => go(t, num, fixed, as, date, yes = true, repo)
        case "--repo" :: d :: t   => go(t, num, fixed, as, date, yes, Some(d))
        case n :: t if num.isEmpty && n.toIntOption.exists(_ >= 0) => go(t, n.toIntOption, fixed, as, date, yes, repo)
        case other :: _ => Left(s"unexpected/incomplete argument '$other' (see tt issue --help)")
    go(args, None, None, None, None, yes = false, None)

  // --- pure list-row rendering ---

  final case class Row(num: Int, dirState: String, head: String, labels: String, summary: String)

  /** Cap a summary for the one-line list; truncation is visible, never silent. Pure. */
  def truncate(s: String, max: Int = 72): String =
    if s.length <= max then s else s.take(max - 1).reverse.dropWhile(_ == ' ').reverse + "…"

  /** One list line: number, state (the preamble's finer word — parked/wontfix/... — else the
    * directory), labels, summary; a ⚠ tail when preamble and directory DISAGREE about open vs
    * closed, the one invariant the README says must never break. Pure. */
  def listLine(r: Row): String =
    val state = if r.head.nonEmpty then r.head else r.dirState
    val warn =
      if r.head.isEmpty then "  ⚠ no status: field in the preamble"
      else if r.dirState == "open" && closedStates(r.head) then
        s"  ⚠ preamble says '${r.head}' but the file is in open/"
      else if r.dirState == "closed" && !closedStates(r.head) then
        s"  ⚠ preamble says '${r.head}' but the file is in closed/"
      else ""
    f"${nnn(r.num)}  $state%-9s [${r.labels}]  ${truncate(r.summary)}$warn"

  // --- help ---

  private val Help: String =
    """tt issue — typed verb for the in-repo issue workflow (reqts/issues/)
      |
      |Executes the rules reqts/issues/README.md states in prose: the next free number is the
      |highest across open/ AND closed/ plus one (never reused); closing = rewrite the `> status:`
      |preamble AND move the file open/ -> closed/, as ONE operation so the two cannot disagree.
      |LOCAL clone only — no forge calls; committing stays with you.
      |
      |Usage:
      |  issue next  [--repo <dir>]      print the next free NNN (zero-padded), nothing else
      |  issue list  [--state open|closed|all] [--repo <dir>]
      |                                  one line per issue: number, state, labels, summary
      |                                  (default --state open; ⚠ flags a preamble that
      |                                  disagrees with its directory)
      |  issue close <NNN> (--fixed-by <ref> | --as <text>) [--date YYYY-MM-DD] [--yes] [--repo <dir>]
      |                                  PREVIEWS the status rewrite + move; --yes applies both
      |Flags (close):
      |  --fixed-by <ref>                the fixing commit on main (hash or tag); becomes
      |                                  `closed <date>, fixed by <ref>` in the preamble
      |  --as <text>                     the non-fix endings instead: wontfix, duplicate, ...
      |                                  (exactly one of --fixed-by / --as is required)
      |  --date YYYY-MM-DD               closure date; DEFAULT is today from the system clock
      |                                  (the same source `tt chrono now` reads) — never guessed
      |  --yes                           actually rewrite + move (without it: preview, exit 0)
      |  --repo <dir>                    repo root holding reqts/issues/ (default: walk up
      |                                  from the cwd, like the other root-finding tools)
      |
      |close REFUSES (exit 1) rather than guessing: number not found, already in closed/, a file
      |in BOTH directories, several files claiming one number, a missing `> status:` line, or a
      |preamble already declaring a closed state while the file sits in open/.
      |
      |After a real close, stage BOTH paths so git records the rename, e.g.:
      |  tt git commit --repo <dir> --message-file tmp/msg.txt \
      |    --add reqts/issues/open/<file> --add reqts/issues/closed/<file>
      |
      |Examples:
      |  tt issue next                                   # 039
      |  tt issue list --state all
      |  tt issue close 23 --fixed-by 456f038            # preview only
      |  tt issue close 23 --fixed-by 456f038 --yes      # rewrite status + move the file
      |  tt issue close 19 --as wontfix --yes
      |
      |Full reference: tools/README.md""".stripMargin

  // --- effectful driver: filesystem reads, and for `close --yes` ONE rewrite + ONE move (os-lib) ---

  /** Locate <root>/reqts/issues: an explicit --repo wins, else walk up from the cwd (the house
    * root-finding idiom, cf. Lib.toolsDir). Fails loudly, never guesses a tracker into being. */
  private def issuesDir(repoOpt: Option[String]): os.Path =
    val root = repoOpt match
      case Some(d) => os.Path(d, os.pwd)
      case None =>
        Iterator.iterate(os.pwd)(p => if p.segmentCount > 0 then p / os.up else p).take(8)
          .find(d => os.isDir(d / "reqts" / "issues"))
          .getOrElse(fail("no reqts/issues/ found walking up from the cwd (pass --repo <dir>)"))
    val dir = root / "reqts" / "issues"
    if !os.isDir(dir / "open") || !os.isDir(dir / "closed") then
      fail(s"$dir lacks open/ and closed/ (not an in-repo issue tracker?)")
    dir

  private def namesIn(dir: os.Path): Vector[String] =
    if os.isDir(dir) then os.list(dir).map(_.last).toVector.sorted else Vector.empty

  /** Today's date from the system clock — the same source `tt chrono now` formats — never guessed. */
  private def todayStamp(): String =
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
      .withZone(java.time.ZoneId.systemDefault())
      .format(java.time.Instant.ofEpochMilli(System.currentTimeMillis()))

  private def cmdNext(repo: Option[String]): Unit =
    val base = issuesDir(repo)
    println(nnn(nextNumber(namesIn(base / "open") ++ namesIn(base / "closed"))))

  private def rowsIn(base: os.Path, dirState: String): Vector[Row] =
    namesIn(base / dirState).flatMap: name =>
      issueNum(name).map: n =>
        val pre = preambleOf(os.read(base / dirState / name)).getOrElse("")
        Row(n, dirState, statusHead(statusField(pre).getOrElse("")),
          labelsField(pre).getOrElse(""), summaryField(pre).getOrElse(""))

  private def cmdList(state: String, repo: Option[String]): Unit =
    val base = issuesDir(repo)
    val states = state match
      case "open" | "closed" => Vector(state)
      case "all"             => Vector("open", "closed")
      case other             => fail(s"--state must be open|closed|all, got '$other'")
    val rows = states.flatMap(s => rowsIn(base, s)).sortBy(_.num)
    if rows.isEmpty then println(s"no $state issues under $base")
    else rows.foreach(r => println(listLine(r)))

  private def cmdClose(o: CloseOpts): Unit =
    val base = issuesDir(o.repo)
    val id = nnn(o.num)
    val inOpen   = namesIn(base / "open").filter(n => issueNum(n).contains(o.num))
    val inClosed = namesIn(base / "closed").filter(n => issueNum(n).contains(o.num))
    if inOpen.isEmpty && inClosed.isEmpty then refuse(s"no issue $id under $base")
    if inOpen.nonEmpty && inClosed.nonEmpty then
      refuse(s"issue $id exists in BOTH open/ (${inOpen.mkString(", ")}) and closed/ (${inClosed.mkString(", ")}) — fix that by hand first")
    if inOpen.isEmpty then refuse(s"issue $id is already closed: closed/${inClosed.head}")
    if inOpen.length > 1 then refuse(s"issue $id claims ${inOpen.length} files in open/: ${inOpen.mkString(", ")}")
    val name = inOpen.head
    val from = base / "open" / name
    val to   = base / "closed" / name
    val text = os.read(from)
    val pre  = preambleOf(text).getOrElse(refuse(s"open/$name has no block-quote preamble to rewrite"))
    val status = statusField(pre).getOrElse(refuse(s"open/$name has no `status:` field in its preamble"))
    if closedStates(statusHead(status)) then
      refuse(s"open/$name already declares '${statusHead(status)}' in its preamble — directory and preamble disagree; fix by hand")
    val newStatus = closedStatus(o.date.getOrElse(todayStamp()), o.end)
    val newText = rewriteStatus(text, newStatus).fold(e => refuse(s"open/$name: $e"), identity)
    println(s"issue close $id${if o.yes then "" else " — PREVIEW (re-run with --yes to apply)"}")
    println(s"  status: '$status' -> '$newStatus'")
    println(s"  move:   reqts/issues/open/$name -> reqts/issues/closed/$name")
    if o.yes then
      os.write.over(from, newText)   // rewrite in place first: a crash here leaves a state close detects
      os.move(from, to)              // then the move; directory and preamble now agree by construction
      println(s"closed: $to")
      println("committing stays with you — stage BOTH paths so git records the rename:")
      println(s"  tt git commit --repo ${base / os.up / os.up} --message-file <msg> \\")
      println(s"    --add reqts/issues/open/$name --add reqts/issues/closed/$name")

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    args.toList match
      case "next" :: rest =>
        rest match
          case Nil                  => cmdNext(None)
          case "--repo" :: d :: Nil => cmdNext(Some(d))
          case other :: _ => fail(s"unexpected/incomplete argument '$other' (usage: tt issue next [--repo <dir>])")
      case "list" :: rest =>
        @tailrec def go(r: List[String], state: String, repo: Option[String]): Unit = r match
          case Nil                 => cmdList(state, repo)
          case "--state" :: s :: t => go(t, s, repo)
          case "--repo" :: d :: t  => go(t, state, Some(d))
          case other :: _ =>
            fail(s"unexpected/incomplete argument '$other' (usage: tt issue list [--state open|closed|all] [--repo <dir>])")
        go(rest, "open", None)
      case "close" :: rest => parseClose(rest).fold(fail, cmdClose)
      case _ =>
        System.err.println(
          """issue: usage:
            |  tt issue next  [--repo <dir>]
            |  tt issue list  [--state open|closed|all] [--repo <dir>]
            |  tt issue close <NNN> (--fixed-by <ref> | --as <text>) [--date YYYY-MM-DD] [--yes] [--repo <dir>]
            |close PREVIEWS by default; --yes rewrites the status line AND moves the file as one operation.""".stripMargin)
        sys.exit(2)
}

@main def trackInRepoIssues(args: String*): Unit = Issue.dispatch(args*)
