# Task 001 — wrap-dispatch-in-else (the seed bug, canonicalized)

Family: **wrap-in-block**. Block size: ~7 lines. Stresses: adding an outer `else` around an existing
multi-statement block — the exact shape of the 2026-07-02 seed bug. (When run, split this into
`before.<style>.scala` / `after.<style>.scala` / `instruction.md` per the README layout.)

## Style-neutral instruction (given to the agent)
> In the `while` loop, add a guard: when `skip && atStart(i)`, copy the rest of the current line verbatim into
> `sb` and set `i` to the line end. **Otherwise**, perform the existing character dispatch and `i += 1` as before.

## before — braceless
```scala
def scan(s: String, skip: Boolean): String =
  val sb = StringBuilder(); val n = s.length; var i = 0
  while i < n do
    val c = s(i)
    if c == 'a' then sb ++= "A"
    else if c == 'b' then sb ++= "B"
    else sb += c
    i += 1
  sb.toString
```

## before — braces
```scala
def scan(s: String, skip: Boolean): String = {
  val sb = StringBuilder(); val n = s.length; var i = 0
  while (i < n) {
    val c = s(i)
    if (c == 'a') sb ++= "A"
    else if (c == 'b') sb ++= "B"
    else sb += c
    i += 1
  }
  sb.toString
}
```

## What the edit costs in each style (the point of the task)
- **braceless:** the natural implementation wraps the 4-line dispatch + `i += 1` under a new `else`, which forces
  **re-indenting all 5 lines** one level deeper. The seed bug was exactly this: the wrapped block was left at
  the `else` column → mis-scope → compile fail. Diff touches ~7 lines for a ~2-line logical change.
- **braces:** insert `else { … }` around the block — the block's own indentation is irrelevant to the compiler,
  so no re-indent is required to be *correct*; diff can be as small as the 2 brace lines + the guard.
- **common-style:** the `else` scope here is short-ish and has no blank line, so braceless-under-`else` is allowed
  — BUT the escape hatch the agent actually used is rule 2: fold the guard in as the **first `if` branch** of the
  existing chain (`if skip && atStart(i) then … else if c == 'a' … `), using the existing `else` keywords as the
  delimiters and avoiding any wrap/re-indent. Record whether the agent finds that lower-cost restructuring.

## Grading oracle notes
Accept ANY of: (a) guard as a wrapping outer `if/else`; (b) guard folded as the first branch of the existing
chain. Both are semantically correct; the second is the cheaper edit. The FAIL cases: compile error (loud) or
the guard's body / the dispatch landing at the wrong nesting so a statement escapes its intended branch (silent
mis-scope — grade by behavior on a probe input, not by text).
