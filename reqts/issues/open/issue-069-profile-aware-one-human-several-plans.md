# Issue 069: one human, several plans — should genscalator be profile-aware, and how far?

> status: open 2026-09-24 · labels: discussion, scope, statusline, multi-profile ·
> measured against: v0.10.2 (from `VERSION.txt`); claims re-verified at `5adb395` (2026-09-23);
> account facts read from two live accounts on one Linux box, Claude Code 2.1.273
> · summary: running two Claude
> plans (a work seat and a private Max) from one Unix user is a real and growing setup that genscalator
> currently cannot see. Proposes a three-tier answer — fix the config-home bug (issue 067), add
> read-only *profile* and *plan* chips to the statusline (the profile one DERIVED from the environment,
> not declared), and deliberately DECLINE to manage profiles inside `tt` — and asks the maintainer to
> rule on the middle tier before any code is written.

## Description

### The setup this is about

One Unix user, two Claude accounts: an organisation seat (Team/enterprise, stock `~/.claude` layout)
and a privately paid Max subscription (`CLAUDE_CONFIG_DIR=~/.claude-personal`). Both run at once, in
different terminals, against the same `~/.local/bin/claude`. The keeping-them-apart machinery —
a profile registry, a deep-merged settings baseline handed in via `claude --settings`, and a launcher
that resolves each profile's config home — is ~270 lines of Scala living outside genscalator.

Why it is not niche: anyone with an employer seat and a private subscription lands here, and the number
of people holding both is rising. The stock advice is "set `CLAUDE_CONFIG_DIR`", which is necessary and
not sufficient: setting it to `$HOME/.claude` makes Claude Code read `~/.claude/.claude.json` instead of
`~/.claude.json` and silently orphans an existing sign-in, so the naive version of the advice costs you
the account you were trying to keep.

genscalator is *used* in this setup daily and works well. But it cannot see the setup at all, in three
separable ways.

### Tier 1 — correctness. Prepared as issue 067.

Six call sites resolve `$HOME/.claude` literally and ignore `CLAUDE_CONFIG_DIR`, so state lands in the
wrong plan's home and `tt skillcheck`'s recovery hint reports skills as absent that are installed.
Self-contained, testable, no design question. **Independent of everything below** — it should land
whatever is decided here.

### Tier 2 — visibility. The question this issue is actually asking.

Two plans mean two rate limits, two usage budgets, and two very different consequences for pasting the
wrong material into the wrong window. Nothing in the session distinguishes them. The statusline is the
obvious place: it already carries repo, context fill, rot, modes and box health — everything *except*
which account is paying for the tokens it is counting.

What a chip would need to say: the profile (`personal`, `work`) and the plan (`max5x`, `team`, `pro`).
The profile half turns out to be the cheap one and is worked out in its own section below — it is a
single environment lookup. The plan half is the non-trivial one, because **the plan is not in one
field**:

| account type | where the tier actually is |
|---|---|
| Team/enterprise seat | `oauthAccount.seatTier` |
| Max subscription | `seatTier` is `null`; the tier is in `organizationRateLimitTier` (e.g. `default_claude_max_5x`) |

Code that reads only `seatTier` reports a Max account as having no plan — verified, and the bug that
prompted writing it down. That detection is the part genuinely worth upstreaming: it is a small pure
function over `<configHome>/.claude.json` (with the beside-vs-inside asymmetry above), it is the kind
of undocumented-internals knowledge that rots quietly, and genscalator already reads adjacent harness
state for the statusline.

### Related, split out: how the statusline gets wired at all

Two facts found while wiring the statusline here bear on any chip proposal, since they concern the
same command line — but they hit single-profile users identically, so they are **filed separately**
(issue 068):

* the documented wiring `"command": "tt statusline"` **never resolves**, because the plugin's `bin/`
  goes on the Bash tool's PATH and not the `claude` process's, and a failing statusline renders a
  blank line rather than an error;
* the plugin launcher costs **~656 ms per render** against **~8 ms** for the native install, and
  starts a JVM each time — which `--box-line` then reports as box load.

Both point at `"$HOME/.genscalator/bin/tt statusline …"` as the wiring that works. Relevance here:
whatever a plan chip ends up costing per render is spent on top of that, so the chip's performance
budget depends on issue 068 landing first.

Sketch, not a proposal to implement yet:

