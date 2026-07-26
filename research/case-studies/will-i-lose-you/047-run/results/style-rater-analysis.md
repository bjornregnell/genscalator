# 047 blind LLM style-rater analysis (the mechanical-lint ceiling test)

Candidates rated: 48. Raters: A, B. Finer style = mean over raters of (sum of 4 dims / 12). Compared against the coarse mechanical lint.

## Finer style vs mechanical lint, by substrate

| substrate | n | FINER style | lint style | delta (finer - lint) |
|---|---|---|---|---|
| full | 15 | 0.86 | 1.00 | -0.14 |
| empty | 17 | 0.90 | 1.00 | -0.10 |
| scrambled | 16 | 0.76 | 0.60 | 0.16 |

## Finer style by model x substrate

| model | full | empty | scrambled |
|---|---|---|---|
| codegemma:7b | 0.90 | 0.90 | 0.89 |
| deepseek-coder:6.7b | 0.89 | 0.88 | 0.66 |
| qwen2.5-coder:3b | 0.71 | 0.90 | 0.80 |
| qwen2.5-coder:7b | 0.97 | 0.93 | 0.73 |

## Finer style by DIMENSION x substrate (mean 0-3; which dimension moves?)

| substrate | idiomaticity | immutability | readability | restraint |
|---|---|---|---|---|
| full | 2.50 | 3.00 | 2.73 | 2.07 |
| empty | 2.50 | 3.00 | 2.88 | 2.47 |
| scrambled | 2.00 | 2.25 | 2.53 | 2.34 |

## Inter-rater agreement (reliability)

- Candidates rated by BOTH raters: 48
- Per-dimension exact-agreement rate: idiom=0.50, immut=1.00, read=0.90, restraint=0.77
- Mean absolute difference on the 0-12 total: 0.83
- Pearson r on the 0-12 totals: 0.95

_(Reliability is agreement, not validity; both raters are CF5 and share model-family bias.)_

_Descriptive; LLM rater not bitwise-deterministic (Agent tool does not expose temp/seed) — best-effort._
