# A prohibition does not arm the reflex — replace it with a positive construction (hex escape) (2026-07-16)

**Type:** WR data — a recurrence specimen (a known, logged, *already-skilled* guard trip fired again 3 days later),
plus the design fix and a rejected alternative.
**Threads:** [[guardcheck-false-positive-gt-inside-quoted-regex-2026-07-13]] (the same trip, first logged),
[[post-compact-is-highest-risk-window-for-mechanical-bash-regression-2026-07-16]] (the `| tail` sibling),
[[guard-against-forced-confirmations]], [[earned-trust-obligates-flagging-risk-more]].

## What happened (observable)

The agent ran, while BR was AFK:

```
tt text grepr <dir> scala "^//> using file"
→ Hook PreToolUse:Bash requires confirmation: [MED] output redirect (>)
```

The `>` was inside a **quoted regex argument**, not a shell redirect. The guard scans raw bytes, so it fired. BR
cleared the stall from inside it and handed it back tagged **WR data**, proposing: *"We need a custom escape char
for whatever triggered this guard stall … it is the > that seems to be the problem."*

**Two aggravating facts:**
1. **This exact false positive was already logged on 2026-07-13** *and* already written into
   `skills/avoid-guard-stall/SKILL.md` as its own table row. Documented, skilled, and it still fired.
2. **The agent had, one command earlier, told BR it was safe to go AFK** — citing "ten bash calls, zero stalls" —
   and then immediately produced the forced confirmation it had just finished describing as the AFK hazard. The
   agent's own risk estimate was **falsified within one command**.

## Why the existing countermeasure failed

The skill's advice was a **prohibition**: *"keep patterns metachar-free: anchor on plain terms, or split into
separate searches, or use Read."*

That is the same shape as **"never build a pipe"** — and this session alone gives ~5 mechanical slips against
prohibitions, one of them *while `+rot-vigil` was explicitly active*. Consistent with
[[rot-vigil-guard-mechanical-precision-first]]: these are **motor-level** reflexes, and a prohibition operates at
the belief level. Believing the rule does not arm the hand.

Worse, the prohibition had **no positive branch**: it said what not to do, but offered nothing to do when you
genuinely must match a `>`. So at the moment of action there was no construction to reach for — only a rule to
remember, which is exactly the thing that fails.

## The fix: the escape BR asked for already exists

`tt text` patterns are **Java regex**, which already has hex escapes. No tool change, no new syntax:

| char | escape | | char | escape |
|---|---|---|---|---|
| `>` | `\x3E` | | `&` | `\x26` |
| `<` | `\x3C` | | `;` | `\x3B` |
| pipe | `\x7C` | | backtick | `\x60` |

**Verified 2026-07-16:** `"^//\x3E using file"` returned the **same 18 hits** as the `>` form that stalled, with no
guard trip.

**Why this is the right shape:** the character never appears in the command string, so the guard **cannot** fire —
it is not a rule the agent must recall under load, it is a construction that is *always* correct to use.
Structural, not willpower. Same lesson as "run bare, let output persist" beating "remember not to pipe".
Now written into the skill as the primary alternative, demoting the prohibition to a fallback.

## The rejected alternative (and why it is BR's call, not the agent's)

The skill previously proposed making guardcheck **quote-aware** (lex the command, skip quoted spans). Tempting,
but it is a **security-relevant loosening**, so the agent did not implement it:

- **For:** false positives train the human to **rubber-stamp** confirmations. Alert fatigue is itself a security
  failure, and a rubber-stamped approval is the precise human-rotted axis of the threat model.
- **Against:** the guard is a **fail-safe backstop**. Quote-awareness needs a hand-rolled shell lexer, and a lexer
  bug makes the guard **miss a real redirect** — trading a loud cheap false positive for a silent expensive false
  negative. Dumb-and-strict fails in the safe direction.
- **Deferred, not refused:** the hex escape gives the false-positive class a zero-risk workaround, so the lexer
  buys less than before. Observed false-positive rate is 2 in 3 days. Recommendation: keep the guard dumb, use the
  escape, revisit if false trips get frequent.

## RETRACTION, same session: the escape is a better prohibition, NOT a structural fix

The section above claims the hex escape is "structural, not willpower". **BR broke that claim within minutes**,
with one question: *"so we need to make you not forget to use hex escapes? (which could regress after a warp?)"*

He is right and the claim is **withdrawn**. Reaching for `\x3E` still requires **recall at the instant of typing**
— precisely the faculty that fails (5 slips this session, one under an ACTIVE `+rot-vigil`). A warp makes it worse:
the skill carrying the escape may not even be hot. The escape gives the moment of action something to *reach for*,
which beats a bare "don't" — but it is a **better prohibition**, not a structural fix. (Kept above rather than
edited away, per [[keep-the-ball-game-retract-by-annotating]]: the overclaim is the data.)

