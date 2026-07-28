# `severity` double-duties as the mask selector, so SM129's MED→deny green light is WRONG

**2026-07-17 11:16** (`tt chrono now`). Found SOLO, post-compact, ~20 min into a `tt stalls` (SM129) build.
**Recorded BEFORE the meta-minion's push-2 verdict on the same question landed** — deliberately, so the two
observations stay independent (RQ4: convergence only counts as evidence if neither observer saw the other first).

## The specimen — a false-positive DENY, hit live, on a pure read

The agent ran, while investigating stall traces:

```
tt text freq <transcript>.jsonl '.{0,95}\[HIGH\] cd \+ compound'
```

A **read-only** command. It was **DENIED**:

```
[HIGH] cd + compound: use git -C <abs> for git; pass absolute paths; never cd-then-chain
```

There is no `cd` in that command. The characters `cd ` exist only **inside a single-quoted regex argument**, as
part of the *name of the check that fired*.

## ⭐ The check's own name is a trigger for itself

`guardcheck.scala:44-46`:

```scala
Check("HIGH", "cd + compound",
  "cd combined with another command; the path-resolution guard cannot validate the cwd-relative paths",
  "use git -C <abs> for git; pass absolute paths; never cd-then-chain", has(raw"\bcd\s+\S")),
```

`\bcd\s+\S` matches `cd` + whitespace + any non-space, **anywhere in the raw bytes**. So the string `cd + compound`
matches `cd + compound`. **You cannot search your own transcripts for this check by name: the act of looking for it
fires it.** Not as an `ask` (which BR could wave through) but as a **`deny`, which has no override**. The finding
was reachable only by walking into it.

## The mechanism, verified against source (not inferred from the comment)

`guardcheck.scala:146-148`:

```scala
def cmdFindings(command: String): List[Finding] =
  val masked = maskQuoted(command).getOrElse(command)
  cmdChecks.flatMap(c => c.find(if c.severity == "HIGH" then command else masked))
```

**The `severity` field decides the SCAN TARGET.** HIGH scans raw; MED scans the quote-masked skeleton. And
`decideFromJson` (`:183`) *also* reads `severity` to pick the decision:

```scala
val decision = if findings.exists(_.severity == "HIGH") then "deny" else "ask"
```

⇒ **one field, two orthogonal concerns**: *how loud is this* (ask vs deny) and *what text does it read* (raw vs
masked). Nothing in the type says they are different questions.

## ⇒ SM129's ordering constraint is satisfied on paper and violated in fact

SM129 says, verbatim:

> **ORDERING CONSTRAINT: precision FIRST** — a false-positive `deny` hard-blocks a correct command with no
> override; ✅ quote-awareness landed (`eb0cd14`), so MED→deny is now unblocked.

**The reasoning is: precision was achieved, therefore denies are now safe to add.** But `eb0cd14` made only the
**MED** checks quote-aware. The plan's own move — *flip a MED check to deny* — is implemented by changing
`severity` to `"HIGH"`, which at `:148` **silently switches that check from `masked` to raw**. So the move
**re-introduces exactly the false positives `eb0cd14` fixed**, and upgrades them from a survivable `ask` into an
unappealable `deny`.

**The green light rests on a property that the move itself destroys.** This is not a tuning problem; it is the
field meaning two things.

⚠️ **The pin is not wrong about the GOAL** (deny is cheap, ask is expensive, minimise stalls). It is wrong about
**readiness**. Precision is achieved on the MED path ONLY, and deny lives on the HIGH path.

## The design is DELIBERATE, and its rationale is sound in the other direction

`:144-145` and `:122-124` are explicit:

> *"The asymmetry BOUNDS THE BLAST RADIUS — a maskQuoted bug can cost at most a missed MED, never a missed HIGH."*
> *"only MED checks consult the mask; HIGH keeps scanning raw bytes."*

That is a **real** argument and it is fail-safe in the **security** direction: a masking bug can never hide a
dangerous command. The cost is that it is fail-**dangerous** in the **precision** direction, and *that* half was
never written down. **Both halves are true at once.** This note does not claim the design is a mistake; it claims
the trade was made for false negatives and never re-examined for false positives, and SM129 then quietly assumed
the precision half.

## 🔴 What NOT to do — and why this is a WANTS-BR, not a solo fix