```
tt plan            # -> max5x  (personal)      read-only, no network
tt statusline --plan-chip
```

Open sub-questions: Is `--plan-chip` opt-in like `--mode-line` and `--box-line`, or on whenever
detection succeeds? And the transcript-read performance note in `statusline.scala` applies — reading
`.claude.json` on every render needs the same guard, or a cache.

### The profile chip — DERIVED from the environment, with declaration as a marked fallback

**Proposal: a chip naming the profile (`profile personal`, `profile work`), derived by the statusline
from its own environment.** No declaration, no configuration, no account-metadata read.

The idea arrived as a declared chip — `+Work`, the ordinary mode mechanism — and that form does work
today with zero code. For most chips it would settle the question. **This chip is different, and the
difference is why derivation is primary.** Every other mode is a claim about the agent's or the human's
state; this one is a claim about *which account is paying and which data boundary applies*. A stale
`Afk` chip misleads about autonomy. A stale or mistyped `Work` chip tells someone it is safe to paste
work material into the private session, or the reverse — the exact error the two-profile separation
exists to prevent. A wrong profile chip is worse than no profile chip, because it converts uncertainty
into false confidence.

Derivation removes the failure mode rather than mitigating it: an environment variable cannot be stale,
cannot be forgotten from a previous session, and cannot be typed wrong. Three mechanisms, in the order
they should be tried:

**1. Derived from the environment — PRIMARY. Free, and verified available.** The `claude` process
carries the variable that defines the profile, and a statusline subprocess inherits it. Checked on a
live session 2026-09-15:

```
$ grep -zao "CLAUDE_CONFIG_DIR=[^\x00]*" /proc/$(pgrep -n claude)/environ
CLAUDE_CONFIG_DIR=/home/bjornr/.claude-personal
```

So `tt statusline` can read its own environment and name the profile with **no configuration, no
declaration and no account-metadata read** — which also sidesteps the sensitivity question in decision
1 above. The basename gives `personal`. Note the asymmetry: a correctly-built stock profile leaves the
variable *unset* (setting it to `$HOME/.claude` orphans the sign-in), so "unset" is itself meaningful —
it means the stock config home — though it carries no name.

**2. Launcher-exported name — one line, and the only integration contract.** The dir basename is a
proxy; the profile's real name is a launcher concept. A launcher that exports `CLAUDE_PROFILE=<name>`
alongside `CLAUDE_CONFIG_DIR` hands genscalator the real name for free, and the whole contract is one
environment variable — no file format, no protocol, nothing to version. This is the smallest possible
seam between a profile launcher and genscalator, and it is worth specifying even if nothing else in
this issue lands.

**3. Declared (`+Work`) — LAST RESORT, and it must be marked as a claim.** For anyone with no launcher
and no variable, a declared chip is better than nothing. But it should not *look* like a derived fact.
The statusline already has a convention for exactly this: a human-declared limit renders with a tilde
(`f5·~84%`) to distinguish it from a measured one. Reuse it — `profile ~Work` is a claim,
`profile Work` is derived — and the reader can tell at a glance which one they are trusting.

Two consequences worth stating:

* **The agent should not self-declare this chip.** Elsewhere the agent declaring its own modes is the
  design (`HotHarvest`, `RotVigil`). Here the agent has no privileged knowledge — it would be guessing
  from the same environment the tool can read directly — and a guess rendered as a chip is the failure
  mode above.
* **A declared profile chip needs the expiry work from issue 071.** Without a timestamp, a `Work` chip
  declared in a previous session is indistinguishable from one declared now. Derived chips have no such
  problem, which is another argument for mechanism 1.

### Tier 3 — management. Argued against, on purpose.

The tempting next step is `tt profile <name>` — the launcher itself, inside the toolbox. I do not think
genscalator should take it, for three reasons:

1. **Scope.** The toolbox is text, files, git, diagrams, and session-state chips. Launching a harness
   against a chosen credential store is a different domain; `tt profile` would be the first tool whose
   worst failure mode is *orphaning a sign-in*.
2. **It already works.** The existing launcher is a 5 ms native binary with a `doctor` subcommand. A
   port buys distribution, not capability, while adding a release cycle between a bug and its fix.
3. **The `AGENTS.md` self-check answers it.** "Generally useful, or project-specific?" — profile
   launching is generally useful but *separately* useful; it does not want to be reached through `tt`.

