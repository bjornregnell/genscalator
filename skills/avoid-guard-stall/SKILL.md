---
name: avoid-guard-stall
description: Construct Bash commands that do NOT trip the guardcheck PreToolUse hook, so no confirmation prompt stalls the agent or races an absent human. Consult before running any Bash command, and especially when a guard confirmation just fired. Core reflexes (they regress to base-model defaults across a compaction, so this skill re-surfaces them): use `tt text grepr` not raw `grep`; use run_in_background not `| tail`/`| head`/`| wc` or `> file`; keep shell metachars (`|` `>` `<` backtick `&&` `;`) OUT of quoted search patterns; one bare allowlist-matchable command per call, never `cd &&` or compound `;`; write files with the Write tool, not a shell redirect.
allowed-tools: Read Bash(tt text *) Bash(tt files *)
---

# avoid-guard-stall — write guard-clean Bash

The genscalator **guardcheck** hook (`tt guardcheck hook`, wired as a `PreToolUse` matcher on `Bash`) fires a
**confirmation prompt** on risky command *shapes*. A confirmation is not a block, but it is a **stall**: the
agent halts mid-flow, and if the human is AFK it **races them** ([[guard-against-forced-confirmations]]). The
guard is deliberate (the [[guardcheck-hook-structural-fix]] structural fix); the job here is to construct
commands that never *need* the confirmation. Structure over willpower.

**Why a skill and not just memories:** these are fine-grained learned overrides. Across a compaction they
**regress to base-model defaults** (the model reaches for `grep`, for `| tail`) because memory files are not
auto-injected into the fresh window — but a skill's description **is** re-listed every session. So this skill
is the compaction-surviving carrier of the reflexes. ([[compaction-regresses-fine-grained-reflexes-2026-07-13]].)

## The guard-tripping shapes and their clean alternatives

| Tripping shape | Why it fires | Guard-clean alternative |
|---|---|---|
| `grep -r` / raw recursive `grep` | tool-choice drift | **`tt text grepr <ABS-dir> <ext> <regex>`** (dir first, regex last) [[use-tt-grepr-not-raw-grep]] |
| `... \| tail` / `\| head` / `\| wc` | "pipe to head/tail/wc" | run the command with **`run_in_background: true`** (harness captures output; Read the tail from the output file). If the tool has one, use its own `--limit` / `--tail` / `--count` flag. |
| `... > file` (output redirect) | "output redirect" | **run_in_background** (harness captures stdout), or for creating a file use the **Write tool**, never a shell redirect. |
| `cd <dir> && <cmd>` / compound `;` | "command chain" / cd+redirect path-bypass | **one bare command per call**, absolute paths. For git, `tt git ... --repo <dir>` [[commit-via-tt-git-not-raw-cd-git]]. |
| a shell metachar (`\|` `>` `<` backtick `&`) **inside a quoted pattern** | the guard scans raw bytes, not the unquoted skeleton — a **false positive** | **write the metachar as a Java-regex hex escape** — `tt text` patterns are Java regex, so `\x3E`=`>`, `\x7C`=pipe, `\x3C`=`<`, `\x26`=`&`, `\x3B`=`;`, `\x60`=backtick. It matches identically and the character never appears in the command string, so the guard **cannot** fire. VERIFIED 2026-07-16: `"^//\x3E using file"` returned the same 18 hits as the `>` form that stalled. Prefer this over the old "just keep patterns metachar-free" advice: a **prohibition does not arm the motor reflex** (this tripped again 3 days after being logged + skilled), whereas an escape gives the moment of action something to *reach for*. **But be honest about its limit (BR, 2026-07-16): this is a BETTER prohibition, NOT a structural fix** — it still needs recall at the instant you type, which is the thing that fails, and a warp can leave this skill cold. Expect to forget it; the guard is the backstop that does not. See "Where the fix must live" below. Fallbacks: anchor on plain terms, split searches, or Read. (Placeholders like `\x3Clabel\x3E` carry literal angles.) [[guardcheck-false-positive-gt-inside-quoted-regex-2026-07-13]] |
| `2>/dev/null` / stderr suppression | "let the tool self-report" | drop it; tolerate benign stderr, or let the tool write a file and Read it. |
| interpreter one-liners (`python3 -c`, `bash -c`) | blank-shell / TOCTOU | never allowlist interpreters; use an audited tool (`tt`, `scala-cli`) [[never-allowlist-interpreters]]. |
| files under `/tmp` for scratch | path-bypass surface | use the in-repo `tmp/` (allowlisted `rm -f .../tmp/*`) [[prefer-inrepo-tmp-over-slash-tmp]]. |

## ⛔ The escape is for FALSE POSITIVES ONLY — never to silence a true finding
The hex escape above is legitimate under **one narrow condition**: the character sits inside a **quoted argument
to a typed tool** and is **provably not a shell operator** (the shell was always going to pass it through as a
literal — the guard simply mis-scanned). Nothing is hidden, because nothing dangerous was there.

**"Encode it until the guard stops complaining" is a scanner-evasion reflex, and it must NOT generalize.** Today
it is benign; the day the guard is RIGHT, the same move silences a true finding. That is the agent defeating its
own safety rail, which is strictly worse than the stall it avoids.

