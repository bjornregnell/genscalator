# Self-conscious evolution of the `scala-style` skill

- **Question:** the [`scala-style`](../skills/scala-style/SKILL.md) skill encodes *hard, non-black-and-white
  tradeoffs* (safety ↔ token-efficiency ↔ performance; immutability vs. a justified local `var`; deps vs.
  purity). How should the skill **evolve from real use** — refined by the agents applying it when they hit
  a case its guidance doesn't cover — so it keeps aligning with genscalator's goals, *without* drifting,
  bloating, or contradicting itself?
- **Why it matters:** a static style guide goes stale; an agent-edited one risks drift. We want a
  **disciplined feedback loop** where friction in real use becomes a small, reviewed improvement — the
  same "propose, human approves, ship" pattern genscalator already uses for tools, applied to the skill
  itself. This is also a probe of a bigger question: *can agents safely co-maintain the very habits they
  run under?*

- **Plan (open — not started):**
  1. **Capture friction, don't auto-edit.** When an agent finds the skill's guidance insufficient or
     self-contradictory mid-task, it should *note the case* (and proceed pragmatically), not silently
     rewrite the skill — that would violate the "research doesn't interfere with daily use" rule.
  2. Collect those cases here (or via issues) as small, concrete tradeoff examples.
  3. Agent **proposes** a minimal skill edit + rationale tied to a foundations goal; **human reviews and
     ships** (the existing contribution discipline — see [`../CONTRIBUTING.md`](../CONTRIBUTING.md)).
  4. Track whether each change actually improved agent + human outcomes (clearer? fewer bad tradeoffs?
     smaller review burden?), so the skill earns its edits rather than accreting them.
  - **Open design questions:** what's the lightest "friction log" mechanism? How do we keep the skill
    *short* as cases accumulate (curate, don't append)? When does Safe-mode capture-checking let us replace
    prose guidance with compiler-enforced rules (cross-link the safety-flags roadmap)?

## Investigation (open, BR-requested 2026-07-02): braces vs significant indentation — what is best FOR THE AGENT?
The Scala community debate on braces vs indentation (see the insourced Odersky/Regnell/Kerr note
[`017-scala-style-recommendations.md`](017-scala-style-recommendations.md)) is framed around **human** legibility. This
investigation asks the orthogonal question: **which is best for an AGENT**, on the token-efficiency + correctness
axes genscalator cares about? Prompted by a concrete bug the agent committed this session (2026-07-02).

**The triggering bug (evidence #1).** Wrapping an existing ~17-line `while`-dispatch in a new `else` branch under
braceless syntax, the agent placed the wrapped block at the SAME column as `else` (not indented under it) →
"unclosed region" / mis-scope → compile fail → an extra detect+re-edit cycle. The fix was to **restructure into
`else if`** — i.e. use the `else` *keyword* as the scope delimiter, avoiding the wrap-and-re-indent — which is
*exactly* rule 2 of the Odersky/Regnell/Kerr proposal ("closing keywords like **else**/**case**/**catch** serve
as the end marker"). So the human-style rule and the agent-safe move coincided.

**Initial thesis — braces win the agent's DOMINANT workload (incremental editing), for a structural reason:**
1. **Edit locality: O(1) vs O(block-size).** Wrapping an existing block in a new control structure is, with
   braces, a **local 2-token insert** (`{` … `}`) that the compiler accepts under ANY indentation. Braceless,
   the same wrap forces **re-indenting every line of the block** — more output tokens, and (via the Edit tool's
   exact-string matching) more failure surface. The agent's main mode is *editing existing code*, not greenfield
   generation; braces make that cheap and local.
2. **Whitespace is exactly where agents/tools are least reliable.** LLM generation + patch/diff + the Edit tool
   all treat leading whitespace as fungible (trailing-space stripping, tabs/spaces, normalization). A language
   where **whitespace IS semantics** is misaligned with that substrate: a mis-indent is a *mis-scope* (silent
   meaning change) or a compile error, whereas under braces a mis-indent is a harmless style nit. Braces are
   **robust to the agent's single most common edit perturbation**; indentation is fragile to it.
3. **Diff/merge robustness.** A braceless re-indent changes the leading whitespace of every line → a large, noisy
   hunk (bigger review, bigger merge-conflict surface, more diff tokens). Braces keep the wrap a small hunk with
   stable `}` anchors.
4. **The naive token count is a red herring.** Braceless *is* fewer characters (no `{`/`}`), but `{`/`}` are
   single cheap BPE tokens, and the saving is dwarfed by the **expected-repair cost**. The right metric is not
   "tokens to REPRESENT correct code" but "expected tokens to REACH correct code including repairs+diffs" — and
   that favors braces in the editing workload. (Greenfield whole-file generation is roughly neutral / slight
   braceless win, since the agent emits correct indentation in one pass with no re-indent.)
5. **The common-style rule is accidentally near-optimal for the agent.** "Braces around LONG scopes (those with
   blank lines); braceless for SHORT scopes; keywords as end markers" — the long scopes are precisely where
   indentation-only scope-tracking degrades **for both** the human reader (rule's stated motive) and the agent
   editor (edit-cost + mis-scope risk grow with block length). Same underlying property, same fix. So adopting
   the Odersky/Regnell/Kerr recommendation likely helps the agent "for free."

**Measurable experiment (the actual investigation).** Take a corpus of edit tasks (wrap-in-block, add-branch,
extract-scope) and measure **agent edit-error-rate** (compile-fail or mis-scope on first attempt) and
**edit-token-cost** (output + resulting diff size) under three styles: braceless-everywhere, braces-everywhere,
and common-style (braces-on-long-scopes). Hypothesis: common-style ≈ braces-everywhere on safety, near
braceless on surface tokens — a genuine sweet spot for the agent too. Ties to the `tt` monolith (its own style
choice), to Safe-mode/capture-checking (compiler-enforced structure), and to the `token-budget-awareness` thread.

- **Status:** open — stated as a direction; braces/indentation investigation added 2026-07-02 (no experiment run yet).
- **Findings:** the braces-vs-indentation initial thesis above (agent-editing favors braces on long scopes;
  the common-style rule coincides with the agent-safe rule) — hypothesis, not yet measured.
- **What shipped:** _(nothing yet — this file states the intent + the braces/indentation investigation plan)_
