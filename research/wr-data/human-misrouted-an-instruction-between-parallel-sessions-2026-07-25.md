# Specimen: the HUMAN misrouted an instruction between parallel sessions (2026-07-25 ~16:0x)

Almost every specimen in this directory is an agent failure. This one is not, and that is why it is
worth filing: the same substrate that makes an agent's state hard to see also makes the HUMAN's
addressing hard to get right, and the second failure has had no instrument at all.

## The event

BR runs concurrent Claude Code sessions (this one, and a "prontopop" SPA session). He typed into this
session:

> "see my commits and check out the TODO:s WDYT? if you agree with them being not to ambiguous go
> update/re-engineer the PRD.md and propose a brief high-level design of the TODOs here and then await my
> instructions"

The agent looked for the commits, found none: `genscalator`'s five most recent commits were all its own,
`introprog`'s HEAD was its own, and a fetch of both changed nothing. Before it reported the mismatch, BR
sent:

> "sorry WRONG session (we need the session name chip)"

He then sent a second message telling the agent to ignore the first, which it already had.

## Cost, and why it was small this time

Four tool calls: two `tt gitinfo`, two `git fetch`. Both fetches are read-only, and no PRD edit was
started because the agent was still establishing what "my commits" referred to. So the incident cost
about a minute.

**But look at what was one step away.** The instruction was *"go update/re-engineer the PRD.md"*. Had the
agent been less literal about first locating the commits — or had this repo happened to contain plausible
recent commits with TODOs in them — it would have begun re-engineering the wrong project's requirements
document, in the wrong repository, on the strength of an instruction meant for a different session. The
near-miss is the finding; the actual cost is not.

## Why the agent could not have caught it

It has no way to know which terminal it is in, and nothing to compare an instruction against. An agent
cannot detect a misaddressed message, because a misaddressed message is indistinguishable from a
surprising one: "see my commits" when there are none reads exactly like a human misremembering, or like
the agent being out of date on a fetch. The agent's correct move was what it did — go and check the
substrate rather than assume — but that only converts a wrong action into a wasted minute. It cannot
prevent the class.

The one asymmetry worth noting: the agent's own defence was *verifying before acting*. Had it trusted the
premise and started work, the misroute would have produced real damage. That is the same
verify-don't-assume discipline that this project applies to inherited claims, working here on a claim
inherited from the human in the same turn.

## The instrument that would have caught it, and it is already pinned

BR's own diagnosis, in the same breath as the apology: **"we need the session name chip"** — SM208's
addendum, proposed by him a few hours earlier, where the statusline shows `Session: [[Name]] modes: …`
with the name in inverted white. He would have seen `prontopop` or `genscalator` in the line above the
prompt before pressing enter.

So this specimen does not motivate new work. It **upgrades an existing proposal from convenience to
correctness**: the chip was pitched as "it is irritating that modes are global when running parallel
sessions", i.e. an annoyance about state display. This shows the same missing affordance also lets a
human issue a repo-mutating instruction to the wrong agent. Those are different severities.

## BR's extension, which is a bigger hazard than the one above

BR, on reading this: *"a simple misplaced order in the wrong session could be a hazard — I could have
told one claude what another one should only know."*

That is a different and worse failure mode than a wrong action, and it deserves its own line rather than
being folded in. The incident above was **an instruction landing in the wrong context**. The hazard BR
names is **INFORMATION landing in the wrong context**, and it is concrete on this machine right now:

- this session's working repo is the **PUBLIC** genscalator, pushed to three remotes as routine;
- the same machine holds embargoed correspondence (the SIP/LAMP thread), copyrighted book material that
  must never leave the closed repo, and a private work repo.

Paste any of that into the wrong terminal and the agent does what it has done well all day: files it,
commits it, pushes it. **Confidential material carries no label an agent can read.** There is no
mechanism by which a helpful agent distinguishes "context for you" from "context you must never write
down", so the failure is silent and fast, and unlike a wrong edit it is not recoverable by reverting —
it is in git history on several mirrors within seconds.

Note that this is the SAME failure as the `printenv` leak the same day, seen from the other end. Both put
confidential content into a durable record that outlives the moment: once because the agent read too
broadly, once because the human addressed the wrong reader. The output side now has a tool (`tt env`,
guardcheck NOTE). **The input side has nothing.** That asymmetry is the finding.

Two things follow that are NOT solved by the session-name chip, and should not be claimed as solved:

1. The chip helps a human notice *which* agent they are addressing. It does nothing about *what that
   agent may be told* — a correctly-addressed session can still be handed embargoed text.
2. Anything already pasted is already in that session's transcript on disk, whatever happens next.

So the chip is necessary and not sufficient. A fuller answer would need something like a declared
confidentiality level per session, or a repo-visibility chip (`PUBLIC` in red would be the cheap,
high-value version, since the irreversible step is a push to a public remote), which is worth raising in
SM208 rather than assuming the name chip covers it.

## The generalisable point

The project's vocabulary is careful about whose state a mode describes (`modes-dont-say-whose-state`)
and about an agent verifying inherited claims. It has had nothing about **whose ATTENTION a message is
addressed to**. With one human and one session that question cannot arise. With one human and several
sessions it arises constantly and silently, and the human is the only party who can answer it — while
being the party with the least support for doing so, since every terminal looks identical.

Worth carrying into the SM208 build: the chip's value is not that it labels a session, but that it makes
the addressing question answerable *at the moment of typing*, which is the only moment it can be caught.

Ties SM208 (and its 2026-07-25 addendum), [[mode-chips-camelcase-vocabulary]],
`wr-data/modes-dont-say-whose-state-2026-07-17.md` (the same ambiguity, one layer down), the statusline.
