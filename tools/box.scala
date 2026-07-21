//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8
//> using file boxstats.scala

// box — safe box ops: host-pinned REMOTE ops for a known compute box (default bjornyx.local), plus
// LOCAL health/kill shapes for THIS machine (SM181). Replaces the dual-use `ssh *` and `ps`/`pkill`
// reflexes with a narrow, allowlistable tool: a FIXED verb enum, NO shell passthrough, host + model
// names validated against strict patterns so nothing the caller supplies can inject remote shell, and
// kill limited to a closed dev-server enum resolved via /proc + ProcessHandle (never shell patterns).
// ⚠ ALLOWLIST (changed by SM181): with `kill` aboard, `Bash(tt box *)` is NO LONGER blanket-safe —
// kill stays human-gated ([[never-blanket-allow-destructive-commands]], hardening dance before ship);
// allowlist granular read-only verbs instead (`tt box health*`, `tt box df*`, ...).
//   tt box health        [--top N] [--wide]             LOCAL snapshot: top-N by RSS + CPU%, mem, load
//   tt box kill <t> [--yes]   t = bloop|sbt|scala-cli   LOCAL SIGKILL of a known dev server (enum-only
//                                                       targets; DRY-RUN listing without --yes)
//   tt box models        [--host H]                     ollama inventory (name/size/modified)
//   tt box df            [--host H]                      disk: size/used/avail on / (GB)
//   tt box gpu           [--host H]                      nvidia-smi utilization + mem (csv)
//   tt box freegb        [--host H]                      just the free-GB integer on / (for scripting)
//   tt box pull <model>  [--host H] [--min-free-gb N]    ollama pull, REFUSED if free disk < N (default 50)
import scala.util.Try

// All helpers live INSIDE this object so they don't pollute the package namespace — top-level `private def`s
// (freeGb, ssh, usage, …) would collide by name with every other tool when the toolbox is compiled together
// (`scala-cli compile tools` / the Scala MCP). Only the @main entry stays top-level. See skills/scala-style.
object Box {
  private val DefaultHost = "bjornyx.local"
  private val HostRe  = "^[a-zA-Z0-9._-]+$".r          // no spaces/metachars → cannot inject
  private val ModelRe = "^[a-zA-Z0-9._:/-]+$".r        // e.g. qwen2.5-coder:1.5b, library/foo
  private val DefaultMinFreeGb = 50
  private val QuickMs = 60_000L   // df / list / gpu
  private val PullMs  = 3_600_000L // one model pull ceiling (1h)

  private val Help: String =
    """tt box — safe box ops: LOCAL health/kill for this machine + host-pinned remote ops
      |for a known compute box (default bjornyx.local)
      |
      |Remote ops run a FIXED set of read/pull operations over ssh with NO shell passthrough:
      |host and model names are validated against strict patterns, so nothing the caller
      |supplies can inject remote shell. Local ops read /proc directly (no ps) and kill only
      |a closed enum of dev servers by PID via ProcessHandle (no pkill patterns).
      |
      |Usage:
      |  box health        [--top N] [--wide]            local top-N by RSS with CPU%, mem, load
      |  box kill <t>      [--yes]                       t = bloop | sbt | scala-cli; SIGKILL the
      |                                                  matched dev server(s); DRY-RUN without --yes
      |  box models        [--host H]                     ollama inventory (name/size/modified)
      |  box df            [--host H]                     disk usage on / (human-readable)
      |  box gpu           [--host H]                     nvidia-smi utilization + memory (csv)
      |  box freegb        [--host H]                     just the free-GB integer on / (scripting)
      |  box pull <model>  [--host H] [--min-free-gb N]   ollama pull; REFUSED if free disk < N
      |Flags:
      |  --top N                         health: how many processes to show (default 10)
      |  --wide                          health: full command lines, no truncation
      |  --yes                           kill: actually SIGKILL (without it: dry-run listing)
      |  --host H                        target host (default bjornyx.local)
      |  --min-free-gb N                 free-disk floor for pull in GB (default 50);
      |                                  pull refuses to start below it
      |
      |ssh runs with BatchMode (never hangs on a password prompt); quick ops time out after
      |60 s, a pull after 1 h.
      |
      |Examples:
      |  tt box health                          # what is eating this machine right now?
      |  tt box kill bloop                      # dry-run: which bloop server WOULD be killed?
      |  tt box models                          # what models does the box have?
      |  tt box gpu                             # is the GPU busy right now?
      |  tt box pull qwen2.5-coder:1.5b         # pull, guarded by the 50G free-disk floor
      |
      |Full reference: tools/README.md""".stripMargin

