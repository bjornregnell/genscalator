# Project code conventions (Scala 3)

Follow these conventions when writing code for this project:

1. Immutability and visibility: prefer `val` over `var`. Expose immutable state as
   public `val`s and computed values as public pure `def`s. Do NOT hide them behind
   `private` without a specific reason.
2. Braces: put braces `{ }` on long or multi-statement scopes; short single-expression
   scopes may use Scala 3 significant indentation.
3. Scala 3 idioms (latest stable): use `enum` for closed sets of cases; use `extension`
   methods and `using`/context parameters where apt; prefer current Scala 3 syntax over
   Scala 2 forms.
4. Functional style: prefer expressions over statements; use `Option` instead of `null`;
   use collection combinators (`find`/`map`/`filter`/`fold`) over manual loops; use
   immutable collections.
5. Naming: descriptive camelCase, meaningful names.
