//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::ujson:4.4.3

// guardcheck — flag the shell-command / commit-message patterns that trip the confirmation guard OR are banned
// agent reflexes, and print the safe rewrite. This is the "prosthetic habit" as a tool (foundations glossary):
// run it on a proposed command/message BEFORE submitting, so the safe form is reached by STRUCTURE, not by
// recalling a rule at the instant of action. Derived from the real guard fires in
// research/013-confirmation-guard-static-analysis.md (§5 avoidance ruleset, §6 the pre-submit check).
//
// The `cmd`/`msg` CHECKS are PURE (text in, findings out). The `hook` mode wires the SAME cmd-checks into a
// Claude Code PreToolUse Bash hook: it reads the tool-call JSON on stdin (or as an arg, for testing) and emits
// a permission-decision JSON (deny on any HIGH finding, ask on MED-only) so the safe form is reached
// AUTOMATICALLY, not by remembering to run the check. See tmp/guardcheck-hook-proposal.md (SM007c).
//   tt guardcheck cmd "<shell command>"     # chaining / substitution / pipes / redirects / raw grep + literals
//   tt guardcheck msg "<commit message>"    # commit-message traps: line-leading #, =word, <-> / <N-M>
//   tt guardcheck hook [<json>]             # PreToolUse hook: stdin (or arg) JSON -> permission-decision JSON
// Exit: cmd/msg -> 0 clean, 1 finding(s), 2 usage. hook -> always 0 (it signals via the emitted JSON).
import scala.util.matching.Regex

// Helpers (the Check/Finding types, the detector combinators, the check lists, report/usage) scoped in this
// object so their generic names (usage/report/has/rx) don't collide with other tools when the toolbox
// compiles together. Only the @main entry is top-level. See skills/scala-style.
object Guardcheck {
  final case class Finding(severity: String, name: String, why: String, fix: String)

  /** A check: a name/severity/why/fix plus a detector over the input text. */
  final case class Check(severity: String, name: String, why: String, fix: String, hit: String => Boolean):
    def find(text: String): Option[Finding] = if hit(text) then Some(Finding(severity, name, why, fix)) else None

  def rx(p: String): Regex = p.r
  def has(p: String): String => Boolean = t => rx(p).findFirstIn(t).isDefined
  /** any whitespace-separated token starts with `=` (zsh equals-expansion; robust vs a variable-length lookbehind) */
  def hasLeadingEqualsWord: String => Boolean = t => t.split("\\s+").exists(w => w.length >= 2 && w.startsWith("="))

