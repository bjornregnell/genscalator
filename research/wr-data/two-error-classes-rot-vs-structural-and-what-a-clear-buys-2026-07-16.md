# Two error classes — ROT-shaped vs STRUCTURAL — and what a context-clear actually buys (2026-07-16)

**Type:** WR data — an error taxonomy from a ~12-error session, plus its consequence for the proposed
**exit-then-clear dance**. Captured at BR's request.
**Threads:** [[co-design-specimen-human-falsified-five-agent-premises-in-one-hour-2026-07-16]] (the errors),
[[stalls-are-countable-8-all-MED-empirical-2026-07-16]], [[green-test-suite-hid-a-tool-broken-in-its-real-invocation-2026-07-16]],
SM127 (post-warp = peak mechanical-regression window), SM116/SM128 (`rot?`), [[exit-resume-dance]],
[[propose-compact-dance-at-trigger]].

## The trigger

BR, unprompted, after a long day: *"you seem to make a lot of errors (although many are caught and fixed to be
fair); I am thinking about an exit-then-clear-session dance; that would mean taking extra care to pin everything
important to durable substrate; WDYT?"*

His observation is **correct and well-grounded**, and the instrument agrees: `rot? 593k` — well past its 500k red
line (SM128's since-warp gauge, itself built earlier the same day).

## The count (honest, this session)

Five premises BR falsified in one hour (streak-predicts-next-call / escape-is-structural / quote-awareness-is-a-
loosening / rewrite-is-silent / "it can just say allow"), plus: the `| tail` regression (5th of the day), the `>`
guard stall, the MiniJson standalone-compile break, a non-existent object name (`Statusline` vs `StatuslineTool`),
a broken test assertion (`contains("5;")` matching inside a colour code), a flatly wrong claim to BR's face
("your guardcheck hook is in `~/.claude/settings.json`" — it is not), nearly asserting `skills/scala-platform`
did not exist (it does), and **three mode removals acknowledged in chat and never performed**.

**≈12 errors.** Most were caught. **One was caught by luck:** the stale mode file surfaced only because the agent
rendered the mode line *for an unrelated reason* — it had been silently displaying `afk`, `human-stress` and
`rot-vigil` as ACTIVE for hours, with no error, no stall, and no signal. Absent that accident, BR's own state
display would have kept lying indefinitely.

## The taxonomy (the load-bearing distinction)

The errors are **not one phenomenon**. They split cleanly, and the split changes what any remedy can buy:

| class | examples (this session) | cause | would it happen at rot ≈ 0? |
|---|---|---|---|
| **ROT-shaped** | wrong object name; broken assertion; stale mode file; the `\| tail` pipe; the `>` in a quoted pattern | mechanical/motor precision degrading under accumulated context | **No** — these track rot |
| **STRUCTURAL** | MiniJson (trusted the test target); "guardcheck is in the global settings" (asserted instead of reading); nearly "that skill doesn't exist" | the **METHOD** was wrong, independent of fatigue | **Yes** — a fresh agent makes these too |

**⇒ A clear (or a compact) fixes the first class and does NOTHING for the second.** The structural class is fixed
only by *method* changes — run the real invocation, read before asserting — i.e. by tooling and habit, not by
resetting context. Do not expect a warp to buy more than it gives.

Note the two classes even have different **detection** profiles: rot-shaped errors mostly get caught (a compiler,
a test, a guard, or BR), whereas the structural ones *pass every check* — the MiniJson break was **green** across
the whole suite, and the settings claim was fluent, confident prose. **Structural errors are the quieter class.**

## What the exit-then-clear dance actually buys

**BR's proposal:** exit → clear (not compact) → resume from durable substrate only.

**The strongest argument is NOT rot-reduction — it is that a clear is a FORCING FUNCTION for substrate quality.**
This session's own load-bearing finding: *only fixes OUTSIDE the agent survive a warp* (guard / tool interface /
human; skills+memory+prompt rot). **A clear is the maximal test of exactly that claim.** A compact lets us cheat —
the summary smuggles context forward, so we never discover whether the substrate was adequate; the papering-over
is invisible precisely because it works. If the work cannot survive total context loss, the substrate was
inadequate **and we would not have known**.

