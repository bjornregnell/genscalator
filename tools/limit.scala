//> using scala 3.8.4
//> using jvm 21
//> using file minijson.scala
//> using file limitstore.scala

// limit — DECLARE a usage limit the harness feed does not carry (born 2026-07-24, the f5 case:
// Claude Code's statusline JSON has NO per-model weekly window, while /usage shows Fable at 84%).
// The human reads the number in /usage or a warning banner and declares it here; the statusline
// renders it inside the lim block as `f5·~84%·3d` — the `~` marks HUMAN-DECLARED (vs the measured
// clusters), the countdown stays live (computed from the declared reset anchor), and the cluster
// AUTO-DROPS once the declared reset passes, so a dead declaration can never lie.
// Store logic lives in limitstore.scala (mainless, shared with statusline — see its header).
//   scala-cli run tools/limit.scala -- set f5 84 --resets-in 3d20h
private val LimitHelp: String =
  """tt limit — declare a usage limit the harness feed does not carry (human-declared, honest)
    |
    |The Claude Code statusline JSON carries only the 5h + weekly windows; per-model weekly
    |limits (e.g. Fable) exist only in /usage and warning banners. You read the number there
    |and declare it here; the statusline renders `f5·~84%·3d` in the lim block — `~` marks
    |the % as HUMAN-DECLARED, the countdown is computed live from your declared reset anchor,
    |and the cluster auto-drops once the reset passes (a stale declaration cannot outlive it).
    |
    |Usage:
    |  limit                                 list declarations (with time left)
    |  limit set <label> <percent> [--resets-in <dur>]   declare/update (dur: 3d20h, 5h, 90m;
    |                                        omitted -> keeps the label's existing anchor)
    |  limit rm <label>                      remove one declaration
    |  limit clear                           remove all
    |
    |Notes:
    |  Store: ~/.claude/gs-limits.json — GLOBAL, not per-session (account limits are account-
    |  global; the session-scoped mode store is a different thing). Override with --file F
    |  (tests). Labels: short lowercase [a-z0-9]+, shown verbatim in the bar.
    |
    |Examples:
    |  tt limit set f5 84 --resets-in 3d20h    # from the /usage paste: 84%, resets Tue morning
    |  tt limit set f5 91                      # newer banner, same window: update % only
    |  tt limit rm f5
    |
    |Full reference: tools/README.md""".stripMargin

@main def limit(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(LimitHelp); sys.exit(0) }
  val a = args.toVector
  val fileIdx = a.indexOf("--file")
  val file = if fileIdx >= 0 && fileIdx + 1 < a.size then java.nio.file.Path.of(a(fileIdx + 1)) else LimitStore.defaultFile
  val rest = if fileIdx >= 0 then a.patch(fileIdx, Nil, 2) else a
  val nowMs = System.currentTimeMillis()
  val decls = LimitStore.read(file)
  rest.toList match
    case Nil =>
      if decls.isEmpty then println("(no declared limits)")
      else decls.foreach(d => println(f"${d.label}%-6s ~${d.usedP.round.toInt}%%   ${LimitStore.fmtLeft(d.resetsAtMs - nowMs)}"))
    case "set" :: label :: pct :: tail =>
      if !label.matches("[a-z0-9]+") then { Console.err.println(s"limit: label must be [a-z0-9]+, got '$label'"); sys.exit(2) }
      val p = pct.toDoubleOption.filter(v => v >= 0 && v <= 100).getOrElse {
        Console.err.println(s"limit: percent must be 0-100, got '$pct'"); sys.exit(2)
      }
      val resetsMs: Long = tail match
        case "--resets-in" :: dur :: _ =>
          LimitStore.durToMs(dur) match
            case Some(ms) if ms > 0 => nowMs + ms
            case _ => Console.err.println(s"limit: --resets-in wants e.g. 3d20h / 5h / 90m, got '$dur'"); sys.exit(2)
        case _ =>
          decls.find(_.label == label).map(_.resetsAtMs).getOrElse {
            Console.err.println(s"limit: first declaration of '$label' needs --resets-in <dur>"); sys.exit(2)
          }
      LimitStore.write(file, decls.filterNot(_.label == label) :+ LimitStore.Decl(label, p, resetsMs, nowMs))
      println(s"declared $label ~${p.round.toInt}% (${LimitStore.fmtLeft(resetsMs - nowMs)})")
    case "rm" :: label :: Nil =>
      LimitStore.write(file, decls.filterNot(_.label == label))
      println(s"removed $label")
    case "clear" :: Nil =>
      LimitStore.write(file, Vector.empty)
      println("cleared")
    case _ =>
      println("usage: limit [set <label> <pct> [--resets-in <dur>] | rm <label> | clear]   (bare: list)")
      sys.exit(2)
