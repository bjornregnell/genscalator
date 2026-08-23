# Issue 046: `tt text count` matches the WHOLE FILE, so `^`/`$` silently return 1 and the count is matches-not-lines

> status: open · labels: toolbox, text, agent-trust, silent-wrong · summary: `text.scala:80` runs the
> regex over the file as ONE string (`pat.r.findAllIn(readUtf8(file)).size`), so it diverges from its
> documented `grep -c` in two ways at once: `^`/`$` anchor to the INPUT, not to each line, and the
> result counts MATCHES rather than matching LINES. Its sibling `tt text match` (`:86`) iterates
> `linesIterator` and is correct, so the two verbs give contradictory answers for the same file and
> pattern. Failure is silent and quantified — the worst shape for a diagnostic.

## Description

Found 2026-08-23 by an exit-side audit minion during a warp-ember review, then reduced on an isolated
3-line probe. The tool returns a plausible number that is simply wrong, with no warning and no error.

### Root cause: the pattern is applied to the file, not to its lines

```scala
// tools/text.scala:80 — case "count" :: file :: pat :: Nil  // grep -c
println(pat.r.findAllIn(Lib.readUtf8(file)).size)
```

`readUtf8` returns the whole file as one `String`. Java regex is **not** in `MULTILINE` mode by
default, so `^` matches only at offset 0 and `$` only at the end of input. And `findAllIn(...).size`
counts occurrences, whereas `grep -c` counts **lines that contain at least one occurrence**.

Contrast the sibling verb four lines below, which is right:

```scala
// tools/text.scala:86 — case "match" :: file :: pat :: Nil  // grep -n
for (line, i) <- Lib.readUtf8(file).linesIterator.zipWithIndex if re.findFirstIn(line).isDefined do
```

### Measured on an isolated probe

A three-line file `p.txt`:

```
aaa
abc
b
```

| command | prints | `grep` truth | |
|---|---|---|---|
| `tt text count p.txt '^a'` | **1** | `grep -c '^a'` = 2 | ✗ anchor bug |
| `tt text count p.txt 'a'` | **4** | `grep -c 'a'` = 2 | ✗ matches, not lines |
| `tt text match p.txt '^a'` | lines 1 and 2 | 2 | ✓ |
| `tt text grepr <dir> txt '^a' --count` | **2** | 2 | ✓ |
| `tt text grepr <dir> txt 'a' --count` | **2** | 2 | ✓ |

So within one tool family, `count` disagrees with both `match` and `grepr --count` — and `grepr
--count` already implements the correct line semantics, which is where the fix can be borrowed from.

A real-world instance from the same session: `tt text count heading-residue-keep.txt '^#'` returns
**1**; the file has **13** comment lines.

**Why it wedges.** Three reasons, in order of cost:

1. **The failure is silent AND quantified.** `count` exists precisely to answer "how many" without
   printing output — so it is reached for exactly when the caller has decided not to look at the
   lines. A wrong number is therefore maximally likely to be believed and minimally likely to be
   cross-checked. This is the same shape as issue 022's "36 dirs": confident, specific, wrong.
2. **`^` is the house style, so the bug is aimed at good practice.** `docs/guard-clean-digest.txt`
   pushes agents off `grep -c` and onto `tt text`, and the ember discipline pushes them toward NARROW
   ANCHORED patterns to avoid flooding context. Anchoring plus counting is the recommended
   combination, and it is the one combination that is broken.
3. **Two sibling verbs contradict each other**, so a caller who checks their work by running `match`
   after `count` sees a discrepancy with no way to know which is authoritative.

Not a general rot: `match`, `context`, `freq`, `cols` and `grepr` all iterate lines. `count` is the
only whole-file consumer of `readUtf8` in `text.scala`.

## How to reproduce it

```
$ printf 'aaa\nabc\nb\n' > p.txt      # or write it with any editor

$ tt text count p.txt '^a'
1                                     # grep -c '^a' p.txt  ->  2

$ tt text count p.txt 'a'
4                                     # grep -c 'a' p.txt   ->  2

$ tt text match p.txt '^a'            # the sibling verb disagrees
     1: aaa
     2: abc
```

## Acceptance sketch

* `count` counts **matching LINES**, matching its own `// grep -c` comment and `grepr --count`:
  iterate `linesIterator` and count lines where `re.findFirstIn(line).isDefined`. That fixes both
  divergences at once — anchors start behaving per line as a consequence.
* Factor the predicate so `count`, `match` and `grepr` share one line-matches-pattern function; the
  bug exists because three verbs implement the same idea separately.
* If a matches-not-lines count is genuinely wanted, it is a **separate flag** (`--occurrences`), not
  the default — the default must match the documented `grep -c`.
* Tests, all pure and platform-independent, over the 3-line probe above: `'^a'` = 2, `'a'` = 2,
  `'b$'` = 1, and a no-match pattern = 0. The `'^a'` case is the regression guard and would have
  caught this without any special environment.
* Check the help text in `TextHelp` states line semantics explicitly, since `grep -c` is the promise
  being made.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-23 19:0x

Filed from a warp-ember exit audit. The finding is doubly on-theme for this repo: the session that
found it had spent the day on a class of bug where **a no-op or a wrong answer is indistinguishable
from a correct one** (see the silent-no-op override keys recorded in that session's harvest), and then
the audit found the same shape living inside the toolbox that is supposed to be the trustworthy
alternative to raw shell.

Both divergences were measured on an isolated 3-line file rather than inferred from the source, and
the source line was then read to confirm the mechanism — `findAllIn` over `readUtf8(file)` with no
`MULTILINE`. The `grepr --count` comparison was re-run in a directory containing only the probe,
because a first attempt against a shared `tmp/` returned 1299 and would have proved nothing.

Suggested priority: higher than its size implies. The fix is small (one `linesIterator` loop) but the
defect silently corrupts any reasoning that used the number, and unlike issue 022 it is not gated on
a platform — it is wrong everywhere, today, for the patterns the house style recommends.

Agent disclosure: the discovery, the reduction and this issue text were produced by an AI agent
(Claude Opus 5) under human direction; the human reviewed and submitted.
