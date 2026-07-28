# Issue 013: bump munit 1.3.3 → 1.3.4 across the test suites

> status: open · labels: dependencies, good-first-issue · summary: every test file pins
> `org.scalameta::munit::1.3.3`; 1.3.4 is out. A mechanical bump across ~40 `//> using dep`
> directives, gated on the suite staying green.

## Description

Noted by the first alpha field test (2026-07-28). The pin appears once per test file (all of
`tools/test/*.scala`, plus `media/blog/References.test.scala`), so the change is repetitive but
trivial — a good first contribution. `tt sub tree` can do it in one preview-then-write pass.

## Acceptance sketch

* All munit pins read `1.3.4`; no other directive changes ride along.
* The full toolbox suite runs green afterwards (that run IS the review).

## Discussion

### Comment by bjornregnell/Charlie at 2026-07-28

Filed on BR's go, from the v0.10.1 mining sweep. Deliberately not bumped before the v0.10.0
publish: it would have invalidated the already-verified release assets for zero tester value.
