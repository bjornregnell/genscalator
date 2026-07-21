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

#### Agent's counter argument on the big refactor of outsourcing research etc

Direction is sound: it is the same audience-relative-home principle we use for notes (a thing's home is where its audience looks), applied at repo scale. But the costs are real and worth weighing first:

* **The decisive question up front: how does a tool-only user actually GET the tools — clone the repo, or install a plugin/package?** If they install a plugin, they never see `research/` `blog/` `pod-casts/`, so the "cruft/noise" problem is cosmetic and the split may not earn its cost. If they clone, it is real. Answer this before deciding.
* **Cross-repo links break the substrate.** Today everything is densely wiki-linked and relative-pathed (blog <-> research <-> foundations). Split into 3 repos and those become broken relative paths or absolute URLs, and the dangling-pointer discipline gets much harder to enforce across repo boundaries.
* **Atomic cross-cutting commits fragment.** One insight today often lands in blog + research + SECURITY-MODEL in a single commit with one narrative. After the split that becomes 2-3 commits in 2-3 repos. Given how cross-cutting the insights are, that is a real workflow tax.
* **Mirror surface multiplies** (each new repo needs the github/gitlab/coursegit mirrors too).
* **Middle path worth considering:** the declutter benefit may be reachable with a clean top-level split INSIDE one repo (a lean tools+skills root, with `research/` and `media/` as clearly-secondary dirs) without paying the cross-repo-link tax — UNLESS the answer to the first question is "they clone", in which case the hard split earns its cost.
* **Interacts with the planned `genscalator.ai/blog` -> `bjornregnell.se/genscalator/blog` URL move:** if `blog/` migrates to a `genscalator-media` repo the URL story changes, so plan the two together, not separately.

**TODO decide later; but we probably will not do outsourcing as it may be too disruptive to links out there, anyways we will not do it until genscalator release v1.0.0-M1.**

#### Migration plan (TODO soon on the migration step): the in-repo middle path — `git mv` blog + pod-casts into `media/`

> **✅ EXECUTED 2026-07-20** (genscalator `b1f29f2` + `96a92c5`; audited by meta-minion push-11). Two
> corrections to the text below, which is kept as written (annotate, never erase): the "exactly one
> functional code change" became TWO by execution time — `deployblog.sc` blogDir AND
> `DesignLang.scala`'s template-output path `../../blog` → `../blog` (that generator post-dates this
> plan) — and `docs/foundations.md` was NOT clean (3 blog-path sites, fixed). Everything else held,
> incl. `deployblog.sc`'s bundled relocation to `deploy/`.

The near-term, low-risk step, and DISTINCT from the outsourcing above (which is deferred and leaning-against). Investigated 2026-07-17; impact is small and bounded, `git mv` preserves history, and it is reversible.

**Target structure:** a new top-level `media/` (sibling to `research/`), holding `media/blog/` and `media/pod-casts/`. `research/` stays put. Keep media and research as SIBLINGS, not nested — they are different audiences (publishable vs findings), and siblings map cleanly onto a later genscalator-media / genscalator-research split if we ever do it.

**Exactly one functional code change:**
* `deployblog.sc`: `blogDir = "blog"` -> `"media/blog"`. That is the only functional edit. The remote/web path (`webroots/www/blog`) is unchanged; the public URL is a SEPARATE concern (the `genscalator.ai/blog` move).
* `tt ssg` needs NO code change — the blog dir is a positional argument, not hardcoded. Only its help-text examples mention `blog` (cosmetic).

**Cross-links (mechanical, modest):**
* blog posts link outward (`../research/`, `../skills/`, `../SECURITY-MODEL.md`); one level deeper means `../` -> `../../` (~a dozen+ across the 27 posts). Note: these look like SUBSTRATE links (research is not deployed to the public site), so they matter for in-repo navigation + ssg's link rewriting, not the live site.
* links INTO blog: only 2 (`research/028` -> `../blog/References.scala`, `research/037` -> `../blog/figures/`). Trivial.
* `docs/foundations.md`: no path-links into blog. Clean.

**Outside the moved dirs:**
* `References.scala` is `package blog` (with a test); it moves with the dir, the package name can stay `blog`, but confirm whatever compiles it uses the new path.
* external tooling/skills that hardcode ABSOLUTE paths into `blog/` need a path sweep.

**Not affected:** the mirrors (whole-repo, transparent to internal moves), pod-casts (only self-references + its README), and every other tool (they reference `research/` in comments only, and research is not moving).

**Order:** `git mv` both dirs -> fix `deployblog.sc` -> bump blog outbound `../` -> `../../` -> fix the 2 research links -> update ssg help text -> sweep external skill paths -> verify (`tt ssg` renders, `deployblog --dry-run` shows the right source, `ssg` + `References` tests pass).

