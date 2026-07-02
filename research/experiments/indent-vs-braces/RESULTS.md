# Results — braces vs indentation (agent edit-cost)

## Run 1 — 2026-07-03 (local-model axis, autonomous overnight)

**Config:** 216 cells = **4 models × 3 tasks × 3 regimes × R=6**, temperature 0.4, varying seed.
Models (local, via modly on bjornyx GPU): `qwen2.5:3b`, `qwen2.5:7b`, `qwen2.5-coder:7b`, `gemma2:9b`.
Tasks: one edit family (**wrap-in-`else`**) at growing block size — 001 small (a), 002 medium (a–e), 003 large (a–j).
Grading: `grade.scala` compiles [candidate, probe] + runs a **behavioral probe** (PASS / FAIL_COMPILE /
FAIL_MISSCOPE — the last is the *silent* hazard). Raw rows: [`results-raw.tsv`](results-raw.tsv). Reproduce:
`scala-cli run sweep.scala -- 6` then `scala-cli run analyze.scala`.

_n = 216 cells; 4 models × 3 tasks × 3 regimes._

### Error-rate by model × regime (fails/attempts)
| model | braceless | braces | common |
|---|---|---|---|
| gemma2:9b | 67% (12/18) | 17% (3/18) | 67% (12/18) |
| qwen2.5-coder:7b | 17% (3/18) | 0% (0/18) | 6% (1/18) |
| qwen2.5:3b | 94% (17/18) | 83% (15/18) | 83% (15/18) |
| qwen2.5:7b | 28% (5/18) | 61% (11/18) | 44% (8/18) |

### Error-rate by task (size) × regime
| task | braceless | braces | common |
|---|---|---|---|
| 001 small | 25% (6/24) | 17% (4/24) | 25% (6/24) |
| 002 medium | 54% (13/24) | 46% (11/24) | 71% (17/24) |
| 003 large | 75% (18/24) | 58% (14/24) | 54% (13/24) |

### Failure-type split by regime (n=72 each)
- **braceless**: 35 pass · 5 compile-fail · **32 misscope** · 0 infra
- **braces**: 43 pass · 11 compile-fail · **18 misscope** · 0 infra
- **common**: 36 pass · 13 compile-fail · 23 misscope · 0 infra

### Mean diff-lines (PASS only) by model × regime
| model | braceless | braces | common |
|---|---|---|---|
| gemma2:9b | 11.0 | 18.4 | 11.0 |
| qwen2.5-coder:7b | 17.5 | 20.8 | 19.4 |
| qwen2.5:3b | 11.0 | 14.0 | 12.0 |
| qwen2.5:7b | 14.4 | 13.9 | 20.8 |

## Findings (honest — this is a PILOT, not a confirmation)

**What holds up (the clean result — a failure-*mode* shift):**
- **Braceless produces the most silent mis-scopes: 32 vs braces' 18** (across 72 attempts each) — ~1.8×. Braces
  also has the most passes (43 vs 35).
- **Braces shifts failures from silent → loud:** braces has *more* compile-fails (11 vs 5) but *far fewer*
  misscopes. I.e. braces makes a wrong edit **fail loudly (compiler catches it)** where braceless lets it
  **pass silently at the wrong scope** — the safety-relevant difference, and the one that motivated the whole
  investigation (the 2026-07-02 seed bug was a braceless mis-scope).
- **Error-rate rises with block size** under both braceless (25→54→75%) and braces (17→46→58%); braceless is
  ≥ braces at every size and the gap widens at the largest (≈17pp).
- **The weakest model suffers most under braceless** (`qwen2.5:3b`: 94% braceless error).

**What does NOT hold up (reported, not buried):**
- **H3 (common ≈ braces) is unsupported** — common often tracked *braceless*, and was the *worst* regime on task
  002 (71%). **Likely a methodological flaw:** our small tasks have no blank lines, so `before.common` is
  byte-identical to `before.braceless`; the "common" regime was therefore mostly a *prompt-directive* difference,
  not a structural one. Fix before trusting any common-style number.
- **Per-model heterogeneity breaks H1's universality** — `qwen2.5:7b` *reversed* it (braceless 28% < braces 61%).
  So "braces are safer" is a population-level/failure-mode effect here, **not** a per-model law.
- **diff-locality** showed no clean regime story (braces PASS diffs were often *larger*, not smaller).

**Caveats (load-bearing):** one edit family (wrap-in-`else`) only; 3 tasks; 4 models from essentially two vendors
(qwen ×3, gemma); single session; R=6 (small); temp 0.4; the common-before flaw above; **the Opus-4.8 strong-model
anchor was not run** (subagent axis is next). `diff_lines` is a crude `diff` line count.

**Verdict:** a suggestive pilot consistent with *"braces make block-structure edits fail more loudly and less
silently"* — the safety-framed version of the thesis. It is **not** a confirmation of H1/H3/H4; the per-model and
common-style pictures are noisy or contradictory and the corpus is tiny. The robust, defensible headline is the
**failure-mode shift (braceless → silent misscope; braces → loud compile)**.

## Next steps (to promote pilot → evidence)
1. **Fix the common regime:** author tasks with blank-line scopes so `before.common` is structurally distinct
   from braceless (otherwise common is untestable here).
2. **Add edit families:** extract-scope, add-branch, reindent-after-rename, merge/split (generality beyond wrap).
3. **Run the Opus-4.8 anchor** via a subagent workflow (the strong-model reference; tests whether the effect
   vanishes at high capability).
4. **More vendors + larger R** (llama/deepseek/phi; R≥20) for stable per-cell rates; **cross-session** to kill
   self-subject bias.
