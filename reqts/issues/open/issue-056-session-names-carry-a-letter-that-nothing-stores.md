# Issue 056: a session's name ends in a letter that nothing on disk records, so the agent picks it from memory and cannot be wrong in a way anyone can detect

> status: open 2026-09-11 · labels: toolbox, session, agent-trust, ergonomics · measured against: v0.10.2
> at `b19862b` · summary: `tt session <Name>` appends a human-chosen suffix to the timestamp, and the
> convention in use has been the NATO alphabet in sequence (…Lima, Mike, November, Oscar). Nothing stores
> that sequence. `tt session list` is directory-scoped and pruned, so the previous letter is unrecoverable,
> and each session learns "you are Oscar" only from prose carried in the previous session's hand-off note.
> It is a counter with no source of truth, and it is also cryptic to anyone who does not know the NATO
> convention. Replace the chosen suffix with a fully derived name that includes the working directory.

## Description

`SessionStore.defaultName` (`tools/sessionstore.scala:46-50`) renders `yyMMdd-HH'h'mm'm'`, and
`displayName` (`:52-56`) appends an optional human suffix, giving names like `260911-14h08m-OSCAR`.

Two separate problems.

**1. The suffix is a remembered counter, and the memory is the only copy.** Measured on 2026-09-11, in a
repository that had hosted at least four prior named sessions:

```
$ tt session list
sessions for /home/bjornr/git/hub/bjornregnell/genscalator-work (newest first; * marks this session):
*  89ee8aec-…  260911-14h08m-OSCAR (chips: TokSaving; 1h old)
```

One entry. Lima, Mike and November are gone. So when a session names itself "Oscar" because the previous
session's hand-off note said "OSCAR is next", that claim cannot be checked against anything: the store is
pruned, and there is no roster file. The hand-off note in use at the time said as much, warning that the
roster is "CARRIED, not durable" and should be treated "as a convention, not a verified fact". A name that
is asserted rather than derived is the same defect this repository keeps filing against itself, this time
in its own identity.

Renaming the convention to plain letters (A, B, C…) does **not** fix this. It inherits the identical
unstored counter, with less distinctiveness and a worse answer for what follows Z.

**2. The name does not say where the work happened.** The store is directory-scoped, so the project is
implicit context. That context is exactly what is lost the moment a session name is quoted somewhere else —
a hand-off note, an issue, a commit message — which is the only place the name is actually used.

### Proposed shape

Separate the stored identity from the rendered chip and from how people say it aloud. The part anyone
speaks is the time, and it is byte-identical in all three, so the date may render friendly on screen while
staying rigorous in the identifier.

* **stored / canonical:** `genscalator-work-260911@1408` — the basename of the working directory, the date
  with year, `@`, and the time. Chronologically sortable, unambiguous across years, filename-safe.
* **chip renders:** `in: genscalator-work  started: aug11@1408` — labelled rather than positional, so a
  reader does not decode the slug.
* **spoken:** "the 1408 session" on the same day; "the aug11@1408 session" when reaching back.

Every component is derived from the working directory and the clock. **There is no decision left for a
session to make, and therefore nothing it can get wrong.**

`@` rather than `:` is deliberate and is not only about typing: a colon is illegal in Windows filenames and
acts as a separator in paths, drive specifiers and URLs, so `14:08` breaks the moment a name reaches a
filename or a path in a log. `sessionstore.scala:47` already records "filesystem-safe by construction (no
colon)" as the reason for the current `HHhMMm` form; this keeps that property while shortening it.

Naming a session by hand should remain possible (`tt session <name>` is useful for a session with a real
subject), but it must stop being something a session is *expected* to do on startup.

## How to reproduce it

```
tt session list
```

in any repository with a history of named sessions. Observe that earlier sessions are absent, so the next
letter in the convention cannot be derived from the store. There is no other file that records it.

## Discussion

### Comment by bjornregnell at 2026-09-11 16:00

Raised because I find the NATO names cryptic: not everyone knows the convention, and it carries no
information. Working through it with an agent surfaced the sharper problem, which is that the sequence is
unverifiable — the `tt session list` output above was the evidence that settled it.

Where this has to be enforced, in order of strength:

1. **`tools/sessionstore.scala`** — `defaultName` builds the new form, including the directory basename,
   so the correct name is what you get without asking. This is the whole fix; the rest only protects it.
2. **`tools/session.scala`** — render the labelled chip, and stop documenting a chosen suffix as the norm
   in the header comment and `--help` (lines 6, 8, 11, 38, 43, 63, 81 all describe the current shape).
3. **`tools/test/session.test.scala`** — pin the format, and assert the property rather than only the
   string: a generated name must contain no colon and must be usable as a filename. A test that only
   compares against an expected literal would pass a future change that reintroduces a separator.
4. **`docs/EMBER-TEMPLATE.md`** — delete the instruction to name yourself and the carried "next letter"
   hint. As long as the hand-off template asks a session to choose a name, it will choose one, and the
   unstored counter comes straight back. This is the line that actually produces the defect.
5. **`tools/README.md`** — describe the new shape and the three reference forms.

Existing names stay as they are wherever they appear; this applies going forward, with no rewriting.