  private def usage(): Nothing =
    System.err.println(
      """box: usage:
        |  tt box health        [--top N] [--wide]
        |  tt box kill <bloop|sbt|scala-cli> [--yes]
        |  tt box models        [--host H]
        |  tt box df            [--host H]
        |  tt box gpu           [--host H]
        |  tt box freegb        [--host H]
        |  tt box pull <model>  [--host H] [--min-free-gb N]
        |local health/kill via /proc + ProcessHandle; host-pinned remote ops, no shell passthrough.""".stripMargin)
    sys.exit(2)

  private def fail(msg: String): Nothing = { System.err.println(s"box: $msg"); sys.exit(2) }

  /** Run `ssh <host> <remoteArgv...>` with BatchMode (never hang on a prompt). Each remote token is passed
    * as a separate argv element — we never build a shell string from caller input. */
  // timeoutMs is a REAL wall-clock cap (os-lib treats 0 as "kill after 0ms" → SIGTERM, so never pass 0).
  private def ssh(host: String, timeoutMs: Long, remote: String*): (Int, String, String) =
    val argv: Seq[String] = Seq("ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=8", host) ++ remote
    Try(os.proc(argv).call(check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = timeoutMs)) match
      case scala.util.Success(res) => (res.exitCode, res.out.text().trim, res.err.text().trim)
      case scala.util.Failure(e)   => (255, "", e.getMessage)

  private def parseHost(args: List[String]): (String, List[String]) =
    args match
      case "--host" :: h :: t => if HostRe.matches(h) then (h, t) else fail(s"invalid --host '$h'")
      case _                  => (DefaultHost, args)

  /** free GB on / via `df -BG /` → 4th column of the data row, strip trailing G. */
  private def freeGb(host: String): Int =
    val (code, out, err) = ssh(host, QuickMs, "df", "-BG", "/")
    if code != 0 then fail(s"df on $host failed: ${if err.nonEmpty then err else s"exit $code"}")
    val dataLine = out.linesIterator.toList.lastOption.getOrElse(fail("df: no output"))
    val cols = dataLine.split("\\s+")
    if cols.length < 4 then fail(s"df: unexpected output: $dataLine")
    cols(3).stripSuffix("G").toIntOption.getOrElse(fail(s"df: cannot parse avail '${cols(3)}'"))

  // ---------- SM181 LOCAL shapes: health (read-only snapshot) + kill (typed dev-server enum) ----------
  // Why: one bloop restart on 2026-07-21 cost 6 guard events + 2 silent pkill failures — `pkill -f` is
  // self-referential under the harness wrapper (its own bash -c cmdline carries the pattern text) and
  // fragile vs toolchain naming drift. So: read /proc directly (JDK-only, no shelling to ps), match
  // structural fingerprints in-process, kill by PID via ProcessHandle. Evidence:
  // research/wr-data/tt-box-lacks-local-health-shape.md.

  /** The ONLY processes `kill` can target — a closed enum, so this can never become a generic killer. */
  enum DevServer:
    case Bloop, Sbt, ScalaCli

  object DevServer:
    val names: Vector[String] = Vector("bloop", "sbt", "scala-cli") // index = ordinal
    def parse(s: String): Option[DevServer] = s match
      case "bloop"     => Some(Bloop)
      case "sbt"       => Some(Sbt)
      case "scala-cli" => Some(ScalaCli)
      case _           => None
    def name(t: DevServer): String = names(t.ordinal)