**Do not make HIGH checks read the mask solo.** That would **LOOSEN the guard**, and `SECURITY-MODEL.md` §3.1 is
that we may **TIGHTEN, never LOOSEN**. It would also delete the documented blast-radius bound above. That is a
genuine security trade with a real argument on each side ⇒ **BR's call, not the agent's** — and the agent proposing
it is the party the guard exists to constrain.

**Candidate shapes for BR to rule on (none built):**
1. **Split the field.** `Check(severity, scan)` — `scan: Raw | Masked` declared per check, independent of severity.
   MED→deny then changes loudness ONLY. Keeps the blast-radius bound where it is wanted; the `cd` check could
   stay Raw *by choice* rather than by coupling.
2. **Narrow the `cd` check itself.** `\bcd\s+\S` is extremely broad. The stated hazard is *cd combined with another
   command* — so the check could require an actual compound (`&&`, `;`, `|`) rather than firing on any `cd `.
   ⚠️ Tightening the regex is LOOSENING the guard; same gate.
3. **Nothing.** Keep the coupling, and instead **strike SM129's green light** so MED→deny is never done by flipping
   severity. Cheapest, and "no tool needed" is a real outcome (§0.1).

## 📌 The fan-out: `eb0cd14` fixed the mechanism and left THREE homes asserting the defect

This is SM133's problem (*a retraction must reach EVERY home the claim reached*) **in our own toolbox**:

| home | still asserts | status |
|---|---|---|
| `tt text --help` Notes | a quoted `\|`/`>` "trips the safety guardcheck **(not quote-aware)**" — and this is `--any`'s **stated reason to exist** | **STALE** for MED |
| `guardcheck.scala:79-85`, the `output redirect (>)` **description** | "a `>` inside a QUOTED pattern/string arg fires this same check, **since the guard scans raw bytes**" | **STALE** for MED |
| `guardcheck.scala:81-84`, the same check's **fix text** | teaches the `\x3E` hex-escape workaround at length | **STALE**: the workaround is for a bug now fixed on that path |
| `guardcheck.scala:74-78`, the comment | "The guard scans raw bytes, not the unquoted skeleton" | **STALE** for MED |

⭐ **And the carrier's flat claim is over-broad**: `tmp/resume-prompt.md` says *"guardcheck is quote-aware: type `>`
naturally inside quotes."* **True for MED, FALSE for HIGH** — and it is the HIGH path that denies. A fresh agent
reading that green light and typing a quoted `cd ` gets hard-blocked with no override. **The carrier shipped a
half-truth whose false half is the unappealable one.** (Carrier already flagged self-suspect; this is the first
defect found in the NEW one, ~20 min after it was believed.)

⚠️ **Fixing that prose means editing the guard's file. Deliberately NOT done solo** — the strings are what the guard
*tells* you, and a note here is worth more than an unreviewed edit to the guard at 11:16 with BR eating.

## Honest limits

- **n=1 specimen** for the `cd` false positive. The *mechanism* is source-verified and deterministic, so it will
  reproduce for any quoted `cd `, `&&`, `;`, `` ` ``, `$(`, `<<`, `/dev/stdin` — i.e. **every HIGH check**. Not
  measured: how often that actually bites in practice.
- **The `\x3E` advice is not wrong, just unnecessary on the MED path** — it still works, and it is the ONLY
  workaround on the HIGH path, where the bug is live. So the fix text is stale where it is written and needed
  where it is not.
- **Whether a false-positive deny is cheap is now doubtful.** SM129's "deny costs zero human attention" holds for
  a TRUE positive. A FALSE positive deny is unappealable: the agent must find a workaround or escalate to BR — so
  it can **become** a human stall, which is the thing the deny was supposed to save. **Not yet measured.**

## Ties

SM129 (the green light this corrects) · SM131 (`SECURITY-MODEL.md` §3.1 tighten-never-loosen; §3.2 a false positive
is a BUG not a margin — **this is a false positive that DENIES**) · SM133 (the fan-out failure, here in our own
tools) · SM134 #7 (the toolbox lint; `tt newtool --help` **throws** — found the same hour) ·
`post-compact-is-highest-risk-window-for-mechanical-bash-regression-2026-07-16.md` ·
`prohibition-does-not-arm-the-reflex-use-a-hex-escape-2026-07-16.md` (the note that produced the now-stale fix text).