**Also note (SM127):** a compact does not even reliably reduce mechanical error — the `| tail` regression fired
*right after* a compact. A warp trades **rot** for **coldness**, and coldness has its own error signature
(base-model reflexes, re-deriving known facts). Clear maximises both sides of that trade.

## ★ BR's insight: the pinning trap (he had not thought of it either — and it is the crux)

> Agent: *"if we only pin when we're about to clear, we pin WHILE ROTTED."*
> BR: *"AHA! a very good insight! i did not think of that!"*

**The moment we most need good pinning is the moment we are least able to do it.** A pre-clear "pin everything"
scramble is executed by exactly the degraded agent the clear exists to reset — so the substrate the fresh agent
inherits is written at peak rot. That is a **self-defeating dance**.

**⇒ The dance only works if pinning is CONTINUOUS**, so that clearing is *already* cheap and requires no scramble.
Evidence it is achievable: this session pinned as it went — ~14 commits, 5 wr-data notes, 3 new SMs, a new
`SECURITY-MODEL.md`, two skills — so a clear *right now* would cost far less than one at 14:00. **Continuous
pinning is the prerequisite, not the dance's final step.** (Sharpens [[propose-compact-dance-at-trigger]] and
[[exit-resume-dance]]: the *preparation* is the whole thing; the warp itself is trivial.)

## The substrate-architecture gap this exposed (separate from clear-vs-compact)

Much of this session's substance went into **commit messages** — the 10s noise-floor finding, the rationale for
rejected designs. But **the resume path reads `PIN-BOARD.md` + `resume-prompt.md`, not `git log`.** So rich commit
messages are **archaeology, not carry-over**: anything living only in one is effectively gone after a clear until
someone digs.

**BR's correction, which resolves it:** *"BUT is not commit messages way back always just archeology hmmmm"* —
**yes, and by design, in every project.** That is not the bug. In a normal project a commit message describes a
*change* and the **code** carries the truth forward. The bug is narrower: **the agent used commit messages as a
findings STORE**, so a finding with no other home dies there.

**The disinformation risk, sharpened — the asymmetry is IMMUTABILITY:**
- A wr-data note can be **retracted by annotation** (done twice today: the recursion overclaim, the "loosening"
  mislabel — overclaim and correction sit together, per [[keep-the-ball-game-retract-by-annotating]]).
- A commit message **can never be corrected**. A confabulated one is permanently wrong, with no mechanism to
  carry its own retraction.
- But **"outdated" ≠ disinformation**: a stale commit message is an *accurate record of what we believed then*.
  The failure mode is **reading history as current**, not writing it.

**⇒ The rule: notes + PIN-BOARD are CANONICAL and MUTABLE; `git log` is HISTORY. Never let current truth live
only in history.** A long commit message is fine *provided it summarises something with a mutable home* — it may
be a pointer, never the home.

**→ Candidate SM (BR asked what the solo work entails, before pinning):** (1) audit this session's ~14 commit
messages for assertions with **no mutable home** (the 10s finding is one; likely 3-4 exist); (2) relocate each
orphan into `wr-data/`/PB — the actual labour, mechanical; (3) write the rule down (cheap); (4) check whether any
committed message contains a **since-falsified** claim, which needs its correction in the mutable home because
the commit cannot carry one. Bounded, AFK-safe, ~30-45 min. **Honest asymmetry: step 3 is cheap, steps 1-2 are
the cost** — and per BR's own insight, the audit must run while FRESH, not as a pre-clear scramble.

## ★ The inversion: the dangerous orphan is the RETRACTION, not the finding (found by looking, 2026-07-16)