  /** PURE fingerprint: does (command, args) look like the target dev server? sbt/scala-cli match
    * STRUCTURAL markers only (boot class / launcher jar / binary basename — a bare `sbt` word is not
    * enough); bloop delegates to the empirical one-home signature (see the case comment). Fingerprints
    * are NOT self-protecting on their own: the kill caller pid-excludes self + all ancestors. */
  def matchesDevServer(target: DevServer, command: String, args: Seq[String]): Boolean =
    val base = command.substring(command.lastIndexOf('/') + 1)
    target match
      case DevServer.Bloop =>
        // ONE-home signature reuse: BoxStats.isBloopCmdline (bloop-and-not-metals; the main class is
        // NOT reliably in the cmdline, and Metals embeds bloop jars — both empirical, 2026-07-19, see
        // boxstats.scala). Residual over-match ACCEPTED exactly like tt bloop: a CONCURRENT kill-tool
        // JVM carries the literal `bloop` argument and would match — self+ancestors are pid-excluded
        // by the caller, and a second concurrent run is rare + disposable.
        base.startsWith("java") && BoxStats.isBloopCmdline((command +: args).mkString(" "))
      case DevServer.Sbt =>
        base.startsWith("java") && args.exists(a =>
          a.contains("sbt-launch") || a.contains("xsbt.boot.Boot"))
      case DevServer.ScalaCli =>
        base == "scala-cli"

  /** PURE: parse a /proc/pid/stat line. comm may contain spaces and parens, so fields are taken after
    * the LAST close-paren. Tick values are in USER_HZ. */
  def parseStat(stat: String): Option[(comm: String, cpuTicks: Long, startTicks: Long)] =
    val open  = stat.indexOf('(')
    val close = stat.lastIndexOf(')')
    if open < 0 || close < open then None
    else
      val rest = stat.substring(close + 1).trim.split("\\s+") // rest(0) = state = stat field 3
      if rest.length < 20 then None
      else
        for
          utime <- rest(11).toLongOption // stat field 14
          stime <- rest(12).toLongOption // stat field 15
          start <- rest(19).toLongOption // stat field 22
        yield (comm = stat.substring(open + 1, close), cpuTicks = utime + stime, startTicks = start)

  /** PURE: MemTotal/MemAvailable/SwapTotal/SwapFree (kB) from /proc/meminfo lines; missing keys → 0. */
  def parseMeminfo(lines: Seq[String]): (totalKb: Long, availKb: Long, swapTotalKb: Long, swapFreeKb: Long) =
    def kb(key: String): Long =
      lines.find(_.startsWith(key + ":")).map(_.split("\\s+"))
        .flatMap(cols => if cols.length >= 2 then cols(1).toLongOption else None).getOrElse(0L)
    (totalKb = kb("MemTotal"), availKb = kb("MemAvailable"),
     swapTotalKb = kb("SwapTotal"), swapFreeKb = kb("SwapFree"))

  /** PURE: kB → human string; Double.toString keeps the decimal dot locale-independent. */
  def fmtKb(kb: Long): String =
    if kb >= 1024 * 1024 then s"${math.round(kb / 1024.0 / 1024.0 * 10) / 10.0}G"
    else if kb >= 1024 then s"${kb / 1024}M"
    else s"${kb}K"

  /** PURE: seconds → compact two-unit age like 2d3h, 3h12m, 2m5s, 45s. */
  def fmtElapsed(secs: Long): String =
    val d = secs / 86400; val h = secs % 86400 / 3600; val m = secs % 3600 / 60; val s = secs % 60
    if d > 0 then s"${d}d${h}h"
    else if h > 0 then s"${h}h${m}m"
    else if m > 0 then s"${m}m${s}s"
    else s"${s}s"

  private val UserHz = 100L // kernel USER_HZ for /proc tick fields; 100 on all mainstream Linux

