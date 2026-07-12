# SM045 fix-to-green suite - DRAFT for BR spot-check

> **Contingent draft.** This exists so SM045 plan **Q4** ("agent drafts ~10 suite items for your spot-check")
> becomes a yes/no with a real artifact in front of you, rather than an abstraction. It **assumes Q1 = fix-to-green
> primary** and **Q4 = yes**. If you pick the schema'd-extraction class (Q1) or decline agent-authoring (Q4), bin
> this - it cost only AFK time. **Nothing runs.** On your OK, these promote into the frozen suite tree
> `research/experiments/te-lagom-fleet/suite/` (one dir per item) that the plan's §3 describes.

**Verified both directions (agent, `scala-cli test`, 2026-07-12):** all 10 reference fixes pass their gold tests
(31/31 green), and every buggy `input.scala` fails at least one test - so each item genuinely discriminates a fix
from the bug. The graded-quality design also checks out: on M2/M3/H1/H2/H3 the buggy version fails *only* its
edge-case test and passes the easy ones, yielding the partial scores (e.g. 2/3) the pilot's knee-finding needs
rather than a blunt pass/fail.

Companion to [`sm045-te-efficiency-pilot-plan.md`](sm045-te-efficiency-pilot-plan.md). The harness contract is
plan §5 (a `te-pilot.scala` that, per `(tier, item, rep)`, sends the model the buggy `input.scala` + the gold
`test.scala`, writes back the patched file, runs `scala-cli test`, and records **quality = fraction of that item's
tests passing**). This doc is only the **inputs** to that harness.

## Design rationale (why these 10)

- **Each item is one small, self-contained Scala 3 top-level `def`** with a single **planted bug**, plus a `munit`
  test file that **fails on the bug and passes on the fix**. Small = the authoring/reviewing cost stays low (plan
  §9: authoring the suite is the expensive resource, not running it).
- **Difficulty is bug subtlety, and it is calibrated to make the knee visible** (plan §2): easy bugs a cheap tier
  should always clear; hard bugs pass the buggy code on *many* inputs and only fail on a specific edge, so a model
  has to actually reason, not pattern-match. That spread is what separates Haiku from Opus on the curve.
- **Bug-type diversity** (not 10 flavours of one mistake): wrong operator, inverted condition, wrong base case,
  wrong combinator, off-by-one boundary, missing normalization, integer-division truncation, over-eager library
  call. A tier that aces operators but misses edge cases shows up as a *pattern*, which is more informative than a
  single scalar.
- **No item can hang the grader** (no possible infinite loop / unbounded recursion in the buggy form) - a hang
  would poison the wall-time measurement. Every buggy version terminates; it just returns a wrong answer or throws.

Per-item layout when promoted: `suite/<id>/input.scala` (given to the model), `suite/<id>/test.scala` (gold, given
to the model as the spec), `suite/<id>/meta.json` `{ "id", "difficulty", "bug_type", "one_line" }`. The
"reference fix" blocks below are **for your spot-check only** - the model never sees them.

---

## EASY (3) - a cheap tier should clear all of these

### E1 - `celsiusToFahrenheit` (bug: missing term)
```scala
// input.scala
def celsiusToFahrenheit(c: Double): Double = c * 9.0 / 5.0
```
```scala
// test.scala
class E1 extends munit.FunSuite:
  test("boiling")  { assertEqualsDouble(celsiusToFahrenheit(100.0), 212.0, 1e-9) }
  test("freezing") { assertEqualsDouble(celsiusToFahrenheit(0.0),   32.0,  1e-9) }
  test("body")     { assertEqualsDouble(celsiusToFahrenheit(37.0),  98.6,  1e-9) }
```
*Reference fix (BR only):* append `+ 32.0`. Buggy gives 180/0/66.6.

