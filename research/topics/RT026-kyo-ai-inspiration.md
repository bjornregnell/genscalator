# Research topic: kyo-ai — what inspiration for genscalator?

Status: **TO INVESTIGATE** (flagged by BR 2026-07-03). Not yet read in depth — this note captures the topic + the
angles to mine, so the investigation has a target.

Source: https://github.com/getkyo/kyo/blob/v1.0.0-RC5/kyo-ai/README.md
(kyo repo: https://github.com/getkyo/kyo — pin the version above when reading, APIs move pre-1.0.)

## What it is (to confirm on read — general prior, not yet verified against the README)
**Kyo** is a Scala 3 library for **algebraic effects** — effects are tracked *in the types* (a computation's
pending effects are part of its type; handlers discharge them), an alternative to monad-transformer stacks. **kyo-ai**
is its LLM/agent module built on that effect system.

## SCOPE (BR 2026-07-03) — the effect system is OUT, the data model is IN
Kyo is **NOT direct style** — it's an effect-oriented (monadic-in-the-types) design. **genscalator is committed to
DIRECT style** (see `skills/scala-style` §1 — direct style, lean deps; and [[DESIGN-single-dispatcher]] — tools are
plain functions returning `Either[ToolError, ToolResult]`, one dispatcher owns IO). **So the whole complex effect
system is out of scope — we are NOT adopting kyo's effects, and Safe-mode / the dispatcher stay a direct-style
design, not a kyo port.** What we CAN learn from is the *other* layer: **how kyo-ai types an AI-model call and
shapes its data model** — that part is style-agnostic and portable into direct-style code.

## What to mine (narrowed to typed model calls + data model)
- **Typed model-call surface** — how is a single LLM/agent call typed? Request/response types, model/params
  config, the return type of "call the model." We currently do ad-hoc `requests.post` to ollama (autotranslator
  model tier; the modly server; the indent-vs-braces sweep) with hand-parsed JSON — is there a cleaner *data*
  shape to copy (typed request, typed response, typed errors)?
- **Structured output** — how does kyo-ai get typed/validated results out of a model (schemas, decoders, retry on
  malformed)? Reusable as plain values in our sweeps/autotranslator without any effect machinery.
- **The data model for messages/tools/streaming** — message roles, tool-call descriptors, streaming chunks *as
  data types*. Even if we never touch kyo's effects, its ADTs for these may be a good template.

## Investigation questions to answer
- What are kyo-ai's **data types** for a model call (request, response, message, tool-spec, stream chunk), setting
  the effect wrapper aside? Which are worth re-expressing in direct style?
- How does it get **structured/validated output** from a model, and can that validation be lifted out as a plain
  function `String => Either[Err, T]`?
- Dependency weight / Native-friendliness is **only relevant if** we lift actual code — for pure inspiration
  (borrow the data model, reimplement direct-style) it doesn't bind us to kyo as a dep.

## Output of the investigation
A short verdict scoped to: which **data-model / typed-call patterns** (if any) to reimplement in direct style for
our ollama/model-call code — explicitly NOT whether to adopt kyo's effect system (ruled out).
