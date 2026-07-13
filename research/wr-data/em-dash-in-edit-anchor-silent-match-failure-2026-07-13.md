# WR data: an Edit failed on a long multi-line anchor that also carried an em-dash (2026-07-13)

**Context.** Updating a `foundations.md` glossary entry mid-session. The agent's `Edit` `old_string` was a
**seven-line span** copied out of the file, and it contained an **em-dash glyph** (in "Proposed remedy — the
wake-me-up poll"). The edit failed with *"String to replace not found"*, and the tool added a diagnostic:

> "Edit also tried swapping \uXXXX escapes and their characters; neither form matched, so the mismatch is likely
> elsewhere in old_string."

**What fixed it.** Re-doing the same change as **four small, single-line, em-dash-free anchors** (e.g. the plain
word `Proposed remedy`) succeeded on the first try, each one.

**The honest reading (do not over-claim the cause).** BR's live hypothesis was "those irritating em dashes"
broke it, and that is a *tempting* story because the failed anchor had one and the successful ones did not. But
the tool's own diagnostic pushes back: it says it tried the em-dash both as a literal char and as `\uXXXX` and
**both matched** — so the mismatch was **likely elsewhere** in the long span (a wrap-point whitespace difference,
a soft-wrap artefact, an invisible normalisation somewhere in seven lines). So:

- **Confirmed lesson (already a memory):** anchor on a **short, unique, single-line** substring, never a long
  soft-wrapped multi-line span. See `edit-anchor-short-unique-substring`. This is what actually recovered it.
- **Suspected-but-unconfirmed:** the em-dash *may* still be an aggravator (Unicode normalisation between what the
  model emits and what is on disk is a real class of bug), but this episode did **not** isolate it — the tool
  hinted the opposite. Filed as a hypothesis, not a finding.

**Why it is still good WR data.** It is a specimen of a **silent, causally-ambiguous tool failure**: the error
gives no line, no diff, just "not found", so the human reaches for the nearest salient difference (the em-dash)
and forms a plausible-but-unproven causal story. The tool *could* close this by reporting the **longest matching
prefix** of `old_string` (i.e. "matched up to char N, diverged here"), which would point straight at the real
divergence instead of leaving human and agent to guess. That is the harness-side ask.

**Bonus irony for blog 004:** the em-dash is *also* BR's publication bête noire (`br-dislikes-em-dashes`), so the
same glyph bites on two unrelated axes in one session — a nice small anecdote for the UX-pains post.

Related: `edit-anchor-short-unique-substring`, `br-dislikes-em-dashes`, `research/wr-data/harness-ux.md`.