  // ---- command checks (a shell command about to run) ----
  val cmdChecks: List[Check] = List(
    Check("HIGH", "&& command chain",
      "chains commands in one call — the retired git-&& reflex; the guard/BR flags it",
      "split into separate bare commands, ONE per Bash call", has("&&")),
    Check("HIGH", "; command chain",
      "semicolon-chains commands in one call (same family as &&)",
      "split into separate bare commands, one per call", has(";")),
    Check("HIGH", "cd + compound",
      "cd combined with another command; the path-resolution guard cannot validate the cwd-relative paths",
      "use git -C <abs> for git; pass absolute paths; never cd-then-chain", has(raw"\bcd\s+\S")),
    Check("HIGH", "command substitution $(...)",
      "dynamic substitution is unanalyzable by construction — the guard is right to distrust it",
      "list dirs with the Glob/Read tools or pass a literal path; never capture-then-reuse", has("\\$\\(")),
    Check("HIGH", "backtick substitution",
      "backtick command substitution is unanalyzable by construction",
      "use the typed file tools or a literal path", has("`")),
    Check("HIGH", "/dev/stdin commit sink",
      "feeding a commit message via /dev/stdin — the banned shape that produced empty commits this session",
      "write the message to a FILE, then tt git commit --repo <dir> --message-file <path> --add <path> --push",
      has("/dev/stdin")),
    Check("HIGH", "heredoc / here-string (<<)",
      "a heredoc or here-string feeds a shell blob the path-resolution guard cannot analyse (empty-commit trap)",
      "write content to a file with the Write tool and pass it as a --message-file / file argument, never via <<",
      has("<<")),
    Check("MED", "pipe to head/tail/wc",
      "an output-SHAPING pipe a typed tool should absorb as a flag",
      "use the tool's --limit / --tail / --count flag instead of a pipe", has(raw"\|\s*(head|tail|wc)\b")),
    Check("MED", "stderr suppression (2>/dev/null)",
      "shell-suppressing stderr — memory says tolerate harmless JVM warnings, do not suppress",
      "let the tool self-report to a file and Read it; tolerate benign stderr", has(raw"2>\s*/dev/null")),
    Check("MED", "raw recursive grep",
      "a raw recursive grep for a scan — the banned reflex",
      "use tt text grepr <abs-dir> <ext> <regex>", has(raw"\bgrep\s+-\S*r")),
    Check("MED", "grep context flags (-A/-B/-C)",
      "raw grep with -A/-B/-C context flags is not allowlisted -> guard stall (the banned reflex)",
      "use tt text context <file> <regex> <n>, or tt text grepr <abs-dir> <ext> <regex>",
      has(raw"\bgrep\s+-\S*[ABC]")),
    // Two DIFFERENT causes fire this one check, so the fix must name both. The guard scans raw bytes, not the
    // unquoted skeleton, so a `>` inside a QUOTED PATTERN arg (tt text grepr ... "^//> using file") trips it with
    // no redirect present — twice in 3 days. Naming only the redirect fix taught nothing in that case and the
    // agent bounced off it; a fix that does not apply is worse than silence. See the wr-data note
    // prohibition-does-not-arm-the-reflex-use-a-hex-escape-2026-07-16.
    Check("MED", "output redirect (>)",
      "a > redirect (esp. combined with cd) trips the path-resolution guard — and a > inside a QUOTED pattern/string arg fires this same check, since the guard scans raw bytes",
      "if it IS a redirect: use the tool's file-sink flag or run_in_background; never redirect around it. " +
        "If the > sits inside a quoted regex arg: write it as the Java-regex hex escape \\x3E — same match, no > in the command " +
        "(also \\x7C pipe, \\x3C <, \\x26 &, \\x3B ;, \\x60 backtick). Do NOT hex-escape a regex metachar you meant AS a metachar: " +
        "\\x28 is a LITERAL paren, ( is a group.",
      has(raw"[^0-9]>\s*\S")),
  )

  // ---- message checks (a commit message going to git commit -m) ----
  val msgChecks: List[Check] = List(
    Check("HIGH", "line-leading # (newline-then-#)",
      "a newline then # can hide args from path validation — the guard trips on the commit -m body",
      "never start a commit-body line with #; write 'turn N', not '#N' (reflow so # is never first)",
      has(raw"(?m)^\s*#")),
    Check("MED", "leading-= word (=cmd)",
      "zsh equals-expansion — the guard flags a =word literal even inside quotes",
      "rephrase; drop the leading = (fine in file content, not in a shell arg)", hasLeadingEqualsWord),
    Check("MED", "angle-bracket glob (<-> or <N-M>)",
      "zsh reads the angle-bracket form as a numeric/range glob",
      "write N..M or 'the arrow form'; never spell the angle-bracket literal in a shell arg",
      has(raw"<->|<\d+-\d+>")),
  )

  def report(mode: String, text: String, checks: List[Check]): Int =
    val findings = checks.flatMap(_.find(text))
    if findings.isEmpty then
      println(s"guardcheck [$mode]: clean — no guard-trip / reflex patterns found")
      0
    else
      println(s"guardcheck [$mode]: ${findings.size} finding(s)")
      for f <- findings.sortBy(f => if f.severity == "HIGH" then 0 else 1) do
        println(s"  [${f.severity}] ${f.name}")
        println(s"      why: ${f.why}")
        println(s"      fix: ${f.fix}")
      1

