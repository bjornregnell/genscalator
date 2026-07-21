# Issue 001: split References data from rendering

> status: parked · labels: refactor, media · summary: `media/blog/References.scala` mixes the
> reference DATA with `toMarkdown`/`toHtml`/`toBibTex` rendering logic; lift the renderers out
> (e.g. `mkMarkdown(r): Markdown` in a separate render module) so the data stays a pure model.

## Description

`media/blog/References.scala` (package `blog`) carries both the typed reference entries (the
data, incl. the Iron-refined `Year`) and the output rendering (`toMarkdown`/`toHtml`/`toBibTex`).
The refactor: split data from rendering — lift the renderers into a separate module (likely a
`Markdown.scala`-style render resource, plausibly shared with the `tt ssg` static-site work for
bjornregnell.se). Full analysis and the boundary lean: `research/036-references-refactor-plan.md`.

Trigger condition (from the original note): act when the file feels cramped or when SSG work
needs the renderers — SSG exists now (`tt ssg`), so the second trigger is armed; the pain has
not yet bitten, hence parked rather than in-progress.

## Discussion

### Comment by bjornregnell/CF5 at 2026-07-21 18:25

*(Stamp corrected per meta-minion push-16: originally written "18:30", forward-estimated ~4 min
past its enclosing commit `ee6bd8d` 18:25:58 — the thrice-caught estimated-stamp reflex. Rule
adopted: a stamp in an issue file is the enclosing commit's time or a fresh `tt chrono` read,
never an estimate.)*

Transcribed from `HUMANS.inbox.md` (item added ~2026-07-05) during the big repo refactor, as the
inbox's only surviving live item; the inbox file itself is retired now that `reqts/issues/` is
the real successor. Ratified as issue-001 by BR in-feed 2026-07-21.
