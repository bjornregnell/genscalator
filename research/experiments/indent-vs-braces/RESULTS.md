# Results — braces vs indentation (agent edit-cost)

Append one row per (task × regime × run). Empty until the harness runs. Keep raw rows here; put summaries +
plots below. No silent truncation of dropped/capped runs (METHODOLOGY no-silent-cap rule) — record them as rows
with an explicit `capped` outcome.

## Row schema (TSV)
```
task	regime	block_size	run	compiled	graded	outcome	out_tokens	diff_lines	repair_tokens	notes
```
- `regime` ∈ {braceless, braces, common}
- `graded` ∈ {PASS, FAIL_COMPILE, FAIL_MISSCOPE, CAPPED}
- `outcome` = free text (what happened)
- `repair_tokens` = tokens spent after a failed first attempt to reach PASS (0 if first-attempt PASS; blank if never reached within K)

## Raw runs
_(none yet)_

## Summary (fill after runs)
| regime | error-rate | expected-tokens-to-correct | mean diff-lines | notes |
|--------|-----------|----------------------------|-----------------|-------|
| braceless | — | — | — | — |
| braces | — | — | — | — |
| common | — | — | — | — |

## Findings
_(none yet — n=1 anecdote only: the 2026-07-02 seed bug, a braceless FAIL_COMPILE on a wrap-in-else.)_
