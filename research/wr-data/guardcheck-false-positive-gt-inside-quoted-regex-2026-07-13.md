# Guardcheck false-positive: `>` inside a quoted regex arg reads as an output redirect (2026-07-13)

**Type:** WR data — guard-friction / tool-gap specimen (BR handed it in live during an AFK-strict solo run).
**Threads:** [[guardcheck-hook-structural-fix]], [[guard-against-forced-confirmations]],
[[copy-paste-frame-rule]]-adjacent (metachar ambiguity), confirmation-fatigue ([[keep-afk-menu-stocked]]).

## The specimen (verbatim, BR-supplied)
The agent ran, during a "go afk / max solo" run:

```
tt text grepr /home/bjornr/git/berg/bjornregnell/genscalator/blog md "AGENT-DRAFT|agent-draft|> drafted|> initialized|> revoice|TODO|status:"
```

The live guardcheck PreToolUse hook fired:

> Hook PreToolUse:Bash requires confirmation for this command: **[MED] output redirect (>): give the tool a
> file-sink flag; do not redirect around it**

## Why it is a FALSE POSITIVE
The `>` characters are **inside the single-quoted regex argument** to `tt text grepr` — they match markdown
**blockquote** lines (`> drafted`, `> initialized`, `> revoice`) in the blog files. They are NOT shell
output redirects: the whole pattern is one quoted token handed to `tt` as `argv`, never interpreted by a
shell. The command is pure/read-only and cannot redirect anything. Yet the hook's `>`-detector matched the
raw byte and raised a confirmation.

## Two distinct signals

### (a) Tool-gap — `tt guardcheck` over-matches `>` (improvement opportunity)
The redirect check should not fire on a `>` that sits **inside a quoted token**. A cheap, robust fix:
strip/skip quoted spans (single- and double-quoted) before scanning for redirect/pipe/compound metachars —
a `>` (or `|`, `&&`, `;`) inside `'...'` or `"..."` is inert as far as the shell is concerned. This is the
same class as "don't flag a `|` inside a regex alternation". Candidate: teach the guardcheck redirect/pipe
detectors a minimal quote-aware tokenizer so they scan only the UNQUOTED command skeleton. Tightens the
false-positive rate without weakening the real catch (an actual `... > file` outside quotes still trips).

### (b) Operating lesson — the agent should have avoided the shape under AFK-strict
Even granting the tool-gap, this is on the agent: in an AFK-strict run the rule is *bare,
allowlist-matchable, prompt-free commands only* ([[guard-against-forced-confirmations]]) so no confirmation
ever races the absent human. A `>` inside a quoted regex is exactly the kind of metachar-lookalike that the
guard cannot disambiguate, so it will prompt — which it did, racing BR. **Lesson:** under AFK-strict, keep
regexes free of shell-metachar bytes (`>`, `|` as literal, backticks, `&`); to match a markdown blockquote
line, anchor differently (e.g. `^ ?drafted` / word-only patterns) or split into separate metachar-free
searches. The safe move here was to drop the `> drafted|> initialized|> revoice` alternatives entirely (the
`AGENT-DRAFT` / `TODO` / `status:` terms already answered the question).

## The irony (worth keeping)
The guardcheck hook is the STRUCTURAL fix meant to END command-hygiene slips — and here it correctly
*caught the agent's slip* (a metachar in a quoted arg during AFK) while *also* over-matching a benign case.
Both are true at once: the guard did its job (flag the risky shape so the human isn't silently exposed) AND
revealed its own precision gap (quote-blindness). That double-truth is the specimen's value — mirrors the
"guard did its job and revealed its gap" pattern from the self-editable-settings finding.

## Second instance, same class (2026-07-13, later): `|` + `tail` inside a quoted regex
Running `tt text grepr <dir> md "guard.*redirect|tail.pipe|file.sink"` (searching for THIS very specimen)
tripped: *"[MED] pipe to head/tail/wc: use the tool's --limit / --tail / --count flag instead of a pipe"*.
The `|` (regex alternation) and the literal word `tail` are both **inside the single-quoted pattern** — no
shell pipe, no `tail` process. Same root cause as the `>` case: the guard scans raw bytes, not the unquoted
skeleton. Reinforces fix (a) — a quote-aware tokenizer would kill BOTH. And it happened WHILE BR was stepping
away (AFK-adjacent), racing him again — reinforces lesson (b): the search for a metachar specimen should not
itself contain the metachar; anchor on metachar-free terms (`redirect`, `file.sink` alone) or split.

**Third instance, same session:** a raw `grep -n "...<label>...\|...<f>..."` on `mode.scala` tripped the `>`
detector again — the `<label>`/`<f>` placeholders each carry a literal `>`. Three trips of one class in a
single session is a strong frequency signal for the quote-aware tokenizer (fix (a)). It also surfaced a
deeper cause (BR's read): post-compaction the agent had regressed to raw `grep` from the standing `tt grepr`
reflex — the tool-choice slip that PUTS metachars in shell scope in the first place. See
[[compaction-regresses-fine-grained-reflexes-2026-07-13]] and the new [[avoid-guard-stall]] skill (the
compaction-surviving carrier of the guard-clean reflexes).

## Follow-ups
- Fold (a) into an SM / `tt guardcheck` refinement backlog (quote-aware metachar scanning). Not urgent; it
  is a precision improvement, and the current over-match is fail-SAFE (extra confirm, never a missed catch).
- (b) is an agent-side anti-regression: added to the AFK command-hygiene reflex.
