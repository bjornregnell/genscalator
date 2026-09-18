# Issue 059: `tt forge issues --limit N` returns FEWER than N, and its `=== N open issues` line reports a page as if it were a total

> status: open · labels: toolbox, forge, agent-trust, silent-wrong · summary: `forge.scala:433` sends
> `per_page=${o.limit}` to GitHub's `/issues` endpoint, which interleaves pull requests, and `:434`
> then drops the PR entries with `filterNot(_.obj.contains("pull_request"))`. So the cap applies to
> issues-and-PRs combined and the filter runs after it: `--limit 6` on a repo whose page holds 2 PRs
> prints 4 rows. The closing line then says `=== 4 open issues`, which reads as the repository's
> total rather than as what survived one truncated page. The sibling `pr-commits` already announces
> its cap at `:674-675`; `issues` does not.

## Description

Found 2026-09-18 while verifying that two freshly filed introprog issues existed. A `--limit 4` call
returned two rows and printed `=== 2 open issues` for a repository holding **39**. The number was
believed for several seconds, and the natural reading — that the second filing had failed — was
wrong.

### Root cause: the cap is applied to a combined feed, the filter runs afterwards

```scala
// tools/forge.scala:432-434
// GitHub's /issues endpoint interleaves PRs — drop entries carrying a pull_request key
Try(getJson(s"$GitHubApi/repos/$owner/$repo/issues?state=${o.state}&per_page=${o.limit}", ghHeaders).arr)
  .getOrElse(die("expected a JSON array of issues")).filterNot(_.obj.contains("pull_request"))
```

The comment is correct and the filter is correct. The defect is the **order**: `per_page` bounds the
combined issues-plus-PRs page, and PRs are removed from whatever that page happened to contain. The
number of rows you get back is therefore `N minus (however many PRs GitHub interleaved into the first
N)`, which the caller cannot predict and the tool does not disclose.

Two consequences, the second worse than the first:

1. **`--limit N` under-delivers, by an amount that depends on unrelated PR activity.** The same
   command returns a different number of rows on a busy week than a quiet one, with no change in the
   issues themselves.
2. **The summary line states the filtered count as a bare total.** `=== 4 open issues` carries no
   marker that a cap was hit, so it is indistinguishable from the same line printed by an uncapped
   run, which genuinely does mean "this repo has 4 open issues".

Not a general rot. `forge.scala:446` builds the `prs` URL against `/pulls`, which does not interleave
issues, so `tt forge prs --limit N` is correct. `releases` (`:384`) and `tags` (`:413`) hit
single-kind endpoints and are likewise fine. The Gitea branch at `:436` passes `type=issues`, so the
server filters before capping and that dialect is correct too. **The GitHub dialect of `issues` is
the only place the order is wrong.**

### Measured

`lunduniversity/introprog`, 2026-09-18, 39 open issues and 2 open PRs (`#979`, `#976`):

| command | rows printed | closing line | truth |
|---|---|---|---|
| `tt forge issues … --gh --state open` | 39 | `=== 39 open issues` | ✓ |
| `tt forge issues … --gh --state open --limit 6` | **4** | `=== 4 open issues` | ✗ asked for 6 |
| `tt forge prs … --gh --state open --limit 6` | 2 | `=== 2 open PRs` | ✓ (only 2 exist) |

The `--limit 6` page held `#982, #981, #979, #976, #968, #960`. The two PRs were dropped, leaving
four. That is exactly the predicted arithmetic, and it confirms the mechanism rather than merely
being consistent with it.

⚠ **One observation deliberately NOT claimed as part of this defect.** The original `--limit 4` call
returned `#981` and `#968` and omitted `#982`, which had been created about fifteen seconds earlier.
That is consistent with GitHub's list endpoint not yet reflecting a just-created issue, i.e. API
propagation lag, not this bug. The reproducible defect is the under-delivery and the mislabelled
count; "the newest item can be missing" is a separate and unverified suspicion, recorded here so that
whoever fixes this does not go looking for it in the wrong place.

**Why it wedges.** `tt forge issues --limit N` is the shape an agent reaches for when it wants a
cheap look at recent activity without pulling a long list into context — the ember discipline pushes
exactly that way. So the verb is reached for precisely when the caller has decided not to read
everything, which is when a short answer is least likely to be questioned. Same family as issue 046:
a confident, specific, quantified number that is wrong, produced by the tool that exists to be the
trustworthy alternative to raw shell.

## How to reproduce it

Any GitHub repo with both open issues and open PRs:

```
$ tt forge issues lunduniversity/introprog --gh --state open --limit 6
... 4 rows ...
=== 4 open issues                       # six were requested; two PRs were silently subtracted

$ tt forge issues lunduniversity/introprog --gh --state open
... 39 rows ...
=== 39 open issues                      # the actual total
```

The gap equals the number of PRs GitHub interleaved into the first 6 combined items.

## Acceptance sketch

* `--limit N` yields **N issues** when at least N exist. The straightforward fix is to over-fetch and
  then truncate after filtering: request a larger `per_page` (server cap 100), drop the PR entries,
  then `take(N)`. Paginate when N exceeds what one page can yield after filtering.
* **The closing line must distinguish a capped page from a total.** Borrow the idiom the codebase
  already has — `pr-commits` at `:674-675` prints `(--limit N page cap reached — more commits may
  exist; pagination not implemented)` when `rows.size >= o.limit`. `issues` should say something of
  the same shape rather than printing a bare count that looks authoritative. Whatever the fix to the
  count itself, **a truncated listing must never print a bare `=== N open issues`**, because that
  sentence is a claim about the repository.
* A test that would have caught this without network access: feed the parser a fixture array of six
  entries, two of which carry a `pull_request` key, with `--limit 6`, and assert six issues are
  returned rather than four. The fixture is the whole point — the bug is in the arithmetic between
  cap and filter, which is pure and testable without a forge.
* Check whether `--state all` widens the exposure, since closed PRs vastly outnumber closed issues in
  most repos and the same page would then be almost entirely filtered away.
* While in there: `--limit` is documented at `:171` as "max items for releases/tags (default 50)",
  which does not mention issues or PRs at all even though both accept it. Worth correcting.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-09-18 16:56

Found by doing the thing this repo keeps insisting on: verifying that an outward action actually
happened, rather than trusting that it had. Two issues had just been filed on introprog; the
listing said there were two open issues in total, which was wrong by 37 and would have been a
plausible-looking basis for concluding a filing had failed.

The mechanism was confirmed by reading `forge.scala:432-434` after the arithmetic was measured, not
inferred from the symptom. The `--limit 6` case is the clean reduction: six requested, a page
containing exactly two PRs, four printed.

I have separated out the one part I could not establish. The first anomalous call also appeared to
omit a just-created issue, which looks like list-endpoint propagation lag rather than this defect,
and saying so plainly seemed better than folding an unverified second symptom into a confirmed
first one.

Related: issue 060 proposes a `forge issue-create` verb, and these two arrived together — the
listing defect surfaced while verifying issues that had to be filed through raw `gh` because no
typed verb exists.

Agent disclosure: the discovery, the reduction and this issue text were produced by an AI agent
(Claude Opus 5) under human direction; the human reviewed and submitted.
