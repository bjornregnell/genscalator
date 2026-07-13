//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// box — safe, host-pinned remote ops for a known compute box (default bjornyx.local). Replaces the
// dual-use `ssh *` reflex with a narrow, allowlistable tool: a FIXED verb enum, NO shell passthrough,
// host + model names validated against strict patterns so nothing the caller supplies can inject remote
// shell. Because it can only run these specific read/pull ops, `Bash(tt box *)` is safe to blanket-allow
// where a bare `ssh *` allowlist grants arbitrary remote code execution on ANY host (exfiltration, RCE,
// persistence — the BadGoal vector). See genscalator/research/wr-data for the rationale.
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
    """tt box — safe, host-pinned remote ops for a known compute box (default bjornyx.local)
      |
      |Runs a FIXED set of read/pull operations over ssh with NO shell passthrough: host and
      |model names are validated against strict patterns, so nothing the caller supplies can
      |inject remote shell. Use it to check the box's ollama models, disk, and GPU, or to
      |pull a model guarded by a free-disk floor.
      |
      |Usage:
      |  box models        [--host H]                     ollama inventory (name/size/modified)
      |  box df            [--host H]                     disk usage on / (human-readable)
      |  box gpu           [--host H]                     nvidia-smi utilization + memory (csv)
      |  box freegb        [--host H]                     just the free-GB integer on / (scripting)
      |  box pull <model>  [--host H] [--min-free-gb N]   ollama pull; REFUSED if free disk < N
      |Flags:
      |  --host H                        target host (default bjornyx.local)
      |  --min-free-gb N                 free-disk floor for pull in GB (default 50);
      |                                  pull refuses to start below it
      |
      |ssh runs with BatchMode (never hangs on a password prompt); quick ops time out after
      |60 s, a pull after 1 h.
      |
      |Examples:
      |  tt box models                          # what models does the box have?
      |  tt box gpu                             # is the GPU busy right now?
      |  tt box pull qwen2.5-coder:1.5b         # pull, guarded by the 50G free-disk floor
      |
      |Full reference: tools/README.md""".stripMargin

  private def usage(): Nothing =
    System.err.println(
      """box: usage:
        |  tt box models        [--host H]
        |  tt box df            [--host H]
        |  tt box gpu           [--host H]
        |  tt box freegb        [--host H]
        |  tt box pull <model>  [--host H] [--min-free-gb N]
        |host-pinned remote ops; no shell passthrough (host/model validated).""".stripMargin)
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

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    args.toList match
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

@main def boxRemoteOps(args: String*): Unit = Box.dispatch(args*)
