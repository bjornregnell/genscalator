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
| a shell metachar (`\|` `>` `<` backtick `&`) **inside a quoted pattern** | the guard scans raw bytes, not the unquoted skeleton — a **false positive** | keep patterns **metachar-free**: anchor on plain terms, or split into separate searches, or use Read. (Placeholders like `<label>` carry a literal `>`.) [[guardcheck-false-positive-gt-inside-quoted-regex-2026-07-13]] |
| `2>/dev/null` / stderr suppression | "let the tool self-report" | drop it; tolerate benign stderr, or let the tool write a file and Read it. |
| interpreter one-liners (`python3 -c`, `bash -c`) | blank-shell / TOCTOU | never allowlist interpreters; use an audited tool (`tt`, `scala-cli`) [[never-allowlist-interpreters]]. |
| files under `/tmp` for scratch | path-bypass surface | use the in-repo `tmp/` (allowlisted `rm -f .../tmp/*`) [[prefer-inrepo-tmp-over-slash-tmp]]. |

## When a guard confirmation DID fire
1. It caught a real shape OR it is a quote-scan false positive — either way, **rewrite to a clean shape** from
   the table rather than re-submitting the same command.
2. If a human handed it in as **WR data**, log the specimen (guard-friction / tool-gap) — it is research data
   on the trust boundary and the guardcheck's precision ([[wr-data-workflow-research]], [[not-afk-safe-solo-yields-wr-data]]).
3. Under **AFK-strict** ([[cue-go-afk]], [[guard-against-forced-confirmations]]): bare, allowlist-matchable,
   prompt-free commands ONLY — a confirmation there races the absent human.

## The complementary tool-lane fix
This skill reduces the agent's tendency to *build* trippy commands. The other half is making the guardcheck
itself **quote-aware** (skip quoted spans before scanning for redirect/pipe/compound metachars), which kills
the false-positive class at the source. Both together: fewer trippy commands AND fewer false trips.
