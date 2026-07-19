//> using scala 3.8.4
//> using jvm 21

// boxstats — SHARED helper (no @main, like lib.scala / seqspec.scala; see research/038): the box-health
// gatherers behind the SM163 box line AND `tt bloop` (T3). Extracted from statusline.scala the moment a
// second tool needed the bloop signature — the signature must have ONE home (`isBloopCmdline`), because
// its consumers must change together. Rendering stays in statusline.scala; this file only reads and
// parses. Linux-only by data source (/proc, /sys); EVERY reader is guarded, so on any other OS gather()
// yields None and callers degrade silently — alpha-tester-safe.

object BoxStats:
  /** One gathered snapshot of the box. PURE data; statusline's `renderBox` renders it (testable without /proc). */
  final case class BoxInfo(memUsedKb: Long, memTotalKb: Long, load1: Double, cores: Int,
                           tempC: Option[Int], jvmCount: Int, jvmRssKb: Long, bloopRssKb: Option[Long],
                           diskFreeKb: Long = 0, diskTotalKb: Long = 0) // 0 total = no disk segment
  /** MemTotal + MemAvailable (kB) from /proc/meminfo content. "Used" = total - available (the kernel's own
    * reclaimable-aware figure — matches what `free` calls available, unlike total - free). PURE. */
  def parseMemInfo(s: String): Option[(Long, Long)] =
    def kb(key: String): Option[Long] =
      s.linesIterator.find(_.startsWith(key + ":")).flatMap(_.trim.split("\\s+").lift(1)).flatMap(_.toLongOption)
    for t <- kb("MemTotal"); a <- kb("MemAvailable") yield (t, a)
  /** 1-minute load average from /proc/loadavg content ("3.50 2.10 1.00 2/1234 5678"). PURE. */
  def parseLoadAvg(s: String): Option[Double] =
    s.trim.split("\\s+").headOption.flatMap(_.toDoubleOption)
  /** VmRSS (kB) from a /proc/<pid>/status content. PURE. */
  def parseVmRssKb(status: String): Option[Long] =
    status.linesIterator.find(_.startsWith("VmRSS:")).flatMap(_.trim.split("\\s+").lift(1)).flatMap(_.toLongOption)

  private def readFile(p: java.nio.file.Path): Option[String] =
    try Option.when(java.nio.file.Files.isReadable(p))(String(java.nio.file.Files.readAllBytes(p), "UTF-8"))
    catch case _: Throwable => None
  /** Hottest thermal zone in whole °C (max across /sys/class/thermal/thermal_zone* — the fan story). */
  def readTempC(): Option[Int] =
    try
      val dir = java.nio.file.Path.of("/sys/class/thermal")
      if !java.nio.file.Files.isDirectory(dir) then None
      else
        val zones = scala.jdk.CollectionConverters.IteratorHasAsScala(java.nio.file.Files.list(dir).iterator()).asScala
          .filter(_.getFileName.toString.startsWith("thermal_zone"))
          .flatMap(z => readFile(z.resolve("temp")).flatMap(_.trim.toLongOption)).toVector
        if zones.isEmpty then None else Some((zones.max / 1000).toInt)
    catch case _: Throwable => None
  /** The bloop-SERVER signature — ONE home, shared by jvmScan and `tt bloop` (they must change together):
    * a JVM's cmdline mentions bloop (the ~/.cache/bloop classpath entries; the main-class string is NOT
    * reliably present — `pkill -f BloopServer` missed a live one 2026-07-19) AND does not mention metals.
    * The metals exclusion is EMPIRICAL (2026-07-19 live probe): Metals embeds bloop jars in its classpath,
    * so a bare "bloop" substring matched 2 Metals JVMs (jps-verified) — a kill on that would take down the
    * editor's language server. Probe data: BloopServer cmdline had 0 "metals" tokens, Metals had 4. */
  def isBloopCmdline(cmdline: String): Boolean =
    val lc = cmdline.toLowerCase
    lc.contains("bloop") && !lc.contains("metals")
  /** Pids + RSS (kB) of processes matching the bloop signature (comm "java" + isBloopCmdline).
    * Effectful /proc scan, guarded like everything here; empty on any failure or non-Linux. */
  def bloopPids(): Vector[(pid: Long, rssKb: Long)] =
    try
      scala.jdk.CollectionConverters.IteratorHasAsScala(
        java.nio.file.Files.list(java.nio.file.Path.of("/proc")).iterator()).asScala
        .filter(_.getFileName.toString.forall(_.isDigit)).toVector
        .flatMap: p =>
          if readFile(p.resolve("comm")).map(_.trim).contains("java")
             && readFile(p.resolve("cmdline")).exists(isBloopCmdline)
          then readFile(p.resolve("status")).flatMap(parseVmRssKb)
                 .map(rss => (pid = p.getFileName.toString.toLong, rssKb = rss))
          else None
    catch case _: Throwable => Vector.empty
  /** Scan /proc for JVMs: (count, total VmRSS kB, bloop's VmRSS kB if a bloop JVM is present). A process is
    * a JVM iff its comm is "java"; it is BLOOP iff isBloopCmdline holds (SUBSTRING heuristic — see there).
    * The cmdline is read, never printed — bounded OUTPUT is the SM160 constraint, and this emits at most
    * two numbers. */
  def jvmScan(): (Int, Long, Option[Long]) =
    try
      val procs = scala.jdk.CollectionConverters.IteratorHasAsScala(
        java.nio.file.Files.list(java.nio.file.Path.of("/proc")).iterator()).asScala
        .filter(_.getFileName.toString.forall(_.isDigit)).toVector
      var count = 0; var rss = 0L; var bloopRss = Option.empty[Long]
      procs.foreach: p =>
        if readFile(p.resolve("comm")).map(_.trim).contains("java") then
          val vm = readFile(p.resolve("status")).flatMap(parseVmRssKb).getOrElse(0L)
          count += 1; rss += vm
          if readFile(p.resolve("cmdline")).exists(isBloopCmdline) then
            bloopRss = Some(bloopRss.getOrElse(0L) + vm)
      (count, rss, bloopRss)
    catch case _: Throwable => (0, 0L, None)
  /** One effectful gather; None when the box offers no /proc (non-Linux) → the line silently absent. */
  def gather(): Option[BoxInfo] =
    for
      (total, avail) <- readFile(java.nio.file.Path.of("/proc/meminfo")).flatMap(parseMemInfo)
      load           <- readFile(java.nio.file.Path.of("/proc/loadavg")).flatMap(parseLoadAvg)
    yield
      val (jc, jr, br) = jvmScan()
      val (dFree, dTot) = // root filesystem; JDK FileStore, no /proc needed. Guarded like everything else.
        try
          val fs = java.nio.file.Files.getFileStore(java.nio.file.Path.of("/"))
          (fs.getUsableSpace / 1024, fs.getTotalSpace / 1024)
        catch case _: Throwable => (0L, 0L)
      BoxInfo(total - avail, total, load, Runtime.getRuntime.availableProcessors, readTempC(), jc, jr, br,
              diskFreeKb = dFree, diskTotalKb = dTot)