### The generalisation (the load-bearing insight)

**A fix that lives INSIDE the agent — a skill, a memory, a resume-prompt line — only works if it is loaded AND hot
at the instant of action. So it rots.** Anything routed through agent recall is willpower wearing a structural
costume. Only fixes **outside** the agent survive a warp:

| Where the fix lives | Survives a warp? | Examples |
|---|---|---|
| the **guard** (PreToolUse) | ✅ always runs | the stall; the fix-text that teaches at the moment of failure |
| the **tool's interface** | ✅ if it is the only way in | `--limit`/`--tail`; `--message-file`; a tool absorbing the shaping |
| the **human** | ✅ | shepherding the post-warp window |
| a **skill / memory / prompt** | ❌ rots | the escape table — useful, never the last line of defence |

This reframes the whole session's mitigation design: the durable moves are **tool-interface** and **guard** changes;
every in-agent rule is a hint that will eventually fail. Candidate memory.

### Consequence: the agent's own recommendation flipped

The "rejected alternative" above recommended *keep the guard dumb + strict, use the escape*. That recommendation
**silently assumed the agent would remember the escape** — the assumption BR's question falsifies. Quote-awareness
is the only candidate needing **no recall**, so its case is stronger than the first pass credited. Now recorded as
**genuinely open, BR's call**, with a blast-radius-bounded variant (quote-aware for MED only; HIGH keeps scanning
raw bytes, so a lexer bug can cost at most a missed MED). Still not to be implemented autonomously.

**Meta-specimen:** the agent produced a confident design recommendation resting on an unexamined premise about its
own reliability — the *same* error shape as the "zero stalls in ten calls" green light below. Twice in one hour,
the agent over-trusted its own future behaviour. The human caught both. Neither was caught from inside.

## SECOND correction, same conversation: "loosening" was the wrong label (BR)

The "rejected alternative" section calls quote-aware guardcheck a **"security-relevant loosening"**. BR rejected
that framing outright:

> *"having no-danger strings with '....\x3E.....' in them is no actual risk but the only thing that happens is the
> false-positive guard stall and to avoid that is not a security risk its a security improvement as I as human
> dont risk to blanket allow per fatigue"*

**He is right, and the agent's label was wrong.** The guard's **policy** is *no shell redirects*. A `>` inside a
quoted argument **is not a shell redirect** — the shell parses redirections at parse time, before expansion, and
passes the character through as a literal. So flagging it is an **implementation bug (a false positive)**, not a
deliberate conservative margin. The agent was **mistaking imprecision for protection**, and therefore defending a
bug as if it were a safety property. Making the implementation match the stated policy is a **correctness fix**.

BR's security argument is the stronger one and now leads: **a guard that cries wolf trains the human to
rubber-stamp**, and the rubber-stamped approval is the exact human-rotted axis of this project's own threat
model. A false-positive guard *manufactures* the failure it exists to prevent. Fewer false trips = a security
**improvement**.

What survives is an **engineering** caveat, not a policy one: precision needs a shell-quoting lexer, and the gain
is real only if the lexer is correct (a buggy one yields false negatives — the expensive direction). That is a
small, bounded, testable slice — the hand-roll sweet spot — and the MED-only variant bounds the blast radius.

