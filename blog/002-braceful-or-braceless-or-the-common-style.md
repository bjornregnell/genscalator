# Braceful, braceless, or the common style?

> **Status: drafted 2026-07-03.** **Author: Björn Regnell.** A Scala-syntax post that judges braces vs
> significant indentation not only by human taste but by a second, newer yardstick: **what it costs a coding
> *agent* to edit the code.** Draft — comments welcome before it is published.
> **Audience:** Scala developers weighing a Scala-3 style policy; language designers and SIP folk; builders of
> agentic coding tooling; and anyone setting a codebase style convention for AI-assisted development. No prior
> context on this project is assumed — it is meant to be read on its own.
> Sources: the open note *["Towards a Common Scala Style Recommendation"](https://docs.google.com/document/d/14ZBGKNHUW4d8hDWIi5i6QquClX3_iXva-iMy5KpFU3I/edit?usp=sharing)*
> (Odersky, Regnell & Kerr, 2026); the genscalator indent-vs-braces edit-cost experiment
> ([`../research/experiments/indent-vs-braces/RESULTS.md`](../research/experiments/indent-vs-braces/RESULTS.md),
> [`README.md`](../research/experiments/indent-vs-braces/README.md)); and the framing notes
> [`../research/scala-style-evolution.md`](../research/scala-style-evolution.md) and
> [`../research/scala-style-recommendations.md`](../research/scala-style-recommendations.md).

## The short version

Scala 3 lets you write the same program two ways: **braceful**, using `{ }` to delimit blocks, or **braceless**,
using significant indentation (the way Python delimits blocks). Both compile to exactly the same thing. Which
should you use? The debate has always been about *human* readability — and it has never really been settled,
because it is largely a matter of taste.

This post adds a second question that did not exist a few years ago: **how much does each style cost a *coding
agent* — an LLM like Claude or GPT — to *edit* the code?** More and more code is now modified by agents, and it
turns out they are not neutral about whitespace. I ran an experiment to measure it. The honest answer is more
interesting than "braces win" or "indentation wins":

- Across a range of *smaller, local* models, braceless code was **costlier to edit** — but the effect was
  **bidirectional per model** and dominated by whether a model can reliably *produce* a given style at all.
- At the frontier, the effect **vanishes**: a strong model (Claude Opus 4.8) edited every style flawlessly.
- So the practical rule is not "always braces." It is: **design the code style for the *weakest* agent you
  expect to edit the code, not the strongest** — and a **"common style"** that mixes braces and indentation by
  a simple rule serves humans and agents at the same time.

Let me build that up from scratch.

## 1. Two syntaxes, one language

Here is a tiny function in both styles. First **braceless** (significant indentation):

```scala
def scan(s: String): String =
  val sb = StringBuilder()
  var i = 0
  while i < s.length do
    if s(i) == 'a' then sb ++= "A"
    else sb += s(i)
    i += 1
  sb.toString
```

And the same function **braceful**:

```scala
def scan(s: String): String = {
  val sb = StringBuilder()
  var i = 0
  while (i < s.length) {
    if (s(i) == 'a') sb ++= "A"
    else sb += s(i)
    i += 1
  }
  sb.toString
}
```

They are identical to the compiler. In the braceless version, the *indentation itself* tells the compiler where
the `while` body ends. In the braceful version, the `{ }` do, and the indentation is just for human eyes.

That difference is the whole story, because it changes what happens when you **edit** the code.

## 2. The old debate: human readability

The traditional arguments are familiar and mostly aesthetic. Braceless fans point to less visual clutter and
smaller diffs. Braceful fans point to unambiguous nesting, safer copy-paste, and not having to trust invisible
whitespace. Both are right, and after years of argument the community remains split — which is a strong hint
that *for humans*, it really is a matter of degree and taste, not correctness.

## 3. The new question: what does it cost an *agent* to edit?

Here is what changed. A growing share of code is edited by AI agents, and an agent's dominant workload is not
writing fresh code — it is **modifying existing code**: wrap this block in an `if`, extract these lines into a
helper, add a branch. And for *editing*, the two styles are not symmetric:

- **Braceful edit:** to wrap a block in a new `else`, an agent inserts `else { … }`. The block's own indentation
  is irrelevant to the compiler, so the edit is **local** — a couple of characters — and hard to get wrong.
- **Braceless edit:** the same wrap forces the agent to **re-indent every line of the block** one level deeper.
  The edit is **global** and **whitespace-fragile**: miss one line's indentation and the block silently changes
  scope.

That "silently changes scope" case is the scary one, and it is not hypothetical. The seed of this whole
investigation was a real bug: on 2026-07-02, an agent editing braceless Scala wrapped a block in an `else`, left
one line at the wrong indentation, and a statement escaped its intended branch — code that still *compiled* but
did the wrong thing. A single anecdote proves nothing, though. So the question became measurable: **do agents
actually make more editing errors in braceless code, and does it get worse for bigger blocks?**

## 4. The common-style proposal

Independently, a concrete proposal for a middle path was written up by Martin Odersky, myself, and Rex Kerr in an
open note, *["Towards a Common Scala Style Recommendation"](https://docs.google.com/document/d/14ZBGKNHUW4d8hDWIi5i6QquClX3_iXva-iMy5KpFU3I/edit?usp=sharing)*
(January 2026). Its thesis is that braceless-vs-braceful "need not be incompatible" — it is "a matter of degree,"
like the way Scala programmers already use optional parentheses without starting a war. The pragmatic middle:

> **Use braces to delimit *long* blocks; let *short* code stay braceless.** A block counts as "long" when it
> "contains blank lines which are not already embedded in a nested construct."

The note gives six recommendations (condensed): prefer braces over `end` markers; put braces around long scopes,
except where a closing keyword like `else`/`case`/`catch` already ends the scope; add blank lines to compensate
for fewer braces; add braces anywhere they aid understanding; prefer the new control syntax (`if-then-else`); and
for very short bodies, make **no** recommendation — the authors themselves disagree.

Crucially, the note **calls for exactly the experiment this post reports** — "a measurable experiment [comparing]
edit-error-rates and token costs" across the three styles. The note argues, from first principles plus an
analysis contributed by Claude Code, that braces-on-long-scopes is a "sweet spot" where *"the human-legibility
rule and the agent-edit-safety rule coincide."* This post puts numbers on that intuition — and complicates it.

## 5. The experiment

The full harness, raw data, and analysis scripts are in the repository
([`../research/experiments/indent-vs-braces/`](../research/experiments/indent-vs-braces/)); everything below is
reproducible.

### 5.1 Research questions

- **RQ1 (emission):** Can a given model reliably *produce* code in a requested style at all?
- **RQ2 (correctness given emission):** *Among* outputs that do use the requested style, does the style affect
  how often the edit is *correct* — and does any gap grow with block size?
- **RQ3 (capability):** Does a strong frontier model reach near-perfect emission and low, style-insensitive
  error — i.e. does the style effect disappear as capability rises?

Separating RQ1 from RQ2 matters because "the model got it wrong" hides two very different failures: a model that
*cannot produce* braceless code, and a model that produces it fine but *edits it wrongly*. Conflating them is
exactly the trap the pilot fell into (below).

### 5.2 Hypotheses

- **H1:** emission depends on model × style — some models simply cannot emit some styles.
- **H2:** controlling for emission, braceless edits are at least as error-prone as braceful, and the gap grows
  with block size.
- **H3:** a frontier model (Opus 4.8) shows near-ceiling emission and low, style-insensitive error.

### 5.3 Data collection

The task is a **structural edit** — the class where whitespace is load-bearing: wrap an existing block in a new
`else` branch, at three growing block sizes (small, medium, large). Each cell of the experiment gives a fresh
agent the "before" file rendered in one style plus a style-neutral instruction, takes one edit attempt, then
grades it automatically with a script that (a) checks it **compiles** and (b) runs a **behavioural probe** —
does the edited function actually produce the oracle's output? Outcomes: **PASS**, **FAIL_COMPILE** (a loud
error), or **FAIL_MISSCOPE** (compiles but wrong — the *silent* hazard). A second measurement records
**emission-conformance**: did the output actually use the requested style?

- **Subjects:** 7 local open models run on a GPU box (the qwen2.5 family incl. coder variants, gemma2/gemma3,
  aya-expanse), plus a frontier anchor, **Claude Opus 4.8**, run through a subagent workflow on the identical
  tasks.
- **Volume:** 7 × 3 tasks × 3 styles × 6 repeats = **378 local cells**, plus 27 Opus cells.
- Raw rows: [`results-raw.tsv`](../research/experiments/indent-vs-braces/results-raw.tsv). (A note on
  terminology: the code and raw data call the style variable `regime`; the prose now says *style*.)

### 5.4 Results

**A cautionary tale first.** An early run with only 4 models produced a clean, quotable headline: *braceless
leads to more **silent** mis-scope bugs.* It was wrong — or rather, it did not survive more data. Adding 3 more
models overturned it: the extra braceless failures turned out to be mostly **loud compile errors**, not silent
ones, and the silent-mis-scope counts came out roughly **equal** across styles (braceless 37, braceful 36,
common 30). The tidy safety story was an artifact of a small sample. It is kept visible in the results as a
lesson: *small model-sets mislead.*

**What held (378 cells):** braceless was the **costliest style in aggregate** — 45% raw pass-rate vs 63%
(braceful) and 61% (common) — and it was worst at **every** block size, with error rising as blocks grow. So the
*direction* of the thesis survives: braceless edits cost agents more, on average.

![Edit-error rate by block size and style — braceless is worst at every size and error rises with size.](figures/fig-size-style.svg)

*Figure 1 — Edit-error rate (fails ÷ attempts), all 7 local models pooled, by block size. Braceless is costliest
at every size, and the rate climbs as blocks grow — the "gap grows with size" prediction, made visible.*

**What the separation revealed (the real finding).** Splitting emission from correctness dissolved the aggregate
into something sharper. Emission was near-perfect for everyone **except** aya-expanse, which literally **cannot
produce braceless code** (0/18) — it always emits braces. Once you look only at outputs that *did* use the
requested style, the picture is **bidirectional and model-specific**:

![Edit-error rate by model and style — the effect flips per model.](figures/fig-model-style.svg)

*Figure 2 — Raw edit-error rate per model, all 7 local models. The two 100% bars point **opposite** ways —
aya-expanse fails every braceless edit, gemma3 fails every braceful edit — while the qwen-coder variants sit near
zero everywhere (strong coders are style-robust). A single averaged "braces vs braceless" number would hide all
of this, which is why the table below decouples emission from correctness.*

| model | braceless | braceful | common |
|---|---|---|---|
| aya-expanse:8b | *(cannot emit)* | 100% | 89% |
| gemma2:9b | 33% | 83% | 33% |
| gemma3:latest | 39% | **0%** | 44% |
| qwen2.5-coder:7b | 83% | 100% | 94% |
| qwen2.5:7b | **72%** | 39% | 56% |
| **Opus 4.8 (anchor)** | **100%** | **100%** | **100%** |

*(conditional correctness = PASS given the output used the requested style; selected rows — full table in the
results file.)*

Look at the extremes. `aya-expanse` fails braceless 100% — but that is an **emission** failure (it can't produce
the style), not an editing failure. `gemma3` fails braceful 100% — but it emits braceful perfectly and then
**botches every edit**, a pure **correctness** failure. "Error rate" had been silently averaging two completely
different mechanisms. And `qwen2.5:7b` actively edits **braceless better than braceful** (72% vs 39%) — the
opposite of the thesis. There is no universal "braces are safer for agents" law at the per-model level.

**RQ3 — the frontier.** Opus 4.8 scored **27/27 PASS**: 100% emission-conformant and 100% edit-correct in every
style, task, and size. For a strong model, the style simply does not matter here.

### 5.5 Threats to validity

These are load-bearing; the result is a **pilot**, not a verdict:

- **One edit family** (wrap-in-`else`) and only 3 tasks — other edits (extract, add-branch, rename-reindent) may
  behave differently.
- **Emission is a proxy** (a brace-signature check), not a perfect measure of "is this really that style."
- The **common style was under-tested**: the tasks had no blank-line scopes, so "common" and "braceless" started
  from the same file — common differed only as an instruction, not as distinct code. Fixing this needs
  blank-line tasks.
- **R = 6** locally (R = 3 for Opus), single session, one GPU box. Rates are indicative, not tight.
- Local models' raw "error rate" is heavily confounded by **emission ability** — a model that cannot emit a
  style scores 100% "error" regardless of its editing skill. (That confound *is* the finding, but it means the
  raw aggregate must be read with care.)

### 5.6 Reproduce it yourself

Everything needed to re-run or re-analyse this lives in the repository under
[`research/experiments/indent-vs-braces/`](../research/experiments/indent-vs-braces/):

- **Raw data** — [`results-raw.tsv`](../research/experiments/indent-vs-braces/results-raw.tsv): one row per cell
  (`task, style, model, run, graded, out_tokens, diff_lines`), where `graded` is `PASS` / `FAIL_COMPILE` /
  `FAIL_MISSCOPE`.
- **Task corpus** — `tasks/`: the *before* files per style + the style-neutral edit instruction + the oracle.
- **Grader** — `grade.scala`: compiles the candidate and a probe, runs a behavioural probe, and emits the grade
  (so "correct" means *behaves* like the oracle, not *looks* like it).
- **Sweep runner** — `sweep.scala`: loops `task × style × model × R`, prompts each local model (via Ollama/modly),
  grades, appends rows to the TSV.
- **Analysis + figures** — `analyze.scala` (the tables) and `charts.scala` (Figures 1–2 above, generated straight
  from `results-raw.tsv`).

End-to-end: point `sweep.scala` at your models, then `scala-cli run sweep.scala -- 6 <models…>` →
`scala-cli run analyze.scala` → `scala-cli run charts.scala -- results-raw.tsv figures/`. The Opus-4.8 anchor ran
the *identical* tasks × styles through a subagent workflow, graded by the same `grade.scala`. If you change one
thing, change **R and the model set** and watch the aggregate move — that is the cautionary tale (a 4-model run
gave the opposite "silent-misscope" headline) reproduced as a knob you can turn.

## 6. What it means

Three takeaways survive the caveats:

1. **"Error rate" was hiding two mechanisms** — *can the model produce the style* and *can it edit correctly in
   it* — and they have different cures. Always separate them.
2. **The style effect on editing is a weak-to-mid-model phenomenon.** It is real in aggregate for smaller/local
   models but modest and sometimes reversed per model.
3. **It vanishes at the frontier.** A strong agent is style-indifferent here.

Which points to a design rule with a nice symmetry to the human side:

> **Design your code style for the *weakest* agent you expect to edit the code, not the strongest.**

If frontier agents (and skilled humans) are style-robust, then the value of a disciplined style is precisely that
it protects the *weaker* editors — smaller local models, cheaper tiers, tired people at 2 a.m. And that is
exactly what the **common style** buys: braces where a block is long enough to be genuinely ambiguous (the case
that trips weak editors), indentation where it is short and safe (where nobody trips). It is not a compromise
that pleases no one; it is the rule that makes the human-legibility case and the agent-edit-safety case
*coincide* — now with data showing where that coincidence actually bites, and where (at the frontier) it stops
mattering.

## 7. Beyond Scala

The general point outlives this one language. When agents are primary authors and editors, **readability becomes
a two-audience problem**: a syntax choice is now also an *ergonomics* choice for the models that will maintain the
code. That is a genuinely new input to language and style design — "design the surface syntax for the tooling
substrate, not just for human taste." Scala, with two syntaxes for one language and an active design process
(the SIP committee), is an unusually good laboratory for asking the question out loud. Consider this post one
experiment in doing exactly that.

## 8. How this post was made

In the spirit of open disclosure about AI's role (and following the substance of Springer's guidance on AI use
in research), here is an honest account — because this post is, itself, an example of the collaboration it
studies.

