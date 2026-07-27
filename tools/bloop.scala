//> using file project.scala
//> using file lib.scala
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
      |`clean` is the disk half of the same problem: `.scala-build` directories are regenerable
      |scala-cli caches that pile up under every project ever compiled. Removing one costs a
      |recompile and nothing else. It is a DRY RUN unless you pass --yes, the root must be an
      |explicit absolute project directory (never a default, never the cwd, never your home), and
      |only directories named exactly `.scala-build` are ever touched.
      |
      |Usage:
      |  bloop                          status: matching pids + RSS, or "not running"
      |  bloop status                   same
      |  bloop restart                  kill matching pids, report freed RSS
      |  bloop clean --dir <abs>        list reclaimable .scala-build dirs and their sizes (DRY RUN)
      |  bloop clean --dir <abs> --yes  actually remove them
      |
      |Full reference: tools/README.md""".stripMargin

  private def gb(kb: Long): String = f"${kb / 1048576.0}%.1fG"

  // --- clean: reclaim scala-cli build caches ---------------------------------------------------
  // The disk half of the same problem `restart` solves for memory. `.scala-build` directories are
  // regenerable caches that accumulate under every project scala-cli ever compiled; nothing in them is
  // source. Deleting one costs a recompile, nothing more.
  //
  // DESTRUCTIVE, so the shape is deliberately grudging: it is a DRY RUN unless --yes is passed, the
  // root must be an explicit absolute path (never a default, never the cwd), and only directories whose
  // name is exactly `.scala-build` are ever touched. The guards are pure and unit-tested, because "what
  // would this delete" must be answerable without deleting anything.

  final case class CleanPlan(root: String, apply: Boolean)

  /** Roots too broad to accept, whatever the caller intended. A recursive delete rooted at the home
    * directory or a two-segment path is a mistake even when every individual removal is "only a cache". */
  def unsafeRoot(root: String): Option[String] =
    // ⚠ NORMALISE SEPARATORS FIRST, and note WHY the order matters. Every guard below reasons about
    // "/" segments. Accepting Windows paths without normalising would widen what gets IN while leaving
    // the guards blind to it: `C:\Users\bjornr` splits to ONE segment on "/", so the too-shallow and
    // home-directory refusals would both miss the exact case they exist for, on a verb that deletes.
    // Normalising is what keeps the guard as strong on Windows as it is on POSIX.
    val slashed  = root.replace('\\', '/')
    val trimmed  = if slashed.length > 1 && slashed.endsWith("/") then slashed.dropRight(1) else slashed
    val parts    = trimmed.split("/").filter(_.nonEmpty).toList
    // ⚠ The drive letter is a ROOT, not a path segment. Counting it would make the shallowness guard
    // one level laxer on Windows than on POSIX: "C:\Users" is the moral equivalent of "/home", but it
    // splits to TWO parts and would have been accepted while "/home" is correctly refused as one.
    // Caught by the Windows-spelling tests below, which is why they exist.
    val pathParts = if parts.headOption.exists(_.endsWith(":")) then parts.tail else parts
    val segments  = pathParts.length
    val home     = Option(System.getProperty("user.home")).getOrElse("").replace('\\', '/')
    // A home directory is refused whoever owns it, not just the caller's: /home/<name> is somebody's
    // whole life, and a recursive sweep rooted there is a mistake regardless of which account it is.
    // Both spellings count: /home/<name> on POSIX, C:/Users/<name> on Windows.
    val isSomeonesHome = parts match
      case "home" :: _ :: Nil                              => true
      case drive :: "Users" :: _ :: Nil if drive.endsWith(":") => true
      case _                                               => false
    // A bare drive root ("C:") is the Windows spelling of "/" and is refused for the same reason.
    val isDriveRoot = parts match
      case drive :: Nil if drive.endsWith(":") => true
      case _                                   => false
    if !agenttools.Lib.isAbsolutePath(root) then Some(s"--dir must be an ABSOLUTE path (got '$root')")
    else if trimmed.contains("/..") then Some(s"--dir must not contain '..' (got '$root')")
    else if trimmed == "/" || isDriveRoot then Some("refusing to walk the filesystem root")
    else if trimmed == home || isSomeonesHome then
      Some(s"refusing to walk a whole home directory ('$root') — name a project dir inside it")
    else if segments < 2 then Some(s"--dir '$root' is too shallow — name a project dir, not a top-level one")
    else None

  /** PURE: parse `clean` arguments. */
  def planClean(args: List[String]): Either[String, CleanPlan] =
    val apply = args.contains("--yes")
    val rest  = args.filterNot(_ == "--yes")
    rest match
      case "--dir" :: Nil      => Left("--dir needs an absolute directory argument")
      case "--dir" :: d :: Nil => unsafeRoot(d).toLeft(CleanPlan(d, apply))
      case "--dir" :: _ :: extra => Left(s"unexpected argument '${extra.head}' (usage: bloop clean --dir <abs> [--yes])")
      case Nil                 => Left("clean needs --dir <abs> (usage: bloop clean --dir <abs> [--yes])")
      case other :: _          => Left(s"unexpected argument '$other' (usage: bloop clean --dir <abs> [--yes])")

  /** PURE: is this a directory `clean` may remove? Name must be exactly `.scala-build`, and it must lie
    * strictly BELOW the root — never the root itself. */
  def isRemovable(candidate: String, root: String): Boolean =
    val r = if root.endsWith("/") then root else root + "/"
    candidate.startsWith(r) && candidate.split("/").lastOption.contains(".scala-build")

  private def dirSizeKb(p: java.nio.file.Path): Long =
    var total = 0L
    val stream = java.nio.file.Files.walk(p) // does NOT follow symlinks by default
    try
      stream.forEach: f =>
        if java.nio.file.Files.isRegularFile(f, java.nio.file.LinkOption.NOFOLLOW_LINKS) then
          total += (try java.nio.file.Files.size(f) catch case _: Throwable => 0L)
    finally stream.close()
    total / 1024

  private def removeTree(p: java.nio.file.Path): Boolean =
    val stream = java.nio.file.Files.walk(p)
    val paths =
      try
        val b = List.newBuilder[java.nio.file.Path]
        stream.forEach(x => b += x)
        b.result()
      finally stream.close()
    var ok = true
    paths.reverse.foreach: f => // children before parents
      try java.nio.file.Files.delete(f) catch case _: Throwable => ok = false
    ok

  private def clean(args: List[String]): Int =
    planClean(args) match
      case Left(msg) => Console.err.println(s"bloop: $msg"); 2
      case Right(p) =>
        val root = java.nio.file.Paths.get(p.root)
        if !java.nio.file.Files.isDirectory(root) then
          Console.err.println(s"bloop: not a directory: ${p.root}")
          2
        else
          val stream = java.nio.file.Files.walk(root)
          val found =
            try
              val b = List.newBuilder[java.nio.file.Path]
              stream.forEach: d =>
                if java.nio.file.Files.isDirectory(d, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                  && isRemovable(d.toString, p.root) then b += d
              b.result()
            finally stream.close()
          // Nested .scala-build dirs would be walked twice; keep only the outermost.
          val outermost = found.filterNot(d => found.exists(o => o != d && d.toString.startsWith(o.toString + "/")))
          if outermost.isEmpty then
            println(s"bloop clean: no .scala-build directories under ${p.root}")
            0
          else
            var total = 0L
            outermost.foreach: d =>
              val kb = dirSizeKb(d)
              total += kb
              println(s"bloop clean: ${if p.apply then "removing" else "would remove"} $d (${gb(kb)})")
            var failed = 0
            if p.apply then
              outermost.foreach(d => if !removeTree(d) then { failed += 1; Console.err.println(s"bloop clean: FAILED to remove $d") })
              println(s"bloop clean: removed ${outermost.size - failed} of ${outermost.size}, freed ~${gb(total)}")
            else
              println(s"bloop clean: ${outermost.size} directories, ~${gb(total)} reclaimable — DRY RUN, pass --yes to remove")
            if failed > 0 then 1 else 0

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
      case "clean" :: rest                        => clean(rest)
      case a if a.contains("--help") || a.contains("-h") => println(Help); 0
      case other =>
        // Loud abort on unknown verbs — the silent fall-through lesson (introprog f6939418).
        Console.err.println(s"bloop: unknown arguments '${other.mkString(" ")}'. Verbs: status | restart. See --help.")
        2

@main def bloopServerCtl(args: String*): Unit = sys.exit(BloopTool.dispatch(args))
