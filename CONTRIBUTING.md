# Contributing to genscalator

> **genscalator v0.10.2** — contributing-policy version. Stale policy fails silently and outward:
> an old checkout can point you at the wrong forge or a stale address, and nothing else will warn
> you. Check what you have with `tt --version` and update (`tt update`) before filing an issue or
> opening a PR.

genscalator is built **with and for both humans and agents** — so this guide speaks to both. The repo
grows the way its tools were born: someone (human *or* agent) does real work, builds a small typed tool to
replace a brittle bash/grep/awk reflex, and — if it turns out to be generally useful — contributes it back.

> New here? Skim [`docs/foundations.md`](docs/foundations.md) (goals, stakeholders, glossary) and
> [`tools/README.md`](tools/README.md) (the toolbox conventions) first.

## Issues — in the repo, not (only) on a forge

genscalator tracks issues **inside the repository**, under [`reqts/issues/`](reqts/issues/README.md),
so they travel with the code to every mirror (forge-independent by design). The short form:

- **One-time reporter?** File on any forge tracker as usual; a maintainer transcribes it in-repo with credit.
  The how-and-why is "Issue Zero":
  [`issue-000-how-to-make-issues-on-genscalator.md`](reqts/issues/open/issue-000-how-to-make-issues-on-genscalator.md).
- **Contributor?** Add a file `reqts/issues/open/issue-NNN-your-chosen-issue-name.md` (next free
  three-digit number) and open a PR with it.
- The identity scheme, open/closed rules, and file syntax live in ONE place:
  [`reqts/issues/README.md`](reqts/issues/README.md).

Requirements-level contributions (features, goals) belong in the reqT-lang
[`reqts/PRD.md`](reqts/PRD.md) — see the `reqt-lang` skill for the language, and always verify
edits with `tt parsereqt parse` + `lint`.

## What belongs here
A tool is a good fit when it is:
- **Project-agnostic** — no hardcoded paths/hostnames or single-project assumptions; useful across repos.
- **A reflex-replacer** — removes a common dynamic-shell habit (grep/awk/sed/find/wc/python one-offs) with
  a typed, compiler-checked tool.
- **Convention-following** — the latest-LTS Scala the toolbox pins, RCs included (see `tools/README.md`), reuses `lib.scala` helpers, pure where possible
  (read → compute → print), clean `===` output, scaffolded from `tools/template.scala.txt` via `newtool`.
- **Self-contained & safe** — pure tools use only the JDK; effectful drivers are separate and declare deps.

If it's specific to one project, keep it in that project's own `scratch/` — don't upstream it.

## For agents 🤖
You are a first-class contributor. When you build or scaffold a tool while helping a user:

1. **Self-check (every new tool):** ask *"generally useful, or project-specific?"* — see the
   self-monitoring note in [`AGENTS.md`](AGENTS.md).
2. **If generally useful, propose it to your human** — do **not** push to this repo yourself. e.g.:
   *"This `<tool>` looks project-agnostic and reusable — want me to prepare a contribution to genscalator
   (an issue + PR at https://github.com/bjornregnell/genscalator)?"*
3. **On the human's go-ahead, prepare** (don't publish unprompted): **generalize beyond your specific
   case** — follow the `contribute-tool` skill's recipe (step back to the general class, sane defaults +
   customization, keep the original case working, verify with adversarial fixtures, strip project
   specifics). Confirm it follows the conventions above, then draft an **issue** (the gap it fills + the
   safe-by-design angle) and a **PR** adding the tool file + a `tools/README.md` entry.
4. **The human reviews and submits.** Opening/pushing to the public repo is human-authorized — matching
   genscalator's ethos: *the agent proposes, the human approves.*

## For humans 🧑
- When your agent flags a tool worth sharing, give it a quick review (does it generalize? is it safe?),
  then submit — or ask the agent to prepare the issue + PR for you to push.
- You're equally welcome to contribute tools you wrote yourself.

## Using AI agents

Using AI agents in your contribution is fine, if applied responsibly. Two rules:

- **No assistant credit in commits.** Do not add `Co-Authored-By: <assistant>` trailers or "Generated with ..."
  badges. Each commit is attributed to the human contributor who makes the change and stands behind it.
- **Disclose in the PR.** In the pull request thread, add a short, honest note on what you used an agent for and
  what you did yourself. Or say plainly that you used none.

## PR threads — the forge is transport, not storage

Issues travel with the repo to every mirror; a PR comment thread does not — it lives only on the
forge where the PR was opened. So anything load-bearing in a PR conversation gets **landed in-repo
before or at merge**:

1. **Per-issue review points** → that issue's `## Discussion` section, as an append-only,
   attributed, dated comment (the same `### Comment by handle/Agent at YYYY-MM-DD HH:MM` convention
   issues already use). No separate PR-thread heading: a review comment about issue-NNN *is*
   discussion of issue-NNN.
