# Contributing to genscalator

genscalator is built **with and for both humans and agents** — so this guide speaks to both. The repo
grows the way its tools were born: someone (human *or* agent) does real work, builds a small typed tool to
replace a brittle bash/grep/awk reflex, and — if it turns out to be generally useful — contributes it back.

> New here? Skim [`docs/foundations.md`](docs/foundations.md) (goals, stakeholders, glossary) and
> [`tools/README.md`](tools/README.md) (the toolbox conventions) first.

## What belongs here
A tool is a good fit when it is:
- **Project-agnostic** — no hardcoded paths/hostnames or single-project assumptions; useful across repos.
- **A reflex-replacer** — removes a common dynamic-shell habit (grep/awk/sed/find/wc/python one-offs) with
  a typed, compiler-checked tool.
- **Convention-following** — latest stable Scala, reuses `lib.scala` helpers, pure where possible
  (read → compute → print), clean `===` output, scaffolded from `tools/template.scala.txt` via `newtool`.
- **Self-contained & safe** — pure tools use only the JDK; effectful drivers are separate and declare deps.

If it's specific to one project, keep it in that project's own `scratch/` — don't upstream it.

## For agents 🤖
You are a first-class contributor. When you build or scaffold a tool while helping a user:

1. **Self-check (every new tool):** ask *"generally useful, or project-specific?"* — see the
   self-monitoring note in [`AGENTS.md`](AGENTS.md).
2. **If generally useful, propose it to your human** — do **not** push to this repo yourself. e.g.:
   *"This `<tool>` looks project-agnostic and reusable — want me to prepare a contribution to genscalator
   (an issue + PR at https://codeberg.org/bjornregnell/genscalator)?"*
3. **On the human's go-ahead, prepare** (don't publish unprompted): generalize the tool (strip project
   specifics), confirm it follows the conventions above, draft an **issue** (the gap it fills + the
   safe-by-design angle) and a **PR** adding the tool file + a `tools/README.md` entry.
4. **The human reviews and submits.** Opening/pushing to the public repo is human-authorized — matching
   genscalator's ethos: *the agent proposes, the human approves.*

## For humans 🧑
- When your agent flags a tool worth sharing, give it a quick review (does it generalize? is it safe?),
  then submit — or ask the agent to prepare the issue + PR for you to push.
- You're equally welcome to contribute tools you wrote yourself.

## Submitting (Codeberg / Forgejo)
1. **Fork** https://codeberg.org/bjornregnell/genscalator (Codeberg runs Forgejo; the flow is ~ GitHub's).
2. **Branch** from `main`, add the tool under `tools/` (+ a `tools/README.md` cheat-sheet entry), commit.
3. **Open an issue** describing the tool: the bash/grep habit it replaces, why it's general, how it fits.
4. **Open a PR** from your fork's branch, linking the issue. (CLI option: the `tea` Forgejo CLI.)

## Checklist
- [ ] Project-agnostic (no `/home/...`, hostnames, or single-project assumptions)
- [ ] Replaces a real dynamic-shell reflex with a typed tool
- [ ] Follows `tools/README.md` conventions (latest Scala, `lib` reuse, pure if possible)
- [ ] Added a `tools/README.md` entry
- [ ] Issue + PR opened (issue explains *why*; PR has the code)

By contributing, you agree your contribution is licensed under the repo's [Apache-2.0](LICENSE).
