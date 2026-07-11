# genscalator

**Power tools for agents: smarter, safer, faster.**

This repository is mirrored across several hosts for digital sovereignty; see [Mirrors and digital sovereignty](#mirrors-and-digital-sovereignty) at the bottom.

## What

Genscalator is a toolbox + workflow for coding agents that replaces the brittle
bash/grep/awk/python reflex with **typed, compiler-checked, reusable Scala tools**. Pick a tool, give it
args. No re-deriving logic each time, no dynamic-shell surprises. The compiler catches mistakes before
they run, and a small launcher (`tt`) makes every tool a single, statically-analyzable command that a
narrow allowlist can trust.

## Why

Out-of-the-box agent workflows lean on approving dense bash compounds and archaic Unix tools 
stitched together in a difficult to review blob. 
Much of the guardrail machinery exists precisely to contain what can go wrong there, and the 
cost is **confirmation fatigue** and bad UX from reviewing cryptic, dynamic, unsafe code.

Genscalator shifts to **safe, compiled code with static guarantees**. Every time the agent would reach for a
one-off bash/grep/awk helper, it instead creates (or reuses) a persistent, self-contained Scala tool.
That earns static guarantees, reduces the agent getting stuck debugging brittle helpers, and shrinks the
number of dangerous operations that need human approval at all.

See [`docs/foundations.md`](docs/foundations.md) for the full goals, stakeholders (human / agent / Black
Hat Hacker threat model), and glossary.

### The bigger picture

Genscalator is also a research project into agentic software engineering workflow productivity. The invention of typed tools is supported by a dog-fooding action research approach where genscalator is used in meta-level experiments and case studies on human-agent workflows. Emerging research questions and findings are reported in [`blog/`](https://bjornregnell.se/blog) and research studies are brainstormed, designed and executed in [`research/`](research/), as we go, supported by the genscalator typed tools and joint human-agent workflow under development.     

## Install

**Prerequisites:** [scala-cli](https://scala-cli.virtuslab.org/) and a JDK (to run the tools), plus `git`
(to clone this repo).

**Platforms:** Linux, macOS, and WSL (Windows Subsystem for Linux) — anywhere `bash` + `scala-cli` run. On native Windows, use WSL or Git Bash.

> On **Claude Code** you can skip the manual steps below and install genscalator as a plugin — `tt` lands
> on your PATH automatically. See [Use as a Claude Code plugin](#use-as-a-claude-code-plugin). The steps
> here are the manual path that works with any agent.

**1. Clone the repo:**
```
git clone https://codeberg.org/bjornregnell/genscalator.git
cd genscalator
```

**2. Put the `tt` launcher on your PATH.** Run this *from the repo root* (so `$PWD` is your clone),
symlinking the launcher into a directory that's on your PATH:
```
ln -s "$PWD/tools/tt" ~/.local/bin/tt    # ensure ~/.local/bin is on your PATH
```
"typed tools" — one literal, allowlist-friendly command from any repo. First run of a tool compiles
(~a couple of seconds); reruns are cached. Verify with `tt files src .scala --count`.

### Companions for Scala code (recommended)

genscalator integrates — but does **not** bundle — two upstream tools for Scala *code* intelligence (the
`tt` tools cover text and logs). Install whichever you need; [`docs/tool-selection.md`](docs/tool-selection.md)
says which tool answers which question.

- **[scalex](https://github.com/nguyenyou/scalex)** — fast, symbol-aware Scala navigation. On Claude Code:
  ```
  /plugin marketplace add nguyenyou/scalex
  /plugin install scalex@scalex-marketplace
  ```
  For a standalone binary / other agents, see the upstream repo.
- **[Metals MCP](https://scalameta.org/metals/docs/features/mcp/)** — compiler-grade truth (inferred types,
  real diagnostics, run tests, refactor); heavier. Enable it through your editor's Metals + MCP-client
  config per the linked setup page.

## Usage

```
tt <tool> <args...>                           # from any repo (recommended)
scala-cli run tools/<tool>.scala -- <args>    # explicit, from this repo's root
```

Examples:

```
tt text count build.log '^! '          # count matches (grep -c)
tt text grepr src .scala 'TODO'        # recursive search → file:line:match
tt files src .scala --count            # count matching files (find|wc)
```

Full cheat-sheet: [`tools/README.md`](tools/README.md).

**Companions for Scala code:** `tt`/grep are for text and logs; for Scala *structure* (where defined, who
calls, what extends, resolve imports) genscalator recommends [scalex](https://github.com/nguyenyou/scalex)
— a symbol-aware "grep for the AST", separately installed. For **compiler-grade truth or mutation**
(inferred types, real diagnostics, run tests, refactor) it escalates to
[Metals MCP](https://scalameta.org/metals/docs/features/mcp/). Which tool for which question — and the
escalation ladder: [`docs/tool-selection.md`](docs/tool-selection.md).

## Tests

The test suite is **co-located with the tools** it covers, under [`tools/test/`](tools/test/): `cli.test.scala`
(CLI-contract tests — each tool run as a subprocess, exit code + stdout asserted) and `lib.test.scala` (unit tests
for the shared `tools/lib.scala` helpers). Run the whole toolbox plus its tests from the repo root:

```
scala-cli test tools
```

The `*.test.scala` files compile in scala-cli's **test scope**, which *extends* the toolbox's main scope — so the
tests see the tool sources without any `//> using file` wiring, and a plain `scala-cli compile tools` still builds
**only the tools** (the test files are excluded from the main compile). More: [`tools/README.md`](tools/README.md#tests).

## Tool dependencies

Most `tt` tools need only **scala-cli + a JDK** — scala-cli fetches the Scala compiler and the small library set on
first run, then caches them (no manual library install). A few tools additionally shell out to an **external program**
for one job; install that only if you use the tool.

| Requirement | What / version | Install | Docs |
|------|----------------|---------|------|
| **`tt` runner + all tools** | **Scala 3.8.4** compiler on a **JDK 21+**, run via **scala-cli**. The per-tool library deps — `os-lib` 0.11.8, `ujson` 4.4.3, `requests` 0.9.3 (plus `munit` 1.3.3 for the test suite) — are declared inline and **fetched automatically by scala-cli** (cached after first run). | Install **scala-cli + a JDK** — see [Install](#install); scala-cli fetches the compiler + libraries on first use. | [scala-cli](https://scala-cli.virtuslab.org/) · [Scala 3.8.4](https://www.scala-lang.org/download/) |
| **`tt gvdot`** *(optional)* | **graphviz** (`dot`) — lays out sequence diagrams to pdf/png/svg | `sudo apt install graphviz` (Debian/Ubuntu); `brew install graphviz` (macOS) | [graphviz.org](https://graphviz.org/) · `dot -h` · `man dot` |

Tools degrade gracefully when their dependency is missing: `tt gvdot` still prints DOT source without `dot`, and
errors with the install hint only on the render path. (The sibling renderers `tt svg` and `tt ascii` need **no**
external dependency — pure JDK.)

## Roadmap

What's shipped so far, per release: [`CHANGELOG.md`](CHANGELOG.md).
For the toolbox-specific roadmap (new/extended `tt` tools), see [`tools/README.md`](tools/README.md#roadmap).
For general goals and requirements see the [Product Requirements Document](PRD.md)

**Planned, not yet built - roughly cheapest-to-build first:**
- **Update awareness** — an *inform-only* update skill + a read-only version-check (compare the installed
  version against upstream `marketplace.json`) so staleness is visible. It hands the human the update
  commands but **never self-updates the operating rules** — adopting new rules stays a human-reviewed step
  (see [`docs/updating.md`](docs/updating.md)).
- **One-command install** of genscalator + companions (scalex + Metals MCP) for newcomers who want
  everything at once — as a **reviewable, version-pinned installer script you read before running**, not
  a blind `curl … | bash` (that opaque-pipe pattern is exactly the confirmation-fatigue / RCE risk
  genscalator argues against).
- Native compilation.
- Tool safety flags: `--safe-mode`, `--sandboxed`, `--audit`.
- Capture-Checking **Safe-mode** PoC → pure tools safe by default.
- **Cross-tool packaging:** an MCP server so the tools are first-class in Codex/opencode too. (The
  Claude Code plugin already ships — see *Use as a Claude Code plugin* below.)

## Use as a Claude Code plugin

This repo doubles as its own Claude Code plugin marketplace, so `tt` lands on your PATH automatically and
skills teach the habit (tool selection, Scala style, tool contribution) — no manual symlink:
```
/plugin marketplace add https://codeberg.org/bjornregnell/genscalator.git
/plugin install genscalator@bjornregnell
```
Use the **full Codeberg URL with `.git`** — the short `owner/repo` form resolves to GitHub, and the
`.git` suffix makes Claude Code clone the repo (where `marketplace.json` lives).
Details, the recommended allowlist, and caveats: [`docs/claude-plugin.md`](docs/claude-plugin.md).
(You still need `scala-cli` + a JDK installed.)

### What you get

Installing the plugin puts the `tt` toolbox on your PATH (see [Usage](#usage)) and adds a set of **skills** — focused
playbooks the agent invokes by name, or by matching what you ask for:

| Skill | What it does |
|-------|--------------|
| `tt-toolbox` | how to use and choose the `tt` tools — the toolbox habit |
| `contribute-tool` | scaffold, test, and contribute a new `tt` tool back upstream |
| `scala-style` | the common Scala style (braces vs braceless — the Odersky/Regnell/Kerr recommendation) |
| `scala-code-review` | review Scala code for correctness, style, and safety |
| `reqt-lang` | write requirements in reqT-lang (the markdown subset this repo's [`PRD.md`](PRD.md) is written in) |
| `crud-web-app-seed` | seed a complete, runnable Scala web app (JDK server + Scala.js/Laminar client) into a directory you choose — see *Try it* above |
| `research-methods` | SE research-methods helper (case-study + experiment checklists, a validity cheat-sheet) |
| `in-session-experiment` | design and run a small, reproducible in-session experiment |
| `blog-assistant` | help draft and shape a blog post |

The plugin also ships the operating contract [`AGENTS.md`](AGENTS.md) — the shared human↔agent **conventions** (tool
selection, comms shorthand, the workflow "dances", the safe-by-design allowlist habit) that the agent reads as its
modus operandi. Full glossary and cues live in [`docs/foundations.md`](docs/foundations.md).

### Recommended Claude Code settings (initial cut)

To get the low-friction, safe-by-design payoff, add a **narrow** allowlist to `.claude/settings.local.json` that
trusts the *typed tools* while keeping raw shell and destructive ops gated. Essentials:
```json
{
  "permissions": {
    "allow": [
      "Bash(tt *)",
      "Bash(scala-cli *)",
      "Bash(scalex *)",
      "Bash(git -C /ABSOLUTE/PATH/TO/YOUR/REPO *)",
      "Bash(rm -f /ABSOLUTE/PATH/TO/YOUR/REPO/tmp/*)"
    ],
    "deny": [
      "Bash(rm -rf *)",
      "Bash(git push --force *)",
      "Bash(git reset --hard *)"
    ]
  }
}
```
Principles: **allow the typed tools, not raw shell** (so `tt text grepr` runs silently but `grep -rnE` still
prompts); **scope `git`/`rm` by absolute path**, never broad `Bash(git *)`/`Bash(rm *)`; and **keep destructive +
catastrophic ops gated** even when you want low friction. Replace the placeholder paths with your repo's absolute
path(s). This is a starting point, not the full story — the why, safe-growth strategy, and open questions (tiers,
a `tt init-settings` scaffolder, deny-list defaults) are in
[`research/018-recommended-plugin-settings.md`](research/018-recommended-plugin-settings.md); the fuller allowlist +
caveats are in [`docs/claude-plugin.md`](docs/claude-plugin.md). *(Initial cut — expect refinement as we prune
the essentials and possibly ship a merge-able settings fragment.)*

**Add the Scala-code companions.** This plugin installs `tt` only — it does **not** pull in the companions
transitively (Claude Code plugins can't install other plugins or system tools). Add them yourself:
```
/plugin marketplace add nguyenyou/scalex      # scalex: fast, symbol-aware Scala navigation
/plugin install scalex@scalex-marketplace
```
**Metals MCP** (compiler-grade truth — types, diagnostics, tests, refactor) isn't a plugin: enable it via
your editor's Metals + MCP-client config — see https://scalameta.org/metals/docs/features/mcp/. Then add
the companion entries to your allowlist (`Bash(scalex *)`; Metals' effectful MCP tools stay per-use, not
blanket-allowed) — details in [`docs/claude-plugin.md`](docs/claude-plugin.md). Which tool for which
question: [`docs/tool-selection.md`](docs/tool-selection.md).

### Try it: seed a working web app

New to genscalator? The fastest way to see it work is to let the agent **seed a complete, runnable Scala web app** for
you, then run and read it. In a fresh Claude Code session with the plugin installed, just ask in plain language, naming
the directory you want:

> Use the crud-web-app-seed skill to create a todo web app in ./my-todo

The agent runs the **`crud-web-app-seed`** skill, which writes a small full-stack project into the directory you chose:
a shared datamodel, a **JDK-only** HTTP server, and a **Scala.js + Laminar** browser client, plus a `PRD.md` written in
reqT-lang and a test suite. Then follow the generated `README.md`: `sbt client/fastLinkJS`, `sbt server/run`, and open
<http://localhost:8080>; run `sbt test` to see the tests (the JSON codec round-trips plus an end-to-end HTTP CRUD
test). It is deliberately small and commented so you can read the whole thing and adapt it to your own domain.

## Portability

genscalator targets *any* capable coding agent, not one vendor. The tools (scala-cli scripts + the `tt` launcher)
are agent-agnostic; the agent-specific parts are thin harness integration (allowlist, memory, skill
packaging). We aim to support frontier tools (Claude, Codex) and open-source agent frameworks/models.

## Research

genscalator is developed **as an on-going, open research project** — its tools, skills, and docs are distilled
from real investigations. Plans and results live in [`research/`](research/), e.g. *how the `scala-style`
skill should self-consciously evolve from agent use without drifting or bloating*. Research notes are
deliberately kept **out of agents' daily working context** (an agent reads them only when explicitly
investigating), so exploration never adds confirmation/context overhead to ordinary tool use.

## Contributing

Humans **and** agents are welcome — especially new general-purpose tools. If your agent builds a tool that
turns out to be project-agnostic, it should suggest contributing it back here (issue + PR). The agent
proposes; you as accountable human approve and submit. See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Licenses

* All code in this repo is licenced under Apache-2.0 — see [`LICENSE`](LICENSE).
* All blog posts and research topics are licenced as CC-BY 4.0.

## Copyright

Copyright of all code in this repo is owned by the maintainers of the genscalator repository. Any code contributor to this repo implicitly transfers copyright to genscalator maintainers by contributing. Before you contribute you should send a copyright transfer note via email to genscalator at bjornregnell.se with the subject "Copyright transfer" and body containing "I hereby transfer copyright of my contributions to genscalator to the maintainers of genscalator" and your name and contact details. 

## Maintainers

The genscalator repository is currently maintained by:
* [Professor Björn Regnell](https://bjornregnell.se)
* You? If you are interested to become a maintainer, send email to genscalator at bjornregnell.se

## Commercial Support

* For commercial support and consultancy in using genscalator to improve agentic software engineering productivity contact genscalator@bjornregnell.se

## Donations

Genscalator is developed as a liberally licenced open source software project that anyone can use. If you want to support the maintenance and implementation of new features of genscalator contact genscalator@bjornregnell.se

## Mirrors and digital sovereignty

The genscalator repo is mirrored from [Codeberg](https://codeberg.org/bjornregnell/genscalator) to [GitHub (owned by Microsoft)](https://github.com/bjornregnell/genscalator), [GitLab](https://gitlab.com/bjornregnell/genscalator) and [LTH coursegit](https://coursegit.cs.lth.se/bjorn.regnell/genscalator) in the spirit of [digital sovereignty](https://en.wikipedia.org/wiki/Digital_sovereignty), to address the debated "kill switch" potentially enabled by US laws such as:
- [IEEPA (1977)](https://en.wikipedia.org/wiki/International_Emergency_Economic_Powers_Act)
- the [CLOUD Act (2018)](https://en.wikipedia.org/wiki/CLOUD_Act)
- [FISA Section 702](https://en.wikipedia.org/wiki/FISA_of_1978_Amendments_Act_of_2008)

Even former Microsoft CEO Bill Gates, while calling a shutdown unlikely, said he was "certain until recently" about Microsoft's security from such interference and expects to be "certain again" in a few years; the sharper warnings come from Danish and Swedish security officials and Sweden's prime minister. See [Swedish public service news SVT (updated 2026-01-29)](https://www.svt.se/nyheter/utrikes/bill-gates-om-europas-techoro-var-saker-fram-till-nyligen) (text in Swedish, video clip in English).