//> using file project.scala
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
//
// THREE SEVERITIES. HIGH and MED are about shell SYNTAX (chains, substitution, redirects) and carry a
// decision. NOTE is about TOOL CHOICE and carries none — it emits `systemMessage` only, so it can nudge
// without ever costing or granting an approval. NOTE exists because every syntax check missed the SM228
// specimen: `python3 -m json.tool <file>` is syntactically perfect and tripped nothing, so a missing typed
// verb got reached around rather than flagged. Naming the reach IS the fix; see docs/guard-clean-digest.txt.
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

  /** Files whose CONTENT is credentials by construction. Reading one is a disclosure act however
    * read-only the command is (SM231). A `.ssh` PUBLIC key is deliberately excluded: it is not a secret,
    * and a nag people learn to ignore is worse than no nag at all. ⚠ That exclusion is a LOOKAHEAD over
    * the whole token, not a trailing lookbehind: a lookbehind after `\S*` is defeated by backtracking,
    * because the star simply shrinks until the lookbehind is happy and the match succeeds anyway. The
    * first cut had exactly that bug and the negative test caught it. */
  val credentialFileRx: String =
    raw"(\.env\b|\.netrc\b|\.git-credentials\b|\.pgpass\b|\.npmrc\b|\.pypirc\b|\.aws/credentials\b" +
      raw"|\.kube/config\b|\.docker/config\.json\b|\.ssh/id_(?!\S*\.pub\b)\S*)"

  /** The bulk-credential-surface detector, spelled out in parts because each part has its own reason to
    * exist and its own false positive to avoid. WIDENED 2026-07-26: the first cut listed printenv, a bare
    * `env`, and three credential files, and live probes showed `set`, `export -p` and a read of
    * `~/.aws/credentials` all coming back CLEAN — the class was still under-covered by the very check
    * written to cover the class. The dump forms anchor at the START of the command, because that is the
    * only position where they ARE the dump: `tt mode set` and `set -euo pipefail` must never nag. */
  val bulkCredentialReadRx: String = List(
    raw"(?<!tt )\bprintenv\b",                       // exists to print VALUES, targeted or not
    raw"^\s*env\s*($$|[|>])",                        // the dump, incl. piped/redirected; `env --chdir=<abs> <cmd>` must not nag
    raw"^\s*set\s*$$",                               // bash `set` with no args prints every variable and function
    raw"^\s*(export|declare|typeset)\s+-[px]\s*$$",  // the same dump under three other names
    raw"\b(cat|head|tail|less|more|strings|xxd|od|base64|grep)\b.*" + credentialFileRx
  ).mkString("|")

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
    // ---- NOTE tier: TOOL CHOICE, not shell syntax (SM230) ----------------------------------------
    // Every check above reasons about shell SYNTAX. `python3 -m json.tool <file>` has flawless syntax
    // and tripped nothing — which is exactly how a missing typed verb got reached around instead of
    // flagged (SM228). NOTE never denies and never asks: it is a nudge toward the typed verb, not a
    // prohibition, because a genuine one-off interpreter call is a legitimate and cheap choice.
    Check("NOTE", "json via an interpreter",
      "reading or validating JSON through jq / python -m json.tool, where a typed verb exists",
      "use tt json check|get|keys|pretty (dot paths, numeric array indexes: permissions.allow.3)",
      has(raw"\bjq\b|\bpython3?\s+-m\s+json\.tool\b")),
    // Added 2026-07-25 after the NOTE tier — built that same morning for guard-invisible tool-choice
    // mistakes — failed to catch a bare `printenv` that dumped two live API tokens into a transcript.
    // The original tier listed INTERPRETERS, i.e. the shape that had already bitten, rather than the
    // class. This is the class: a bulk read of a credential-bearing surface. Read-only is not safe when
    // the output lands in a durable, copied record.
    Check("NOTE", "bulk read of a credential-bearing surface",
      "dumping the whole environment (or a credentials file) into the transcript — read-only, but the " +
        "output is durable and copied, so this is a DISCLOSURE act; it leaked two live tokens on 2026-07-25",
      "use tt env list <regex> (names only), tt env has <NAME> (exit code only), or tt env get <NAME> " +
        "(one value, redacted). Ask for the NAME you need, never for everything",
      // Detector + its false-positive reasoning live in `bulkCredentialReadRx` above. `tt env …` must
      // never match — flagging the fix would be self-defeating.
      has(bulkCredentialReadRx)),
    Check("NOTE", "general-purpose interpreter",
      "reaching for a general-purpose interpreter — usually the signal that a typed verb is MISSING, " +
        "and the one gap class the guard cannot otherwise see (no cd, no pipe, no redirect to catch)",
      "if a tt verb exists, use it; if the need RECURS, build the verb and name it after the noun " +
        "(tt json is why that works); a genuine one-off scratch or interpreter call is fine — say so rather " +
        "than pretending it was the only option",
      has(raw"\b(python3?|perl|ruby|node)\s+[-\w/.]")),

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

  /** Replace every QUOTED span (the quotes included) with a single space, leaving the command's unquoted
    * SKELETON — the only part the shell can interpret as an operator. None when quoting is UNBALANCED. PURE.
    *
    * WHY: the guard scans raw bytes, so a `>` inside a quoted ARG (`tt text grepr d s "^//> using file"`) fired
    * the redirect check with no redirect present. That is an IMPLEMENTATION BUG, not a conservative margin: the
    * policy is "no shell redirects", and the shell parses redirections at parse time BEFORE expansion, so a
    * quoted `>` is passed through as a literal argument and can never redirect. Fixing it is a correctness fix.
    * It also grants NO new authority — a clean command emits nothing and defers to the user's own permission
    * rules, exactly as today (see the ⛔ never-emit-allow rule on decideFromJson).
    *
    * Shell quoting handled: '...' is wholly literal (no escapes); "..." honours a backslash escape; a backslash
    * OUTSIDE quotes escapes the next char.
    *
    * MASK WITH A SPACE, never deletion — deliberate, and the property that makes this safe: replacing a span can
    * only ADD token boundaries, never join text across one. So a pathological `grep" "-r` collapses to `grep -r`
    * and is STILL flagged. Every error this can make points toward a false POSITIVE.
    *
    * UNBALANCED -> None -> the caller scans the RAW string (today's behaviour): ambiguity fails toward flagging.
    *
    * HONEST LIMIT: for a DELIBERATELY CRAFTED command, quoting a shape hides it from the MED checks. Accepted,
    * and bounded on purpose — only MED checks consult the mask; HIGH keeps scanning raw bytes (see cmdFindings).
    * guardcheck exists to catch the agent's own REFLEXES, not to withstand a crafted attack. */
  def maskQuoted(cmd: String): Option[String] =
    val sb = StringBuilder()
    var i = 0
    while i < cmd.length do
      val c = cmd(i)
      if c == '\\' && i + 1 < cmd.length then
        sb.append(c).append(cmd(i + 1)); i += 2            // an escaped char outside quotes: pass both through
      else if c == '\'' || c == '"' then
        var j = i + 1
        var closed = false
        while j < cmd.length && !closed do
          if c == '"' && cmd(j) == '\\' && j + 1 < cmd.length then j += 2   // \" inside "..." is not the closer
          else if cmd(j) == c then { closed = true; j += 1 }
          else j += 1
        if !closed then return None                        // unterminated quote -> ambiguous -> RAW scan
        sb.append(' '); i = j
      else { sb.append(c); i += 1 }
    Some(sb.toString)

  /** Severity ordering for display and for the hook decision: HIGH > MED > NOTE. PURE. */
  def rank(severity: String): Int = severity match
    case "HIGH" => 0
    case "MED"  => 1
    case _      => 2

  /** The cmd checks, quote-aware: HIGH scans the RAW command, MED and NOTE scan the masked skeleton. The
    * asymmetry BOUNDS THE BLAST RADIUS — a maskQuoted bug can cost at most a missed MED/NOTE, never a missed
    * HIGH. Masking matters for NOTE too: the word `python3` inside a quoted search pattern is data, not a
    * command, and must not nag. PURE. */
  def cmdFindings(command: String): List[Finding] =
    val masked = maskQuoted(command).getOrElse(command)   // unbalanced quotes -> fail safe: scan the raw string
    cmdChecks.flatMap(c => c.find(if c.severity == "HIGH" then command else masked))

  def report(mode: String, findings: List[Finding]): Int =
    if findings.isEmpty then
      println(s"guardcheck [$mode]: clean — no guard-trip / reflex patterns found")
      0
    else
      println(s"guardcheck [$mode]: ${findings.size} finding(s)")
      for f <- findings.sortBy(f => rank(f.severity)) do
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
      val findings = cmdFindings(command)   // quote-aware: HIGH raw, MED/NOTE masked — same fn as `tt guardcheck cmd`
      if findings.isEmpty then ""
      else
        val sorted = findings.sortBy(f => rank(f.severity))
        val text   = sorted.map(f => s"[${f.severity}] ${f.name}: ${f.fix}").mkString("  |  ")
        // NOTE-ONLY -> emit `systemMessage` ALONE: no permissionDecision, so the user's own permission flow
        // applies untouched (the documented "defer" default). A nudge must never cost an approval click, and
        // must never grant one — same ⛔ asymmetry as above, we may tighten, never loosen.
        //
        // HONEST LIMIT, verified against the hook docs 2026-07-25: this reaches the HUMAN, not Claude.
        // `systemMessage` is "shown to you, not to Claude", and PreToolUse's `additionalContext` — the field
        // that WOULD reach Claude — is documented as "Ignored when permissionDecision is defer". So there is
        // no way to whisper a nudge to the agent without also changing the decision to ask (which stalls an
        // unattended run) or allow (forbidden). Consequence: NOTE closes the interactive hole, NOT the AFK
        // one. Closing the AFK hole needs a PostToolUse hook returning additionalContext AFTER the command
        // ran — one turn late, but no stall. That is a settings change, so it is BR's to apply.
        if !findings.exists(f => f.severity == "HIGH" || f.severity == "MED") then
          ujson.write(ujson.Obj("systemMessage" -> s"guardcheck: $text"))
        else
          val decision = if findings.exists(_.severity == "HIGH") then "deny" else "ask"
          ujson.write(ujson.Obj(
            "hookSpecificOutput" -> ujson.Obj(
              "hookEventName" -> "PreToolUse",
              "permissionDecision" -> decision,
              "permissionDecisionReason" -> text)))

  /** PURE: the PostToolUse counterpart, and the ONLY channel that can reach CLAUDE rather than the human.
    *
    * WHY a second mode exists at all (verified against the hook docs 2026-07-25, not assumed): a PreToolUse
    * hook cannot whisper. `systemMessage` is "shown to you, not to Claude", and PreToolUse's
    * `additionalContext` is "Ignored when permissionDecision is defer" — so the only pre-execution ways to
    * reach the agent are `ask` (which STALLS an unattended run) or `allow` (forbidden: it would bypass the
    * user's own permission rules). PostToolUse has no such coupling: it carries `additionalContext` with no
    * decision to make, because the command has already run.
    *
    * The trade is explicit: this fires ONE TURN LATE. The interpreter call already happened. What it buys is
    * that the agent LEARNS — "you just reached past a typed verb" — with no stall and no approval cost, which
    * is exactly the combination an AFK run needs and the PreToolUse path cannot give.
    *
    * NOTE-ONLY by design. HIGH and MED already spoke pre-execution via deny/ask; repeating them here would be
    * noise on a command the user already adjudicated. */
  def postFromJson(stdinJson: String): String =
    val command =
      try ujson.read(stdinJson).obj.get("tool_input").flatMap(_.obj.get("command")).map(_.str).getOrElse("")
      catch case _: Throwable => ""
    if command.isEmpty then ""
    else
      val notes = cmdFindings(command).filter(_.severity == "NOTE")
      if notes.isEmpty then ""
      else
        val text = notes.map(f => s"${f.name}: ${f.fix}").mkString("  |  ")
        ujson.write(ujson.Obj(
          "hookSpecificOutput" -> ujson.Obj(
            "hookEventName" -> "PostToolUse",
            "additionalContext" ->
              s"guardcheck (tool-choice note, the command already ran): $text")))

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
      |  guardcheck posthook [<json>]         Claude Code PostToolUse hook: emits NOTE findings as
      |                                       additionalContext AFTER the command ran. One turn late,
      |                                       but it reaches CLAUDE with no stall — the only channel
      |                                       that does (PreToolUse additionalContext is ignored on a
      |                                       defer, and systemMessage reaches the human, not Claude)
      |
      |Severities:
      |  HIGH / MED   shell SYNTAX problems — these carry a hook decision (deny / ask)
      |  NOTE         TOOL CHOICE nudges (e.g. jq or python where tt json exists). NEVER decides:
      |               it emits systemMessage only, so it cannot cost or grant an approval. It
      |               reaches the human, not Claude — see the note in decideFromJson for why.
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
      case "cmd" :: rest if rest.nonEmpty => sys.exit(report("cmd", cmdFindings(rest.mkString(" "))))
      // msg is a COMMIT MESSAGE, not a shell command — no quote-masking: its checks are about zsh/glob
      // expansion of the message text itself, where quotes carry no shell-skeleton meaning.
      case "msg" :: rest if rest.nonEmpty => sys.exit(report("msg", msgChecks.flatMap(_.find(rest.mkString(" ")))))
      case "hook" :: rest =>
        val json = if rest.nonEmpty then rest.mkString(" ") else scala.io.Source.stdin.mkString
        val out = decideFromJson(json)
        if out.nonEmpty then println(out)
        sys.exit(0)
      case "posthook" :: rest =>
        val json = if rest.nonEmpty then rest.mkString(" ") else scala.io.Source.stdin.mkString
        val out = postFromJson(json)
        if out.nonEmpty then println(out)
        sys.exit(0)
      case _ => usage(); sys.exit(2)
}

@main def checkGuardPatterns(args: String*): Unit = Guardcheck.dispatch(args*)
