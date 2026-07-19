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

- [`research/theory/genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md`](research/theory/genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md) for the founding
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
