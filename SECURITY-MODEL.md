# The genscalator security model

genscalator is a typed Scala toolbox and a set of working habits for a human collaborating with an AI
coding agent on a real machine. This page explains what "security" means in that setting and how the design
keeps the pairing safe. It is written for a developer new to genscalator who wants to understand the model,
not just follow rules.

If you want the deeper rationale first (the founding argument, the pillars, and the hard central question),
read the theory note linked at the end. This page is the operational picture: what protects you, and why.

## The problem

An AI agent that can run shell commands on your computer is both useful and dangerous. It can build, test,
commit, and deploy for you. It can also delete files, leak a token, or install something that persists,
either by mistake or because someone has deliberately steered it there. The genscalator security model
exists to make that pairing safe by design: the agent does real work, but neither the agent's mistakes nor a
moment of your own inattention can quietly cause harm.

## The threat model

The adversary is a Black Hat Hacker (BHH): anyone trying to turn the human and agent pairing toward a
BadGoal. The BadGoals the model is built to prevent are concrete:

- gaining control of your system, for example remote code execution through a command you approved,
- stealing secrets or credentials such as tokens, SSH keys, or environment variables,
- gaining persistence through cron, a shell rc file, `~/bin`, or a tampered tool,
- weaponizing confirmation fatigue (the way a stream of approval prompts wears a person down until they
  rubber-stamp) by hiding a dangerous operation inside an approved-looking command,
- supply-chaining a tool through a malicious dependency,
- tampering with the record of what happened, to hide any of the above.

The unusual part of this model is that **either party can be the failure point**. The agent can make a
mistake, or be steered by a hostile operator. The human can get tired and rubber-stamp a prompt, or be the
bad actor. So vigilance is symmetric, and the agent's ethical floor has to hold even against the human
directing it. Neither party is treated as automatically trustworthy.

## Three foundations

1. **Save nothing.** The hosted surface (the genscalator.ai service, when you use it) is built to persist as
   little as possible. What is never stored cannot leak.
2. **Fully open.** The code and the operating policy are both public. Nothing about how the system works is
   hidden.
3. **No security by obscurity.** The safety does not *depend* on any of that staying secret, so publishing it
   costs nothing. A design that is only safe while secret is not safe, and this one is published on the
   assumption that an adversary has read all of it.

## How it works

### Typed tools instead of raw shell

The `tt` toolbox replaces brittle shell habits (raw `grep`, `sed`, `python3 -c`) with narrow, typed Scala
commands. This is a security decision, not a matter of taste. A claim written in prose, a comment or a rule,
can ship while being false and stay false for a long time. A claim written in typed code either compiles or
does not, so whole classes of error are caught before the command ever runs. Each tool is also a small,
reviewable, purpose-built executable rather than a blank shell that can do anything.

### When the tool's job is to run code

Narrowing works when a tool *can* be narrow: "search this directory" or "GET this URL" have no dangerous
degrees of freedom, so the typed version is safe whatever the input. But some tools exist precisely to run
generated code, a build-and-test runner or the app you are developing. In agentic software engineering the
agent writes code and runs it; that is the work, not an abuse to be prevented. "Safe by construction" does not
reach this case, because executing a program has every degree of freedom there is.

For that residual class the posture is not prevention but three layered levers:

1. **Keep the human on the outward, irreversible effects** — a deploy, a destructive git command, anything
   that leaves the machine. Scarce human review is spent where a mistake cannot be undone, not on every test
   run.
2. **Bounded, earned trust** for the routine inward loop, where the operation is the intended one.
3. **Contain the blast radius** so that even un-reviewed code cannot reach a secret, the network, or the
   filesystem beyond its box. This is what the capture-checking direction below aims at.

This also sharpens what a typed wrapper buys when it cannot make execution itself safe. Allowlisting a raw
interpreter such as `scala-cli *` is dangerous not because building the project is dangerous, that is the
intended operation, but because the same broad grant *silently* permits everything else the interpreter can
do: inline `-e` evaluation, running a script from anywhere on disk, execution with nothing to do with the
project. A typed runner narrows the *surface* it exposes, directory-scoped verbs, no inline eval, no
arbitrary-path run, and so lets that blanket grant be removed. It does not make running code safe; it removes
the surplus authority a broad allow hands out for free. The rule of thumb: narrow what is *silently permitted*,
not what the human is *able to intend*.

### The guard

