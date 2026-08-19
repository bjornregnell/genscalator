# Issue 010: `tt parsereqt` lint reports 5 eternal false positives on the PRD's own grammar illustration

> status: closed 2026-08-19, fixed by `e4d1284` · labels: reqt, lint · summary: the lint cannot distinguish a grammar illustration
> from a mistake, so `reqts/PRD.md` reports 5 unknown-concept hits forever — metasyntactic
> placeholders like `ENT: id`, not typos.

## Description

Found in the first alpha field test (2026-07-28): `gs reqt` came back green with 5 lint hits, all
at the PRD block headed "Abstract example of concrete syntax with abstract terms". The tester's
agent adjudicated them correctly as false positives — but every future reader repeats that work,
and a check that always reports 5 trains people to ignore its number.

## Acceptance sketch

* The lint skips fenced code blocks (a grammar illustration lives in one; real reqT content does
  not), OR an explicit ignore mechanism that records WHY each exception exists.
* `reqts/PRD.md` lints to 0 findings, and a test pins that so a regression is loud.
* The fenced-skip must apply to LINT only — the parser's handling of fences stays as is.

## Discussion

### Comment by bjornregnell/Charlie at 2026-07-28

Filed on BR's go, from the v0.10.1 mining sweep. Origin: the first alpha tester's session, whose
transcript flagged exactly the "known baseline of 5" trap.
