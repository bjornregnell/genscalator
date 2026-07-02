//> using scala 3.8.4
//> using jvm 21

// guardcheck — flag the shell-command / commit-message patterns that trip the confirmation guard OR are banned
// agent reflexes, and print the safe rewrite. This is the "prosthetic habit" as a tool (foundations glossary):
// run it on a proposed command/message BEFORE submitting, so the safe form is reached by STRUCTURE, not by
// recalling a rule at the instant of action. Derived from the real guard fires in
// research/confirmation-guard-static-analysis.md (§5 avoidance ruleset, §6 the pre-submit check).
//
// PURE: reads the text arg, computes, prints. No I/O beyond stdout.
//   tt guardcheck cmd "<shell command>"     # chaining / substitution / pipes / redirects / raw grep + literals
//   tt guardcheck msg "<commit message>"    # commit-message traps: line-leading #, =word, <-> / <N-M>
// Exit: 0 = clean, 1 = at least one finding, 2 = usage error.

import scala.util.matching.Regex

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
  Check("MED", "pipe to head/tail/wc",
    "an output-SHAPING pipe a typed tool should absorb as a flag",
    "use the tool's --limit / --tail / --count flag instead of a pipe", has(raw"\|\s*(head|tail|wc)\b")),
  Check("MED", "stderr suppression (2>/dev/null)",
    "shell-suppressing stderr — memory says tolerate harmless JVM warnings, do not suppress",
    "let the tool self-report to a file and Read it; tolerate benign stderr", has(raw"2>\s*/dev/null")),
  Check("MED", "raw recursive grep",
    "a raw recursive grep for a scan — the banned reflex",
    "use tt text grepr <abs-dir> <ext> <regex>", has(raw"\bgrep\s+-\S*r")),
  Check("MED", "output redirect (>)",
    "a > redirect (esp. combined with cd) trips the path-resolution guard",
    "give the tool a file-sink flag; do not redirect around it", has(raw"[^0-9]>\s*\S")),
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

def usage(): Unit =
  println("""guardcheck — flag shell/commit-message patterns that trip the guard or are banned reflexes
    |  tt guardcheck cmd "<shell command>"    check a command (chaining, substitution, pipes, redirects, raw grep)
    |  tt guardcheck msg "<commit message>"   check a commit message (line-leading #, =word, angle-glob)
    |exit: 0 clean, 1 finding(s), 2 usage""".stripMargin)

@main def guardcheck(args: String*): Unit =
  args.toList match
    case "cmd" :: rest if rest.nonEmpty => sys.exit(report("cmd", rest.mkString(" "), cmdChecks))
    case "msg" :: rest if rest.nonEmpty => sys.exit(report("msg", rest.mkString(" "), msgChecks))
    case _ => usage(); sys.exit(2)
