# Issue 049: a legitimate `updated` transition silently drops a post from the deploy set, because one field is both the reader-facing revision history and the deploy-state machine

> status: open 2026-08-25 · labels: ssg, blog, deploy, silent-wrong · measured against: `main` after
> `f624c24` · summary: `ssg` reads the CURRENT status as the LAST verb in the `**Status: …**` history,
> and `deployblog --push` renders the set whose current status is `deployed`. `updated` is a
> first-class verb — `readerByline` renders it for readers — so appending it, which is exactly what a
> revision is, moves the post OUT of the deploy set. The set render then PRUNES the post's page and
> uploads an index that no longer links it. Nothing warns. Hit twice on 2026-08-25 on blog 000, the
> second time after the author edited the status by hand.

## Description

Two facts, each correct alone, that compose into a trap.

**1. The current status is the last verb.** `ssg.scala:318-326`: the status preamble is a
`;`-separated history of `<verb> <date>` transitions, and `currentStatus` returns the LAST verb,
lowercased. **2. The deploy set is selected by current status.** `deployblog.sc:149` renders
`--status deployed` for `--push`; `ssg.scala:482` keeps posts whose `currentStatus` is in the set.

Now the third fact that makes them collide: **`updated` is a supported, reader-facing verb.**
`readerByline` (`ssg.scala:336-349`) explicitly renders `Published <d> · updated <d>` from the latest
`updated` transition, and documents that `initialized`/`drafted`/`deployed` are dropped as internal
bookkeeping. So an author recording a revision — the one thing the byline exists to show — appends
`; updated <date>` and thereby sets the post's current status to `updated`.

`updated` is not in `{deployed}`. The post leaves the deploy set.

### Why it is worse than "the post is skipped"

A set render does not merely skip it. `ssg.scala:526-534` prunes the out-dir so it holds EXACTLY the
selected set, so the post's `.html` is **deleted from `tmp/site`** and `index.html` is regenerated
**without a link to it**. The upload is additive (`deployblog.sc` mirrors without `--delete` by
default), so the stale page survives on the server and nothing 404s — the post simply **disappears
from the blog index** while remaining reachable only by a URL nobody is given any more. There is no
error, no warning, and the deploy reports success.

The only visible tell is one line in the render output:

```
ssg: pruned stale /…/tmp/site/000-why-genscalator.html
```

which reads as routine housekeeping rather than "your published post just left the site".

### Observed, twice, on 2026-08-25

* The author appended `; updated 2026-08-25` to blog 000's status by hand, intending exactly the
  reader byline that verb produces. The next `deployblog --push --dry-run` pruned `000-why-genscalator.html`
  and listed an upload set of five files with the post absent. Caught only because the dry-run output was
  read line by line; a `--push` without the dry-run would have removed blog zero from the index.
* The workaround — render `--status deployed,updated` explicitly, upload, then stamp
  `updated:deployed` afterwards — restored it. That ordering is right for a different reason
  (`deployblog.sc:148` calls push-then-stamp "keeps the source honest"), but nothing in the tool
  suggests it, and it has to be reconstructed from the source each time.

## How to reproduce it

```bash
# a post whose status ends in `deployed`
tt ssg --status-update deployed:updated --date 2026-08-25 media/blog/000-why-genscalator.md
scala-cli run deploy/deployblog.sc -- --push --dry-run
#   => ssg: pruned stale .../tmp/site/000-why-genscalator.html
#   => the "would send" list no longer contains the post, and index.html has lost its entry
```

Expected: either the post stays in the deploy set, or the tool says plainly that a currently-deployed
post is about to leave the site. Observed: a success report and a pruned page.

## Acceptance sketch

* **Decide what `updated` means, and say it in one place.** Two readings are defensible and the code
  currently implements neither deliberately: (a) `updated` is a *revision marker*, orthogonal to
  deployment, in which case the deploy set should be selected by the last DEPLOYMENT verb rather than
  the last verb of any kind; or (b) `updated` is a genuine state meaning *changed since deploy, not yet
  re-deployed*, in which case it is the natural input to `--release` and the pair `updated → deployed`
  should be as first-class as `published → deployed`. (b) matches how it was actually used here.
* **Make `--push` cover the deployed-and-since-edited set**, not `{deployed}` alone. Under reading (b)
  that is `--status deployed,updated`, which is what had to be typed by hand. Under (a) it falls out of
  the selector change. Either way the common case — "re-deploy what is live, including my edits" —
  should not require reconstructing the status machine from the source.
* **Never prune a page out of the out-dir silently when its source still exists.** The prune line is
  correct for a genuinely retired post and dangerous for this case, and the two are indistinguishable
  in the output today. At minimum name it: `pruned X (its post is no longer in the selected set —
  current status: updated)`. That one clause would have made this self-diagnosing.
* **Anchor a test on the trap, not just on the mechanism.** `SsgSelectionSuite` already covers pruning;
  what is missing is the composed case — a post whose status ends in `updated` under a `deployed`
  selector — asserting whatever the decision above makes correct. That test is what would have caught
  this, and it is cheap because the whole thing is pure status-string logic.
* Out of scope: the byline rendering itself (`readerByline` is right — readers should see the revision
  date), and the push-then-stamp ordering (also right; see `deployblog.sc:148`).

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-25 21:22

Filed from two live near-misses in one session, both on blog 000, the second after the author edited
the status preamble by hand to record a revision. The failure is a composition rather than a bug in
either part: `currentStatus`-is-the-last-verb is correct, selection-by-current-status is correct, and
`updated`-is-reader-facing is correct. What is missing is a decision about whether the field is a
history or a state machine, because it is currently being asked to be both.

Worth recording that the tool's own defaults are what made it dangerous rather than merely wrong: the
prune keeps the out-dir exact (right), and the upload is additive (right), and together they produce
the specific outcome that the page survives on the server while vanishing from the index — the state
hardest to notice from either end.

Not verified: whether any post other than blog 000 has ever carried `updated` as its final verb, and
therefore whether this has bitten silently before. Blog 002's history ends
`updated 2026-07-13; deployed 2026-07-20`, so it was re-stamped and is unaffected; the other posts were
not audited.

Agent disclosure: an AI agent (Claude Opus 5) hit the trap, diagnosed it and drafted this issue under
human direction; the human reviewed and submitted.
