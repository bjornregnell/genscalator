# PLAN: Refactor genscalator repo structur

## Why

### Reason 1: move non-workflow relevant stuff

For users who just want to use the genscalator typed tools, workflow and skills all of this is cruft/noise:

* research/
* blog/
* pod-casts/

The plan is to move these things to a new repos tentatively called 

* `genscalator-research` : here all topics, wr-data and experiments and case-studies can evolve in th eopen

* `genscalator-media` : here we can have the stubbs and rafts evolving stuff into punlishable units, e.g. the `tt ssg` static site generator tool will bring blogs to genscalator.ai/blog  etc. Also the pods ideas go here. 

### Reason 2: insource issues as first class git citizen

For sovereignty reasons we mirror genscalator from codeberg to e.g. github and gitlab. But any current [forge](https://en.wikipedia.org/wiki/Forge_(software)) has its own issue-workflow story and api.

One way to dis-entangle the issue workflow from the forge is to insource and git track issues *in* the repo.

The current ide is to:
* greate gescalator/issues/open and genscalator/issues/closed dirs
* have the following issue identity and naming policy:
  * each issue have a file name of this strucure ID-NAME.md where
    - ID is YYYYMMDDA where A is some unique non-empty sequens of lower-case chars a-z, starting with `a` and monotonically incremented in this patter b, c, ..., aa, ab, etc.
    - NAME is snake-case words with letters in a-z spearated by -.
    - Example: `20260717a-this-is-a-name-of-and-issue.md`
  * why: file names and ids should be uniqie. The names and ids can be unique after consolidation: two issuers create issues on the same day and a clash is identifed then the last issuer increments A and updates the issue file name accordingly.
  * workflow: when open issues are closed they are moved with git mv from the `open` dir to the `closed` dir.
* issue syntax:
  - the title in th #-level heading is a readable version of the file name. 
  - after the title there is a preamble in block quotes > that declares the status of the issue, labels it has, and summarizes it very briefly (TODO: create status model and a type-of-issue labeling scheme and preamble syntax)
    - status source-of-truth rule (co-design 2026-07-17): the `open/` vs `closed/` DIR is the single source of truth for open-vs-closed. The preamble status field carries only FINER states (triage, in-progress, blocked, wontfix, ...) so the dir and the preamble can never disagree on the basic open/closed fact.
  - issues have these ##-level headings:
    * "## Description"  mandatory
    * "## How to reproduce it"  optional
    * "## Discussion" an append-log of discussion items
      - "### Comment by userhandle at date-time-stamp"

### Open decisions (TODO — from co-design 2026-07-17)

* **Participation vs sovereignty (TODO decide later).** Insourced issues raise the bar for OUTSIDE bug-filers: a drive-by contributor won't clone, hand-author an ID-named markdown file, and open a PR just to report a bug. One option that keeps both: **keep the forge issue tracker ENABLED as an "outsider inbox"** for drive-by reports, and transcribe accepted ones into in-repo canonical issues (the git-tracked files stay the source of truth). Decide later whether to do this, or to accept the higher bar as a quality filter.
* **TODO investigate git-bug** (issues stored in git objects, with github/gitlab bridges) for lessons on the hard parts (distributed IDs, forge bridging) — but **insourced markdown-in-dirs is probably what we want instead of it**: more transparent and greppable, and it fits the genscalator ethos, whereas git-bug's storage is opaque.









