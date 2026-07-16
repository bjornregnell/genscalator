# The hangover hook (naming an agent blackout on resume)

A **SessionStart** hook that tells the agent it was out, and for how long, the moment it comes back (SM121).

## The problem it fixes

The agent cannot perceive a **blackout** from the inside. During a guard stall, a long idle, a compact, an
exit/restart, or a box crash there is no running observer, and afterwards no marker is left behind. The agent
resumes mid-sentence as if nothing happened, with a stale sense of "now" (see the `agent-lacks-felt-time`
problem: the injected clock gives a date, not the gap). A human notices that they walked away for eleven
hours; the agent does not.

What the agent *can* feel is the **hangover** afterwards: compare now to the last conversational record in
the transcript and a gap that dwarfs any plausible execution time means it was out.

## What it does

`tt hangover hook` reads Claude Code's SessionStart hook JSON on stdin and prints one line, which Claude Code
injects into the fresh session's context:

```
hangover: ~11h 37m since your last activity — the session was resumed (exit/restart at the seam).
```

Two fields carry it. `transcript_path` gives the gap. `source` names the **seam**: one of `startup`,
`resume`, `clear`, `compact`. That naming is the hook's whole advantage over the bare tool — from a gap alone,
an idle, a stall, a long command, and a crash all look identical, so `tt hangover <file>` can only say "cause
unknown". SessionStart fires on all four boundaries, so the hook covers resume, restart, clear, and compact.

**It is silent unless there is a hangover.** The output is injected on *every* session start, so it only
speaks when the agent actually was out (default threshold: a gap of 15 min or more). A fresh transcript with
no records says nothing.

## Setup (personal, per machine — human-gated)

Merge into `~/.claude/settings.json`:

```json
"hooks": {
  "SessionStart": [
    { "hooks": [{ "type": "command", "command": "tt hangover hook" }] }
  ]
}
```

Omitting the matcher matches all four sources, which is what you want. To only speak on longer blackouts, add
a threshold: `"command": "tt hangover hook --threshold-sec 3600"`.

Hooks written to `settings.json` take effect live. If `settings.json` did not exist at session start, open
`/hooks` once so the settings watcher picks it up.

## Fail-soft by construction

A SessionStart hook runs before the agent can do anything, so an error here would greet you at the exact
moment you start working. Malformed JSON, a missing or unreadable transcript, an unparseable timestamp: each
yields silence and exit 0. This tool is never worth breaking a session start over.

## Honest limits

- **`source` names the seam, not the whole gap.** A `resume` says the session was exited and restarted at the
  boundary; it says nothing about how long the human sat idle *before* exiting. The wording says "at the
  seam", never "because of", on purpose.
- **Mid-session blackouts are still uncovered.** A guard stall or a long idle *within* a session fires no
  SessionStart, so nothing reports it. Closing that gap needs a different surface (a statusline segment, or a
  `gs warm` check); it is a known, deliberate hole.
- **The threshold is a guess.** 15 min separates "a slow command ran" from "you were out" on BR's box; it is
  not calibrated against data.

## See also

- `tools/hangover.scala` — the tool (the pure core plus this hook surface).
- `research/sm-investigations/` — the transcript probe grounding the timestamp and `compact_boundary` facts.
- `docs/compact-bing-bing.md` — the sibling hook, which calls the *human* back after a compact. This one tells
  the *agent* it was gone. Together they cover both sides of the same blackout.