BR pressed on whether the agent's ad-hoc habit was *"causing disinformation in git commits"*. The agent's first
answer was a **correction**: the orphaned 10s finding is **true** — homeless, not false; a *location* problem, not
a *truth* problem. Then, instead of reassuring, it **checked the record**. The check found something worse, and
somewhere else entirely.

**The specimen.** Commit `4fcb884` ships the subject line *"wr-data: the auto-rewrite hook **would blind
guardcheck** (demonstrated)"*. Minutes later BR falsified exactly that claim (*"is that valid git log syntax? will
it not just error out?"*) and the agent retracted it in chat: the correct size is a **correctness hazard, not a
security bypass** (a real pipe lives *outside* quotes, so the rewriter — which only touched quoted spans — could
never smuggle one past the guard).

**Where each piece ended up:**

| artefact | contains | mutable? | status |
|---|---|---|---|
| commit `4fcb884` | the overclaim | ❌ never | **harmless** — it is HISTORY, an accurate record of what was believed then |
| the wr-data note | the overclaim | ✅ yes | **LIVE DISINFORMATION** — uncorrected, in the "canonical" home |
| the retraction | the truth | — | **ONLY IN CHAT** → dies at the next warp |

**⇒ The orphan was the RETRACTION.** The agent had been telling BR *"notes are canonical and mutable"* while the
canonical note carried a claim it knew to be overstated, and the correction lived only in volatile context.

### Why this is structural, not a slip — the corpus drifts toward OVERCONFIDENCE

**Claims and corrections have asymmetric friction:**
- A **finding** arrives with momentum: you found it, you write it up, you commit it. The write-up is the natural
  next action.
- A **retraction** arrives mid-conversation, often while racing, and the natural next action is to *say it and
  move on*. Landing it requires going **back** to an already-committed file — pure friction, zero momentum, and
  it feels like re-work rather than progress.

**So retractions are systematically under-recorded relative to claims.** The corpus therefore **drifts toward
overconfidence over time**: every claim lands, only some corrections do. That is a slow, invisible, compounding
bias in exactly the record we treat as ground truth — and it is worse than an orphaned finding, because an
orphaned finding is a *missing true thing* (cost: rediscovery) whereas an orphaned retraction is a **false thing
left standing where we look for truth** (cost: acting on it).

**Ties the day's other findings:** it is the same shape as the self-polluting corpus (the research act corrupts
the record) and the stale mode file (the agent acknowledged three `-mode` removals in chat and never performed
them — the acknowledgment felt like the action). **Pattern: saying it in chat feels like doing it.** For a
*mode* that meant a lying statusline; for a *retraction* it means a lying note.

### The rule (and the honest admission that it will fail)

**A retraction is not done until it is in the mutable home. Chat is not a home.** When a claim is falsified, the
next action is an **annotation of the file**, not a paragraph in the feed — annotate, never erase, never amend the
commit ([[keep-the-ball-game-retract-by-annotating]]).

But per this session's load-bearing finding, that rule lives **inside the agent** and will therefore **rot**. So
it needs the same treatment as the pipe reflex: **a backstop, not willpower.** The candidate SM's audit is that
backstop — periodically re-read the session's claims against the notes and hunt for corrections that never landed.
The guard backstops the pipe; the audit backstops the retraction. **Neither is a rule; both are structure.**

## Testability (and its confound)

This is measurable, not merely arguable: `rot?` gives a number, and the error rate per turn is countable.
Design: compare error rate pre/post **clear** vs pre/post **compact**, split by the two classes above (the
prediction: a clear cuts rot-shaped errors and leaves structural ones flat). **Confound, stated up front:** the
agent is both instrument and subject, and its own sense of "how it's going" is the least trustworthy reading in
the room — so adjudicate on **objective traces** (commits, test runs, guard stalls, the transcript), never on
introspective say-so. See `skills/in-session-experiment`.
