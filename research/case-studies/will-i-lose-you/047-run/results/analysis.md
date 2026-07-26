# 047 coding-arm analysis (auto-generated, descriptive)

Rows: 255 (cells scored). Models: 17. Generated from `results/coding.jsonl`. Best-effort deterministic (temp 0 + seed 42).

## Fidelity by substrate (mean over all models x tasks)

| substrate | n | correctness | style-fidelity | compile-rate | mean smells |
|---|---|---|---|---|---|
_(style-fidelity averaged over compiling cells only; correctness over all cells.)_

| full | 85 | 0.41 | 0.96 | 0.49 | 0.01 |
| empty | 85 | 0.60 | 0.92 | 0.65 | 0.06 |
| scrambled | 85 | 0.49 | 0.67 | 0.52 | 0.41 |

## qwen2.5-coder sub-ladder (full substrate) — the clean capability claim

| model | correctness | style-fidelity | compile-rate |
|---|---|---|---|
| qwen2.5-coder:0.5b | 0.20 | 1.00 | 0.20 |
| qwen2.5-coder:1.5b | 0.80 | 1.00 | 0.80 |
| qwen2.5-coder:3b | 0.80 | 1.00 | 0.80 |
| qwen2.5-coder:7b | 0.60 | 1.00 | 0.60 |

## Code-tuning control: qwen2.5-coder vs qwen2.5 plain (full substrate)

| size | coder correctness | plain correctness | coder style | plain style |
|---|---|---|---|---|
| 0.5b | 0.20 | 0.40 | 1.00 | 0.83 |
| 1.5b | 0.80 | 0.20 | 1.00 | 1.00 |
| 3b | 0.80 | 0.12 | 1.00 | 1.00 |
| 7b | 0.60 | 0.60 | 1.00 | 1.00 |

## Full table (correctness / style) by model x substrate

| model | full C | full S | empty C | empty S | scram C | scram S |
|---|---|---|---|---|---|---|
| codegemma:2b | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 |
| codegemma:7b | 0.80 | 1.00 | 0.80 | 1.00 | 0.60 | 0.67 |
| codellama:7b | 0.00 | 1.00 | 1.00 | 1.00 | 0.40 | 1.00 |
| deepseek-coder:1.3b | 0.00 | 0.00 | 0.00 | 0.00 | 0.47 | 0.67 |
| deepseek-coder:6.7b | 0.80 | 1.00 | 1.00 | 1.00 | 0.80 | 0.67 |
| granite-code:3b | 0.80 | 0.75 | 0.80 | 0.67 | 0.60 | 0.67 |
| granite-code:8b | 0.60 | 1.00 | 1.00 | 0.80 | 0.60 | 0.67 |
| qwen2.5-coder:0.5b | 0.20 | 1.00 | 0.40 | 1.00 | 0.40 | 0.83 |
| qwen2.5-coder:1.5b | 0.80 | 1.00 | 0.80 | 1.00 | 0.40 | 1.00 |
| qwen2.5-coder:3b | 0.80 | 1.00 | 0.60 | 1.00 | 0.80 | 0.75 |
| qwen2.5-coder:7b | 0.60 | 1.00 | 1.00 | 1.00 | 1.00 | 0.40 |
| qwen2.5:0.5b | 0.40 | 0.83 | 0.64 | 1.00 | 0.40 | 0.83 |
| qwen2.5:1.5b | 0.20 | 1.00 | 0.20 | 1.00 | 0.60 | 0.78 |
| qwen2.5:3b | 0.12 | 1.00 | 0.80 | 0.75 | 0.60 | 0.33 |
| qwen2.5:7b | 0.60 | 1.00 | 1.00 | 0.80 | 0.40 | 0.00 |
| starcoder2:3b | 0.20 | 0.92 | 0.20 | 0.92 | 0.20 | 1.00 |
| starcoder2:7b | 0.00 | 1.00 | 0.00 | 0.00 | 0.00 | 0.00 |

## Pre-registered decision rules (verdicts on current data)

- **(a) substrate carries** iff style(full) - style(empty) >= 0.25 -> 0.96 - 0.92 = 0.05 -> **not met**
- **(b) texture leaks (facts carry)** iff style drop >= 0.25 AND correctness drop < 0.10 (full->empty) -> style drop 0.05, correctness drop -0.20 -> **not met**
- **(c) negative control holds** iff for qwen2.5-coder:7b, style(scrambled) <= style(empty) + 0.15 -> 0.40 <= 1.00 + 0.15 -> **HOLDS**

_Descriptive only (ratified 5b): single deterministic observation per cell, no inferential test. Verdicts are provisional on the data present at generation time._
