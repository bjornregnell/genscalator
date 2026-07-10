# Pasted from  https://chatgpt.com/c/6a50ce3f-f000-83eb-b23b-eb549ac1c88a

> Given the genscalator approach as documented in https://codeberg.org/bjornregnell/genscalator answer these questions with brief sharp answers after very deep investigations: 1. What is truly novel with genscalator (if anything)? 2. Why has not Anthropic done something similar to genscaltor (typed tools etc) for Claude Code?

Based on the public agent ecosystem (Claude Code, OpenAI Codex, MCP, typed agent frameworks, Kotlin/Java/Go libraries, etc.) and what I could infer about genscalator's goals, here is my assessment.

### 1. What is truly novel with genscalator (if anything)?

**If the implementation matches the design, then yes, there are genuinely novel aspects.**

The most interesting contribution is **not** "typed tools" by themselves.

Typed tools already exist in several frameworks:

* Agents.KT has statically typed tools with JSON schema generation. ([GitHub][1])
* Many agent libraries (Go, Java, etc.) expose typed tool registries. ([pkg.go.dev][2])

The potentially novel part is the **combination** of:

1. **Native Scala 3 type system as the agent DSL**

   * tools are ordinary Scala values/functions
   * compile-time checking
   * enums, opaque types, ADTs, givens, extension methods
   * no secondary DSL

2. **Type-safe orchestration instead of JSON-first orchestration**

   * JSON becomes an implementation detail rather than the programming model.

3. **Bringing agent programming into the host language**
   rather than
   "LLM API + handwritten schemas + glue code".

This is closer to how LINQ embedded querying into C# than how most current AI SDKs work.

If genscalator succeeds in making the *entire* agent pipeline feel like ordinary Scala programming, that is a significant design contribution.

---

### 2. Why hasn't Anthropic done something similar for Claude Code?

I think there are several reasons.

#### (a) Claude Code is intentionally language-agnostic.

Anthropic wants Claude Code to work equally well with

* Python
* Rust
* Go
* Java
* C#
* Scala
* TypeScript
* C++

A Scala-specific typed abstraction would only help a tiny fraction of users.

---

#### (b) Claude Code optimizes for natural-language programming.

Their philosophy is approximately:

> the prompt is the program.

Strong static typing works against that philosophy because it introduces compile-time structure before the model starts reasoning.

Anthropic generally prefers flexible protocols (MCP, JSON schemas, Markdown instructions) over language-specific APIs.

---

#### (c) Typed APIs reduce flexibility.

Suppose a tool evolves from

```scala
search(query: String)
```

to

```scala
search(query: String, depth: Int, filters: ...)
```

A strongly typed API requires recompilation.

A JSON protocol often keeps working.

Anthropic optimizes for rapid iteration.

---

#### (d) Claude Code is a product, not a programming language.

Anthropic's investments have instead gone into

* MCP
* Claude Code
* skills
* subagents
* memory
* permission models

rather than creating an embedded DSL for one language.

---

### My overall assessment

If genscalator merely says

> "Scala wrappers around tool calls"

then it is **not** novel.

If it instead achieves

> **agent programming as statically typed Scala programming, where the compiler becomes part of the agent framework**, with JSON and schemas largely hidden,

then I think it occupies a space that is surprisingly empty today. Existing work tends to stop at typed tool definitions, whereas the orchestration layer remains dynamic and JSON-centric. ([GitHub][1])

I would summarize the novelty as:

> **Not "typed tools", but making the *host language's type system* the primary abstraction for building LLM agents, rather than treating typing as a thin wrapper around JSON schemas.**

That is a stronger and more distinctive claim than simply offering compile-time tool signatures.

[1]: https://github.com/Deep-CodeAI/Agents.KT?utm_source=chatgpt.com "GitHub - Deep-CodeAI/Agents.KT: Typed Kotlin DSL framework for AI agent systems. · GitHub"
[2]: https://pkg.go.dev/codeberg.org/gai-org/gai?utm_source=chatgpt.com "gai package - codeberg.org/gai-org/gai - Go Packages"

