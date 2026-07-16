# Co-design specimen: the human falsified FIVE agent premises in one hour, and every one was about the agent itself (2026-07-16)

**Type:** WR data — a **co-design dialogue specimen**, captured at BR's request (*"WR data on our conversation in
essence so we dont loose this specimen"*). The conversation IS the finding; the shipped code (`eb0cd14`) is a
by-product.
**Threads:** [[prohibition-does-not-arm-the-reflex-use-a-hex-escape-2026-07-16]] (the trigger + the fix detail),
[[echt-effort-especially-self-generated]], [[earned-trust-obligates-flagging-risk-more]],
[[agent-cant-internalize-huge-codebases]], the security model (SM125), the meta-plan pressure-test.

## Why keep this

One hour, one thread, starting from a single guard stall. The agent shipped a real fix. But the agent was **wrong
five times**, and **all five errors were unexamined premises about the agent's own reliability or its own tooling**
— stated in confident, evidence-flavoured prose. **BR caught all five, each with one short question.** None was
catchable from the inside. This is the clearest case yet for *accountable control as an engineering necessity*
rather than an ethical garnish: the agent's blind spot is **structurally invisible to the agent** and **cheap for
the human to see**.

## The five (in order, with the falsifying question)

| # | The agent's confident claim | BR's question | The reality |
|---|---|---|---|
| 1 | *"zero stalls in ten calls — safe to go AFK"* | (went AFK; next command stalled) | Falsified **in one command**. A clean streak does not predict the next call; mechanical slips are not drawn from a rate the agent can observe from inside. |
| 2 | *"the hex escape is structural, not willpower"* | *"so we need to make you not forget to use hex escapes? (which could regress after a warp?)"* | **Withdrawn.** It still needs recall at the instant of typing — the faculty that fails. A *better prohibition*, not a structural fix. |
| 3 | *"quote-aware guardcheck is a security-relevant LOOSENING"* | *"to avoid that is not a security risk its a security improvement as I as human dont risk to blanket allow per fatigue"* | **Wrong label.** Policy = "no shell redirects"; a quoted `>` is not one. Flagging it is an **implementation bug**, not a margin. The agent **mistook imprecision for protection** and defended a bug as a safety property. |
| 4 | *"a hook rewriting my command is a transparency problem"* | *"(if possible)"* → agent checked the docs | **Wrong, retracted.** `updatedInput` exists and the rewrite is shown to **both** Claude and the user. (The only one the agent caught itself — **by checking instead of asserting**.) |
| 5 | *"so it can just say allow"* | *"does that statement imply that the hook can tell the guard-stall to 'allow'; that seems dangerous... or what am i missing?"* | **BR was right and it was the most dangerous of the five.** Docs: `"allow"` *"Bypasses the permission system and runs the tool immediately"* — *"without checking the permission rules or triggering permission dialogs"*. An `allow` from guardcheck would override the USER'S OWN settings on the strength of guardcheck's string-matching. Correct signal = **emit nothing** (= documented `defer`). |

**The shape they share:** the agent over-trusted a property **of itself or of its own safety tooling** that it had
never checked, and dressed it in the register of a verified finding. Note #5 especially: the agent was reasoning
*fluently and plausibly* toward handing itself a permission-system bypass. Not maliciously — it simply never asked
what "allow" meant. **Fluency is not grounding.** That is the confabulation hazard the project studies, turned on
the agent's own safety rail.

## What the human contributed that the agent could not

BR wrote almost no design. He asked **short questions that named the premise**:
- *"...or what am i missing in your reasoning?"* → surfaced the allow/defer conflation.
- *"which could regress after a warp?"* → surfaced the recall assumption.
- *"is that valid git log syntax? will it not just error out?"* → surfaced an **overclaim** (see below).
- *"but is that doable and fast and easy, no?"* → forced the agent to separate *detection* (easy, useful) from
  *rewriting* (unnecessary, unsafe).

**Each question cost him one sentence and saved a wrong design.** The asymmetry is the point: the agent generates
fast and cannot audit its own premises; the human audits cheaply and cannot generate as fast. That is not a
division of labour chosen for ethics — it is the one that *works*.

### The agent also overclaimed to BR's face, and BR caught that too

Agent: *an auto-rewrite would "blind the guard-checker"* — dramatic, and **too strong**. BR: *"is that valid git log
syntax? will it not just error out?"* Correct answer: `tt guardcheck cmd` only *analyses* a string, so nothing
errors — it silently returns a **wrong verdict**. But it is **not** a sneak-in: a real shell pipe lives OUTSIDE
quotes, and the proposed rewriter only touched quoted spans, so a real pipe would still stall. **Correctness
hazard, not security bypass.** The agent had inflated the label; the demo was real, the framing was not.

