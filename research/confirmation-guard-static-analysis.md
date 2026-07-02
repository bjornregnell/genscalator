# Confirmation-guard static analysis - true vs false positives, and how `tt` earns silence

- **Question:** the harness gates risky shell commands behind a confirmation prompt. Some prompts are the
  agent's fault (a bad reflex); others are **false positives** on a perfectly benign convention. To make `tt`
  both *safe* and *low-confirmation*, we need a model of **when and why the guard fires**, and which fires are
  "actually unnecessary" (designable-away) versus legitimate. BR: "important input to when we find ways to be
  safe with tt and at the same time avoid actually unnecessary confirmations."
- **Why it matters:** directly serves the human's **no-CF** goal and the **BHH** threat model (never widen the
  attack surface to cut a prompt). The whole `tt` thesis is *safe-by-design AND statically provable -> no prompt*
  (the #6266 `$TT`-vs-literal-`tt` insight, generalized). See [`instruction-adherence-decay.md`](instruction-adherence-decay.md),
  [`inference-time-learning.md`](inference-time-learning.md), and the `wr-data/` ledger.
- **Status:** open, foundational (new 2026-06-30, from the accumulating guard-fire evidence).

## 1. What the guard is
A **static, pre-execution command analyzer**. It parses the proposed shell command and, if it cannot *prove*
the command stays within safe/allowlisted capabilities, it asks the human. The crucial property: it is
**sound, not complete** - it errs toward asking. It *must*, because "what does this shell string actually do?"
is **undecidable in general**: dynamic variable expansion, word-splitting, globbing, and command substitution
mean the executed effect is not a pure function of the visible text. A sound analyzer over-approximates the
danger and prompts whenever it cannot rule it out.

## 2. Two kinds of fire (the key distinction)
Every prompt is one of two categories, and they need **opposite** fixes:

- **TRUE POSITIVE (agent earned it).** The command really is outside the safe envelope, or is ambiguous in a
  way the agent could have avoided: the shell-reflex family - `cd`+redirect, `cd`+git, pipe-to-`tail`, chained
  `cd`, **command substitution / backticks / capture-then-reuse (`d=$(find …); … "$d"`)**, and **destructive
  ops (`rm …`)**. Fix = **change AGENT BEHAVIOR** (`git -C`, one bare command, tools self-report to a file, list
  dirs with Glob/Read not `$(…)`, don't `rm` disposable scratch). Coded **WR-REGRESS**; the durable cure is
  structural (submit-time hook), per `instruction-adherence-decay.md`. NB: command-substitution is genuinely
  unanalyzable **by construction** (the executed text is computed at runtime), so there the guard is *correct* to
  distrust - it is a true positive, not a mis-fire; the fix is to not write dynamic shell, not to re-notate it.
- **FALSE POSITIVE (harness mis-fired).** The command is benign but contains a token pattern the conservative
  parser cannot disambiguate from an attack: `<N-M>` (zsh numeric-range glob), `\n#` (a comment that could hide
  args), `$VAR` (an expansion unknown at analysis time). Fix = **change OUR NOTATION / tooling** so the safety
  proof *succeeds*: `N..M`, no body line starting with `#`, a literal command not `$VAR`. Coded **WR-FRICTION**;
  it is harness incidental complexity, *not* an agent regression.

Conflating the two wastes effort: you cannot exhort away a false positive, and you cannot re-notate away a real
reflex. **First triage every prompt into TP vs FP; only then pick the fix.**

## 3. Why false positives are structural (and fair to route around)
Each false-positive pattern we have hit is a place where the **shell grammar is ambiguous enough that a sound
analyzer must assume the worst**:

| pattern | why the analyzer can't prove it safe |
|---------|--------------------------------------|
| `$TT` / any `$VAR` | the expansion is unknown at analysis time - could resolve to *any* path |
| `=word` (leading `=`, e.g. `=OVERRIDE=`, `=cmd`) | zsh **equals-expansion**: a word starting with `=` resolves to the path of a command (`=ls` -> `/usr/bin/ls`), so the analyzer must treat it as an unknown filename expansion |
| `<N-M>` / `<->` / `<-` | zsh reads the angle-bracket forms as numeric/range globs - could expand to many filenames (all three variants trip it) |
| `\n#` inside a quoted arg | `#` starts a comment at a word boundary unquoted; inside quotes it is literal - analyzer and shell can **disagree**, so a `\n# ...` could hide real args from validation while the shell still runs them (a parser-differential bypass; comments run to end-of-line, so a newline-led `#` can swallow a whole injected line) |
| `cd X && cmd` | `cmd`'s path resolution depends on the `cd`, so the analyzer cannot validate `cmd`'s paths against the *real* cwd |
| multiple `cd` in one command | the effective cwd at each step is path-dependent and hard to track soundly |

These are **not bugs**; they are the price of *soundness over a Turing-complete command language*. BR's framing:
incidental complexity imposed by the guardrail, and it is fair to route our own conventions around it.

## 4. The design rule for `tt` (the synthesis)
A `tt` invocation should be **safe by design AND statically transparent** - the analyzer can prove safety by
inspection, so it never prompts. Concretely, a `tt` command should be:
1. a **literal** command (an allowlisted binary on `PATH`), never via `$VAR`/`eval`;
2. with **literal/typed arguments** that avoid shell metacharacters the analyzer must over-approximate (no
   unbounded globs, no `\n#`, no quoting tricks);
3. **one command per invocation** (no `cd`-compounds, no chains mixing `cd`/redirect/git/tools);
4. with **bounded, declared effects** (the `--sandboxed` / `--safe-mode` / `--audit` flags from #7180) so a
   one-time "always allow" is genuinely low-stakes (defusing the #6305 fatigue-whitelisting hazard).

When all four hold, the analyzer's **soundness requirement is satisfied rather than violated**, and the safe
action runs **silently**. That is the precise mechanism converting "statically analyzable" into "no
confirmation." The two goals - *be safe* and *avoid unnecessary prompts* - are therefore **the same goal**
viewed from two sides: a command the analyzer can prove safe is both.

## 5. The avoidance ruleset (living; each entry is a FALSE-POSITIVE fix)
Conventions that keep benign commands provable, derived from observed guard fires:
- ranges `N..M` (or two ints), **never** `<N-M>` / `<->` / `<-` - all three angle-bracket forms are zsh
  numeric/range globs. (Fine in FILE content; in a commit `-m` or grep pattern, rephrase to "the arrow form".)
- **no word starting with `=`** in a shell arg (`=OVERRIDE=`, `=cmd`) - zsh equals-expansion. (Fine in FILE
  content; in a commit `-m` or grep pattern, rephrase or drop the leading `=`.) Also **don't invent-and-search**
  speculative marker literals - grep only KNOWN real tokens.
- **no command substitution / backticks / `x=$(…)` then `$x`** - genuinely dynamic (the guard is *right* to
  distrust, §2 TRUE POSITIVE): list a directory with the Glob/Read tools or pass a **literal** path; never
  capture-then-reuse in a shell string.
- **don't `rm` gitignored scratch** - `tmp/` is disposable-in-place, so leave it (overwritten next run). If a
  cleanup is genuinely wanted, add a **scoped** allow-rule (`Bash(rm -f <repo>/tmp/*)`), never a broad `rm`.
  (This is a TRUE-positive gate the human relaxes deliberately for a proven-safe path - like the AFK
  git-loosening - not a false-positive re-notation. See the settings mirror `wr-data/settings-local-mirror.json`.)
- **wait for async work with `run_in_background` + END TURN** (the harness push-notifies on completion), or the
  `Monitor` tool - never a `while/until … do … done` poll or a foreground `sleep`-poll (a blocking shell loop).
- **no body line starts with `#`** inside a quoted arg (commit messages etc.); write `- #N` or `turn N` -
  comment-hiding bypass. (Keep `#N` freely in FILE content - it only bites inside shell-quoted args.)
- `git -C <abs>` , never `cd X && git` - untrusted-hooks + cd path-resolution.
- **one bare command per call**; never chain `cd`/git/tools with `&&` - multiple-cd / path-resolution-bypass.
- never combine `cd` with a pipe/redirect - path-resolution-bypass; instead a tool **self-reports to a file**
  (instrumentation-by-default) and the agent Reads it.
- literal `tt`/commands, never `$VAR` - unresolvable expansion.
- batch work via the **harness's parallel tool calls**, not `&&`-chaining in one shell string (the META-7 trap:
  the token-efficiency drive itself can produce a chain that trips a guard).

(TRUE-POSITIVE fixes - the agent-behavior side - live in `instruction-adherence-decay.md` + the memories.)

## 6. Open: a `tt`-lint / pre-submit transparency check
Could genscalator ship a tiny checker that, before a shell command is submitted, flags any §5 pattern and
rewrites it to the provable form? That moves avoidance from **agent memory** (substrate #1/#2, unreliable -
the reflex re-fires regardless) to **structure** (substrate #3: the submit-time hook), now with a **precise
target list derived from real guard fires**. It would intercept *both* categories: rewrite FALSE-POSITIVE
notation (e.g. `<N-M>` -> `N..M`) and the TRUE-POSITIVE reflexes (`cd X && git` -> `git -C X`). Measurement:
guard prompts per session, before vs after - the regression-rate metric from `inference-time-learning.md` §7.

## 7. The honest limit (BHH lens)
Routing around a guard must **never** be routing around real safety. The test (from the #7180 threat model):
a notation change is legitimate only if the command was *already* safe and we are merely making that **provable**
to the analyzer. We change *how the safe action is spelled*, never *what it does*. The day a "guard workaround"
would let an actually-unsafe action through, it stops being incidental complexity and becomes the guard doing
its job - keep the distinction sharp so the no-CF goal never quietly serves a BHH BadGoal.

## What shipped
- Nothing yet (analysis note). Feeds: the **submit-time hook** design (§6), the `tt` flag taxonomy
  (`--sandboxed`/`--safe-mode`/`--audit`), and the avoidance ruleset that the memories + `RawData` workflow
  already encode piecemeal. Candidate graduation: a `docs/` guide "Why your safe command still prompts, and how
  to spell it so it doesn't" + the `tt`-lint checker.
