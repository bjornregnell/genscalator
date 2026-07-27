# genscalator blog

Public-facing posts distilled from `research/` (research = evidence + working-out; blog = the narrative for an
outside reader). Posts are named `NNN-slug.md` — a zero-padded sequence number, **no dates in filenames** (a
post's date lives in its status banner, not its name).

**Why the numbering has gaps:** this directory holds the posts that are actually out in the world. Unpublished
drafts and stubs keep their reserved numbers but live in the closed work repo until they are ready, so a
newcomer reading here meets finished writing rather than a construction site.

## Posts
- [`000-why-genscalator.md`](000-why-genscalator.md) — the project's foundational "why":
  dynamic-shell default → typed, safe-by-design tools.
- [`002-braceful-or-braceless-or-the-common-style.md`](002-braceful-or-braceless-or-the-common-style.md) —
  Scala braces vs significant indentation vs a shared "common style", judged by agent edit-cost. An
  experiment, and an honest null result.

*Status: initialized 2026-07-03; arc 004→008 added 2026-07-04; updated 2026-07-26 (unpublished drafts moved to
the closed work repo, so this list names only what is live).*

## Status model
Each post (and this README) carries a **status banner** tracking where it is in its lifecycle. The states run
in order, and the final one repeats:

`initialized` → `drafted` → `published` → `deployed` → `updated` → `updated` → …

- **initialized** — the slot exists (number + slug reserved; maybe just a seed or outline), not yet real content.
- **drafted** — a full draft is written but not yet public.
- **published** — released to the outside world (shared/announced); the citable version.
- **deployed** — live on the site (**bjornregnell.se**, via the static-site generator). Distinct from *published*: a
  post can be published-as-text yet not yet on the site, and a *deployed* post that is later **updated** has drifted
  from what's live (→ needs a redeploy). Track `deployed` vs `updated` so we always know which live posts are stale.
- **updated** — a post-publication revision; **repeats**, each with its own date, so the change history stays visible. **When you revise an already-`deployed` post, append a fresh `updated <date>` line to its banner *before* re-deploying** so the stamp records the edit and marks the live copy stale until the redeploy lands (enacted 2026-07-11 on post 002). A purely cosmetic or structural fix (e.g. reordering preamble lines) does **not** warrant an `updated` stamp; `updated` tracks reader-relevant revisions, not trivial edits.

Each status is stamped with its date — e.g. *Status: drafted 2026-07-03*, or after release
*Status: published 2026-08-01; deployed 2026-08-02; updated 2026-09-15*. A post never silently changes state.

### Where a post lives, and one deliberate exception

**The rule, from 2026-07-27 onward: a draft lives in the closed work repo until it is ready.** A post
appearing in *this* directory is therefore public by that act alone — visible to anyone browsing the repo,
whether or not it has reached the site. That is the sense of *published* used in
[`docs/foundations.md`](../../docs/foundations.md)'s glossary: **published = in the open repo, deployed =
on bjornregnell.se.**

**The exception: a few posts predating that rule remain here at `drafted` or scaffold status.** They are
not moved, deliberately, because inbound references point at them and relocating them would break those
refs to no benefit — the cure would cost more than the condition. They are harmless to have public: the
status banner at the top of each says plainly that the prose is unrevised, so a reader is not misled about
what they are reading. New drafts do not get this treatment; they start closed.

Which posts these are is deliberately **not listed here** — a list would go stale as they graduate, and a
stale list is worse than none. **Each post's own status banner is the source of truth.**

⚠ Note the collision this creates, since both readings are in live use: such a post is *published* in the
glossary sense (it is in the open repo) while its status field still says `drafted`. When it matters, say
which sense you mean.

## Audience
Each post's banner also names its **Audience** — *who may want to read this?* — right after the Status line, so
a reader can tell at a glance whether the post is for them, and so drafting stays reader-focused.

## Authorship & voice (human decision, 2026-07-04)
These posts are **written by BR (the human); the agent assists** — drafts, structure, raw material, argument-checking,
and the human's own words verbatim. The finished prose is the human's, in the human's voice. **Why:** the web is
already full of generated slop; the value here is *real human intention and experience surfacing* — **äkthet**
(Swedish: authenticity / genuineness). So agent-produced text in this dir is **raw scaffold for BR to rewrite**, not
ship-ready copy; where the human's own words already exist (e.g. the "panic writes"), they go in **verbatim**. This
**resolves the "authorial-voice" open question** (was: agent-authored essays about the agent = a hall of mirrors) —
the human is the genuine author; the agent's introspection enters as *quoted data*, not as the narrating voice.

## Deployment
The static-site generator exists: `tt ssg` renders the posts and `deploy/deployblog.sc` uploads them. The flow is
**status-driven** — only posts whose banner says `published` or `deployed` are rendered, so the banner is what
decides whether a post is live, and a draft cannot reach the site by accident.

```
scala-cli run deploy/deployblog.sc -- --serve             # render the live set, preview locally
scala-cli run deploy/deployblog.sc -- --release --dry-run # show what WOULD upload, change nothing
scala-cli run deploy/deployblog.sc -- --release           # promote published to deployed, and push
```

## Later
Do we need a **blog-skill** (standardize note→post: structure, status banner, auto-numbering, cross-links to
`research/`)? Parked until a few posts exist and the repeated shape is worth automating.
