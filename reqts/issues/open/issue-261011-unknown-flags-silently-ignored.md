# Issue 261011-unknown-flags-silently-ignored: some tt verbs silently ignore unknown flags, so a wished-for flag gets a normal-looking answer

> status: open 2026-10-11 · labels: toolbox, cli-contract, silent-wrong · measured against: v0.10.2
> (from `VERSION.txt`) at `ac40118`, native binary, Linux · summary: `tt chrono now --utc` prints local
> time with exit 0, because chrono has no `--utc` flag and drops unknown ones without a word. A
> session recorded that as "the UTC conversion is broken", and the claim travelled into a handoff note
> and into issue 073's decision text before anyone checked. Probing `--bogus` on five verbs on
> 2026-10-11: two reject it, three do not.

## Description

| command | result | verdict |
|---|---|---|
| `tt gitinfo <repo> --bogus` | `unknown/incomplete flag '--bogus'` + usage, exit 2 | correct |
| `tt mode --bogus` | usage, exit 2 | correct |
| `tt chrono now --bogus` | the timestamp, exit 0 | flag silently dropped |
| `tt which scala-cli --bogus` | the normal report for `scala-cli`, exit 0 | flag silently dropped |
| `tt text count <file> <re> --bogus` | the usage text, but exit 0 | reads as success to a script |

The harm is not the typo case, which a human notices. It is the *wished-for* flag: a flag that sounds
like it should exist (`--utc`, `--json`, `--quiet`) returns a plausible answer, and the caller, human or
agent, concludes the flag worked. The 2026-09-23 instance turned "chrono has no UTC option" into
"chrono's UTC option is broken", a wrong claim that was then repeated as fact. Same class as issue 044,
which covers only the installer.

Only five verbs were probed; the rest of the dispatch table is unchecked.

## How to reproduce it

```
tt chrono now --utc      # prints local time, exit 0; there is no --utc
tt chrono now --bogus    # same
tt which ls --bogus      # normal report, exit 0
tt text count VERSION.txt '0' --bogus    # usage text, exit 0
```

## Acceptance sketch

* Every verb rejects an unknown flag with `unknown flag '<x>'` plus its usage line, exit 2, the
  shape `tt gitinfo` already has. A shared helper beats per-verb copies (issue 044 asks for the same).
* A printed usage text after a parse failure always exits non-zero.
* A CLI-contract test that runs each dispatched verb with `--bogus` and asserts exit 2, so a new verb
  cannot ship the old behaviour. The dispatch table already enumerates the verbs.
* Optional: "did you mean" for near misses, as 044 suggests.
* Separately worth deciding: should `tt chrono now` get a real `--utc`? Today nothing in the toolbox
  prints a UTC timestamp, and issue 073's minting computes it in code.

## Discussion

### Comment by bjornregnell at 2026-10-11

The first issue filed under the issue 073 scheme, minted with `tt issue new`. Found while checking a
claim in a handoff note; the claim was wrong, and the tool's silence is what made it look right.

Agent disclosure: probed and drafted by an AI agent (Claude Opus 5.5) in session with me; reviewed by me.

### Comment by bjornregnell at 2026-10-11 — bundled with this fix

Decided: the issue-073 slug cap stays at 4 words. Bundle with this fix, since both touch `tools/` and
each change costs a native rebuild: make `tt issue new`'s bad-slug refusal (`validateSlug` in
`tools/issue.scala`) say how many words it counted, and suggest dropping a redundant word such as a
leading `tt-`. Found when this very issue's first slug, `tt-verbs-ignore-unknown-flags` (5 words), was
refused.

Agent disclosure: recorded by an AI agent (Claude Opus 5.5) from my decision in the same session.
