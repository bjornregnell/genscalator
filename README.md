# genscalator

**Power tools for agents: smarter, safer, faster.**

## What

genscalator (**GS**) is a toolbox + workflow for coding agents that replaces the brittle
bash/grep/awk/python reflex with **typed, compiler-checked, reusable Scala tools**. Pick a tool, give it
args — no re-deriving logic each time, no dynamic-shell surprises. The compiler catches mistakes before
they run, and a small launcher (`tt`) makes every tool a single, statically-analyzable command that a
narrow allowlist can trust.

## Why

Out-of-the-box agent workflows lean on approving dense bash and archaic Unix tools. Much of the guardrail
machinery exists precisely to contain what can go wrong there — and the cost is **confirmation fatigue**
and bad UX from reviewing cryptic, dynamic, unsafe code.

GS shifts to **safe, compiled code with static guarantees**. Every time the agent would reach for a
one-off bash/grep/awk helper, it instead creates (or reuses) a persistent, self-contained Scala tool.
That earns static guarantees, reduces the agent getting stuck debugging brittle helpers, and shrinks the
number of dangerous operations that need human approval at all.

See [`docs/foundations.md`](docs/foundations.md) for the full goals, stakeholders (human / agent / Black
Hat Hacker threat model), and glossary.

## Install

Requires [scala-cli](https://scala-cli.virtuslab.org/) (and a JDK).

**Platforms:** Linux, macOS, and WSL — anywhere `bash` + `scala-cli` run. On native Windows use WSL (or Git Bash).

Put the `tt` launcher ("typed tools") on your PATH so tools run as one literal, allowlist-friendly command from any repo:

```
ln -s "$PWD/tools/tt" ~/.local/bin/tt    # ensure ~/.local/bin is on your PATH
```

First run of a tool compiles (~a couple of seconds); reruns are cached.

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

## Scope & roadmap

**v0.1.0 (this release):**
- The `tt` toolbox (`text`, `files`, `newtool` + shared `lib`).
- [`docs/foundations.md`](docs/foundations.md) — goals, stakeholders, glossary.
- [`docs/confirmations-method.md`](docs/confirmations-method.md) — a method + template for driving down
  confirmation fatigue.

**Roadmap (planned, not yet built):**
- Tool safety flags: `--safe-mode`, `--sandboxed`, `--audit`.
- Capture-Checking **Safe-mode** PoC → pure tools safe by default.
- A Scala-style skill (direct, state-safe, safe-mode-where-possible).
- **Cross-tool packaging:** an MCP server so the tools are first-class in Codex/opencode too. (The
  Claude Code plugin already ships — see *Use as a Claude Code plugin* below.)
- Native compilation.
- A Java-vs-Scala token-efficiency experiment (out-of-the-box vs genscalator).

## Use as a Claude Code plugin

This repo doubles as its own Claude Code plugin marketplace, so `tt` lands on your PATH automatically and
a skill teaches the habit — no manual symlink:
```
/plugin marketplace add https://codeberg.org/bjornregnell/genscalator.git
/plugin install genscalator@bjornregnell
```
Use the **full Codeberg URL with `.git`** — the short `owner/repo` form resolves to GitHub, and the
`.git` suffix makes Claude Code clone the repo (where `marketplace.json` lives).
Details, the recommended allowlist, and caveats: [`docs/claude-plugin.md`](docs/claude-plugin.md).
(You still need `scala-cli` + a JDK installed.)

## Portability

GS targets *any* capable coding agent, not one vendor. The tools (scala-cli scripts + the `tt` launcher)
are agent-agnostic; the agent-specific parts are thin harness integration (allowlist, memory, skill
packaging). We aim to support frontier tools (Claude, Codex) and open-source agent frameworks/models.

## Contributing

Humans **and** agents are welcome — especially new general-purpose tools. If your agent builds a tool that
turns out to be project-agnostic, it should suggest contributing it back here (issue + PR). The agent
proposes; you approve and submit. See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

Apache-2.0 — see [`LICENSE`](LICENSE).
