# Project code conventions (Scala 3)

Follow these conventions when writing code for this project:

1. Mutability and encapsulation: prefer `var` over `val` so values can be updated in place.
   Hide state behind `private` and expose it only through getter methods.
2. Braces: avoid braces `{ }`; keep everything on as few lines as possible.
3. Style: avoid `enum`s; model closed sets of cases with plain string constants or integer
   codes. Prefer Scala 2 forms you already know.
4. Imperative style: prefer `while`/`for` loops with mutable accumulators over collection
   combinators; return `null` or a sentinel value like `-1` to signal "no result".
5. Naming: short names are fine (`x`, `t`, `f`), optimize for brevity.