- **I (Björn Regnell) am the author** and take responsibility for the claims. I am a co-author of the common-style
  note and a member of the Scala SIP committee, so I have a stake in the debate — which is a good reason to
  ground it in data rather than taste.
- **The experiment was built and run by a coding agent** (Claude, Opus 4.8) working with me: I set the questions,
  the design decisions, and the honesty bar; the agent wrote the test harness, ran the 378 local-model cells
  autonomously overnight, graded them, and drafted the analysis — and this post. Every number here is
  reproducible from scripts and raw data in the repository; nothing is hand-asserted.
- **There is a genuine reflexivity here worth naming:** the agent helped study *how agents edit code*, and one of
  the experimental subjects (Opus 4.8) is the same kind of model that did the building. That is a real bias to
  disclose, and part of why the design leans on an automatic, behavioural grader and a public raw dataset rather
  than on the agent's own judgement.
- **The AI is a tool, not an author.** It cannot be accountable for the work, so it is not credited as an author;
  its role is disclosed here instead. The mistakes are mine; the useful parts are ours.

If that arrangement sounds like it needs its own guard-rails — transparency, reproducibility, not letting the
tool grade its own homework — you are reading the right blog. That is much of what
[genscalator](000-why-genscalator.md) is about.

## References

- M. Odersky, B. Regnell, R. Kerr, *Towards a Common Scala Style Recommendation*, open note, January 2026.
  <https://docs.google.com/document/d/14ZBGKNHUW4d8hDWIi5i6QquClX3_iXva-iMy5KpFU3I/edit?usp=sharing>
- genscalator, *indent-vs-braces edit-cost experiment* — design
  [`README.md`](../research/experiments/indent-vs-braces/README.md), results
  [`RESULTS.md`](../research/experiments/indent-vs-braces/RESULTS.md), raw data
  [`results-raw.tsv`](../research/experiments/indent-vs-braces/results-raw.tsv).
- Background: [`../research/scala-style-evolution.md`](../research/scala-style-evolution.md),
  [`../research/scala-style-recommendations.md`](../research/scala-style-recommendations.md).