  private def readProcFile(path: String): Option[String] =
    Try(String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)))).toOption

  /** One /proc pass per process: pid, comm, argv, VmRSS kB, cpu ticks, start ticks. Pids that vanish
    * mid-read are silently skipped (they raced us; that is normal). */
  private case class Snap(pid: Long, comm: String, argv: Vector[String], rssKb: Long, cpuTicks: Long, startTicks: Long)

  private def snapshotProcs(): Vector[Snap] =
    val dir = java.io.File("/proc")
    if !dir.isDirectory then fail("health/kill need Linux /proc")
    val pids = Option(dir.list()).getOrElse(Array.empty[String]).toVector
      .filter(n => n.nonEmpty && n.forall(_.isDigit)).flatMap(_.toLongOption)
    pids.flatMap: pid =>
      readProcFile(s"/proc/$pid/stat").flatMap(parseStat).map: st =>
        val argv = readProcFile(s"/proc/$pid/cmdline")
          .map(_.split(0.toChar).toVector.filter(_.nonEmpty)).getOrElse(Vector.empty)
        val rss = readProcFile(s"/proc/$pid/status")
          .flatMap(_.linesIterator.find(_.startsWith("VmRSS:")))
          .flatMap(_.split("\\s+").lift(1)).flatMap(_.toLongOption).getOrElse(0L)
        Snap(pid, st.comm, argv, rss, st.cpuTicks, st.startTicks)

  private def uptimeSecs(): Double =
    readProcFile("/proc/uptime").flatMap(_.split("\\s+").headOption).flatMap(_.toDoubleOption)
      .getOrElse(fail("cannot read /proc/uptime"))

  /** Fingerprint tag for the health listing ("" when no dev server matches). */
  private def tagOf(s: Snap): String =
    if s.argv.isEmpty then ""
    else DevServer.values.find(t => matchesDevServer(t, s.argv.head, s.argv.tail))
      .map(DevServer.name).getOrElse("")

  private def health(args: List[String]): Unit =
    @annotation.tailrec
    def parse(r: List[String], top: Int, wide: Boolean): (Int, Boolean) = r match
      case Nil               => (top, wide)
      case "--top" :: n :: t => n.toIntOption match
        case Some(v) if v > 0 => parse(t, v, wide)
        case _                => fail(s"--top needs a positive integer, got '$n'")
      case "--wide" :: t     => parse(t, top, true)
      case other :: _        => fail(s"unexpected argument '$other' (health takes --top N, --wide)")
    val (top, wide) = parse(args, 10, false)

    val load = readProcFile("/proc/loadavg").map(_.split("\\s+").take(3).mkString(" ")).getOrElse("?")
    val mem  = parseMeminfo(readProcFile("/proc/meminfo").map(_.linesIterator.toVector).getOrElse(Vector.empty))
    val up1  = uptimeSecs()
    val s1   = snapshotProcs()
    Thread.sleep(400) // two samples → REAL current CPU%, not a diluted since-start average
    val up2  = uptimeSecs()
    val ticks2: Map[Long, Long] = s1.map(s =>
      s.pid -> readProcFile(s"/proc/${s.pid}/stat").flatMap(parseStat).map(_.cpuTicks).getOrElse(s.cpuTicks)).toMap
    val dt = math.max(up2 - up1, 0.001)

    println(s"load $load   mem ${fmtKb(mem.availKb)} avail / ${fmtKb(mem.totalKb)}   " +
      s"swap ${fmtKb(mem.swapTotalKb - mem.swapFreeKb)} used / ${fmtKb(mem.swapTotalKb)}")
    println(f"${"PID"}%7s ${"RSS"}%8s ${"CPU%"}%6s ${"AGE"}%7s ${"TAG"}%-9s CMD")
    s1.sortBy(-_.rssKb).take(top).foreach: s =>
      val cpu  = math.round((ticks2(s.pid) - s.cpuTicks) / UserHz.toDouble / dt * 1000) / 10.0
      val age  = fmtElapsed(math.max((up2 - s.startTicks.toDouble / UserHz).toLong, 0L))
      val cmd0 = if s.argv.isEmpty then s"[${s.comm}]" else s.argv.mkString(" ")
      val cmd  = if wide || cmd0.length <= 100 then cmd0 else cmd0.take(97) + "..."
      println(f"${s.pid}%7d ${fmtKb(s.rssKb)}%8s ${cpu.toString}%6s $age%7s ${tagOf(s)}%-9s $cmd")

  /** Our own pid + every ancestor (tt → scala-cli → this JVM): NEVER kill candidates. */
  private def selfAndAncestors(): Set[Long] =
    import scala.jdk.OptionConverters.*
    Iterator.iterate(Option(ProcessHandle.current()))(_.flatMap(_.parent().toScala))
      .takeWhile(_.isDefined).flatten.map(_.pid()).toSet

  private def killDevServer(args: List[String]): Unit =
    val yes  = args.contains("--yes")
    val rest = args.filterNot(_ == "--yes")
    rest match
      case name :: Nil =>
        val target = DevServer.parse(name)
          .getOrElse(fail(s"kill target must be one of ${DevServer.names.mkString(" | ")} (got '$name')"))
        val excluded = selfAndAncestors()
        val matches = snapshotProcs().filter: s =>
          s.argv.nonEmpty && !excluded.contains(s.pid) && matchesDevServer(target, s.argv.head, s.argv.tail)
        if matches.isEmpty then
          println(s"box kill: no $name process found")
          if yes then sys.exit(1)
        else if !yes then
          println(s"box kill: DRY-RUN — would SIGKILL ${matches.size} $name process(es); re-run with --yes to kill:")
          matches.foreach(m => println(f"  ${m.pid}%7d ${fmtKb(m.rssKb)}%8s  ${m.argv.mkString(" ").take(120)}"))
        else
          import scala.jdk.OptionConverters.*
          matches.foreach: m =>
            ProcessHandle.of(m.pid).toScala match
              case Some(h) =>
                h.destroyForcibly() // SIGKILL: the 07-21 saga shows a wedged bloop shrugs off less
                val gone = Try(h.onExit().get(5, java.util.concurrent.TimeUnit.SECONDS)).isSuccess
                println(s"box kill: SIGKILL ${m.pid} (${fmtKb(m.rssKb)} rss) " +
                  (if gone then "-> exited" else "-> STILL RUNNING after 5s"))
              case None => println(s"box kill: ${m.pid} already gone")
      case _ => fail("usage: kill <bloop|sbt|scala-cli> [--yes]   (DRY-RUN without --yes)")

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    args.toList match
      case "health" :: rest => health(rest)

      case "kill" :: rest => killDevServer(rest)

      case "models" :: rest =>
        val (host, _) = parseHost(rest)
        val (code, out, err) = ssh(host, QuickMs, "ollama", "list")
        if code != 0 then fail(s"ollama list on $host failed: ${if err.nonEmpty then err else s"exit $code"}")
        println(out)

      case "df" :: rest =>
        val (host, _) = parseHost(rest)
        val (code, out, err) = ssh(host, QuickMs, "df", "-h", "/")
        if code != 0 then fail(s"df on $host failed: ${if err.nonEmpty then err else s"exit $code"}")
        println(out)

      case "gpu" :: rest =>
        val (host, _) = parseHost(rest)
        val (code, out, err) = ssh(host, QuickMs, "nvidia-smi",
          "--query-gpu=utilization.gpu,memory.used,memory.total,temperature.gpu", "--format=csv")
        if code != 0 then fail(s"nvidia-smi on $host failed: ${if err.nonEmpty then err else s"exit $code"}")
        println(out)

      case "freegb" :: rest =>
        val (host, _) = parseHost(rest)
        println(freeGb(host))

      case "pull" :: model :: rest =>
        if !ModelRe.matches(model) then fail(s"invalid model name '$model'")
        // parse --host + --min-free-gb from the remainder
        @annotation.tailrec
        def parse(r: List[String], host: String, floor: Int): (String, Int) = r match
          case Nil                        => (host, floor)
          case "--host" :: h :: t         => if HostRe.matches(h) then parse(t, h, floor) else fail(s"invalid --host '$h'")
          case "--min-free-gb" :: n :: t  => n.toIntOption match
            case Some(v) if v >= 0 => parse(t, host, v)
            case _                 => fail(s"--min-free-gb needs a non-negative integer, got '$n'")
          case other :: _                 => fail(s"unexpected argument '$other'")
        val (host, floor) = parse(rest, DefaultHost, DefaultMinFreeGb)
        val free = freeGb(host)
        if free < floor then
          fail(s"REFUSED: $host has ${free}G free on /, below floor of ${floor}G — not pulling '$model'")
        System.err.println(s"box: $host has ${free}G free (floor ${floor}G) → pulling '$model'…")
        val (code, out, err) = ssh(host, PullMs, "ollama", "pull", model)
        if out.nonEmpty then println(out)
        if err.nonEmpty then System.err.println(err)
        if code != 0 then fail(s"ollama pull '$model' on $host failed: exit $code")
        val after = freeGb(host)
        System.err.println(s"box: pulled '$model'; $host now ${after}G free on / (was ${free}G)")

      case _ => usage()
}

@main def boxOps(args: String*): Unit = Box.dispatch(args*)