### E2 - `isEven` (bug: inverted predicate)
```scala
// input.scala
def isEven(n: Int): Boolean = n % 2 == 1
```
```scala
// test.scala
class E2 extends munit.FunSuite:
  test("four")  { assert(isEven(4)) }
  test("seven") { assert(!isEven(7)) }
  test("zero")  { assert(isEven(0)) }
```
*Reference fix:* `n % 2 == 0`.

### E3 - `larger` (bug: returns the smaller)
```scala
// input.scala
def larger(a: Int, b: Int): Int = if a < b then a else b
```
```scala
// test.scala
class E3 extends munit.FunSuite:
  test("a<b")   { assertEquals(larger(3, 7), 7) }
  test("a>b")   { assertEquals(larger(10, 2), 10) }
  test("equal") { assertEquals(larger(5, 5), 5) }
```
*Reference fix:* `if a < b then b else a` (or `>` swapped).

---

## MEDIUM (4) - a mid tier should clear most; edge cases start to bite

### M1 - `factorial` (bug: wrong base case)
```scala
// input.scala
def factorial(n: Int): Int =
  if n == 0 then 0 else n * factorial(n - 1)
```
```scala
// test.scala
class M1 extends munit.FunSuite:
  test("0!") { assertEquals(factorial(0), 1) }
  test("1!") { assertEquals(factorial(1), 1) }
  test("5!") { assertEquals(factorial(5), 120) }
```
*Reference fix:* base case returns `1`. The buggy base `0` zeroes every result - a whole-suite-failing bug, but the
fix requires spotting that the base, not the recursion, is wrong.

### M2 - `reverse` (bug: wrong list combinator = identity)
```scala
// input.scala
def reverse[A](xs: List[A]): List[A] =
  xs.foldLeft(List.empty[A])((acc, x) => acc :+ x)
```
```scala
// test.scala
class M2 extends munit.FunSuite:
  test("three") { assertEquals(reverse(List(1, 2, 3)), List(3, 2, 1)) }
  test("one")   { assertEquals(reverse(List(42)), List(42)) }
  test("empty") { assertEquals(reverse(List.empty[Int]), Nil) }
```
*Reference fix:* `x :: acc` (prepend) instead of `acc :+ x` (append). The buggy version returns the list unchanged,
so the one-element and empty tests pass - only the real reversal test exposes it.

### M3 - `binarySearch` (bug: off-by-one loop bound)
```scala
// input.scala
def binarySearch(arr: Vector[Int], target: Int): Int =
  var lo = 0
  var hi = arr.length - 1
  while lo < hi do            // stops one iteration too early
    val mid = (lo + hi) / 2
    if arr(mid) == target then return mid
    else if arr(mid) < target then lo = mid + 1
    else hi = mid - 1
  -1
```
```scala
// test.scala
class M3 extends munit.FunSuite:
  val a = Vector(1, 3, 5, 7, 9)
  test("last")    { assertEquals(binarySearch(a, 9), 4) }   // exposes the bug
  test("middle")  { assertEquals(binarySearch(a, 5), 2) }
  test("first")   { assertEquals(binarySearch(a, 1), 0) }
  test("absent")  { assertEquals(binarySearch(a, 4), -1) }
```
*Reference fix:* `while lo <= hi`. With `<` the search misses a target sitting alone at `lo == hi` (e.g. the last
element), but succeeds for values it lands on earlier - so only the boundary test fails.

### M4 - `countVowels` (bug: missing case normalization)
```scala
// input.scala
def countVowels(s: String): Int =
  s.count(c => "aeiou".contains(c))
```
```scala
// test.scala
class M4 extends munit.FunSuite:
  test("mixed")  { assertEquals(countVowels("Apple"), 2) }   // 'A' missed by the bug
  test("upper")  { assertEquals(countVowels("AEIOU"), 5) }
  test("none")   { assertEquals(countVowels("xyz"), 0) }
```
*Reference fix:* `s.toLowerCase.count(...)`. The buggy version silently undercounts uppercase vowels; lowercase-only
inputs would hide it, so the tests deliberately include capitals.

---

