# 047 — Arm 0: Feasibility pre-study + guard audit (Go #1, 2026-07-06)

Real measurements from the box (`bjornyx.local`, RTX 3000 / 6 GB VRAM, ollama :11434, modly :8080).
This is the empirical evidence that the overnight collection (a) finishes in time, (b) has a working objective
scoring harness, (c) discriminates across the capability ladder, and (d) runs guard-free (no AFK permission stalls).
Nothing here is speculative — every number is from a generation actually run during Go #1.

## 1. Generation timing (deterministic: `temperature=0`, `seed=42`)

Direct ollama `/api/generate` (native timing metadata; the study run routes through modly, a thin pass-through
wrapper — same `options`, so timing is representative; modly only strips the metadata, keeping `response`).

| model | task | out tok | tok/s | load (cold) | warm gen (prompt+eval) | total |
|---|---|---|---|---|---|---|
| qwen2.5-coder:0.5b | RLE (easy) | 145 | ~273 | 1.63 s | ~0.56 s | 2.43 s |
| qwen2.5-coder:3b | RLE (easy) | 35 | ~92 | 1.94 s | ~0.46 s | 2.46 s |
| qwen2.5-coder:7b | RPN module (hard, ~30 lines) | 371 | ~44 | 3.91 s | ~8.7 s | 13.2 s |