2. **Cross-issue or process points** → the accompanying report under `research/reports/`, as a
   dated appended section. Only a decision that fits neither an issue nor a report earns a record
   file of its own.
3. **The merge commit message names the PR** (number + title), so mirrored history carries the
   cross-reference; the PR URL is a courtesy pointer, not a dependency.

The test: *would it matter in six months, or on a mirror where the PR does not exist?* If yes, land
it in a tracked file; if no (rebase requests, logistics), it may die with the forge thread.
Contributors land review outcomes inside their open PR when asked; maintainers land them as a
follow-up commit after merge.

## Submitting
1. **Fork** https://github.com/bjornregnell/genscalator
2. **Branch** from `main`, add the tool under `tools/` (+ a `tools/README.md` cheat-sheet entry), commit.
3. **Open an issue** by adding a file under [`reqts/issues/open/`](reqts/issues/open/) describing the tool: the
   bash/grep habit it replaces, why it's general, how it fits.
4. **Open a PR** from your fork's branch, linking the issue.

## Checklist

For a **tool PR**:
- [ ] Project-agnostic (no `/home/...`, hostnames, or single-project assumptions)
- [ ] Replaces a real dynamic-shell reflex with a typed tool
- [ ] Follows `tools/README.md` conventions (latest Scala, `lib` reuse, pure if possible)
- [ ] Added a `tools/README.md` entry
- [ ] Issue + PR opened (issue explains *why*; PR has the code)
- [ ] Sent the copyright transfer email to genscalator @ bjornregnell.se (see [Copyright](#copyright) below)

For an **issue-only or docs-only PR** (sanctioned above — issues and reports are contributions too):
- [ ] Issue file follows [`reqts/issues/README.md`](reqts/issues/README.md): next free `NNN`
      (count numbers claimed by open PRs too), preamble, mandatory sections
- [ ] Names the version measured against (`tt --version`, or `VERSION.txt` + sha where it cannot run)
- [ ] Claims that can be executed were executed — run, not reasoned; what was not verified is
      labelled as such, and assumptions are stated as assumptions
- [ ] Additions to an existing issue's `## Discussion` are append-only comments in the documented form
- [ ] No assistant-credit trailers or badges in commits or the PR body; agent involvement disclosed
      in prose instead (see [Using AI agents](#using-ai-agents))
- [ ] Sent the copyright transfer email (first contribution only)

By contributing, you agree your contribution is licensed under the repo's [Apache-2.0](LICENSE).

## Tests

The test suite is **co-located with the tools** it covers, under [`tools/test/`](tools/test/): `cli.test.scala`
(CLI-contract tests — each tool run as a subprocess, exit code + stdout asserted) and `lib.test.scala` (unit tests
for the shared `tools/lib.scala` helpers). Run the whole toolbox plus its tests from the repo root:

```
tt scala test tools
```

Run it **from the repo root**: several suites locate the toolbox by walking up from the current directory, so
from anywhere else they resolve a *different* `tools/` and fail in ways that look like real defects. If you must
run from elsewhere, say which toolbox you mean: `tt scala test <abs>/tools --prop tt.tools=<abs>/tools`.

The `*.test.scala` files compile in scala-cli's **test scope**, which *extends* the toolbox's main scope — so the
tests see the tool sources without any `//> using file` wiring, and a plain `scala-cli compile tools` still builds
**only the tools** (the test files are excluded from the main compile). More: [`tools/README.md`](tools/README.md#tests).

## Roadmap

Where to look, by question:

- **When does it land?** → [`reqts/ROADMAP.md`](reqts/ROADMAP.md) — the single source of truth for the
  version-by-version plan. Kept there, not duplicated here, so it cannot drift.
- **What shipped already?** → [`CHANGELOG.md`](CHANGELOG.md), per release.
- **Toolbox-specific plans** (new/extended `tt` tools) → [`tools/README.md`](tools/README.md#roadmap).
- **Goals and requirements** → the [Product Requirements Document](reqts/PRD.md).

## Copyright

Copyright of all code in this repo is owned by the maintainers of the genscalator repository. Any code contributor to this repo **implicitly transfers copyright** to genscalator maintainers by contributing. Before you contribute you should send a copyright transfer note via email to genscalator at bjornregnell.se with the subject "Copyright transfer" and body containing "I hereby transfer copyright of my contributions to genscalator to the maintainers of genscalator" and your name and contact details.

## Maintainers

The genscalator repository is currently maintained by:
* [Professor Björn Regnell](https://bjornregnell.se)
* You? If you are interested to become a maintainer, send email to genscalator at bjornregnell.se
