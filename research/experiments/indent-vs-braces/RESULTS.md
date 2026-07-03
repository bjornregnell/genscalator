# Results — braces vs indentation (agent edit-cost)

## Pilot experiments 

### Run 1+2 — 2026-07-03 (local-model axis, autonomous overnight)

**Config:** 378 cells = **7 models × 3 tasks × 3 regimes × R=6**, temperature 0.4, varying seed.
Models (local, via modly on bjornyx GPU): `qwen2.5:3b`, `qwen2.5:7b`, `qwen2.5-coder:7b`, `qwen-coder-local`,
`gemma2:9b`, `gemma3:latest`, `aya-expanse:8b`.
Tasks: one edit family (**wrap-in-`else`**) at growing block size — 001 small (a), 002 medium (a–e), 003 large (a–j).
Grading: `grade.scala` compiles [candidate, probe] + runs a **behavioral probe** (PASS / FAIL_COMPILE /
FAIL_MISSCOPE — the last is the *silent* hazard). Raw rows: [`results-raw.tsv`](results-raw.tsv). Reproduce:
`scala-cli run sweep.scala -- 6 [models]` then `scala-cli run analyze.scala`.

> **Note on revision:** Run 1 (4 models) suggested a clean "braceless → more *silent* mis-scopes" headline.
> Adding 3 more models (Run 2) **overturned that specific claim** — see Findings. Kept visible as a lesson: a
> small model-set misleads.

#### Error-rate by model × regime (fails/attempts)
| model | braceless | braces | common |
|---|---|---|---|
| aya-expanse:8b | **100% (18/18)** | 0% (0/18) | 11% (2/18) |
| gemma2:9b | 67% (12/18) | 17% (3/18) | 67% (12/18) |
| gemma3:latest | 61% (11/18) | **100% (18/18)** | 56% (10/18) |
| qwen-coder-local | 17% (3/18) | 0% (0/18) | 6% (1/18) |
| qwen2.5-coder:7b | 17% (3/18) | 0% (0/18) | 6% (1/18) |
| qwen2.5:3b | 94% (17/18) | 83% (15/18) | 83% (15/18) |
| qwen2.5:7b | 28% (5/18) | 61% (11/18) | 44% (8/18) |

#### Error-rate by task (size) × regime
| task | braceless | braces | common |
|---|---|---|---|
| 001 small | 40% (17/42) | 24% (10/42) | 29% (12/42) |
| 002 medium | 60% (25/42) | 40% (17/42) | 43% (18/42) |
| 003 large | 64% (27/42) | 48% (20/42) | 45% (19/42) |

#### Failure-type split by regime (n=126 each)
- **braceless**: 57 pass · **32 compile-fail** · 37 misscope · 0 infra
- **braces**: 79 pass · 11 compile-fail · 36 misscope · 0 infra
- **common**: 77 pass · 19 compile-fail · 30 misscope · 0 infra

#### Findings from pilot 

* Run 2 revised Run 1: TODO summary statemt of why

**Holds (7 models, 378 cells):**
- **Braceless is the costliest regime in aggregate** — pass-rate 45% (57/126) vs braces 63% and common 61% —
  and it is worst at **every block size** (40/60/64% error vs braces 24/40/48%). **Error-rate rises with block
  size** under every regime. This supports the *direction* of the thesis (braceless edits cost agents more).

**Revised (more data corrected the Run-1 headline — reported, not buried):**
- Run 1's "braceless → more **silent** mis-scopes" did **NOT survive**. At 7 models the mis-scope counts are
  ~equal across regimes (braceless 37, braces 36, common 30). Braceless's *extra* failures are predominantly
  **loud compile-fails (32 vs braces' 11)** — so the failure-mode difference is "braceless → more compile
  errors," not "→ more silent hazards." The Run-1 safety framing was an artifact of the 4-model sample.

**Most striking result — bidirectional, model-specific regime sensitivity:**
- The regime effect is large but **goes both ways per model**, dominated by whether a model can reliably *emit*
  the requested style at all:
  - `aya-expanse:8b`: braceless **100% fail**, braces **0%** (cannot produce braceless; aces braces).
  - `gemma3`: the opposite — braces **100% fail**, braceless 61%.
  - `qwen2.5:7b`: prefers braceless (28% vs braces 61%).
  - `qwen2.5-coder` / `qwen-coder-local`: regime-robust (≤17% everywhere — the strong coders don't care).
- So **"braces are universally safer for agents" is refuted at the per-model level.** There is no universal
  regime law here; the dominant variable is a model's style-emission ability.

**Caveats (load-bearing):** one edit family (wrap-in-`else`); 3 tasks; 7 local models but the "error-rate" is
heavily confounded by **style-emission ability** (a model that can't emit braces scores 100% "error" regardless
of edit reasoning); single session; R=6; the **common-before == braceless-before flaw** (no-blank-line tasks) so
common is only a directive difference; **no Opus-4.8 anchor yet**; `diff_lines` crude.

**Verdict:** braceless is the costliest regime in aggregate and at every size (thesis *direction* supported), but
the effect is bidirectional per-model, dominated by style-emission ability, and the Run-1 silent-misscope safety
headline did not survive more data. A genuinely informative pilot — and a clean cautionary tale that small
model-sets mislead. Confirmation needs the next steps.

### Next steps (to promote pilot → evidence)
1. **Separate the two effects:** style-emission ability (can the model produce the regime at all?) vs edit
   correctness *given* it emitted the style. Grade emission and correctness independently.
2. **Fix the common regime:** tasks with blank-line scopes so `before.common` ≠ braceless.
3. **Add edit families** (extract-scope, add-branch, reindent) and **the Opus-4.8 anchor** (subagent workflow).
4. **Larger R (≥20)** for stable per-cell rates; **cross-session / more vendors** to kill self-subject bias.


## Main Experiment

(stubb)

### Reserach questions

### Design

* Independent variables:
 - ...

* Dependent vairables:
 - ...

* Hypotheses:
 - ...(null hyps etc)

### Data Collection

### Results

### Discussions

### Conclusions