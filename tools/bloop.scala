//> using scala 3.8.4
//> using jvm 21
//> using file boxstats.scala

// bloop — targeted BloopServer control (SM146c / roadmap T3): status + restart. Bloop is a DISPOSABLE
// daemon that respawns lazily on the next scala-cli / Metals compile, so "restart" is honestly a targeted
// kill + lazy respawn. WHY kill -9 and not a polite `bloop exit`: the empirical wedge lesson (SM150,
// wedged twice 2026-07-18) — when bloop is wedged, polite protocols hang; a signal is the reliable
// unwedge. WHY targeted-only: only pids matching the bloop signature are touched (comm "java" + cmdline
// mentioning bloop; the signature lives in ONE home, BoxStats.isBloopCmdline — the
// main-class string is not reliably in the cmdline, which is why `pkill -f BloopServer` misses). WHY this
// rides the blanket `tt` allow unlike the ask-gated general kill (SM166c): unwedging must work exactly
// when stalls are worst (AFK, wedged box), and the blast radius is one disposable daemon (SM166a).
// This whole tool is an effectful DRIVER by nature (a kill); the pure parts it leans on (signature,
// VmRSS parse) live in BoxStats and are tested there.
//   tt bloop            status: matching pids + RSS, or "not running"
//   tt bloop status     same
//   tt bloop restart    kill matching pids (targeted), report freed RSS; respawn is lazy
// Costs to know: a kill during an ACTIVE compile loses that compile (rerun it); nothing else is lost.
// Residual over-match, ACCEPTED + documented (2026-07-19 probes): a concurrently-running `tt bloop` tool
// JVM matches the signature (its cmdline carries the literal source path tools/bloop.scala) — self is
// excluded by pid, and a SECOND concurrent run is rare + disposable. The DANGEROUS over-match (Metals,
// which embeds bloop jars) is excluded in the signature itself and regression-tested.

object BloopTool:
  private val Help =
    """tt bloop — targeted BloopServer control (status + restart)
      |
      |bloop is a disposable compile daemon: it respawns lazily on the next scala-cli / Metals
      |compile. So `restart` = targeted kill of pids matching the bloop signature (a java process
      |whose cmdline mentions bloop), then lazy respawn. Kill -9 on purpose: a WEDGED bloop hangs
      |polite exit protocols; the signal always works. A kill during an active compile only costs
      |that compile. The box line (tt statusline --box-line) suggests `restart?` when bloop's RSS
      |goes red; this tool is the declared action behind that measured hint.
      |
      |Usage:
      |  bloop            status: matching pids + RSS, or "not running"
      |  bloop status     same
      |  bloop restart    kill matching pids, report freed RSS
      |
      |Full reference: tools/README.md""".stripMargin

  private def gb(kb: Long): String = f"${kb / 1048576.0}%.1fG"

  private def status(): Int =
    val ps = BoxStats.bloopPids()
    if ps.isEmpty then println("bloop: not running (it spawns on the next scala-cli / Metals compile)")
    else ps.foreach(p => println(s"bloop: pid ${p.pid} rss ${gb(p.rssKb)}"))
    0

  private def restart(): Int =
    // Never self-target, whatever the signature says of our own cmdline (belt-and-braces; observed
    // tool JVMs do NOT match the signature, but a kill tool earns paranoia).
    val self = java.lang.ProcessHandle.current().pid()
    val ps = BoxStats.bloopPids().filterNot(_.pid == self)
    if ps.isEmpty then
      println("bloop: not running — nothing to kill (it spawns on the next compile)")
      0
    else
      var failed = 0
      ps.foreach: p =>
        // Re-resolve the handle at kill time; ProcessHandle.of is empty if the pid vanished meanwhile.
        val ok = java.lang.ProcessHandle.of(p.pid).map(_.destroyForcibly()).orElse(false)
        if ok then println(s"bloop: killed pid ${p.pid} (freed ~${gb(p.rssKb)} rss)")
        else { failed += 1; Console.err.println(s"bloop: FAILED to kill pid ${p.pid}") }
      println("bloop: respawns lazily on the next scala-cli / Metals compile")
      if failed > 0 then 1 else 0

  def dispatch(args: Seq[String]): Int =
    args.toList match
      case Nil | ("status" :: Nil)                => status()
      case "restart" :: Nil                       => restart()
      case a if a.contains("--help") || a.contains("-h") => println(Help); 0
      case other =>
        // Loud abort on unknown verbs — the silent fall-through lesson (introprog f6939418).
        Console.err.println(s"bloop: unknown arguments '${other.mkString(" ")}'. Verbs: status | restart. See --help.")
        2

@main def bloopServerCtl(args: String*): Unit = sys.exit(BloopTool.dispatch(args))