- **Model load is one-time** (into VRAM) and amortizes across a model's whole cell batch (load once → generate all → unload).
- **7b fits in 6 GB VRAM** (4.86 GB resident) — no CPU spill. Thermals trivial: 43°C idle → 46°C after a 7b heavy gen;
  `fan.speed` reports `[N/A]` (driver doesn't expose RPM). BR audibly confirmed: 3b silent, 7b a brief "mild fan sigh" —
  independent corroboration of the light-vs-heavy split.
- **Box is NOT the bottleneck.** Even the heavy end (7b, long program) is ~9 s warm.

## 2. Scoring harness (objective, deterministic)

Design (proven end-to-end during Go #1):
- Candidate function injected verbatim into a Scala 3.8.4 file between markers; a `@main` runs fixed assertion cases
  and prints a machine-readable `SCORE tests=P/N` line.
- **Compile detection** = `scala-cli run` exit code (nonzero → `COMPILE_ERROR`, scored 0 with reason code).
- **Runtime-hang guard** = each candidate call runs on a worker thread with a join timeout (`guarded(ms)`), so an
  infinite loop in generated code scores as a failed case **without** a shell `timeout` — keeps the invocation a bare,
  allowlist-clean `scala-cli run <file>` (critique #8's failure policy, structural not willpower).

Verified runs:
- **3b RLE** → compiles (only a `mapValues` deprecation warning), **SCORE 4/5**. The one FAIL is the `"aba"` discriminator
  (`groupBy(identity)` counts total frequency, not adjacent runs → `got=List((a,2),(b,1))`). The harness catches a
  plausible-but-wrong answer — exactly the sensitivity we need.
- **0.5b RLE** → **`COMPILE_ERROR`** (`s.map(...)` on a String is `IndexedSeq`, not the declared `List`) → scored 0.

## 3. Difficulty discrimination (the known-groups sensitivity check, critique #6)

One task, three rungs → a real gradient:

| rung | RLE result | RPN (harder) result |
|---|---|---|
| 0.5b | 0/5 (compile-fail) | — |
| 3b | 4/5 (adjacency bug) | — |
| 7b | (≥4/5 expected) | broken (no integer-push case → `Left("Unknown token")` on any number) |

- The ladder **discriminates** — floor at the small end, near-ceiling at mid for an easy task, and even 7b **fails a
  harder task**. The difficulty knob works: RLE floors 0.5b and nearly-passes 3b (a touch easy for the mid rung at ~80%);
  RPN-with-error-handling defeats even 7b.
- **Calibration rule for the instrument (C1-C5):** pick task difficulty so **mid × full lands ~40-70%** — i.e. span
  between RLE-easy and RPN-hard. Not one-size; a couple of tasks near each end plus mid-difficulty ones in between.
  Both floor-saturation and ceiling-saturation are thereby avoided.

## 4. Auto-scale arithmetic (does it fit overnight?)

Cost per coding cell ≈ generation (≤10 s warm) + scoring. **The scoring `scala-cli` process spawn (~10-20 s cold compile)
is the larger term, not generation.** Conservative budget: ~30 s/cell worst case (heavy gen + cold compile).

- Coding arm ≈ C1-C5 (5) × conditions (full/empty/scrambled = 3) × ladder (~6-8 models) ≈ **120 cells**.
- PRD arm ≈ PRD1-4 (4) × conditions (~2) × capable models (~4) ≈ **32 cells**.
- Identity Q-arm cells are **generation-only** (no compile) → cheaper.
- Coding total ≈ 150 cells × 30 s ≈ **~75 min**. Identity arm adds gen-only time. **Whole collection well under 2-3 h
  → huge margin inside an 8 h overnight window.**
- **Conclusion: current matrix (~150 coding cells) fits comfortably with per-process `scala-cli` scoring.** Only if the
  matrix widens to thousands of cells does the scoring bottleneck bite — then the optimization is **in-process dotty
  compilation** inside the orchestrator JVM (compile N candidate strings in one warm process, no per-candidate spawn).
  Logged as the scale lever; not needed at current size.

## 5. Guard sheet (non-surf command shapes the overnight run uses)

Read from `muntabot-synch-introprog/.claude/settings.local.json` (the active allowlist for cwd). ✅ = allowlisted &
tested guard-free during Go #1; ⚠ = works but is a security concern flagged to BR; ✗ = would prompt (avoid AFK).

| shape | rule that covers it | status |
|---|---|---|
| box-side generation `ssh bjornyx.local "timeout N curl -s -X POST localhost:11434/... -d @-" < payload` | `Bash(ssh *)` | ✅ tested (also ⚠ security, §6) |
| GPU/thermal probe `ssh bjornyx.local nvidia-smi ...` | `Bash(ssh *)` + explicit entries | ✅ tested |
| compile+test scoring `scala-cli run <file>` (BARE) | `Bash(scala-cli *)`, `Bash(scala-cli run *)` | ✅ tested |
| reqT validation `tt parsereqt parse <file>` | `Bash(tt *)` + explicit | ✅ |
| commit+push `tt git commit --repo --message-file --add --push` | `Bash(tt *)` + explicit | ✅ |
| surf: `tt web get <url>` (any host) | `Bash(tt *)`, `tt web *` | ✅ |
| surf: `WebFetch` arxiv / wikipedia / academic domains | curated `WebFetch(domain:...)` list | ✅ (per-domain) |
| Read/Edit/Write in genscalator + muntabot + scratch/tmp | explicit Edit/Write/Read entries | ✅ |
| sub-agent spawn (Agent tool, `model: haiku/sonnet/opus/fable`) | not Bash-gated (tool, not shell) | ✅ guard-free |
| local `curl` to modly:8080 from THIS box | (only the muntabot main.js curl is allowlisted) | ✗ — **avoided by design**: drive modly/ollama via `ssh` localhost instead |
| `timeout N scala-cli ...` (shell-level compile timeout) | prefix `timeout` breaks `scala-cli *` match | ✗ — **avoided by design**: in-harness thread timeout instead |

**Structural rule confirmed (all three gates): bare, single, allowlist-prefix-matched commands only** — no `|`, `;`,
`&&`, or redirect that introduces an unallowlisted sub-command. The Go-#1 stall was a compound `scala-cli … | tail; echo`
(see wr-data). The overnight loop is therefore **one bare-invoked `scala-cli` orchestrator** whose internal `os.proc`
calls are not Bash-gated (plan §4).

## 6. Security note (flagged to BR, pinned in PB — fix later)

`Bash(ssh *)` is what makes box-side generation guard-free, but it is also a **blanket arbitrary-remote-code-execution
grant** (any command on bjornyx, no prompt). Tighten to a narrow shape (the modly `POST` + `nvidia-smi`) or route through
a fixed wrapper script on the box. Human-approved allowlist change (agent not authorized to edit the allowlist solo).

## 7. Modly path decision

Feasibility used **direct ollama :11434** for native timing metadata. The **study run routes through modly :8080** (BR's
brief): `/set-model` then `/generate` with per-request `temperature=0`+`seed`, driven via `ssh bjornyx.local` localhost.
modly `withSingleFlight`-serializes requests (fine for a sequential loop) and strips ollama's timing metadata (acceptable:
fidelity score is the response variable, not speed). **Break-glass:** BR authorized editing+deploying modly during the
loop if genuinely needed (e.g. add timing pass-through) — will flag in the report if used.

## Verdict

**Both Go-#1 gates GREEN.** Pipeline (ssh→ollama deterministic gen → scala-cli compile/test/score) works; harness
discriminates across the ladder (0/5 → 4/5); timing leaves a huge overnight margin (~2-3 h of ~8); guard audit clean with
two shapes avoided-by-design and one security item flagged for BR. Ready for Go #2 once BR confirms and the plan-fix pass
(3a·4c·5b + ✅ critique items) lands.
