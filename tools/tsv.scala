//> using file project.scala
//> using jvm 21

// tsv — READ and FILTER a tab-separated file: what columns are there, how many rows match, show me those
// rows. The noun-coverage gap RT056 ranked fifth, and the toolbox's own most embarrassing one: the `tt`
// launcher appends to `tmp/tt-perf.tsv` on EVERY call, so the toolbox has been writing a format it could
// not read.
//
//   tt tsv cols  <file>                     column names (or arity, if there is no header) + row count
//   tt tsv count <file> [filters]           how many data rows match
//   tt tsv rows  <file> [filters] [--limit N]   print matching rows
//   tt tsv drop  <file> [filters] --out <new>   write the rows that do NOT match to a NEW file
//
// Filters (all optional, ANDed; a row must satisfy every one):
//   --col <name|index> --eq <value>         that column equals the value exactly
//   --col <name|index> --matches <regex>    that column matches (Java regex, unanchored)
//   --col <name|index> --same-as <name|idx> two columns hold the SAME text  (the key==value shape)
//   --no-header                             treat line 1 as data; columns are 0-based indexes only
//
// SAFETY, and the reason `drop` is shaped the way it is: this NEVER edits in place. `drop` demands an
// explicit `--out` that must NOT already exist, so the input file is untouchable by construction and a
// mistake costs a stray file rather than data. Purging a cache is exactly the operation where an in-place
// tool with a wrong predicate is unrecoverable, so the tool refuses to be that.
//
// Also deliberately NOT here: any notion of what the text MEANS. `--same-as` finds key==value rows; whether
// such a row is a stale fallback or a legitimately identical term is a JUDGEMENT the caller supplies via
// `--matches` (e.g. a Swedish-letter class). Keeping the mechanical and the semantic halves separate is the
// point — see introprog#960, where conflating them would have purged correct rows.
//
// TSV, not CSV, on purpose: no quoting rules, no embedded-newline ambiguity. Fields split on \t, period.

object TsvTool {

  final case class Table(header: Option[Vector[String]], rows: Vector[Vector[String]])

  /** PURE: split TSV text. Blank trailing lines are dropped; no quote processing (TSV has none). */
  def parse(text: String, hasHeader: Boolean): Table =
    val lines = text.split("\n", -1).toVector.dropRight(if text.endsWith("\n") then 1 else 0)
    val cells = lines.map(_.split("\t", -1).toVector)
    if hasHeader && cells.nonEmpty then Table(Some(cells.head), cells.tail) else Table(None, cells)

  /** PURE: resolve a column name or 0-based index against the header. */
  def columnIndex(spec: String, header: Option[Vector[String]]): Either[String, Int] =
    header.flatMap(h => h.indexOf(spec) match { case -1 => None; case i => Some(i) }) match
      case Some(i) => Right(i)
      case None =>
        spec.toIntOption match
          case Some(i) if i >= 0 => Right(i)
          case Some(i)           => Left(s"column index must be >= 0 (got $i)")
          case None =>
            val known = header.map(h => s"; columns: ${h.mkString(", ")}").getOrElse("; file has no header")
            Left(s"no such column '$spec'$known")

  /** A single predicate over one row. */
  enum Pred:
    case Eq(col: Int, value: String)
    case Matches(col: Int, regex: scala.util.matching.Regex)
    case SameAs(a: Int, b: Int)

  /** PURE: does the row satisfy the predicate? A row too short for the column never matches — it is
    * missing data, not an empty match, and silently treating it as "" would over-select. */
  def holds(p: Pred, row: Vector[String]): Boolean =
    def at(i: Int): Option[String] = if i < row.length then Some(row(i)) else None
    p match
      case Pred.Eq(c, v)      => at(c).contains(v)
      case Pred.Matches(c, r) => at(c).exists(s => r.findFirstIn(s).isDefined)
      case Pred.SameAs(a, b)  => (at(a), at(b)) match
        case (Some(x), Some(y)) => x == y
        case _                  => false

  /** PURE: a row matches when EVERY predicate holds. No predicates = every row matches. */
  def matches(preds: Vector[Pred], row: Vector[String]): Boolean = preds.forall(p => holds(p, row))

  val Help: String =
    """tt tsv — read and filter a tab-separated file
      |
      |Usage:
      |  tt tsv cols  <file> [--no-header]                    column names + row count
      |  tt tsv count <file> [filters]                        how many data rows match
      |  tt tsv rows  <file> [filters] [--limit N]            print matching rows
      |  tt tsv drop  <file> [filters] --out <new>            write NON-matching rows to a NEW file
      |
      |Filters (optional, ANDed):
      |  --col <name|index> --eq <value>          column equals value exactly
      |  --col <name|index> --matches <regex>     column matches a Java regex (unanchored)
      |  --col <name|index> --same-as <name|idx>  two columns hold the same text (the key==value shape)
      |  --no-header                              line 1 is data; refer to columns by 0-based index
      |
      |Examples:
      |  tt tsv cols tmp/tt-perf.tsv --no-header
      |  tt tsv count cache.tsv --col 0 --same-as 1
      |  tt tsv count cache.tsv --col 0 --same-as 1 --col 1 --matches [åäöÅÄÖ]
      |  tt tsv drop  cache.tsv --col 0 --same-as 1 --out pruned.tsv
      |
      |NEVER edits in place: `drop` requires --out and refuses an existing path, so the input cannot be
      |damaged by a wrong predicate. It knows nothing about what the text MEANS — pair --same-as with
      |--matches to express that judgement yourself.
      |
      |Exit: 0 ok; 2 usage / unreadable file / bad column; count exits 0 even when the count is 0.""".stripMargin

