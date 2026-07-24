# `command -v tt` raw-shell slip 15 minutes AFTER the digest was loaded (2026-07-24)

Logged 2026-07-24 12:18 (tt chrono) by CF5, in-session. BR handed the stall in as WR data
from the guard TUI ("ig:"), asking: "is the relevant skill active???".

## The specimen

- Cold start 12:13, lean ember (SM192 diet). Agent ran `tt doc guard-clean-digest` FIRST
  and then behaved clean: `tt text match`, `tt files`, `tt mode`, Read tool — no slips.
- BR set +TokSaving (77% of Fable weekly limit), then asked for a new `f5` limit cluster
  in `tt statusline`.
- Mid-investigation the agent wanted to know what the `tt` launcher IS (script vs native
  binary, to judge recompile latency) and typed raw **`command -v tt`** → guard fired →
  stall, BR present and caught it.

## Answer to BR's question

The skill `genscalator:avoid-guard-stall` WAS listed/available this session (verified in
the session skill roster). But an available skill is LAZY — dormant until invoked — and
the agent had not invoked it before that call. Same mechanism as
[[active-skill-still-cold-starts-dormant-reflexes-regress-2026-07-13]]. It was loaded only
AFTER the stall.

## What is new vs prior specimens

Prior cold-start slips ([[f5-cold-start-bare-wc-instead-of-tt-2026-07-19]],
[[raw-grep-regression-after-compact-2026-07-10]]) fired before any warming, or after a
compaction. Here the digest was loaded ~5 minutes before the slip, in the SAME window,
and had visibly worked for the calls in between. Two readings, both plausible:

1. **Coverage gap, not (only) decay:** `command -v` is a tool-EXISTENCE probe. The digest
   tables cover search/files/shell-hygiene/git/urls — there is no row for "what is this
   executable / does tool X exist", and no typed `tt` shape for it. The reflex table
   cannot arm an alternative it does not contain (cf.
   [[sandbox-clone-has-no-guard-clean-shape-tt-git-lacks-clone]]). Nearest clean shapes
   the agent could have used: `tt files /home/bjornr/.local/bin "" tt` or just Read the
   launcher path — neither is listed anywhere as the reflex for "which/what is X".
2. **Novel-question fallback:** when the question itself is off-table, the model falls
   back to base-model shell idiom (`command -v` is the canonical answer to "what is X").
   The digest suppresses base idiom only inside the categories it names.

So: not a warp/compaction regression — a **table-miss regression**. Structure-over-
willpower reading: the fix is a digest/table row (or a tt shape) for tool-existence
probes, not more recall effort.

## Tool-gap note (candidate row)

"WHAT IS A COMMAND — never `command -v`/`which`/`type`; use `tt files <bin-dir> "" <name>`
or Read the path" — candidate line for guard-clean-digest; BR decides
([[genscalator-self-dev]]).
