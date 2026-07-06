# genscalator

**Power tools for agents: smarter, safer, faster.**

## What

Genscalator is a toolbox + workflow for coding agents that replaces the brittle
bash/grep/awk/python reflex with **typed, compiler-checked, reusable Scala tools**. Pick a tool, give it
args — no re-deriving logic each time, no dynamic-shell surprises. The compiler catches mistakes before
they run, and a small launcher (`tt`) makes every tool a single, statically-analyzable command that a
narrow allowlist can trust.

## Why

Out-of-the-box agent workflows lean on approving dense bash compunds and archaic Unix tools 
stitched  together in a difficult to review blob. 
Much of the guardrail machinery exists precisely to contain what can go wrong there, and the 
cost is **confirmation fatigue** and bad UX from reviewing cryptic, dynamic, unsafe code.

Genscalator shifts to **safe, compiled code with static guarantees**. Every time the agent would reach for a
one-off bash/grep/awk helper, it instead creates (or reuses) a persistent, self-contained Scala tool.
That earns static guarantees, reduces the agent getting stuck debugging brittle helpers, and shrinks the
number of dangerous operations that need human approval at all.

See [`docs/foundations.md`](docs/foundations.md) for the full goals, stakeholders (human / agent / Black
Hat Hacker threat model), and glossary.

## Install

**Prerequisites:** [scala-cli](https://scala-cli.virtuslab.org/) and a JDK (to run the tools), plus `git`
(to clone this repo).

**Platforms:** Linux, macOS, and WSL — anywhere `bash` + `scala-cli` run. On native Windows use WSL (or Git Bash).

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

Most `tt` tools need only **scala-cli + a JDK** (the pure tools use just the JDK). A few tools shell out to an
**external program** for one job; install it only if you use that tool:

| Tool | External dependency | Install | Docs |
|------|--------------------|---------|------|
| `tt gvdot` | **graphviz** (`dot`) — lays out sequence diagrams to pdf/png/svg | `sudo apt install graphviz` (Debian/Ubuntu); `brew install graphviz` (macOS) | [graphviz.org](https://graphviz.org/) · `dot -h` · `man dot` |

Tools degrade gracefully when their dependency is missing: `tt gvdot` still prints DOT source without `dot`, and
errors with the install hint only on the render path. (The sibling renderers `tt svg` and `tt ascii` need **no**
external dependency — pure JDK.)

## Roadmap

What's shipped so far, per release: [`CHANGELOG.md`](CHANGELOG.md).
For the toolbox-specific roadmap (new/extended `tt` tools), see [`tools/README.md`](tools/README.md#roadmap).

**Planned, not yet built — roughly cheapest-to-build first:**
- **Update awareness** — an *inform-only* update skill + a read-only version-check (compare the installed
  version against upstream `marketplace.json`) so staleness is visible. It hands the human the update
  commands but **never self-updates the operating rules** — adopting new rules stays a human-reviewed step
  (see [`docs/updating.md`](docs/updating.md)).
- **One-command install** of genscalator + companions (scalex + Metals MCP) for newcomers who want
  everything at once — as a **reviewable, version-pinned installer script you read before running**, not
  a blind `curl … | bash` (that opaque-pipe pattern is exactly the confirmation-fatigue / RCE risk
  genscalator argues against).
- Native compilation.
- A Java-vs-Scala token-efficiency experiment (out-of-the-box vs genscalator).
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

## License

Apache-2.0 — see [`LICENSE`](LICENSE).
