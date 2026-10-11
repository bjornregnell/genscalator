//> using file project.scala
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// issue — typed verb for the IN-REPO issue workflow (issue-032): the rules reqts/issues/README.md
// states in prose, made executable. `list` is a PURE read; `new` creates ONE file (refusing to
// overwrite); `close` is a status-line rewrite + a file move, as ONE operation so directory and
// preamble can never disagree, and PREVIEWS by default with `--yes` to actuate, following
// `tt forge release-delete`.
// Issue identity (issue 073): new issues are `issue-YYMMDD-slug.md`, YYMMDD the UTC date the tool
// mints, cited as `YYMMDD-slug`. The legacy numbers 000-073 are frozen; `list` and `close` take both.
// `next` (the old global counter) is retired: it refuses and points at `new`.
// LOCAL clone only — no forge/network calls. Staging + committing stay with the caller: stage BOTH
// paths (`tt git commit --add` twice) so git records the rename from content similarity — which is
// also why a standalone move verb stayed OUT (issue-032's design question: verb sprawl for a shape
// only this workflow uses; the rename detection survives the bare move once both paths are staged).
//   tt issue new   <slug> [--title <text>] [--date YYYY-MM-DD] [--repo <dir>]
//   tt issue list  [--state open|closed|all] [--repo <dir>]
//   tt issue close <ID> (--fixed-by <ref> | --as <text>) [--date YYYY-MM-DD] [--yes] [--repo <dir>]
import scala.annotation.tailrec
import java.time.{Clock, LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Helpers live INSIDE this object so top-level names don't collide with the other tools when the
// toolbox compiles as one unit. Only the @main entry is top-level. See skills/scala-style §1.
object Issue {
  private def fail(msg: String): Nothing = { System.err.println(s"issue: $msg"); sys.exit(2) }
  private def refuse(msg: String): Nothing = { System.err.println(s"issue: refusing: $msg"); sys.exit(1) }

  // --- pure core: identity, names, preambles, the status rewrite (unit-tested without a filesystem) ---

  /** Zero-padded 3-digit rendering (widens naturally past 999). Pure. */
  def nnn(n: Int): String = f"$n%03d"

  private val Yymmdd = DateTimeFormatter.ofPattern("yyMMdd")

  /** A date as the 6-digit ID prefix: 2026-10-11 -> "261011". Pure. */
  def yymmdd(d: LocalDate): String = Yymmdd.format(d)

  /** "261011" -> 2026-10-11; None for a non-date ("261399"). Two-digit years are 20YY. Pure. */
  def parseYymmdd(s: String): Option[LocalDate] =
    if s.length != 6 || !s.forall(_.isDigit) then None
    else scala.util.Try(LocalDate.of(2000 + s.take(2).toInt, s.slice(2, 4).toInt, s.drop(4).toInt)).toOption

  /** An issue's identity. Legacy = the frozen numbers 000-073; Dated = `YYMMDD-slug` (issue 073). */
  enum IssueId:
    case Legacy(n: Int)
    case Dated(date: LocalDate, slug: String)

    /** How the ID is cited and how it appears in the file name: `073`, `261011-issue-identity`. */
    def render: String = this match
      case Legacy(n)       => nnn(n)
      case Dated(d, slug)  => s"${yymmdd(d)}-$slug"

  /** Legacy first (by number), then dated (by day, then slug). Agrees with plain file-name sort for
    * every real file (legacy names start with 0, dated ones with 2), but does not depend on it. */
  given idOrdering: Ordering[IssueId] = Ordering.fromLessThan: (a, b) =>
    (a, b) match
      case (IssueId.Legacy(x), IssueId.Legacy(y))         => x < y
      case (IssueId.Legacy(_), IssueId.Dated(_, _))       => true
      case (IssueId.Dated(_, _), IssueId.Legacy(_))       => false
      case (IssueId.Dated(d1, s1), IssueId.Dated(d2, s2)) => d1.isBefore(d2) || (d1 == d2 && s1 < s2)

  /** A slug: 1 to 4 lower-case kebab-case words (letters and digits). */
  private val SlugRe = "[a-z0-9]+(?:-[a-z0-9]+){0,3}"

  /** Legacy file names: `issue-NNN-name.md` (3 to 5 digits, so `issue-1000-...` still parses, while a
    * 6-digit date prefix is never mistaken for a number). */
  private val LegacyName = "issue-(\\d{3,5})-[a-z0-9][a-z0-9-]*\\.md".r
  /** New file names: `issue-YYMMDD-slug.md`. */
  private val DatedName = s"issue-(\\d{6})-($SlugRe)\\.md".r
  /** A dated ID as cited on the command line: `YYMMDD-slug`. */
  private val DatedId = s"(\\d{6})-($SlugRe)".r

  /** An issue file name -> its ID, or None for non-issue files (READMEs, strays, a bad date). Pure. */
  def issueId(name: String): Option[IssueId] = name match
    case DatedName(d, slug) => parseYymmdd(d).map(IssueId.Dated(_, slug))
    case LegacyName(d)      => d.toIntOption.map(IssueId.Legacy(_))
    case _                  => None

  /** An issue file name -> its LEGACY number, or None (dated issues have no number). Pure. */
  def issueNum(name: String): Option[Int] = issueId(name).collect { case IssueId.Legacy(n) => n }

  /** A command-line ID: legacy `23` / `073`, or dated `261011-issue-identity`. A bare date is refused,
    * because it is not unique: a citation always carries the slug. Pure. */
  def parseId(s: String): Either[String, IssueId] = s match
    case d if d.nonEmpty && d.forall(_.isDigit) && d.length <= 5 => Right(IssueId.Legacy(d.toInt))
    case d if d.length == 6 && d.forall(_.isDigit) =>
      Left(s"'$d' is a bare date, not an issue ID: cite the full ID, date and slug (YYMMDD-slug)")
    case DatedId(d, slug) =>
      parseYymmdd(d).map(IssueId.Dated(_, slug)).toRight(s"'$d' in '$s' is not a valid YYMMDD date")
    case other => Left(s"'$other' is not an issue ID (legacy NNN, or YYMMDD-slug)")

  /** Validate a slug for `new`: 1 to 4 lower-case kebab-case words. Pure. */
  def validateSlug(slug: String): Either[String, String] =
    if slug.matches(SlugRe) then Right(slug)
    else Left(s"bad slug '$slug': use 1 to 4 lower-case words of letters and digits, joined by '-' (e.g. issue-identity)")

  /** Today's date in UTC, whatever zone the clock (and the JVM default) is in. The clock is injected
    * so tests can pin it. Deliberately NOT `tt chrono` (its --utc printed local time, issue 073). Pure
    * given the clock. */
  def utcToday(clock: Clock): LocalDate = LocalDate.now(clock.withZone(ZoneOffset.UTC))

  /** Whole days from an issue's ID date to today; None for legacy IDs (their numbers carry no date). Pure. */
  def ageDays(id: IssueId, today: LocalDate): Option[Long] = id match
    case IssueId.Dated(d, _) => Some(ChronoUnit.DAYS.between(d, today))
    case IssueId.Legacy(_)   => None

  /** The minimal new-issue file in the house format. Pure. */
  def newIssueText(date: LocalDate, slug: String, title: Option[String]): String =
    s"""# Issue ${IssueId.Dated(date, slug).render}: ${title.getOrElse("TODO")}
       |
       |> status: open $date · labels: · summary:
       |
       |## Description
       |
       |## How to reproduce it
       |
       |## Acceptance sketch
       |
       |## Discussion
       |""".stripMargin

  /** An optional title, trimmed; empty or multi-line is refused (it becomes the `#` heading). Pure. */
  def validateTitle(title: Option[String]): Either[String, Option[String]] = title.map(_.trim) match
    case Some(t) if t.isEmpty                               => Left("--title is empty")
    case Some(t) if t.exists(c => c == '\n' || c == '\r')   => Left("--title may not contain a newline")
    case other                                              => Right(other)

  /** Plan `new`: validate, mint the ID from `date`, and refuse an ID already in open/ OR closed/ (or a
    * date after today, which would break inception order). Returns (file name, file text). Takes
    * NAMES so the refusals are testable without a filesystem. Pure. */
  def planNew(open: Seq[String], closed: Seq[String], slug: String, title: Option[String],
              date: LocalDate, today: LocalDate): Either[String, (String, String)] =
    for
      s  <- validateSlug(slug)
      t  <- validateTitle(title)
      _  <- Either.cond(!date.isAfter(today), (), s"date $date is after today ($today, UTC): an ID records when the issue was conceived")
      id  = IssueId.Dated(date, s)
      name = s"issue-${id.render}.md"
      _  <- Either.cond(!open.contains(name), (), s"issue ${id.render} already exists: open/$name (pick another slug)")
      _  <- Either.cond(!closed.contains(name), (), s"issue ${id.render} already exists: closed/$name (IDs are never reused; pick another slug)")
    yield (name, newIssueText(date, s, t))

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

  // --- pure argument parsing (bloop-planClean style: Either, so refusals are unit-testable) ---

  private val DateRe = "\\d{4}-\\d{2}-\\d{2}".r
  // A ref (hash, tag, or ref path) that stays sane inside backticked markdown: no spaces/backticks.
  private val RefRe = "[A-Za-z0-9._/-]+".r

  final case class CloseOpts(id: IssueId, end: String, date: Option[String], yes: Boolean, repo: Option[String])

  /** Parse `close` arguments. Exactly one of --fixed-by (the normal case: the fixing commit on main)
    * or --as (the README's other endings: wontfix, duplicate, ...) is required; preview is the
    * DEFAULT (yes = false). The ID is a legacy number or a dated `YYMMDD-slug`. Pure. */
  def parseClose(args: List[String]): Either[String, CloseOpts] =
    @tailrec
    def go(rest: List[String], id: Option[IssueId], fixed: Option[String], as: Option[String],
           date: Option[String], yes: Boolean, repo: Option[String]): Either[String, CloseOpts] =
      rest match
        case Nil =>
          val end = (fixed, as) match
            case (Some(r), None) => Right(s"fixed by `$r`")
            case (None, Some(t)) => Right(t)
            case (None, None)    => Left("close needs exactly one of --fixed-by <ref> or --as <text>")
            case _               => Left("close takes --fixed-by OR --as, not both")
          for
            i <- id.toRight("close needs an issue ID (legacy NNN, or YYMMDD-slug)")
            e <- end
          yield CloseOpts(i, e, date, yes, repo)
        case "--fixed-by" :: r :: t =>
          if RefRe.matches(r) then go(t, id, Some(r), as, date, yes, repo)
          else Left(s"--fixed-by ref looks wrong: '$r' (letters, digits, . _ / - only)")
        case "--as" :: v :: t =>
          if v.trim.isEmpty then Left("--as text is empty")
          else if v.exists(c => c == '`' || c == '\n' || c == '·') then
            Left("--as text may not contain backticks, newlines, or the · separator")
          else go(t, id, fixed, Some(v.trim), date, yes, repo)
        case "--date" :: d :: t =>
          if DateRe.matches(d) then go(t, id, fixed, as, Some(d), yes, repo)
          else Left(s"--date must be YYYY-MM-DD, got '$d'")
        case "--yes" :: t         => go(t, id, fixed, as, date, yes = true, repo)
        case "--repo" :: d :: t   => go(t, id, fixed, as, date, yes, Some(d))
        case a :: t if id.isEmpty && !a.startsWith("-") =>
          parseId(a) match
            case Right(i) => go(t, Some(i), fixed, as, date, yes, repo)
            case Left(e)  => Left(e)
        case other :: _ => Left(s"unexpected/incomplete argument '$other' (see tt issue --help)")
    go(args, None, None, None, None, yes = false, None)

  final case class NewOpts(slug: String, title: Option[String], date: Option[LocalDate], repo: Option[String])

  /** Parse `new` arguments: one positional slug, optional --title/--date/--repo. The slug's SHAPE is
    * checked later by planNew (a refusal, exit 1); this only checks the argument structure. Pure. */
  def parseNew(args: List[String]): Either[String, NewOpts] =
    @tailrec
    def go(rest: List[String], slug: Option[String], title: Option[String], date: Option[LocalDate],
           repo: Option[String]): Either[String, NewOpts] =
      rest match
        case Nil => slug.toRight("new needs a slug: tt issue new <slug> (1 to 4 kebab-case words)")
                      .map(NewOpts(_, title, date, repo))
        case "--title" :: v :: t => go(t, slug, Some(v), date, repo)
        case "--date" :: d :: t =>
          val parsed = Option.when(DateRe.matches(d))(scala.util.Try(LocalDate.parse(d)).toOption).flatten
          parsed match
            case Some(ld) => go(t, slug, title, Some(ld), repo)
            case None     => Left(s"--date must be a valid YYYY-MM-DD, got '$d'")
        case "--repo" :: d :: t => go(t, slug, title, date, Some(d))
        case a :: t if slug.isEmpty && !a.startsWith("-") => go(t, Some(a), title, date, repo)
        case other :: _ => Left(s"unexpected/incomplete argument '$other' (see tt issue --help)")
    go(args, None, None, None, None)

  // --- pure list-row rendering ---

  final case class Row(id: IssueId, dirState: String, head: String, labels: String, summary: String,
                       age: Option[Long] = None)

  /** Cap a summary for the one-line list; truncation is visible, never silent. Pure. */
  def truncate(s: String, max: Int = 72): String =
    if s.length <= max then s else s.take(max - 1).reverse.dropWhile(_ == ' ').reverse + "…"

  /** One list line: ID (with its age in days for a dated ID), state (the preamble's finer word,
    * parked/wontfix/..., else the directory), labels, summary; a ⚠ tail when preamble and directory
    * DISAGREE about open vs closed, the one invariant the README says must never break. Pure. */
  def listLine(r: Row): String =
    val state = if r.head.nonEmpty then r.head else r.dirState
    val warn =
      if r.head.isEmpty then "  ⚠ no status: field in the preamble"
      else if r.dirState == "open" && closedStates(r.head) then
        s"  ⚠ preamble says '${r.head}' but the file is in open/"
      else if r.dirState == "closed" && !closedStates(r.head) then
        s"  ⚠ preamble says '${r.head}' but the file is in closed/"
      else ""
    val idCol = r.id.render + r.age.fold("")(a => s" (${a}d)")
    f"$idCol  $state%-9s [${r.labels}]  ${truncate(r.summary)}$warn"

  // --- help ---

  /** What `next` says now that the counter is retired (issue 073). */
  val NextRetired: String =
    """issue: `tt issue next` is retired (issue 073): new issues are not numbered any more.
      |  mint an ID and create the file with:  tt issue new <slug> [--title <text>]
      |  (the ID is YYMMDD-slug, the UTC date plus 1 to 4 kebab-case words; legacy numbers
      |  000-073 are frozen; see reqts/issues/README.md)""".stripMargin

  private val Help: String =
    """tt issue — typed verb for the in-repo issue workflow (reqts/issues/)
      |
      |Executes the rules reqts/issues/README.md states in prose. Issue IDs (issue 073) are
      |`YYMMDD-slug`: the UTC date the issue was conceived plus 1 to 4 kebab-case words, minted by
      |`new`, so no counter and no coordination is needed. The legacy numbers 000-073 are frozen;
      |`list` and `close` accept both forms. Closing = rewrite the `> status:` preamble AND move the
      |file open/ -> closed/, as ONE operation so the two cannot disagree.
      |LOCAL clone only — no forge calls; committing stays with you.
      |
      |Usage:
      |  issue new <slug> [--title <text>] [--date YYYY-MM-DD] [--repo <dir>]
      |                                  mint YYMMDD-slug from today's UTC date and CREATE
      |                                  open/issue-YYMMDD-slug.md from the house template;
      |                                  prints the path
      |  issue list  [--state open|closed|all] [--repo <dir>]
      |                                  one line per issue: ID (age in days for dated IDs),
      |                                  state, labels, summary (default --state open; ⚠ flags
      |                                  a preamble that disagrees with its directory)
      |  issue close <ID> (--fixed-by <ref> | --as <text>) [--date YYYY-MM-DD] [--yes] [--repo <dir>]
      |                                  PREVIEWS the status rewrite + move; --yes applies both.
      |                                  <ID> is a legacy number (23, 073) or YYMMDD-slug
      |  issue next                      RETIRED: prints a pointer to `new` and exits 1
      |Flags (new):
      |  --title <text>                  the heading's title (default: TODO)
      |  --date YYYY-MM-DD               the day the issue was conceived, when that was before
      |                                  today (a draft written earlier); never after today.
      |                                  DEFAULT is today's UTC date, computed in-process
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
      |new REFUSES (exit 1) a bad slug, or an ID that already exists in open/ OR closed/ (same
      |UTC day and same slug: pick another slug).
      |close REFUSES (exit 1) rather than guessing: ID not found, already in closed/, a file
      |in BOTH directories, several files claiming one ID, a missing `> status:` line, or a
      |preamble already declaring a closed state while the file sits in open/.
      |
      |After a real close, stage BOTH paths so git records the rename, e.g.:
      |  tt git commit --repo <dir> --message-file tmp/msg.txt \
      |    --add reqts/issues/open/<file> --add reqts/issues/closed/<file>
      |
      |Examples:
      |  tt issue new issue-identity --title "make issue IDs unique"
      |  tt issue list --state all
      |  tt issue close 23 --fixed-by 456f038                     # preview only
      |  tt issue close 261011-issue-identity --fixed-by 456f038 --yes
      |  tt issue close 19 --as wontfix --yes
      |
      |Full reference: tools/README.md""".stripMargin

  // --- effectful driver: filesystem reads; `new` writes ONE file; `close --yes` ONE rewrite + ONE move ---

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

  /** The real clock. Its zone is the JVM default; utcToday ignores that zone by construction. */
  private def systemToday(): LocalDate = utcToday(Clock.systemDefaultZone())

  /** Today's date from the system clock — the same source `tt chrono now` formats — never guessed. */
  private def todayStamp(): String =
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
      .withZone(java.time.ZoneId.systemDefault())
      .format(java.time.Instant.ofEpochMilli(System.currentTimeMillis()))

  private def cmdNew(o: NewOpts): Unit =
    val base = issuesDir(o.repo)
    val today = systemToday()
    planNew(namesIn(base / "open"), namesIn(base / "closed"), o.slug, o.title, o.date.getOrElse(today), today) match
      case Left(e) => refuse(e)
      case Right((name, text)) =>
        val path = base / "open" / name
        os.write(path, text)   // os.write never overwrites: a file appearing since the scan fails loudly
        println(path)

  private def rowsIn(base: os.Path, dirState: String, today: LocalDate): Vector[Row] =
    namesIn(base / dirState).flatMap: name =>
      issueId(name).map: id =>
        val pre = preambleOf(os.read(base / dirState / name)).getOrElse("")
        Row(id, dirState, statusHead(statusField(pre).getOrElse("")),
          labelsField(pre).getOrElse(""), summaryField(pre).getOrElse(""), ageDays(id, today))

  private def cmdList(state: String, repo: Option[String]): Unit =
    val base = issuesDir(repo)
    val states = state match
      case "open" | "closed" => Vector(state)
      case "all"             => Vector("open", "closed")
      case other             => fail(s"--state must be open|closed|all, got '$other'")
    val today = systemToday()
    val rows = states.flatMap(s => rowsIn(base, s, today)).sortBy(_.id)
    if rows.isEmpty then println(s"no $state issues under $base")
    else rows.foreach(r => println(listLine(r)))

  private def cmdClose(o: CloseOpts): Unit =
    val base = issuesDir(o.repo)
    val id = o.id.render
    val inOpen   = namesIn(base / "open").filter(n => issueId(n).contains(o.id))
    val inClosed = namesIn(base / "closed").filter(n => issueId(n).contains(o.id))
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
      // Retired, and LOUD: stdout stays empty and the exit is non-zero, so a script doing
      // `n=$(tt issue next)` fails instead of silently filing under a stale counter.
      case "next" :: _ => { System.err.println(NextRetired); sys.exit(1) }
      case "new" :: rest => parseNew(rest).fold(fail, cmdNew)
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
            |  tt issue new   <slug> [--title <text>] [--date YYYY-MM-DD] [--repo <dir>]
            |  tt issue list  [--state open|closed|all] [--repo <dir>]
            |  tt issue close <ID> (--fixed-by <ref> | --as <text>) [--date YYYY-MM-DD] [--yes] [--repo <dir>]
            |<ID> is a legacy number (073) or YYMMDD-slug. close PREVIEWS by default; --yes rewrites the
            |status line AND moves the file as one operation. `next` is retired (issue 073).""".stripMargin)
        sys.exit(2)
}

@main def trackInRepoIssues(args: String*): Unit = Issue.dispatch(args*)