- ✅ `tt text grepr <dir> scala "^//\x3E using"` — a regex arg; the char is inert.
- ⛔ Escaping/obfuscating any character that would **actually** be interpreted by the shell (a real redirect,
  pipe, chain, substitution). You **cannot** do this anyway — the shell parses redirections/pipes at parse time,
  BEFORE expansion, so `$'\x3e'` is an argument, not an operator — but the *intent* is the alarm, not the
  mechanism. If a rewrite's purpose is to make the guard stop objecting to a real shell operation, **stop and
  ask the human** ([[earned-trust-obligates-flagging-risk-more]]).
- The guard stays the authority. Never optimise commands to *evade* it; optimise them to not *need* it.

## When a guard confirmation DID fire
1. It caught a real shape OR it is a quote-scan false positive — either way, **rewrite to a clean shape** from
   the table rather than re-submitting the same command.
2. If a human handed it in as **WR data**, log the specimen (guard-friction / tool-gap) — it is research data
   on the trust boundary and the guardcheck's precision ([[wr-data-workflow-research]], [[not-afk-safe-solo-yields-wr-data]]).
3. Under **AFK-strict** ([[cue-go-afk]], [[guard-against-forced-confirmations]]): bare, allowlist-matchable,
   prompt-free commands ONLY — a confirmation there races the absent human.

## Where the fix must live (BR, 2026-07-16 — the load-bearing insight)
BR asked: *"so we need to make you not forget to use hex escapes? (which could regress after a warp?)"* — and the
honest answer is **yes, the agent will forget, and yes, a warp makes it worse.**

Generalise it: **a fix that lives INSIDE the agent — this skill, a memory, a resume-prompt line — only works if it
is loaded AND hot at the instant of action. So it rots.** Anything routed through agent recall is willpower wearing
a structural costume. That is the whole lesson of the `| tail` family: 5 mechanical slips in one session, one of
them while `+rot-vigil` was explicitly ACTIVE.

**Only fixes OUTSIDE the agent survive a warp:**
| Where the fix lives | Survives a warp? | Examples |
|---|---|---|
| the **guard** (PreToolUse) | ✅ always runs | the stall itself; the fix-text that teaches at the moment of failure |
| the **tool's interface** | ✅ can't be forgotten if it is the only way in | `--limit`/`--tail` flags; `--message-file`; a tool that absorbs the shaping |
| the **human** | ✅ | shepherding the post-warp window |
| a **skill / memory / prompt** | ❌ rots | this file — useful, but never the last line of defence |

So: write the escape down (above), but do **not** rely on it. Design the *tools and guard* so the trippy shape is
not reachable, and treat every in-agent rule as a hint that will eventually fail.

## The complementary tool-lane fix: quote-aware guardcheck (a BUG FIX, not a loosening)
This skill reduces the agent's tendency to *build* trippy commands. The other half is making the guardcheck
itself **quote-aware** (skip quoted spans before scanning for redirect/pipe/compound metachars), killing the
false-positive class at the source.

**Framing corrected by BR (2026-07-16) — the agent had this wrong.** The agent first called this a
"security-relevant loosening". It is not. The guard's **policy** is *no shell redirects*; a `>` inside a quoted
argument **is not a shell redirect** (the shell parses redirections at parse time, before expansion, and passes
it through as a literal). So flagging it is an **implementation bug**, not a deliberate conservative margin —
the agent was mistaking imprecision for protection. Making the implementation match the stated policy is a
**correctness fix**.

- **It is a security IMPROVEMENT (BR's argument):** a guard that cries wolf trains the human to **rubber-stamp**
  confirmations. Alert fatigue is itself a security failure, and the rubber-stamped approval is the exact
  human-rotted axis of the threat model — a false-positive guard *manufactures* the failure it exists to prevent.
- **The surviving caveat is ENGINEERING, not policy:** the gain is real only if the lexer is **correct**. A buggy
  shell-quoting lexer yields **false negatives** (missing a real redirect) — the expensive direction. But that is
  a small, bounded, heavily-testable slice: the hand-roll sweet spot (scala-style §1). Test it hard.
- **The escape does NOT defer it as much as it first seemed** (agent's own recommendation, REVISED after BR's
  warp question): a workaround that routes through **agent recall** is not a reliable fix — per "Where the fix
  must live", the agent WILL forget it. Quote-awareness is the only candidate that needs **no recall at all**,
  so it is stronger than the first pass credited. Observed false-positive rate: 2 in 3 days.
- **Blast-radius-bounded variant** (if it is built): make only the **MED** checks quote-aware and keep the
  **HIGH** ones (`&&`, `;`, `$( )`, backtick, `/dev/stdin`, `<<`) scanning raw bytes. A lexer bug could then
  cost at most a missed MED, never a missed HIGH.

**Agent's recommendation: genuinely open — BR decides.** The first-pass "keep the guard dumb, use the escape"
was under-grounded: it silently assumed the agent would remember the escape. Do not implement the lexer
autonomously; it is a security-relevant loosening.
