# Why agents regress to trained-prior defaults despite explicit instructions

- **Question:** Why does an agent **repeatedly revert** to dynamic-shell bundles (`cd && … | grep | head`,
  `for/if`, `echo` headers) even when a typed `tt` path exists, the rule is in `AGENTS.md`/memory, AND it
  just did the right thing one call earlier? Is something *external* pulling it back, or is it the model's own
  reflexes resisting instruction? And what correction mechanism actually **sticks**?
- **Why it matters:** this is the **foundational justification for genscalator's whole method.** If explicit
  instructions reliably changed behavior, the answer to confirmation fatigue would just be "write better
  rules." They don't — so the answer has to be **environmental** (build the tool, allowlist it, hook-reject
  the anti-pattern). Understanding *why* adherence leaks tells us **which interventions are worth building**
  (tools + hooks) vs which are theater (more exhortation in a doc the agent skims once).
- **Status:** open (mechanism hypothesis below, drawn from this project's own WR-data regressions as evidence).

## The phenomenon (from `wr-data/introprog-autotranslate.md`)
The agent regressed to dynamic shell **after** demonstrating the typed/bare discipline in the *same session*
— git-commit via `cd && add && commit && push | tail` right after using bare `git -C`; repo-overview via
`cd && ls && echo && git log` one call after a clean bare `git -C status`. Not a one-off: the dynamic-shell
bundle is the **single most-logged friction cause** across the case study. So this is **systematic
regression**, not noise — the thing to explain.

## Hypothesis — four compounding causes (NOT an external guardrail)
1. **Trained-prior dominance (the main driver).** Nothing *pulls* the agent toward bash; rather, bash is the
   **default attractor** of the weights. `cd && … | grep | head` is the statistically dominant encoding of
   "inspect a repo / aggregate text" across the training corpus. The genscalator discipline is a **thin,
   recent, local bias** competing against a deep, high-probability prior — and token-by-token generation
   doesn't reliably honor the bias on every call. *There is no external force; the absence of a force
   stopping the prior IS the problem.*
2. **Per-call re-sampling, not memory loss.** The smoking gun: correct call → regressed call → (often)
   correct again. The rule wasn't forgotten; **each command is independently re-drawn from the prior**, and
   the discipline isn't deterministically re-activated per generation. (Distinct from, but compounded by,
   **context rot**: as context fills, the earlier rule loses salience — so regression frequency should rise
   with fill %, a testable prediction.)
3. **No enforcement at the point of action.** A soft instruction never becomes a hard constraint. The
   harness confirmation-guard catches *some* tokens (e.g. zsh `<->`) but not `cd`/`&&`/`echo` bundles, so
   nothing rejects the reflex *before it executes*. Reflexes persist precisely when the environment doesn't
   make the wrong move impossible.
4. **Atomic-intent / non-atomic-encoding gap.** "Show me the repo" *feels* like one thought, so it's emitted
   as one command. The safe form (two bare calls) requires overriding the natural one-intent→one-command
   mapping — friction that the reflex routes around under flow.

## The key tie-in — this is the *Habit vs Reflex* distinction, confirmed
`docs/foundations.md` already separates **Habit** (a deliberate default *strategy* — fixable by changing the
default tool) from **Reflex** (a sub-deliberative *twitch* — fixable only by making the typed path
frictionless so it becomes the new reflex). The regressions are **empirical confirmation**: the bundling is a
*reflex*, and reflexes **do not yield to instruction** — only to a changed environment. So:
> **Instruction-by-exhortation is necessarily leaky.** Adherence to a soft rule asymptotes below 100% because
> the prior is re-sampled every call. The reliable corrections are *structural*, not *hortatory*.

## What actually sticks (ranked by leverage)
1. **Make the typed path the path of least resistance** — a short, memorable, allowlisted `tt` command that
   is *easier* than the bash bundle (and never prompts). Converts the reflex itself to the typed form. This
   is why "ship the tool + allowlist it" beats "add a rule."
2. **Hook/linter that hard-rejects the anti-pattern at submit time** — turn the soft rule into a hard
   constraint: reject command strings containing `cd `+`&&`, `echo "EXIT=$?"`, `| head` after a `tt`, etc.,
   with a one-line "use bare `git -C` / `tt …`" nudge. Makes the wrong move *impossible*, not *discouraged*.
3. **Keep the rule salient / context lean** — checkpoint+compact (slows the rot in cause 2); a terse,
   high-priority placement of the bare-command rule. Helps, but **cannot** fully close the gap (cause 1
   persists at any context length).
4. **Exhortation alone (more doc prose)** — lowest leverage; necessary to *define* the rule but insufficient
   to *enforce* it. The thing NOT to rely on.

## Open directions / tests
- **Measure** regression rate vs context-fill % (does cause 2/rot predict it? → pairs with `smart-zone-ceiling`).
- **Measure** regression rate with vs without an allowlisted typed alternative present (cause 1 leverage).
- Prototype a **submit-time hook** rejecting the top-N dynamic-shell bundles; measure residual rate.
- Is regression **model-dependent** (prior strength varies by model/vendor)? Bears on the portability goal.
- Generalize beyond shell: does the same prior-dominance explain other reflexes (e.g. reaching for `python3`,
  regex over AST)? If so, the structural-fix thesis covers all of genscalator's tool families.

## What shipped
- Nothing yet — note opened 2026-06-30, prompted by BR asking *"why do you regress so often — is there a
  guardrail outside your control, or are reflexes just hard to control?"* Answer: **no external guardrail; it
  is trained-prior reflex re-sampled per call, and the fix is structural (tool+allowlist+hook), not more
  instruction** — which is exactly genscalator's safe-by-design thesis applied to the agent's own behavior.
  Companion to `instrumentation-by-default.md` and the *Habit/Reflex* glossary entries.