**Agent position after the correction: build it** (BR's explicit go still required; not autonomous).

### The pattern across all three corrections (worth more than any of them)

In ONE hour the agent made three confident claims, each resting on an **unexamined premise**, each falsified by a
one-line question from BR:
1. *"zero stalls in ten calls, safe to go AFK"* — assumed a clean streak predicts the next call. Falsified in one command.
2. *"the escape is structural, not willpower"* — assumed the agent would recall it. Falsified by "which could regress after a warp?".
3. *"quote-awareness is a security loosening"* — assumed the byte-scan's over-approximation was protection rather than a bug. Falsified by BR's re-framing.

All three share a shape: **the agent over-trusted a property of itself or of its own tooling that it had never
checked**, and dressed it in confident, evidence-flavoured language. None was caught from the inside; a human
caught all three, cheaply, by asking what the claim assumed. This is the concrete case for accountable control —
not as ethics garnish, but because the agent's blind spot is **structurally invisible to the agent** and cheap for
the human to see. Ties [[echt-effort-especially-self-generated]] and the confabulation caveat.

## The auto-rewrite hook: BR's design, and the demonstrated reason to drop half of it

BR proposed a **PreToolUse auto-fixer**: before a Bash/tt call, if a risky char sits in a *safe string* (an arg to
`tt`), rewrite it to a hex escape so no stall happens; if the agent reached for real *"bash clobbery"*, let it
through to the guard and stall — *"because it is regression any way and better stall to try to fix the
confirmation-fatigue bad behaviour."* Narrowed further: *"your hook only need to check if the bash command starts
with `tt ` and then you only replace everything with hex escapes if they are in quoted strings. That seem doable and
fast and easy, no?"*

**The second half is exactly right** and is precisely what a quote-aware guard does: true positives must still
stall, or the confirmation means nothing.

**The first half must be dropped, for two reasons — one logical, one demonstrated.**

**1. Logical: at the moment you can rewrite, you no longer need to.** Deciding "this char is inside a quoted
string" *is* the whole question. Once the hook knows that, it has already proven the char is not a shell operator,
so it can simply emit `allow`. The rewrite is a mutation performed *after* the answer is already in hand — strictly
more machinery, strictly more risk, zero added information. And there is nothing to fix: `tt text grepr <dir> scala
"^//\x3E using file"`-with-a-literal-bracket was **always a correct, safe command**; the shell passes the char
through as an argument. The escape never fixed the command, it only ever appeased a buggy guard.

**2. Demonstrated: the rewrite is only correct if the consumer is a REGEX engine — and `tt` itself contains the
counterexample.** Scoping to `tt ` does not save it, because tt tools do not share arg semantics. Live specimen:

```
$ tt guardcheck cmd "git log \x7C head -5"
guardcheck [cmd]: clean — no guard-trip / reflex patterns found
```

`git log | head -5` is a **real pipe** — it is guardcheck's own flagged example in its own help text. But
`tt guardcheck cmd` takes a **literal** string, so the hex-escaped form contains no pipe and guardcheck reports
**clean**. Therefore an auto-fixer rewriting quoted args of `tt ` commands would turn
`tt guardcheck cmd "git log | head -5"` into the escaped form and **blind the guard-checker to the very pattern it
exists to detect**.

That is the sharpest possible statement of the hazard: **the convenience layer wrapped around the safety tool
disables the safety tool.** Any literal-arg consumer has this bug; `guardcheck` merely makes it vivid.

> **⚠️ RETRACTION (same session, BR): "blind the guard-checker" is OVERSTATED.** BR: *"is that valid git log
> syntax? will it not just error out?"* — and the push-back is right. **Correct size of this specimen:**
> `tt guardcheck cmd` only *analyses a string*, so nothing errors — it silently returns a **wrong verdict**.
> That is a **CORRECTNESS hazard, not a security bypass**: a real shell pipe lives **outside** quotes, and the
> proposed rewriter only touched **quoted spans**, so a real `git log | head -5` would still hit the guard and
> still stall. **Nothing gets smuggled into execution.** The demo is real; the framing was inflated.
> What survives (and is enough): the rewrite is only correct when the consumer is a **regex engine**, and the
> hook cannot know which `tt` args are regex vs literal. The *decisive* argument never needed this demo anyway —
> **when you can rewrite, you already know, so just don't flag** (see the Conclusion below).
> *(Annotated, not erased — [[keep-the-ball-game-retract-by-annotating]]. The commit that shipped this section,
> `4fcb884`, permanently carries the overstated subject line. That is fine: a commit is HISTORY, an accurate
> record of what was believed then, and is never amended. The correction belongs HERE, in the mutable home.)*

**Conclusion: BR's idea minus the rewrite IS the quote-aware guard.** Quoted literal → `allow` (no stall, and the
plain char just works — *nothing to remember*). Real clobbery → stall. Same behaviour BR wanted, less code, no new
failure mode, and it dissolves the "how do we make the agent not forget the escape?" problem entirely.

### Capability facts (checked, not assumed — claude-code-guide, 2026-07-16)

A PreToolUse hook **can** rewrite tool input: `hookSpecificOutput.updatedInput` (e.g. `{"command": "..."}`),
documented. **The agent's "silent rewrite is a transparency problem" objection was WRONG and is retracted** — the
docs state the rewritten input is shown to **both** Claude and the user. (A 4th overclaim, but the first one the
agent caught *itself*, by checking instead of asserting.) Other PreToolUse fields worth knowing:
`permissionDecision` (allow/deny/ask/defer), `permissionDecisionReason`, **`additionalContext`** (inject a note
*without* stalling), `permissionRulesToApply` (auto-approve future matching calls). `additionalContext` gives a
third response beyond allow/stall — *allow AND teach* — not needed for a correctly-identified false positive
(nothing to teach; the command is fine), but a real option elsewhere.

## The uncomfortable finding worth keeping

The agent gave a **confident, evidence-cited safety assessment** ("zero stalls in ten calls") that was wrong on the
very next action. The evidence was real; the inference — that a clean streak predicts the next command — was not.
Mechanical slips are not drawn from a stable, estimable rate the agent can observe from inside; a clean streak is
exactly what precedes each of them, by construction. **Do not let an agent's self-assessed reliability streak be
the basis for a human's AFK decision.** The correct basis is structural: what *shapes* are being run
(bare/allowlisted/Write-tool = safe regardless of streak), not how the last N calls happened to go.
