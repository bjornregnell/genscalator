# WR data — the agent forgot an available resource in its own substrate (closed-repo books/) (2026-07-12)

**Category:** substrate awareness / external-memory limit + copyright discipline. Sibling of
[`agent-lacks-felt-time-...`](agent-lacks-felt-time-stale-night-frame-2026-07-12.md) and the
[[agent-cant-internalize-huge-codebases]] theme. BR flagged it.

## The specimen

Planning the asymmetry study (blog 021 / SM054), the agent needed the CSR methods book and offered:
*"do you have a PDF I can point at, or should I plan from my knowledge?"* — when a PDF was sitting in
**our own closed repo** at `muntabot-synch-introprog/books/case-study-research-in-software-engineering.pdf`
the whole time. BR: *"you forgot about the closed-repo books dir with copyrighted pdfs."*

## Mechanism

The agent's "memory" is the external **super-substrate** (files, repos, memories), not a held, continuous
interior. A resource that is not in recent context or in an indexed memory is **effectively invisible** —
the agent does not *feel* "we own that book," it must *look it up*, and it will not look up what it does not
know to look for. So it defaulted to asking / to training knowledge instead of checking `books/`. This is
the **same asymmetry blog 021 is about**, caught reflexively while planning the study of it: the human holds
"we have that book" natively; the agent holds it only if the substrate is indexed and surfaced.

## Two lessons

1. **Substrate-check reflex.** When a reference is needed, CHECK the substrate first (a `books/` dir, the
   memory index, `tt files`) before assuming the choice is "ask the human" vs "rely on training." The fix is
   structural: a durable memory pointer to `books/` (now written) closes *this* gap; the general gap
   (unindexed substrate is invisible) is the standing limit.
2. **Copyright discipline.** `books/` holds COPYRIGHTED PDFs in the CLOSED repo. They may be used to ground
   work (read, cite section/page), but their text must NEVER be reproduced in the PUBLIC genscalator/blog —
   paraphrase in our own words + cite; ideas/methods are free, expression is not. (BR is a CSR co-author, so
   also own the self-reference per [[br-se-methods-coauthor-coi]].)

Ties: [[agent-cant-internalize-huge-codebases]], the substrate-as-external-memory thread, blog 021 (a live
DIFFERENCES specimen: agent memory is external and not auto-held).
