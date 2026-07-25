# Specimen: a Russian word appeared mid-sentence in an English ember (2026-07-25 16:35)

Filed because BR asked where it came from, and because the honest answer includes a limit worth
recording: the agent cannot introspect the cause, only the conditions and the consequences.

## The event

Writing the warp ember, §3, the agent produced:

> "While stale, `tt` routes **через** scala-cli → bloop and can HANG."

**«через» (cherez) is Russian for "through" / "via".** It is not noise or a corrupted token: it is
precisely the word the sentence needed, in the wrong language. The agent caught it on its own re-read
before committing and corrected it to "through" in the next action.

## Why the obvious explanation is wrong

The tempting hypothesis is priming from the day's task: this session spent hours on Swedish-versus-
English word classification — building `swedish-function-words.txt`, an English counterweight list, a
bilingual scorer, and reading Swedish cache rows. A language-mixing slip after that looks explicable.

**But the leak was RUSSIAN, and Russian appeared nowhere in this session at any point.** So
task-language priming does not account for it. If anything it is evidence against the simple story: the
substitution crossed to a language that had no presence in the context at all.

## What can honestly be said, and what cannot

**Cannot:** the agent has no access to why one surface form was emitted over another. Any account of
"what happened in the model" would be a plausible-sounding story with no way to check it, which is the
failure mode this project keeps naming elsewhere (`reassurance is worthless as evidence`, and the
after-inspect rule that intuition is the thing under review). Recorded as unexplained.

**Can — the co-occurring conditions, all checkable:**
- extremely long session (overnight AFK run, a sleep gap, then a full working day; ~17h wall clock);
- heavily multilingual content throughout (Swedish prose, Swedish/English word lists, a bilingual scorer);
- **five prior mechanical slips the same day**, already self-diagnosed, with `DumbZone` set in response;
- the box was thrashing (bloop at 9+ GB, load ~11), and several background runs had died.

The slip therefore lands in an already-recorded pattern: reasoning intact, MECHANICAL precision
degrading. A wrong-language function word is a mechanical defect, not a reasoning one — the sentence's
logic was correct and its rendering was not.

## Severity: near-zero here, and the class is what matters

Caught immediately, in a comment-like line, before commit. But note where it happened: **the ember**, the
one artifact written specifically to be read by a fresh agent at turn zero, where ambiguity is most
expensive. And consider the same defect elsewhere in the day's output:

- in a **commit message** — permanent, and this project's messages are unusually load-bearing;
- in a **published blog post** — BR's voice, public;
- in **code** — an identifier or string literal, where it compiles and hides;
- worst, in the **generated Swedish word list**, where a wrong-language token would sit among hundreds of
  foreign words and look like it belonged.

The last one is the genuinely dangerous case, and it is not hypothetical: the agent hand-authored a
149-word Swedish list today.

## The check this specimen demands, and it is cheap

If one wrong-language token surfaced, others may have. The day's artifacts are committed and therefore
greppable. Worth running before trusting today's output:

- scan the day's committed files for characters outside the Latin-1 + Swedish set (Cyrillic, Greek, CJK
  ranges) — one regex over the diff;
- eyeball the two hand-authored word lists specifically, since a foreign token there is camouflaged;
- BR is the only party who can audit the Swedish list by eye, which he can do in a minute.

**RUN, same sitting, result appended as promised.** `git grep -P` over the Cyrillic, Greek, CJK and
Arabic ranges across all three repositories (genscalator, genscalator-work, introprog):

- **No Cyrillic in any committed text file.** The «через» never landed; it existed only in the
  uncommitted ember draft and was corrected before the commit.
- The only text hits are **legitimate Greek statistical notation** — χ², κ, α, μ, δ, π, τ, Δ — in
  `research/` and two blog posts, all intentional.
- The two hand-authored word lists and `SwedishScore.scala` are **clean**, which was the case worth
  checking hardest, since a foreign token there would have been camouflaged among real foreign words.
- Binary matches (fonts, PNG/JPG figures) are false positives of scanning bytes, not text.

So: one occurrence, one line, caught before commit. The audit found no second instance. That does not
explain the cause, but it does bound the damage to zero, and it means the day's output can be trusted on
this axis without further review.

**Hardware was considered and ruled out for this slip** (BR raised it while the box was thrashing): a
memory fault yields invalid bytes or mojibake, whereas «через» is six Cyrillic characters forming a
correctly-spelled real word meaning exactly the intended concept, in a grammatically apt slot. That is a
model-output signature, not a silicon one. The box instability the same hour has a sufficient boring
explanation — bloop grew 1.8 GB → 9.3 GB, free memory 21 GB → 11 GB, load ~11, background JVMs dying.

## Why this is filed rather than shrugged off

A single caught typo is not interesting. What is interesting: it was caught by the agent's own re-read
rather than by any structure — no test, no guard, no tool could have seen it — in a session where the
agent had already demonstrated that its self-checking was the degraded faculty. That combination (only
human-or-self review can catch it, and self-review is what is failing) is the same shape as SM231, where
the guard was blind and only a human at the prompt noticed.

Ties [[rot-vigil-guard-mechanical-precision-first]], `dumbzone-chip-changed-behaviour-n1-2026-07-25.md`
(the paired positive/negative on the same chip, same day), SM231, [[tired-cue]],
[[summaries-enumerate-dont-totalize]].