  /** PURE: given the raw PreToolUse stdin JSON, return the hook decision JSON (empty string = stay silent).
    * Extracts `.tool_input.command`, runs the SAME cmdChecks; any HIGH -> deny, else MED-only -> ask.
    *
    * ⛔ NEVER EMIT `permissionDecision: "allow"` — NOT EVEN FOR A COMMAND WE ARE SURE IS CLEAN.
    * Per the Claude Code hook docs (verified 2026-07-16): `"allow"` *"Bypasses the permission system and runs
    * the tool immediately"* — *"without checking the permission rules or triggering permission dialogs"*. So an
    * `allow` here would override the USER'S OWN settings.json permissions on the strength of THIS tool's string
    * matching. A bug would then not merely miss a finding, it would silently disable protections that have
    * nothing to do with guardcheck. This tool's job is to ADD findings, never to REMOVE protections.
    * Staying silent (empty string) = the documented `"defer"` default = the user's normal permission flow
    * applies untouched. That is the ONLY correct "we have no objection" signal. (BR caught the agent reasoning
    * loosely toward an `allow` here; the asymmetry is the point — we may tighten, never loosen.) */
  def decideFromJson(stdinJson: String): String =
    val command =
      try ujson.read(stdinJson).obj.get("tool_input").flatMap(_.obj.get("command")).map(_.str).getOrElse("")
      catch case _: Throwable => ""
    if command.isEmpty then ""
    else
      val findings = cmdChecks.flatMap(_.find(command))
      if findings.isEmpty then ""
      else
        val decision = if findings.exists(_.severity == "HIGH") then "deny" else "ask"
        val reason = findings.sortBy(f => if f.severity == "HIGH" then 0 else 1)
          .map(f => s"[${f.severity}] ${f.name}: ${f.fix}").mkString("  |  ")
        ujson.write(ujson.Obj(
          "hookSpecificOutput" -> ujson.Obj(
            "hookEventName" -> "PreToolUse",
            "permissionDecision" -> decision,
            "permissionDecisionReason" -> reason)))

  def usage(): Unit =
    println("""guardcheck — flag shell/commit-message patterns that trip the guard or are banned reflexes
      |  tt guardcheck cmd "<shell command>"    check a command (chaining, substitution, pipes, redirects, raw grep, /dev/stdin, heredoc)
      |  tt guardcheck msg "<commit message>"   check a commit message (line-leading #, =word, angle-glob)
      |  tt guardcheck hook [<json>]            PreToolUse hook: reads tool-call JSON on stdin (or as an arg), emits a permission-decision JSON
      |exit: cmd/msg -> 0 clean, 1 finding(s), 2 usage; hook -> 0""".stripMargin)

  private val Help: String =
    """tt guardcheck — flag shell / commit-message patterns that trip the guard or are banned reflexes
      |
      |Checks a proposed shell command or commit message BEFORE it is submitted, and prints the safe
      |rewrite for each finding — a prosthetic habit: the safe form is reached by structure, not by
      |recalling a rule at the instant of action.
      |
      |Usage:
      |  guardcheck cmd "<shell command>"     check a command: && / ; chains, cd+compound, $( ) and
      |                                       backtick substitution, /dev/stdin, heredocs, pipes to
      |                                       head/tail/wc, raw recursive grep, output redirects
      |  guardcheck msg "<commit message>"    check a commit message: line-leading #, =word
      |                                       (zsh equals-expansion), angle-bracket globs like <->
      |  guardcheck hook [<json>]             Claude Code PreToolUse hook: reads the tool-call JSON
      |                                       on stdin (or as an arg, for testing) and emits a
      |                                       permission-decision JSON (deny on any HIGH finding,
      |                                       ask on MED-only, silent when clean)
      |
      |Exit codes:
      |  cmd/msg: 0 clean, 1 finding(s), 2 usage
      |  hook:    always 0 (it signals via the emitted JSON)
      |
      |Examples:
      |  tt guardcheck cmd "cd repo && git add -A"    # flags the && chain and the cd+compound
      |  tt guardcheck cmd "git log | head -5"        # suggests the tool's --limit flag instead
      |  tt guardcheck msg "fix #42 in parser"        # clean — the # is not line-leading
      |
      |Full reference: tools/README.md""".stripMargin

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then
      println(Help)
      sys.exit(0)
    args.toList match
      case "cmd" :: rest if rest.nonEmpty => sys.exit(report("cmd", rest.mkString(" "), cmdChecks))
      case "msg" :: rest if rest.nonEmpty => sys.exit(report("msg", rest.mkString(" "), msgChecks))
      case "hook" :: rest =>
        val json = if rest.nonEmpty then rest.mkString(" ") else scala.io.Source.stdin.mkString
        val out = decideFromJson(json)
        if out.nonEmpty then println(out)
        sys.exit(0)
      case _ => usage(); sys.exit(2)
}

@main def checkGuardPatterns(args: String*): Unit = Guardcheck.dispatch(args*)
