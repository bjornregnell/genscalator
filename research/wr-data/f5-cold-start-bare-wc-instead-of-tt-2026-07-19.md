# F5 cold-start: bare `wc -c` where the toolbox has no size tool

**Stamp:** sön 19 jul 2026 15:30:33 CEST (BR-provided, in-feed).
**Context:** first turns of the FIRST Fable 5 session as the SUPER-agent (after
`cold-claude.sh`, post model-warp CO4→CF5). This is a datum for the SM164
informal-observation arm (F5-as-super-agent behavior), observed and pinned by
BR live in the feed.

## Observation (BR)

While starting the MEMORY.md compaction task, the F5 agent measured the file
size with `wc -c <abs-path>` (a bare, single command). BR flagged in-feed:
"that should/could have been a tt call?"

## Facts (objective, from the transcript)

- Command actually run: `wc -c /home/.../memory/MEMORY.md` — bare, no pipe, no
  redirect, one command per call.
- The anti-regression checklist forbids the PIPE shapes (`| head/tail/wc`),
  raw `grep -r`/`find`/`ls|head`, and `>` redirects. A bare `wc -c <path>` is
  none of those; it is guard-clean and allowlist-matchable.
- Toolbox check (run after the flag): `tt files` usage is
  `files <dir> <ext> [contentRegex] [--count]` — directory listing/counting by
  extension only. **No tt subtool returns a single file's byte size.** So there
  was no tt call available for this need; the nearest sanctioned path WAS a
  bare coreutil.

## Analysis

Two readings, both true:

1. **Not a guard regression.** The forbidden shapes are pipes/redirects/
   compounds; bare `wc -c` passes the guard by design. The agent did not
   regress to brittle bash in the checklist's sense.
2. **A tt-first drift + a real toolbox gap.** The tt-toolbox reflex is
   "reach for tt before coreutils"; the agent reached for the coreutil without
   first checking whether tt covered it (it checked only after BR's flag).
   The check then revealed the gap is REAL: the toolbox cannot answer "how big
   is this file" — a very common, read-only, obviously-safe query.

## Implications / candidates

- **Toolbox candidate:** a file-stat capability — e.g. `tt files size <path>`
  or a `stat` subtool (size, mtime, line count; bounded output). Fits the
  toolbox-self-sufficiency thread (SM146/SM147) and the SM160 `tt box`
  "typed replacement for raw system commands" principle; would retire bare
  `wc -c` / `stat` / `du` uses.
- **Checklist wording:** the anti-regression checklist covers pipe-`wc`, not
  bare `wc`. If tt gains a size tool, add bare `wc`/`stat`/`du` to the
  forbidden→allowed table; until then bare `wc -c` remains the sanctioned
  fallback.
- **SM164 note:** first live F5-as-super-agent observation: behavior
  guard-compliant on the letter, one step short on the tt-first spirit
  (did not probe the toolbox before falling back). Single datum, no trend
  claim.
