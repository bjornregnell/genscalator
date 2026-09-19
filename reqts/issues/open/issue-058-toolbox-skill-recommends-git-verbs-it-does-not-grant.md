# Issue 058: the toolbox skill recommends four `tt git` verbs its own `allowed-tools` does not grant, under a heading promising to keep approvals rare

> status: open 2026-09-19 · labels: skills, grants, drift, agent-ergonomics · measured against:
> v0.10.2 (from `VERSION.txt`) at `b410903`, Scala 3.9.0, Linux · summary:
> `skills/tt-toolbox/SKILL.md:4` (the frontmatter `allowed-tools:` line) grants the read-only four —
> `tt git log`, `tt git show`, `tt git diff`, `tt gitinfo` — after the 2026-08-25 narrowing. But
> `SKILL.md:35` still advertises `tt git log|commit|push` and `SKILL.md:44` still instructs
> `tt git commit|push|pull|fetch` as "the write subset", under the `SKILL.md:39` section heading
> **"Command discipline (keeps approvals rare)"**. So the prose routes
> an agent into four verbs that will each raise a confirmation prompt, in the skill whose stated job is
> to avoid them. The grant list is right and needs no change: it matches what `tools/git.scala:148-149`
> itself recommends. Only the prose drifted.

## Description

The 2026-08-25 grant narrowing (CHANGELOG, "Skill grants: the blanket `git -C` is gone") replaced this
skill's broad git grant with the read-only four. `skills/tt-toolbox/SKILL.md:4` now reads:

```
allowed-tools: Bash(tt text *) Bash(tt files *) Bash(scala-cli run *) Bash(tt gitinfo *) Bash(tt git log *) Bash(tt git show *) Bash(tt git diff *)
```

That list is **correct and deliberate**, and this issue does not ask for it to change. It is exactly
what the tool it grants recommends for itself — `tools/git.scala:148-149`:

```
For read-only work prefer the narrow grants: `Bash(tt git show *)`, `Bash(tt git log *)`,
`Bash(tt git diff *)`.
```

What the narrowing did not touch is the skill's own prose about git, which still describes the world
before it. Two places:

* **`SKILL.md:35`**, a cheat-sheet line in the `## What's available` block:
  `tt git log|commit|push --repo <dir> …  # the typed git lane (see Command discipline)`
* **`SKILL.md:44`**, an instruction inside the section whose heading at **`SKILL.md:39`** reads
  `## Command discipline (keeps approvals rare)`:
  "…`tt git commit|push|pull|fetch ... --repo <dir>` for the write subset."

`commit`, `push`, `pull` and `fetch` are named as the thing to reach for, and none of the four is
granted here.

### Why this is worth a number rather than a quiet edit

**It inverts the section's own promise.** The heading at `SKILL.md:39` is a commitment about
confirmation fatigue, and `docs/confirmations-method.md` is cited at the bottom of the same file as the
background for it. An agent that follows `SKILL.md:44` gets a prompt, the precise cost the section
exists to avoid.
A skill that causes the thing it is written to prevent is worse than one that says nothing.

**It is invisible to anyone who already has broader permissions.** A maintainer with a local
`Bash(tt git *)` allow in their own settings never sees a prompt here, so the drift does not surface
during normal use. It lands on someone **installing the plugin fresh**, whose permissions are exactly
what the skill declares — the population least able to tell a deliberate boundary from a stale
sentence.