Before the agent runs a shell command, an automatic check runs first (a Claude Code PreToolUse hook,
`tt guardcheck`). It inspects the command and can do one of three things:

- **stay silent**, so your normal permission rules apply unchanged,
- **deny** the command, blocking it and handing the reason to the *agent*, which then retries a safe form,
- **ask**, surfacing a prompt to *you*.

Which of deny or ask you get is decided by how dangerous the shape is. The most dangerous shapes (a command
chain, a heredoc, a `cd` combined with another command) are **denied** outright and handed back to the agent
to rewrite. Milder reflexes (a stray pipe into a pager) raise an **ask**.

One rule governs all of this: the guard may only tighten, never loosen. It never emits "allow", because
"allow" would bypass your own permission settings on the strength of the guard's own string matching. "We
have no objection" is spelled *stay silent*, never *allow*. The guard's job is to add caution, never to
remove a protection you configured.

A `deny` is cheap: it is handled by the agent and costs you no attention. An `ask` is expensive: it spends
your attention and carries a small risk that a tired human approves something they should not. A current
design direction (not yet the behaviour today) is to move milder checks that have a known safe rewrite from
`ask` toward `deny`, so fewer prompts reach you at all.

### The allowlist holds syntax; the tool holds meaning

Your permission allowlist matches the command *string*. That means it can enforce syntactic rules ("no
pipes", "no `&&`") but it cannot enforce a constraint like "stay inside this directory". Path resolution,
including `..` and symlinks, happens *after* the string is matched, at the filesystem layer, so a path can
walk straight out of any prefix you tried to pin.

The consequence shapes the whole toolbox: a semantic constraint has to live in the *tool*, which works with
resolved paths, not in the allowlist, which only sees text. This is the real reason it is safe to allowlist
`tt git`: not because its command string is harmless, but because the tool exposes only a small set of
non-destructive verbs (add, commit, push, fast-forward-only pull, fetch, and read-only show) and keeps the
destructive ones (reset, rebase, force, rm) off entirely. The allowlist grants "run this tool"; the tool
decides what that permits. Put each check where the facts to evaluate it actually are.

### The human needs guards too

The weakest link in the whole system is the "allow, and do not ask again" option. It does not approve one
command; it permanently relaxes your *own* permission rules for every command of that shape, and it is
offered to the person most likely to be tired. Coverage only ever ratchets down, because nothing re-arms it.

So the model narrows the human's reach with structure, not willpower. The standing rules:

- Never blanket-allow `rm` or any destructive or irreversible command. One-time, shown, human-approved is
  fine; a standing "always allow" is the disarm, and the agent must actively flag any move toward one.
- Never allowlist an interpreter (`python3 -c`, `bash -c`). An interpreter is a blank shell; scoping it is an
  illusion.
- Never blanket-allow the agent editing its own settings. Security and permission changes stay approved one
  edit at a time.
- While the human is away, the agent runs only bare, allowlist-matchable, prompt-free commands. A prompt the
  agent triggers cannot be cleared by a human who is not there.

The same principle applies to the human's own reflexes. A mouse click can land on an "allow forever" option
as a side effect of a window gesture, so the safer fix is to remove the reach (for example, disabling
mouse-click approvals) rather than to rely on being careful. Both members of the pair need their reach
narrowed, and neither can do it reliably from the inside at the moment of action.

## What this model does not defend against

Being honest about the boundary is part of the model:

- **A determined, crafted attack.** The guard is built to catch the agent's own careless reflexes. Its checks
  for the most dangerous shapes inspect the raw command, so quoting does not hide them, but the guard is not
  an adversarial sandbox and a crafted attack has avenues it was never meant to cover.
- **A compromised machine or account.** If the box or the credentials are already owned, this model does not
  save you.
- **Supply-chain compromise of a dependency you approve.** Approving a malicious `//> using dep` is outside
  what the guard can see.

Naming these keeps "safe by design" an honest claim rather than a slogan.

## Credentials and tokens

**The policy in one paragraph.** Prefer a credential **fetched at the point of use** over one **exported
into the environment**. A token in a shell rc file is readable by every child process for the life of that
shell; a token obtained at the moment of use exists briefly, inside one tool, on one audited code path, and
self-heals when you re-authenticate. Values are never the default answer: `tt env` offers names, an exit
code, or one variable at a time, and has no whole-environment verb. And a read is not automatically safe —
for an agent the hazard is set by *where the output lands*, so a bulk read of a credential-bearing surface
is a disclosure operation no matter that it mutates nothing.

⚠ **What this policy trades away, stated plainly because a reader will otherwise notice it unaided.** The
control here is **audit and brevity of exposure**, *not* human-in-the-loop. Preferring a helper means that
whenever you are already authenticated to `gh`, a running agent can obtain a token you never handed it.
That is not hypothetical: on 2026-07-27 `tt forge` reported *"no token in env; obtained one from
`gh auth token`"* on essentially every call across a long session, including while the human was away from
the keyboard. Read alongside a native updater that installs binaries (see `reqts/ROADMAP.md`, v0.10.0),
genscalator has deliberately chosen, in two places, to make the safeguard *a narrow audited path plus a
short exposure window* rather than *a human pressing a key each time*. The reasons are that per-use
confirmation on a frequent operation produces exactly the confirmation fatigue this document argues
against, and that a long-lived ambient token is a worse steady state than a momentary one. **The
alternative was considered and declined** (2026-07-27): tokens solely from fixed human-set env names with
no helper fallback, so an agent could never obtain a credential it was not given. If you want that
posture, it is one line of policy and a deleted fallback — the mechanism is still there.

> **The incident this section exists because of.** The model above is careful about
> what an agent may *run* and says almost nothing about what an agent may *see*. On 2026-07-25 that gap
> produced the first real incident: the agent wanted one environment variable, ran a bare `printenv`, and
> put two live API tokens into a durable transcript, forcing a rotation of both. Nothing stopped it —
> `printenv` is not a `cd`, not a pipe, not a redirect, so the guard saw nothing, and the tool-choice NOTE
> tier added that same morning listed interpreters rather than bulk environment reads.

The policy in full, with the reasoning that used to live only in commit messages and WR data:

1. **Read-only is not the same as safe.** For an agent, the hazard of a read is set by *where the output
   lands*, not by whether the command mutates anything. A transcript is durable, copied and quoted, so a
   bulk read of any credential-bearing surface — the environment, `~/.netrc`, `~/.git-credentials`, CI
   config, `.env` files — is a **disclosure operation** however read-only it is. This principle belongs in
   "Three foundations", not in a footnote.
2. **Names are the default answer; values are opt-in and singular.** This is why `tt env` has `list`
   (names only), `has` (exit code only) and `get <NAME>` (one variable, redacted unless `--reveal`), and
   deliberately has *no* whole-environment verb. The shape of the tool is the policy.
3. **One home for "does this look like a credential".** `secrets.scala` holds the detection and redaction
   that `tt harden` and `tt env` share, so the two cannot drift on what a secret is.
4. **Where tokens may come from, and who decides.** `tt forge` historically took tokens *only* from fixed
   human-set env names, so the human's shell decided whether a running agent had credentials at all. On
   2026-07-25 that was widened (BR-authorized, agent-flagged): with no env token, `tt forge --gh` falls
   back to `gh auth token`. The trade is explicit — it removes a long-lived token from the ambient
   environment of every process, at the cost of letting the tool mint one. ✅ **The open question here —
   should that fallback be gated behind an explicit human opt-in rather than being the default? — was
   ANSWERED on 2026-07-27: no. The fallback stays the default.** A per-session consent gate was the third
   option on the table and was declined, on the grounds above: the control is the audited path and the
   short window, and a prompt on a frequent operation buys fatigue rather than safety. What the fallback
   must keep doing is **announcing itself** — it prints which source supplied the token on every call,
   which is what made the AFK self-authorization observable at all, and is therefore load-bearing rather
   than cosmetic. Removing that line would convert this policy from auditable to merely convenient.
5. **Ambient versus momentary exposure.** A token exported in a shell rc file is visible to every child
   process continuously; a token fetched at the point of use exists briefly inside one audited tool. State
   **The preference: momentary wins, and it is a preference rather than a prohibition.** Two exceptions are
   legitimate. (a) **CI**, where there is no interactive session for a helper to draw on, so a secret
   injected into the job environment is the only mechanism — bounded because the environment dies with the
   job, and because `${{ github.token }}` is scoped to one repository and one run. (b) **A forge with no
   credential helper**, where a fixed env name is the only route; `tt forge` names those variables
   explicitly rather than scanning the environment, so the surface stays one known key instead of everything.
   ⚠ **The exception that is NOT legitimate is a token exported from a shell rc file for interactive
   convenience** — that is the ambient case the preference exists to discourage, it is what the 2026-07-25
   leak actually disclosed, and unlike a helper token it does not self-heal on re-auth.
6. **What to do after a leak.** Revoke before regenerating; find the export by *location* (`grep -l`) not
   by value; edit rc files in an editor rather than via a shell command, because the shell history is
   another durable record; and remember that a token derived as `$(gh auth token)` self-heals on re-auth
   while a literal token in an rc file does not.

Sources: `research/wr-data/printenv-dumped-live-tokens-into-the-transcript-2026-07-25.md`,
`tools/secrets.scala`, `tools/env.scala`, and the trust-boundary note on `ghCliToken` in `tools/forge.scala`.

## Future work

The tools on this page get their narrow authority by construction and review: a `tt` command exposes only the
operations it was written to expose. A stronger form would have the *type system* prove the bound instead of
trusting the author to keep it. This is a direction to look into, not something the model does today.

Scala 3's **capture checking**, still an experimental language feature, tracks capabilities statically: a
capability is a program value that regulates access to an effect or a resource, and the compiler tracks which
capabilities each piece of code can reach. In principle that could turn "this tool is narrow because we wrote
it narrowly" into "this tool cannot touch the filesystem, or the network, or a secret, because the type
checker says so", and it enables *local purity*: proving a sub-computation is side-effect-free, so an agent
could process sensitive data with a compiler guarantee against leaks.

The 2026 paper "Tracking Capabilities for Safer Agents" (Odersky et al., EPFL; linked below) demonstrates
this end to end: the agent expresses its intentions as code in a capability-safe language (Scala 3 with
capture checking) instead of calling tools directly, and the type system statically prevents information
leakage and malicious side effects at no significant cost to task performance. It formalizes the instinct
behind this page.

genscalator has early proof-of-concept experiments in this direction
([`research/experiments/capture-checking/`](research/experiments/capture-checking/)), but adopting capture checking in the toolbox and in this security
model is **open work, not done**, and it carries real limits worth stating up front: the feature is
experimental and still changing; its guarantees cover only effects expressed as typed capabilities, so a
command that shells out to the real terminal escapes back to the allowlist; and it constrains effects, not the
correctness of the agent's decisions. So it is a promising direction to study as a complement to the guard and
the allowlist, not a replacement for them and not a finished part of this model.

## Go deeper

- [`research/theory/TH006-genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md`](research/theory/TH006-genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md) for the founding
  argument, the three pillars in full, and the discriminating criterion (how to hold a firm ethical floor
  without becoming either paternalistic or easy to manipulate).
- [`docs/foundations.md`](docs/foundations.md) for the glossary: BHH, BadGoals, confirmation fatigue, rot.
- [`tools/guardcheck.scala`](tools/guardcheck.scala) for the guard itself, the mechanism behind this page.
- [`skills/avoid-guard-stall/SKILL.md`](skills/avoid-guard-stall/SKILL.md) for the agent-side operational guidance.

## Further reading

genscalator's central move, narrowing authority into small typed tools instead of the ambient shell, is the
instinct behind **capability-based security**: authority should travel as a narrow, purpose-specific
capability rather than as ambient power to name and act on anything. Two ideas from that literature map
straight onto this page:

- The "allowlist holds syntax, the tool holds meaning" section is the classic observation that a command
  string (like a path name) is a *forgeable reference*: it names a target but does not carry the right to use
  it, so it must be validated under the program's *ambient authority*, which is exactly where `..` walks out.
  A capability carries the authority with the reference; a typed tool that resolves and checks the path is the
  pragmatic stand-in.
- The *confused deputy problem* names the BHH attack where a trusted agent, acting on your authority, is
  tricked into misusing it (hiding a dangerous operation inside an approved-looking command). Much of the
  guard exists to keep the agent from becoming a confused deputy.

Being precise: genscalator is not a capability-based system. It runs on a conventional permission allowlist
(an access-control list, the approach capability security contrasts itself against) and borrows the insight
(least authority, no ambient power), not the architecture.

- Capability-based security: <https://en.wikipedia.org/wiki/Capability-based_security> (see its links to the
  Principle of Least Privilege, the Confused deputy problem, and Ambient authority).
- Capture checking in Scala 3 (still an experimental feature):
  <https://docs.scala-lang.org/scala3/reference/experimental/capture-checking/index.html>
- "Tracking Capabilities for Safer Agents" (2026), the TACIT paper: <https://arxiv.org/abs/2603.00991>