## HARD (3) - the buggy code passes on many inputs; only a specific edge fails

### H1 - `average` (bug: integer division truncation)
```scala
// input.scala
def average(xs: List[Int]): Double =
  xs.sum / xs.length
```
```scala
// test.scala
class H1 extends munit.FunSuite:
  test("half")      { assertEqualsDouble(average(List(1, 2)), 1.5, 1e-9) }        // exposes truncation
  test("divisible") { assertEqualsDouble(average(List(2, 4, 6)), 4.0, 1e-9) }     // passes even buggy
  test("quarter")   { assertEqualsDouble(average(List(1, 2, 3, 4)), 2.5, 1e-9) }  // exposes truncation
```
*Reference fix:* `xs.sum.toDouble / xs.length`. The `Int / Int` truncates *before* the widening to `Double`, so the
result is only wrong when the sum is not evenly divisible - it passes cleanly on `List(2,4,6)`. This is the classic
bug that survives a shallow test suite.

### H2 - `isPalindrome` (bug: missing sanitization)
```scala
// input.scala
def isPalindrome(s: String): Boolean =
  val t = s.toLowerCase
  t == t.reverse
```
```scala
// test.scala
class H2 extends munit.FunSuite:
  test("phrase") { assert(isPalindrome("A man a plan a canal Panama")) } // spaces break the bug
  test("simple") { assert(isPalindrome("racecar")) }                     // passes even buggy
  test("no")     { assert(!isPalindrome("hello")) }                      // passes even buggy
```
*Reference fix:* filter to letters/digits first, e.g. `val t = s.toLowerCase.filter(_.isLetterOrDigit)`. The bug
lowercases but never strips spaces/punctuation, so it is correct on single-word inputs and only fails on the
real-world phrase palindrome.

### H3 - `dedupAdjacent` (bug: over-eager library call)
```scala
// input.scala
def dedupAdjacent[A](xs: List[A]): List[A] =
  xs.distinct
```
```scala
// test.scala
class H3 extends munit.FunSuite:
  test("runs")   { assertEquals(dedupAdjacent(List(1, 1, 2, 3, 3, 3, 1)), List(1, 2, 3, 1)) }
  test("nodups") { assertEquals(dedupAdjacent(List(1, 2, 3)), List(1, 2, 3)) }
  test("empty")  { assertEquals(dedupAdjacent(List.empty[Int]), Nil) }
```
*Reference fix (one option):*
```scala
def dedupAdjacent[A](xs: List[A]): List[A] = xs match
  case Nil     => Nil
  case h :: t  => h :: dedupAdjacent(t.dropWhile(_ == h))
```
`distinct` removes *all* duplicates, not just consecutive ones, so it drops the trailing `1` in the first test while
looking correct on any already-distinct input. The model has to notice "adjacent" is not "unique".

---

## Grading note (ties back to plan §5-6)

- **Per item, quality = tests passing / tests total** after the model's patch (so partial credit exists: a fix that
  handles the easy tests but not the edge case scores e.g. 2/3). This finer signal is better than pass/fail for
  locating the knee.
- **Suite aggregate quality Q** = mean over items x reps. The **floor filter** (plan §6, default Q >= 0.9) then
  picks the lagom fleet.
- **Difficulty stratification is reportable**: also break Q down by easy/medium/hard, because "Haiku clears easy +
  medium but not hard" is exactly the kind of shape the pilot exists to find - more useful than a single blended Q.

## What I need from you (Q4, made concrete)

1. Are these 10 a good spot-check basis - right size, right difficulty spread, bug types you trust as a fair grader?
2. Any item you'd cut or swap (e.g. M4 reads borderline-easy; H-tier could go one notch subtler)?
3. On your yes to Q1 (fix-to-green) + Q4 (agent-authored), I promote these into `suite/<id>/` with `meta.json` and
   wire the `te-pilot.scala` harness (plan §5) - still **before any paid run**, which stays your call (SM045 run is
   held).