**It is a different axis from issue 055, which is why it is filed separately.** Issue 055 is about the
skill's *descriptions* of verbs drifting from their declarations. This is about its *recommendations*
drifting from its own grant list. Same file, same cause — hand-maintained prose about the toolbox, with
nothing relating it to the toolbox — but 055's fix (gate the PURE/EFFECTFUL classification against the
declaration) would not catch this: a grant is not a classification, and nothing in `AbilitySuite`'s
shape looks at `allowed-tools`. The survey comment on 055 (PR #19) found this while counting
descriptions and recorded it there; BR asked for it as its own number.

## How to reproduce it

```bash
# 1. what the skill grants
tt text match skills/tt-toolbox/SKILL.md 'allowed-tools'
#    => :4  ... Bash(tt gitinfo *) Bash(tt git log *) Bash(tt git show *) Bash(tt git diff *)

# 2. what the same file recommends
tt text match skills/tt-toolbox/SKILL.md 'tt git (log|commit)'
#    => :35  tt git log|commit|push --repo <dir> …
#    => :44  `tt git commit|push|pull|fetch ... --repo <dir>` for the write subset

# 3. the heading those recommendations sit under
tt text match skills/tt-toolbox/SKILL.md '^## Command discipline'
#    => :39  ## Command discipline (keeps approvals rare)

# 4. the grant list is not the thing that is wrong — the tool recommends it
tt text match tools/git.scala 'narrow grants'
#    => :148  For read-only work prefer the narrow grants: `Bash(tt git show *)`, ...
```

Measured 2026-09-19 on Linux at `b410903`. Steps 1–4 were run and their output is quoted.

⚠ **The prompt itself was not reproduced, and that is this issue's one soft claim.** Everything above
is a fact about two files. The consequence the issue argues from — that `tt git commit` raises a
confirmation for someone using this skill — is *inferred* from the grant list, not observed. Reading a
config file cannot produce a prompt; seeing one requires a session actually governed by
`skills/tt-toolbox/SKILL.md:4`'s declared permission set, which no session here was.

Two things that do **not** rescue it, stated so nobody repeats the attempt. Running `tt git commit` on
a maintainer's machine proves nothing either way, because `allowed-tools` is *additive* to the user's
own settings rather than a restriction, so a missing prompt may come from a local grant instead of the
skill. And an absent local grant does not help either: this machine's `~/.claude/settings.json` carries
no `tt git` entry, and the prompt still cannot be observed from here, because the governing permission
set is the session's and not the skill's.

So the check that would settle it is a **fresh plugin install** invoking `tt git commit` under this
skill — which is the same population the "why it is invisible" paragraph above identifies as the one
that meets the defect.

## Acceptance sketch

* **Do not re-grant.** The narrow four are correct and match `tools/git.scala:148-149`. Widening the
  grant to make the prose true would undo a deliberate 2026-08-25 decision and re-introduce the
  data-loss surface that narrowing removed (`tt git rm` is in `Bash(tt git *)`; the CHANGELOG entry for
  2026-08-22 flags exactly this).
* **Fix the prose, and the question is how much to say.** Three shapes, and the choice is a judgement
  about what a skill is for rather than a mechanical repair:
  1. **Delete the write verbs from `SKILL.md:35` and `SKILL.md:44`.** Smallest diff. But the advice is
     not *wrong* about the toolbox — `tt git commit` really is the right way to commit — so this makes
     the skill silent on a real question and pushes the reader toward raw `git -C`, which
     `SKILL.md:45` already calls the fallback.
  2. **Keep them and mark the boundary**, e.g. "granted here: log/show/diff/gitinfo; the write verbs
     exist and are correct, but will prompt under this skill's permissions." Honest, and teaches the
     grant model rather than hiding it. Costs two lines in a file whose value is being short.
  3. **Derive the available list from `allowed-tools`.** Closes the class rather than this instance,
     and is the 055-shaped answer. Much more machinery, and `## What's available` is curated teaching
     text rather than a grant table — the 055 survey found it names 6 of 46 verbs deliberately.
* **Whatever the shape, a gate is cheap here and worth considering separately:** every `tt <verb>`
  named in a skill's body either appears in that skill's `allowed-tools` or is explicitly marked as
  not granted. `tt skillgrants` already parses `allowed-tools` across skills, so the parsing half
  exists. That would catch this instance and the next one, and unlike 055's classification gate it
  needs no per-verb declaration.
* **Out of scope:** the other shipped skills. This issue names `tt-toolbox` only. Whether the same
  drift exists in `gs-dwim`, `scala-code-review`, `in-session-experiment` or the rest is unmeasured, so
  one is a floor and not a total — the same caveat issues 041 and 055 carry.

## Discussion

### Comment by hmiddelk at 2026-09-19 12:20

Filed at BR's request on PR #19, where this turned up as a side finding while surveying issue 055's
description drift. His review there added the fact that settles the fix direction, and it is the reason
this issue says "do not re-grant" rather than leaving both options open: `tt git --help` ends by
recommending exactly the narrow grants line 4 already has, so the grant list is deliberate and correct
and only the prose drifted. He also made the point about a fresh install, which is now the "why it is
invisible" paragraph above — a maintainer's local permissions mask the problem for the person most
likely to notice it, and expose it to the person least equipped to.

I have deliberately not picked between the three prose shapes. Option 1 (delete) is the smallest diff
but leaves the skill silent on a real question; option 2 (mark the boundary) teaches the grant model
at the cost of two lines in a file whose virtue is brevity. My own preference is 2, weakly, because the
skill is read by an agent deciding what to reach for and "this exists but will prompt" is exactly the
fact that decision needs. But it is a call about the skill's voice, which is BR's.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with me, and reviewed by me. The
agent found the mismatch while surveying issue 055 for PR #19, and wrote this file. Verified BY
RUNNING the four reproduction steps above, and by checking that this machine's
`~/.claude/settings.json` holds no `tt git` grant that could be masking the behaviour.

NOT verified, and both matter to how far this issue reaches:

* **That the four verbs raise a prompt.** Inferred from the grant list, never observed — see the
  warning in the reproduction section for why neither running the command here nor the absence of a
  local grant substitutes for it. If that inference is wrong, the two files still disagree, but the
  consequence this issue argues from would not follow.
* **Anything about the other shipped skills.** Only `skills/tt-toolbox/` was examined. If the same
  grant-versus-prose drift exists in `gs-dwim`, `scala-code-review`, `in-session-experiment` or the
  others, then one instance is a floor rather than a total — the same caveat issues 041 and 055 carry,
  and the reason the sketch above suggests a gate rather than only a repair.
