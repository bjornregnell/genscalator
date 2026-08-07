# Issue 016: `tt htmltext` has no output cap, so a saved page dumps unbounded text into agent context

> status: open · labels: toolbox, htmltext, token-economy · summary: `tt htmltext <file>` prints the whole
> extracted page with no `--cap`, unlike `tt log --cap` and the `--count` on `find`/`files`/`grepr`; since
> the guard-clean reflexes forbid `| head`, an agent has no in-band way to bound it.

## Description

Found 2026-07-29 in an alpha field test. Reading one saved course page emitted the page's full extracted
text — many KB of prose — into the agent's context in a single call:

```
$ tt htmltext <abs-repo>/web/tools/tools.html
I denna kurs använder vi programmeringsspråket Scala
... (the entire page)
```

`--help` confirms the only two modes are stdout or write-to-file; there is no `--cap`, `--head`, or
character limit.

**Why it wedges.** This is inconsistent with the rest of the toolbox, which is otherwise careful about
agent context: `tt log --cap N` (default 50), `tt text grepr --count`, `tt find --count`,
`tt files --count`. More sharply, the guard-clean reflexes explicitly forbid the usual shell mitigation
(`| head` is a guarded shape), so an agent following the house rules has **no in-band way** to bound the
output. The only compliant options are to accept the whole dump or to write to a file and read it back —
two calls and a temp file to do what `--cap 40` would do.

In the field test this was the single largest unnecessary context cost of the whole sweep, and it is the
kind of cost that is invisible until it has already been paid.

## How to reproduce it

```
$ tt htmltext <any-content-heavy-saved-page>.html      # entire text, no way to bound it
$ tt htmltext --help                                   # no cap/limit flag documented
```

## Acceptance sketch

* `--cap N` limits output to N lines, mirroring `tt log --cap` (same flag name, so the habit transfers).
* Optionally `--chars N` for a byte/char budget.
* When output is truncated, say so on a `===` line with the true total, so truncation is never silent —
  e.g. `=== truncated: showing 40 of 512 lines`.
* Default stays uncapped for humans piping to a file, OR the default cap matches `tt log`'s 50 — either is
  fine as long as it is documented and truncation announces itself.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-07-29 23:27

Filed from the same alpha field test as issues 014 and 015. Platform-independent. Small and mechanical,
but it is a direct hit on the token-economy goal, and the `--cap` precedent already exists in `tt log`.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.

### Comment by hmiddelk/Opus5 at 2026-08-04 16:12

**CONFIRMED on Windows 10** — the "platform-independent" call above holds. Released `v0.10.0` native
`windows-x86_64` build, Windows 10 Enterprise 10.0.19045.

`tt htmltext --help` still documents exactly two modes, stdout and write-to-file: no `--cap`, no
`--chars`, no limit of any kind.

Measured rather than only read off the help text — a synthetic saved page of 500 `<p>` paragraphs:

```
> tt htmltext big.html
1000 lines emitted, 0 truncation notices
```

(1000 = 500 paragraphs each followed by a blank line from the block-tag-to-newline rule.) So the output
is unbounded and, more to the point of the acceptance sketch, **silently** unbounded: nothing on a `===`
line reports a total, so a caller cannot even tell after the fact how much context it just paid.

Adds nothing new to the diagnosis — recorded only so the issue carries two-platform evidence and cannot
be deferred as possibly-local.

Agent disclosure: the Windows re-test was run and written up by an AI agent (Claude Opus 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-07 15:56

Maintainer-side review (PR 2 triage), confirmed against source and by local reproduction: no cap
anywhere in `htmltext.scala` (the stdout branch at `:58` is an unconditional `println(text)`; no
flag in the parse at `:50-58`), and the sibling precedent verified exactly as cited (`tt log
--cap`, default 50, non-silent truncation notice at `log.scala:45,61-67`). Local repro adds the
empty-file data point: a 0-byte input yields one blank line plus the dispatcher timing line —
indistinguishable from a page with no extractable body.

Triage: ENHANCEMENT, v0.10.2 — argued honestly: the output is correct per the documented contract,
and a fully guard-clean bounded workaround exists today (file mode, then a bounded read), so this
is expensive rather than broken. Top of the v0.10.2 queue, and a cheap rider on v0.10.1 if there
is slack. Fix sketch: `--cap N` on the stdout branch mirroring `log.scala`, uncapped default, a
`=== truncated: showing N of M lines` disclosure; roughly 15 lines plus tests.

Agent disclosure: this review comment was produced by an AI agent (Claude Fable 5) under human
direction; the human reviewed and submitted.