  final case class Opts(file: String, preds: Vector[Pred], limit: Option[Int], out: Option[String], noHeader: Boolean)

  private def fail(msg: String): Nothing =
    System.err.println(s"tt tsv: $msg")
    sys.exit(2)

  /** Parse args AFTER the verb and file. Column specs resolve against the header, so this needs it. */
  def parseOpts(args: List[String], header: Option[Vector[String]]): Either[String, (Vector[Pred], Option[Int], Option[String])] =
    def go(rest: List[String], preds: Vector[Pred], limit: Option[Int], out: Option[String]): Either[String, (Vector[Pred], Option[Int], Option[String])] =
      rest match
        case Nil => Right((preds, limit, out))
        case "--no-header" :: t => go(t, preds, limit, out)
        case "--limit" :: n :: t =>
          n.toIntOption match
            case Some(v) if v >= 0 => go(t, preds, Some(v), out)
            case _                 => Left(s"--limit needs a non-negative integer (got '$n')")
        case "--out" :: p :: t => go(t, preds, limit, Some(p))
        case "--col" :: spec :: op :: value :: t =>
          columnIndex(spec, header).flatMap: c =>
            op match
              case "--eq"      => go(t, preds :+ Pred.Eq(c, value), limit, out)
              case "--matches" =>
                try go(t, preds :+ Pred.Matches(c, value.r), limit, out)
                catch case e: Throwable => Left(s"bad regex '$value': ${e.getMessage}")
              case "--same-as" => columnIndex(value, header).flatMap(b => go(t, preds :+ Pred.SameAs(c, b), limit, out))
              case other       => Left(s"--col needs --eq, --matches or --same-as (got '$other')")
        case "--col" :: _ => Left("--col needs <name|index> then --eq/--matches/--same-as <value>")
        case "--limit" :: Nil => Left("--limit needs a value")
        case "--out" :: Nil   => Left("--out needs a path")
        case other :: _       => Left(s"unknown option '$other'")
    go(args, Vector.empty, None, None)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then
      println(Help); sys.exit(0)

    val (verb, file, rest) = args.toList match
      case v :: f :: t if Set("cols", "count", "rows", "drop").contains(v) => (v, f, t)
      case v :: _ if Set("cols", "count", "rows", "drop").contains(v) => fail(s"$v needs a <file>")
      case v :: _ => fail(s"unknown verb '$v' (expected: cols, count, rows, drop)")
      case Nil    => fail("usage: tt tsv <cols|count|rows|drop> <file> [filters]  (tt tsv --help)")

    val hasHeader = !rest.contains("--no-header")
    val path = java.nio.file.Paths.get(file)
    if !java.nio.file.Files.isRegularFile(path) then fail(s"not a file: $file")
    val text =
      try String(java.nio.file.Files.readAllBytes(path), "UTF-8")
      catch case e: Throwable => fail(s"cannot read $file: ${e.getMessage}")

    val table = parse(text, hasHeader)
    val (preds, limit, out) = parseOpts(rest, table.header) match
      case Right(t)  => t
      case Left(msg) => fail(msg)

    verb match
      case "cols" =>
        table.header match
          case Some(h) => h.zipWithIndex.foreach((name, i) => println(s"$i\t$name"))
          case None =>
            val arity = table.rows.headOption.map(_.length).getOrElse(0)
            println(s"(no header; $arity columns, refer to them as 0..${math.max(0, arity - 1)})")
        println(s"rows: ${table.rows.size}")

      case "count" => println(table.rows.count(r => matches(preds, r)))

      case "rows" =>
        val hits = table.rows.filter(r => matches(preds, r))
        hits.take(limit.getOrElse(hits.size)).foreach(r => println(r.mkString("\t")))
        if limit.exists(_ < hits.size) then
          System.err.println(s"tt tsv: showing ${limit.get} of ${hits.size} matching rows")

      case "drop" =>
        val target = out.getOrElse(fail("drop needs --out <new-file> (this tool never edits in place)"))
        val outPath = java.nio.file.Paths.get(target)
        if java.nio.file.Files.exists(outPath) then
          fail(s"--out already exists: $target (refusing to overwrite; pick a new path)")
        if preds.isEmpty then fail("drop with no filter would copy the file unchanged — give a filter")
        val kept = table.rows.filterNot(r => matches(preds, r))
        val dropped = table.rows.size - kept.size
        val body = (table.header.toVector ++ kept).map(_.mkString("\t")).mkString("", "\n", "\n")
        try java.nio.file.Files.write(outPath, body.getBytes("UTF-8"))
        catch case e: Throwable => fail(s"cannot write $target: ${e.getMessage}")
        println(s"tt tsv: dropped $dropped of ${table.rows.size} rows -> $target (input untouched)")

      case _ => fail(s"unhandled verb '$verb'")
}

@main def tsvRead(args: String*): Unit = TsvTool.dispatch(args*)