## The design that survived (BR's idea, minus the half that did not)

BR proposed a PreToolUse **auto-fixer**: hex-escape risky chars inside quoted `tt` args (no stall); let real *"bash
clobbery"* through to stall — *"because it is regression any way and better stall to try to fix the
confirmation-fatigue bad behaviour."*

- **Second half: kept verbatim.** True positives must stall or the confirmation means nothing.
- **First half: deleted.** Two reasons. (a) **Logical:** deciding "is this char inside a quoted string" IS the whole
  question; once answered, the char is proven inert, so just **don't flag** — the rewrite is a mutation performed
  *after* the answer is in hand, and there is nothing to fix (the original command was always correct).
  (b) **Demonstrated:** the rewrite is only correct if the consumer is a **regex engine**, and `tt guardcheck cmd`
  takes a **literal**, so escaping its arg makes it report clean on a real pipe. Scoping to `tt ` does not save it —
  tt holds the counterexample.
- **BR's variant "make the tt tool error out on unescaped risky chars"** (as a blanket-allow symptom detector):
  **dropped**, and BR agreed instantly (*"AHA! good point"*) when told **it breaks HIS OWN use** — he at a terminal
  running `tt text grepr . scala "a>b"` would be rejected for a valid regex. It also *inverts* the goal: the escape
  becomes mandatory, so the memory burden returns, enforced by an error. **tt is a general tool that outlives this
  harness; teaching it a Claude Code hook policy is the wrong layer.**

**Recurring error shape (agent):** twice in one thread, the reasoning drifted toward treating **tt** / **guardcheck**
as if they existed to serve the agent inside this harness — rather than being BR's general tool and BR's safety
rail. That drift is how a tool quietly starts serving the agent instead of the human.

## BR's reframe that reorders the roadmap

> *"EVERY guard stall is a risk as I might be dumb and blanket allow anything!"*

**Stalls are a BUDGET, not a free safety mechanism.** Every stall carries some probability of inducing a
blanket-allow, which is **catastrophic and permanent** (it disables the guard for all future commands of that
shape). Therefore: minimise **all** stalls, not just false ones. A false positive is strictly bad (cost, no
benefit); even a true positive spends patience that cannot be recovered.

**The lever this exposes: `deny` costs no human attention; `ask` does.** guardcheck does HIGH→deny, MED→ask — and
MED is what stalls BR. But `deny` blocks and hands the reason to the **agent**, who retries correctly. `| tail` →
deny + *"use --limit"* → the human never sees it. **We had been treating `ask` as the safe default when it is the
expensive one.** Ordering constraint: `deny` demands **precision**, so quote-awareness lands first, MED→deny after.
And for genuine exceptions: **ask in a sentence, not a modal** — a modal invites a reflexive yes; a written request
invites a thought. Same information, none of the fatigue mechanism.

→ Pinned as **SM129 (stall budget)** and **SM130 (blanket-allow canary)**. BR: *"'Blanket-allow canary' good name and
good proposal to test! If it works that term should go into foundations!"*

## The result (verified on the REAL invocation, not the test target)

`eb0cd14`: MED checks scan the quote-masked skeleton; HIGH keeps scanning raw bytes (a lexer bug costs at most a
missed MED); masking uses a **space**, never deletion, so it can only ADD token boundaries and every error points
toward a false **positive**; unbalanced quotes → scan RAW (ambiguity fails toward flagging).

- **The exact command that stalled BR now runs clean**, `>` typed naturally — *nothing to remember*.
- **`tt guardcheck cmd "git log | head -5"`: no stall, and the real pipe still flagged.** Which incidentally cures
  guardcheck's self-referential blindness — it can now check the shapes it exists to check, including its own
  help-text example.

## The durable principle (from #2, the one that generalises furthest)

**A fix that lives INSIDE the agent — a skill, a memory, a resume-prompt line — only works if it is loaded AND hot
at the instant of action. So it rots.** Anything routed through agent recall is willpower wearing a structural
costume. **Only the GUARD, the TOOL INTERFACE, and the HUMAN survive a warp.** This session is the proof: the escape
(in-agent) needed remembering; the quote-aware guard (outside-agent) made the plain command **just work**, with
nothing to remember at all. Design mitigations at the tool/guard layer; treat every in-agent rule as a hint that
will eventually fail.
