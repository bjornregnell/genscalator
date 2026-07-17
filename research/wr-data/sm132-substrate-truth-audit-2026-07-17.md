# SM132 — the substrate-truth audit: results, and the prediction it was built to test is untestable

**2026-07-17 08:35 (`tt chrono now`).** BR's approved 1-2-3-4, run post-clear as designed. **Corpus: the 54
genscalator commits of 2026-07-16** (`a56ff43` 00:32 → `0e71c9c` 22:55), bodies pulled whole. **The agent's own 8
commits from 23:35 onward are EXCLUDED** — self-authored, and the resume prompt specifies the pre-clear set.

> **⚠️ Read §4 first if you read nothing else. The experiment this audit was supposed to be an arm of has a broken
> control, and that is a bigger result than anything the audit found.**

## 1. ✅ Step 4 first (the important step) — the known-answer test PASSES, and the result is a NULL

**The planted test case `4fcb884`** (*"the auto-rewrite hook **would blind guardcheck**"*, later withdrawn) — **found.**
Trail: the corpus itself names it (`5b1c3d6`'s body), which pointed at the file.

**But the audit's job was not to find the commit — it was to check whether the retraction LANDED.** `5b1c3d6`
*claims* it did (*"Now annotated in place, never erased"*). **Claims are what we are auditing**, so it was checked
against the file, not believed:

- `prohibition-does-not-arm-the-reflex-use-a-hex-escape-2026-07-16.md:192` — *"⚠️ RETRACTION (same session, BR):
  'blind the guard-checker' is OVERSTATED."* ✅ **landed**
- same file `:75` — *"## RETRACTION, same session: the escape is a better prohibition, NOT a structural fix"* ✅
- same file `:212` — *"The agent's 'silent rewrite is a transparency problem' objection was WRONG and is retracted"* ✅

**⇒ STEP 4 FOUND NO UN-LANDED RETRACTIONS IN THE 07-16 CORPUS. A clean, honest NULL.**

**Why the null is unsurprising rather than reassuring:** `5b1c3d6` (22:01) *is* the moment the pair discovered the
retraction problem and swept it. **The corpus was audited for this defect four hours before this audit ran** — by an
agent who then wrote the resume prompt telling the next agent to audit it. ⇒ **Step 4 was aimed at ground that had
already been cleared**, and the null measures the earlier sweep's thoroughness, not this one's.

## 2. ⚠️ The audit's own NAMED ORPHAN is half-false — and the wrong half is the instructive half

The resume prompt seeds the audit: *"(known orphan: **"10s is below the working noise floor"** — TRUE, just
homeless)"*.

**It is homed.** `tools/statusline.scala:376-382`, at the constant it explains, in a **mutable** file:

> *"...normal work (think + a tool call) routinely exceeds 10s — an 18s gap was just a command running. So 10s sits
> BELOW the working noise floor and the chip never clears. 60s is the first value above it. Still a first cut: tune
> from the gap distribution the hook is now collecting."*

**Timeline, and it is the whole point:**

| time | event |
|---|---|
| **22:01** `5b1c3d6` | *"the orphaned 10s finding is TRUE, so it is homeless"* — **correct at the time** |
| **22:16** `5080764` | raises the chip to 60s **and lands the full rationale in the source comment** ⇒ **no longer homeless** |
| **22:38** `084062b` | the consistency sweep runs (5 stale claims + 2 gaps found) — **does not catch this** |
| **~22:55** resume prompt | still says *"TRUE, just homeless"* — **stale by 39 minutes** |

