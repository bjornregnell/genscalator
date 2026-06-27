---
name: contribute-tool
description: How to elevate a one-off scratch tool into a toolbox-worthy genscalator `tt` tool before proposing it upstream — generalize beyond the specific case, then propose (human ships). Trigger whenever you're about to suggest contributing a tool to genscalator, or a scratch tool you built looks generally useful. Pairs with the scala-style skill (which is HOW to write a tool); this is how to GENERALIZE one and contribute it. Mechanics (issue + PR) live in CONTRIBUTING.md.
allowed-tools: Bash(scala-cli run *) Bash(scala-cli compile *) Bash(scalex *)
---

# Contributing a tool to genscalator

A scratch tool solves *your* case. A toolbox tool solves *that whole class* of case. Before proposing a
contribution, generalize — the same case-study-driven method genscalator is built on: **start specific,
generalize-ready.** (You *propose*; the human reviews and ships — never push to the public repo yourself.)

## The recipe
1. **Start from a real case.** The tool earned its place by solving actual work — don't design in the
   abstract. (Bonus: if it came from a [`research/wr-data/`](../../research/wr-data/) confirmation event,
   say so — that's the friction → tool loop.)
2. **Ask "who else hits this?"** Step back from the specific to the general class. (We took `tt log` from
   *LaTeX logs* → *the logs agents actually read*: build, test, runtime/leveled, CI, npm.) Rank coverage by
   how common the need is **across agents and ecosystems**, not just this project.
3. **Sane defaults + customization.** The common case must work with **zero config**; let the caller
   extend or override for theirs (e.g. `--error`/`--warn`, `--no-defaults`). **Keep the original specific
   case still working.**
4. **Don't dilute what made it good.** Carry the discipline forward (for `log`: targeted markers, no
   false positives). Each generalized case stays high-confidence — generality must not become noise.
5. **Verify with adversarial fixtures.** Test real multi-case inputs **and deliberate traps** — lines/inputs
   that *look* like a hit but aren't (e.g. a "0 errors" tally). Prove no regressions and no false positives.
6. **Follow toolbox conventions.** Apply the **scala-style** skill (pure/immutable/Safe-mode-ready, scoped
   imports). One allowlistable command, clean `===` output + a verdict, friendly errors not stack traces,
   reuse `lib.scala`, latest stable Scala.
7. **Strip project specifics.** No `/home/...`, hostnames, `$USER`, or single-project assumptions — it must
   read as project-agnostic.
8. **Document, version, record.** Add a `tools/README.md` entry, a `CHANGELOG.md` line, and bump the version
   (see the release discipline). Then draft the issue (the gap it fills + the safe-by-design angle) and PR.

## Then propose
Hand it to the human with a one-liner ("this looks project-agnostic and reusable — want me to prepare an
issue + PR?"). **The human reviews and submits.** Full mechanics: [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md).

Background: [`../../docs/foundations.md`](../../docs/foundations.md) (case-study-driven, start specific).
