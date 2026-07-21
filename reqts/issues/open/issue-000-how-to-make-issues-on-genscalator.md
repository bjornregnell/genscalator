<!-- issue-000 | status: open | created 2026-07-21 by bjornregnell | the in-repo form of the pinned
     forge Issue Zero. This whole file is forge-paste-ready: these comment lines do not render.
     Forge copies: codeberg.org/bjornregnell/genscalator/issues/1 (canonical), mirrors per below. -->

TLDR: prefer `reqts/issues` but ok to file here if you are a one-timer

# Issue Zero: how to make issues on genscalator

**This issue tracker is an inbox, not the archive.**

genscalator keeps its **issues in the repository itself**, under `reqts/issues/`, so that:

* requirements, decisions, and their history travel with the code,
* issues are replicated on every mirror, offline, forge-independent,
* genscalator keeps its digital sovereignty: no single forge owns this project's memory.

## How to report something

**Option 1** if you are a *one-time contributor* or a *user* who does not want to fork or clone:

* Open an issue here as usual: bug, idea, question, anything.
* A maintainer will transcribe it into the in-repo issue set (crediting you) and reply here with a pointer.
* Your issue here is closed when the corresponding issue under `reqts/issues/` is closed.

**Option 2** if you are a *contributor*, or any user happy to fork and clone:

* Skip reporting here.
* Fork and clone this repo.
* Add your issue as a new file `reqts/issues/open/issue-NNN-your-chosen-issue-name.md`
  (three-digit `NNN`, next free number) and open a Pull Request (also called Merge Request) with it.
* Read more about issue management in `CONTRIBUTING.md`.

This pinned issue (Issue Zero) stays open on purpose, so the tracker is never mistaken for
empty-by-neglect.

## Where to find all the things

* The canonical repository is https://codeberg.org/bjornregnell/genscalator
  * You can also file issues on the mirrors; they are transcribed the same way:
    * GitHub: https://github.com/bjornregnell/genscalator
    * GitLab: https://gitlab.com/bjornregnell/genscalator
* `reqts/` holds the requirements side of the project. First go-tos:
  * the Product Requirements Document: `reqts/PRD.md`
  * the current open issue set: `reqts/issues/open/`
* `HUMANS.md` for how the repo is organized.
* `README.md` for how to install genscalator and get started.
