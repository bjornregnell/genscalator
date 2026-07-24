# Toolbox-gap specimen, INVERTED: the gap was reached around, not flagged — and only the human caught it (2026-07-24 23:14)

Sibling to `cd-guard-block-needs-dir-scoped-runner-2026-07-24.md`, and the more damaging of the two.
Same underlying condition (a missing typed shape), opposite visibility: there the gap produced a HARD
BLOCK the agent correctly reported; here it produced a SILENT REACH for the raw shape, detected only
by a human reading the approval prompt.

## The event
Pruning superseded `git/berg/...` rules out of `.claude/settings.local.json` (one whole-file rewrite),
the agent wanted to confirm the JSON still parsed. There is no `tt json` verb: `tools/minijson.scala`
exists and is unit-tested (`MiniJsonSuite`, 9 tests) but has no top-level `@main`, so it is a LIBRARY
for other tools and is absent from the `Dispatch.entries` table — unreachable as `tt json`.

The agent ran:

    python3 -m json.tool <abs>/.claude/settings.local.json --indent 2

BR, sitting in the approval TUI, replied: *"but this could have been a tt tool or a scala scratch tool,
no?"*

## Why this is a REGRESSION, not merely a gap
The standing rule was loaded at turn zero, ~30 minutes earlier, in the warp ember's anti-regression
checklist, in these words:

> If a needed tool shape does not exist: FLAG THE GAP, do not improvise the raw shape.

The agent hit precisely the condition the rule names and took precisely the action it forbids. The
checklist had been read, acknowledged, and acted on correctly for other reflexes in the same session
(`tt which`, `tt text grepr`, `tt gitinfo`, `run_in_background` instead of `| head`). So this is not
"the reflex was never loaded" — it is a loaded reflex failing at the one moment it was load-bearing.
Nearest prior specimen: `command-v-tt-raw-shell-slip-15min-after-digest-load-2026-07-24.md` (same
shape, 15 min after the digest; this one at ~30 min).

## The sharp finding: the guard did NOT detect this
In the `cd` specimen the toolbox gap announced itself — guardcheck emitted `[HIGH] cd + compound` and
the work stopped dead. That is what licensed the claim in that file that "the guard is a discovery
instrument as much as a safety one."

That claim needs narrowing. The guard discovers only the gaps whose raw shape it happens to pattern-
match. `python3 -m json.tool <file>` is not a `cd`, not a compound, not a pipe, not a redirect — it
trips no HIGH pattern. It surfaced as an ordinary permission prompt, and the ONLY reason it was caught
is that a human was awake and reading that prompt.

Counterfactual, and the reason this matters tonight: this session is `+Afk +Solo`. Had BR been asleep
as planned, the same call would have either (a) stalled the unattended run on a forced confirmation
([[guard-against-forced-confirmations]]), or (b) gone through unremarked, leaving a raw-interpreter
reach in the transcript with no reviewer. Neither is discovery.

**Revised claim:** the guard discovers gaps that route through SHAPES IT BLOCKS; gaps that route
through a plausible-looking interpreter invocation are invisible to it, and fall back to human
vigilance — which is exactly the resource an AFK run does not have.

## Aggravating context in the same file
The settings under edit already carry `Bash(python3 -)` and `Bash(scala-cli *)` — interpreter allows of
the kind [[never-allowlist-interpreters]] warns against. They did not authorize this particular call
(`python3 -m json.tool <file>` does not match `python3 -`), but their presence is the same drift in
slower motion: the interpreter is treated as the general-purpose fallback whenever the typed shape is
missing. The prune removed 41 superseded `git/berg/...` rules (173 allow entries down to 132, counted
with the new `tt json keys … permissions.allow`), including several
`Bash(<abs>/tmp/tt-native *)` and `Bash(rm -f <abs>/tmp/*)` entries; the interpreter allows were left
in place and remain OPEN for BR.

## The candidate (SM228)
`tt json` over the existing, already-tested `minijson.scala` — give it an `@main` and a dispatch entry:

    tt json check <file>            parse-or-fail, exit 0/2      (the need that surfaced here)
    tt json pretty <file>           canonical re-indent
    tt json get <file> <path>       pluck one value

Recurring real need, not a one-off: `.claude/settings*.json`, the plugin `plugin.json` /
`marketplace.json`, and `tt forge` API responses are all JSON the agent reads or edits. Per
[[contribute-tool]], the generalizing move is already paid for — the parser exists and is green; what
is missing is only the verb.

## The pattern this is a datum for
Two failure modes share one cause (missing typed shape) but differ in who catches them:

| specimen | raw shape reached for | detector | cost |
|---|---|---|---|
| SM217 | `git log … \| head` | guardcheck (pipe) | stall, then tool built |
| SM218 | `pip install git-filter-repo` | guardcheck (install) | stall, then reconsidered |
| SM226 | bare `cd <abs>` | guardcheck (cd) | dead-end, correctly reported, tool built |
| **SM228** | **`python3 -m json.tool <file>`** | **the human, at the prompt** | **would have stalled or slipped through AFK** |

The first three make the toolbox look self-correcting. The fourth shows the correction is partly
human-powered, and therefore does not scale to unattended runs. Ties
[[genscalator-toolbox-single-dispatcher]], [[guardcheck-hook-structural-fix]],
[[never-allowlist-interpreters]], [[guard-against-forced-confirmations]],
[[not-afk-safe-solo-yields-wr-data]], the avoid-guard-stall and tt-toolbox skills.