⇒ **A THIRD failure direction in the family, and it is new.** Not *said-but-not-done* (modes, retractions). Not
*done-before-true* (the agent's premature `+afk`). This is **DONE-BUT-STILL-DESCRIBED-AS-UNDONE**: the fix landed and
the carrier never caught up. All three are one defect — **an assertion about state that is never re-checked against
state** — which is also the mode line, the `hangover?` chip, and the provenance gap. **Sixth instance in ~36 hours.**

**And the sharp part: the sweep that fixed 5 stale claims left a stale claim IN THE SECTION DESCRIBING THE AUDIT THAT
HUNTS STALE CLAIMS.** Not a gotcha — evidence for `084062b`'s own thesis: *an audit finds only what it has a theory
for*, and the sweep's theory was *claims fan out, corrections don't*. **A claim that silently became true elsewhere
does not match that shape, so the sweep was blind to it by construction.**

### ⇒ THE RULE (step 3), and the seed example is what teaches it

> ### **A claim's HOME is AUDIENCE-RELATIVE. Ask: *who would look for this, and where?***

The 10s finding serves **two** audiences and needs **two** homes:
- **Engineering** — *why is this constant 60 and not 10?* ⇒ belongs **at the constant**. ✅ landed 22:16.
- **Research** — *what is the pair's working noise floor?* ⇒ belongs in **`wr-data/`**. ❌ **had no home on 07-16.**

**So the resume prompt was half right, and neither "homed" nor "homeless" was the correct verdict.** Checking one
audience generates **false orphans** *and* **false non-orphans**. **The previous agent's implicit model — "home = a
note or the PB" — is what produced the false orphan.** A source comment is a mutable home; it is just not a home for
*researchers*.

> **★ Accident worth recording:** the research orphan was **closed 8 hours before this audit ran, by accident**.
> `hangover-chip-fires-on-the-humans-thinking-pause-2026-07-17.md` (00:15) has a section *"there are TWO noise
> floors, and only one was measured"* — **written for a different reason, by an agent who did not know it was the
> audit's named test case.** Continuous pinning closed the orphan before the audit reached it, which is exactly
> `42b7295`'s argument (*the dance only works if pinning is CONTINUOUS*) landing on the audit itself.

## 3. ✅ A REAL orphan found (step 1) — and relocated (step 2)

**`83596ec`'s follow-up: a toolbox-wide lint that smoke-runs every `tt` tool's `--help`.**

- **Its only home:** `wr-data/green-test-suite-hid-a-tool-broken-in-its-real-invocation-2026-07-16.md:72` — *"or
  simply smoke-run every `tt` tool's `--help` in CI — the cheapest possible standalone-compile check"*.
- **Homed narratively, ORPHANED actionably:** **no SM, not on any menu, not in SM134's register.** Nothing will ever
  cause it to be built.
- **Why it is a real orphan and not a nice-to-have:** the note states it *"alone would have caught this"* — **this**
  being `tt hangover` **shipping broken** (missing `using file minijson.scala`) **behind a 10/10 green suite**. It is
  a known, specific, cheap fix for a bug that **actually shipped**.
- ⇒ **RELOCATED to SM134 as candidate #7.** Clears the anti-quota gate on the strongest possible evidence: the
  specimen is not a hypothetical, it is a shipped defect.

**Sharpens the rule again:** *narratively homed ≠ actionably homed.* A follow-up recorded in a note's prose is **read
by nobody at the moment work is chosen**. ⇒ **the menu/PB is the only actionable home**, and a tool idea that lives
only in a note is an orphan **regardless of how well it is written up**.

## 4. 🔴 THE PREDICTION'S CONTROL IS BROKEN — the audit's biggest finding is about the audit

The resume prompt puts a prediction on record and defines its control:

> *"**PREDICTION ON RECORD: you will find LESS per unit of effort than that sweep did**, despite your lower rot.
> **Judge it on the 51 commit messages (untouched — the same corpus for both;** the carrier was already cleaned, so
> it is not a fair target)."*

> ## ⛔ **"the same corpus for both" is FALSE.**

**The sweep never audited the commit messages.** The handoff states its scope explicitly: *"A CONSISTENCY SWEEP was
run over **this file + `PIN-BOARD.md` + `MEMORY.md`**"* — **the carrier**. Its 5 stale claims + 2 gaps were **all
found in the carrier**.

⇒ **The two arms audited DIFFERENT CORPORA.** Sweep → the carrier. This audit → the commit messages. **There is no
shared corpus**, so "found less per unit of effort" **cannot be computed**. The prompt names the confound it must
avoid (*the carrier is not a fair target*), then **specifies a control that reintroduces it** by asserting a
commonality that does not exist.

**⇒ The carried-vs-hot experiment, as designed, DOES NOT TEST ITS HYPOTHESIS.** Its headline claim —
*"YOU are the other arm of that experiment"* — is not supported: **the two arms are not arms of one experiment.**

**Honest raw counts anyway, offered as description and NOT as a test result:**
- **sweep (hot, rot ~710k, carrier):** 5 stale claims + 2 invisible-carrier gaps = **7**
- **this audit (carried, ~0 rot at start, commits):** the false-orphan seed · the toolbox-lint orphan · the ~54-vs-51
  count · **plus the broken control itself** = **~4**, of which **one is significant** (the orphan) and one is
  trivial.

**Naively, the prediction "holds" (4 < 7). Do not report it that way.** Different corpora, different densities,
different defect classes, n=1 each. **The number is not evidence and calling it evidence would be exactly the
overclaim this project exists to catch.**

> **⚠️ Note the shape of the strongest finding, because it is the tell.** This audit's best result was **not** found
> by hunting the shape it was briefed to hunt (orphans, un-landed retractions). It came from **reading the brief
> itself as a claim**. `084062b` says *an audit finds only what it has a theory for* — **true, and here the theory it
> was handed pointed at ground already cleared (§1), while the defects were in the pointing.**

## 5. What this says about the DAY'S LOAD-BEARING FINDING

**The resume prompt is an in-agent fix, and it warned about itself:** *"⚠️ This very file is an in-agent fix. It will
rot."* ✅ **It did — in 39 minutes**, while its own author was still typing (§2).

**But note the direction, which is not the predicted one.** The carrier did not fail by **fading** (the agent read it
fine, all night). **It failed by being WRONG** — a stale claim, a false orphan, and a broken control, all faithfully
transmitted and faithfully believed. ⇒ **A carrier does not rot in the agent's memory. It rots on disk, while the
world moves.** ***"Carried ≠ armed"* has a sibling: *carried ≠ still true*.** The first is about **heat**; the second
is about **staleness**, and only the second is fixable — by re-checking the carrier against the substrate, which is
what this audit accidentally became.

## 6. Ticks

- [x] **Step 1 — audit the commit messages for homeless claims.** 1 real orphan (§3); 1 false orphan in the seed (§2).
- [x] **Step 2 — relocate.** → SM134 #7.
- [x] **Step 3 — write the rule down.** **A home is audience-relative** + **narratively homed ≠ actionably homed**
      (§2, §3). **Memory candidate**, joining the `commit-*` family — **BR's call**, and it must be said that a
      *memory* encoding this rule is an in-agent fix that will rot exactly as §5 describes.
- [x] **Step 4 — hunt un-landed retractions.** **NULL**, verified by reading files (§1). Ground already cleared at
      22:01.
- **NOT done:** the self-pollution caveat the prompt warns of (*the hunt matches the agent's own prose about
  retractions*) never bit — the corpus is commit messages, and 07-16's retraction-prose is confined to two notes.

## Cross-links

`084062b` (*an audit finds only what it has a theory for* — **this audit is its best evidence and its sharpest
counter-example**) · `5b1c3d6` (the retraction-orphan thesis; **correct at 22:01, stale by 22:16**) · `42b7295`
(*pinning must be CONTINUOUS* — **vindicated by the accident in §2**) · `f048f8e` (the self-polluting corpus) ·
SM133 (the structural backstop; **§4's broken control is a NEW argument for it: even the audit's own brief was a
claim with no back-link to its correction**) · SM134 #7 · `tmp/resume-prompt.md` (**carries three defects found
here: the false orphan, the 51-vs-54 count, and the broken control**).