The proposal is therefore: genscalator becomes **profile-aware, not profile-managing**. It reports which
plan a session belongs to; it does not switch them. The launcher stays a separate tool (a standalone
repo is planned) and the two cross-link.

## Decision needed

For the maintainer, before any code:

1. **Is Tier 2 in scope at all?** A plan chip means genscalator reads account metadata, which is a new
   category of input for it — more sensitive than a transcript, even read-only. A clear no is a useful
   answer and closes this issue; Tier 1 is unaffected either way.
2. **If yes: `tt plan` as a tool, a `--plan-chip` flag, or both?** The tool alone is cheap and useful
   on its own; the chip is where the value is but adds a per-render read to the statusline's hot path.
   Note the profile chip (above) is cheaper than the plan chip and needs no file read at all — an
   environment lookup — so it could land first and independently.
3. **Plan, profile, or both in the chip?** Both are derivable by genscalator alone — the plan from
   `.claude.json`, the profile from `CLAUDE_CONFIG_DIR` in its own environment. Only the profile's
   *human-chosen name* needs an external tool to pass it in (`CLAUDE_PROFILE=<name>`), and that is a
   one-variable contract, the only place the two tools would touch. Declaration (`+Work`) is the
   fallback for setups with neither, rendered as a claim rather than a fact.
4. **Does the tier-detection logic belong in `lib.scala` regardless?** Even without a chip, "read this
   machine's plan correctly" is a fact worth having recorded somewhere it will be maintained.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-09-15 17:33

Filed alongside issue 067 from the same two-plan setup. The split is deliberate: 067 is a defect with a
mechanical fix and should not wait on a scope conversation, while this one is a question and may well be
answered "no".

Provenance for the `seatTier` / `organizationRateLimitTier` claim: verified against both live accounts on
one machine — the Team seat states its tier in `seatTier`, the Max account leaves it `null` and carries
`default_claude_max_5x` in `organizationRateLimitTier`. Also verified that plugin installs are per config
home (`installed_plugins.json` has `"scope": "user"`), which is why a plugin — genscalator included — must
be installed once per profile. The blank-line failure that implies turned out to be broader than a
per-profile matter — a bare `tt` in `statusLine` resolves on *neither* profile — so it is split into its
own draft (026) rather than carried here. I had first written it off as Claude Code behaviour; the
harness half (a failing statusline renders empty) is indeed fixed, but the command the genscalator help
text recommends is the one that cannot resolve, and that half is ours.

Not raised as a defect: `tt mode --file` and `tt statusline --modes-file` already make Tier 1 workable by
hand today, so nothing here is blocking.

Agent disclosure: the account-metadata comparison, the tier-detection finding and this issue text were
produced by an AI agent (Claude Opus 5) under human direction; the human reviewed and submitted.

### Comment by bjornregnell/Opus5 at 2026-09-24 14:05

Re-verified before filing, against `5adb395`. This issue was drafted 2026-09-15 against the **0.10.2
plugin cache** rather than a checkout — the same stale baseline that shifted three line numbers in
issue 067 — so everything citable was re-checked rather than trusted:

* **The environment derivation holds**, and it is the load-bearing claim for mechanism 1:
  `CLAUDE_CONFIG_DIR` is present in the running `claude` process's own environment
  (`/proc/<pid>/environ`), so a statusline subprocess inherits it. The profile chip therefore needs no
  configuration and no account-metadata read. Confirmed on a live session, not reasoned from docs.
* **The plan-detection asymmetry holds**, read from two live accounts: the Team seat states its tier in
  `seatTier`; the Max account leaves it null and carries `default_claude_max_5x` in
  `organizationRateLimitTier`.
* **`tt mode --file` and `tt statusline --modes-file` / `--limits-file` still exist** at `5adb395`
  (`mode.scala:39`, `statusline.scala:571-572`), so the "workable by hand today" note above is current.
* **No overlap with existing issues.** Nothing in `open/` or `closed/` mentions `CLAUDE_CONFIG_DIR`, and
  issue 064 (statusline session chip) is a different chip with a different source of truth.

Still unverified, and deliberately so: whether a `--plan-chip` belongs in scope is the question this
issue exists to ask, and the per-render cost of reading `.claude.json` has not been measured.

Agent disclosure: the re-verification was performed by an AI agent (Claude Opus 5) under human
direction; the human reviewed and submitted.
