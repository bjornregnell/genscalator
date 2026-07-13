# WR data: the statusline, lived-in for a while, is loved (2026-07-13)

A **positive / success** specimen (most WR data logs friction; this logs a design that paid off in daily use).

**BR, unprompted, after living with `tt statusline` for a while:** *"BR has been living with the status line for
a while now and BR loves it! can just take a peek at ctx and go on when its green. Lovely UX!"*

## Why it matters

This validates the statusline strand against the **observability asymmetry** at the heart of blog 004 / the
super-context work: the agent **cannot reliably self-read its own context-fill** (RQ0 family E), so the human's
external read is load-bearing. The statusline turns that read into a **cheap, glanceable, non-interrupting**
action: a green `ctx-fill` segment means "keep going" with **zero** cost — no `/context` interruption, no paste
dance, no turn spent. The value is precisely that it is *ambient*: the human absorbs the gauge peripherally and
acts on it (proceed while green) without breaking flow.

## Design points confirmed

- **Threshold colours carry the signal** — "when it's green" is doing the work; the human reads the colour, not the
  number. Confirms the `⚠ approaching` / red-over bands (compact trigger at 0.8·Z) as the right encoding.
- **Ambient > on-demand** for a fill gauge. A `/context`-style on-demand read costs an interruption exactly when a
  human on a long run least wants one (a 004 pain, "can't read the gauge when you need it"); the always-on line
  removes that.
- **The agent-UX inverse still stands.** The line is instrumented for the *human's* eyes; the agent still has no
  equivalent self-gauge. The win here is one side of the asymmetry closing well; the other (an agent-facing
  self-read) remains open. Cf. blog 004 "the agent's side of the UX".

A small but real datapoint that the instrument-the-human-so-they-can-cheaply-steer approach works, and is felt as
*lovely UX*, not just functional.
