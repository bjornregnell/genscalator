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
either by mistake or because someone has steered it to. The genscalator security model exists to make that
pairing safe by design: the agent does real work, but neither the agent's mistakes nor a moment of your own
inattention can quietly cause harm.

## The threat model

The adversary is a Black Hat Hacker (BHH): anyone trying to turn the human and agent pairing toward a
BadGoal. The BadGoals the model is built to prevent are concrete:

- gaining control of your system, for example remote code execution through a command you approved,
- stealing secrets or credentials such as tokens, SSH keys, or environment variables,
- gaining persistence through cron, a shell rc file, `~/bin`, or a tampered tool,
- weaponizing confirmation fatigue by hiding a dangerous operation inside an approved-looking command,
- supply-chaining a tool through a malicious dependency,
- tampering with the audit trail to hide any of the above.

The unusual part of this model is that **either party can be the failure point**. The agent can make a
mistake, or be driven by a hostile principal. The human can get tired and rubber-stamp a prompt, or be the
bad actor. So vigilance is symmetric, and the agent's ethical floor has to hold even against its own
principal. Neither party is treated as automatically trustworthy.

## Three foundations

1. **Save nothing.** The hosted surface persists as little as it can. What is never stored cannot leak.
2. **Fully open.** Both the code and the policy are public. Nothing depends on a hidden rule.
3. **No security by obscurity.** A design that is only safe while secret is not safe. genscalator is
   published on the assumption that an adversary can read all of it.

## How it works

### Typed tools instead of raw shell

The `tt` toolbox replaces brittle shell habits (raw `grep`, `sed`, `python3 -c`) with narrow, typed Scala
commands. This is a security decision, not a matter of taste. A claim written in prose, a comment or a rule,
can ship while being false and stay false for a long time. A claim written in typed code either compiles or
does not, so whole classes of error are caught before the command ever runs. Each tool is also a small,
reviewable, audited executable rather than a blank shell that can do anything.

### The guard

Before the agent runs a shell command, a `PreToolUse` hook (`tt guardcheck hook`) inspects it and can do one
of three things:

- **stay silent**, so your normal permission rules apply unchanged,
- **deny** the command, blocking it and handing the reason to the *agent*, which then retries the safe form,
- **ask**, surfacing a prompt to *you*.

One rule governs all of this: the guard may only tighten, never loosen. It never emits "allow", because
"allow" would bypass your own permission settings on the strength of the guard's own string matching. "We
have no objection" is spelled *stay silent*, never *allow*. The guard's job is to add caution, never to
remove a protection you configured.

A `deny` is cheap: it is handled by the agent and costs you no attention. An `ask` is expensive: it spends
your attention and carries a small risk that a tired human approves something they should not. So the design
prefers to turn a known-safe rewrite into a `deny` (telling the agent "use the typed flag instead") rather
than an `ask`.

### The allowlist holds syntax; the tool holds meaning

Your permission allowlist matches the command *string*. That means it can enforce syntactic rules ("no
pipes", "no `&&`") but it cannot enforce a constraint like "stay inside this directory". Path resolution,
including `..` and symlinks, happens *after* the match, at a filesystem layer the allowlist never sees, so a
path can walk straight out of any prefix you tried to pin.

The consequence shapes the whole toolbox: a semantic constraint has to live in the *tool*, which works with
resolved paths, not in the allowlist, which only sees text. This is the real reason it is safe to allowlist
`tt git`: not because its command string is harmless, but because git itself refuses to touch files outside
the worktree. Put each check where the facts to evaluate it actually are.

### The human needs guards too

The weakest link in the whole system is the "allow, and do not ask again" option. It does not approve one
command; it permanently disarms the guard for every command of that shape, and it is offered to the person
most likely to be tired. Guard coverage only ever ratchets down, because nothing re-arms it.

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

- **A deliberately crafted attack.** The guard is built to catch the agent's own careless reflexes, not an
  adversary who hides a dangerous shape inside quotes. It is not an adversarial sandbox.
- **A compromised machine or account.** If the box or the credentials are already owned, this model does not
  save you.
- **Supply-chain compromise of a dependency you approve.** Approving a malicious `//> using dep` is outside
  what the guard can see.

Naming these keeps "safe by design" an honest claim rather than a slogan.

## Go deeper

- `research/theory/genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md` for the founding
  argument, the three pillars in full, and the discriminating criterion (how to hold a firm ethical floor
  without becoming either paternalistic or easy to manipulate).
- `docs/foundations.md` for the glossary: BHH, BadGoals, confirmation fatigue, rot.
- `tools/guardcheck.scala` for the guard itself, the mechanism behind this page.
- `skills/avoid-guard-stall/SKILL.md` for the agent-side operational guidance.