**Who/when:** the `git mv` + link fixes are safe-solo mechanical work; the deploy re-verification is BR-present (deployblog touches the live site). So: greenlight -> agent does the mechanical part -> verify the deploy together.

**Sequencing (BR 2026-07-17): do this migration BEFORE the `genscalator.ai/blog` URL/deploy work.** That work configures where the blog renders FROM and deploys TO; if the blog is still at `blog/` when it lands, a later `media/` move would re-touch `deployblog` + the URL config a second time. Move the source layout once, then build the URL story on the final layout.

### Reason 2: insource issues as first class git citizen

> **✅ EXECUTED 2026-07-21** (with Reason 4's amended path `reqts/issues/open|closed/`). Two
> supersessions of the text below, kept as written (annotate, never erase): **(1) the ID scheme**
> — the `YYYYMMDDA` date-ID design below was superseded by `issue-NNN-name.md` (three-digit
> monotonic), decided by BR in the published Issue Zero (live on Codeberg/GitHub/GitLab) before
> this doc was reconciled; NNN is canonical, see `reqts/issues/README.md`. **(2) the
> participation open-decision** resolved: forge trackers stay ENABLED as the outsider inbox with
> transcription (the Issue Zero policy). First citizens: issue-000 (the policy itself) and
> issue-001 (References split, transcribed from the retired HUMANS.inbox.md).

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

### Reason 3: docs-surface refactor — lean README, extended HUMANS.md (JOINT 🤝 BR+CF5, pinned 2026-07-21)

> **⏳ PARTIALLY EXECUTED 2026-07-21**: `HUMANS.inbox.md` REMOVED (triage: 4 of 5 items obsolete,
> 1 transcribed to issue-001 — the successor `reqts/issues/` existed first, rule honored). The
> lean-README / extended-HUMANS.md split remains OPEN (BR's first-screen call + agent drafts).
> Movers that rode this session: `mirror.sc` → `deploy/mirror.sc` (self-check + usage + failure
> hint updated) and `blog-legacy-redirect.htaccess` → `deploy/`.

* **Remove `HUMANS.inbox.md`** — superseded by the in-repo issues of Reason 2: an inbox
  file duplicates what `issues/open/` will do with proper identity and workflow. Part of
  the removal: migrate any still-live inbox items into issues (the file has been untouched
  since 2026-07-05, so expect mostly stale content — triage, don't bulk-copy).
* **Make `README.md` leaner and meaner**: first-screen essentials only — what genscalator
  is, install/use quickstart, and pointers onward. Everything deeper moves to `HUMANS.md`,
  which becomes a kind of **extended README**: the deep explanations PLUS a repo-structure
  guide — what lives where and where to find stuff (the audience-relative-home principle
  made explicit for human readers).
* **Joint split of labour**: co-design the lean/deep split (what earns the first screen is
  BR's call), agent drafts the moves and the structure guide, BR reviews and ships.
  Sequencing is free relative to Reason 2 EXCEPT the inbox removal, which lands with or
  after the `issues/` dirs exist (never delete the inbox before its successor is real).

### Reason 4: future stuff goes into `reqts/` (JOINT 🤝 BR+CF5, pinned 2026-07-21 as SM183)

> **✅ EXECUTED 2026-07-21** (same day it was pinned — BR decided the refactor lands before
> v0.10.0 so Issue Zero could state 0.10.0 facts): `reqts/` created with `reqts/PRD.md` (git mv
> from root; reference sweep: `tt prd` default path in `tools/prd.scala`, gs-dwim `gs reqt`
> default, reqt-lang skill link, README/CONTRIBUTING/RELEASING/foundations links, and the PRD's
> own four outbound links rebased `../`), `reqts/issues/open|closed/` with real README workflow
> docs. Verified: whole-toolbox compile green + `tt prd summarize` resolves the new default
> ("49 gists from .../reqts/PRD.md"). Issue-zero subtask: DONE earlier same day (posted, pinned
> and labeled on Codeberg, labeled on GitHub + GitLab — the GitLab mirror itself revived under
> SM191).

* **New top-level `reqts/` collects the requirements-and-future substrate**, so the repo
  root stays lean and mean (same motive as Reason 3): `reqts/PRD.md` (moves from root)
  and `reqts/issues/` as the home of the insourced issues.
* **AMENDS Reason 2's target path**: the issue dirs become `reqts/issues/open/` and
  `reqts/issues/closed/` (identity/naming/status rules of Reason 2 unchanged).
* **Subtask "issue-zero"**: on ALL genscalator forge mirrors (Codeberg origin, GitHub,
  GitLab, coursegit — wherever a tracker is visible), create one pinned forge issue
  explaining that issues are INSOURCED into the repo at `reqts/issues/` — what that
  means, how to read them, and how an outsider reports (per Reason 2's open decision:
  the forge tracker as outsider inbox, transcribed into canonical in-repo issues).
  Posting to forges is outward: agent drafts issue-zero text, BR posts/pins.









