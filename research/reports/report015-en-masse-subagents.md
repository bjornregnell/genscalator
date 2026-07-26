# SM015 — En-masse sub-agent spawning as a spending-mode strategy

**Status:** investigation report (first cut). **Author:** CF5 research sub-agent, 2026-07-09.
**Board ref:** PIN-BOARD SM015 (a–d). **Grounding refs:** research/047 (fleet-fidelity constraint + platform-stability threat), wr-data/co4-forgot-flaky-box-memory-loss-2026-07-09.md, [[delegation-dance]] (RT049/RT050, BP013), [[token-budget-modes]], [[cue-use-fleet]].

## (a) LIMITS — how many sub-agents can actually run

Two separate ceilings exist, and the *lower* one always wins. On this box the lower one is **local RAM**, not the harness.

**Harness-side caps (the API/orchestration ceiling):**
- **Agent tool (ad-hoc fan-out):** multiple `Agent` calls issued in one assistant message run **concurrently**; they run in the background by default and report back on completion. No numeric cap is documented in-session for plain Agent-tool fan-out; practical parallelism observed in this project has been small fleets (e.g. the 4 fresh-restart-pilot proxies, the verify agents).
- **Workflow tool (structured orchestration):** per the harness tool semantics, a Workflow run caps concurrency at **min(16, cpu_cores − 2)** agents at once, with a **~1000-agent lifetime cap per workflow** and **max 4096 items per single `parallel()`/`pipeline()` call**. On blixten (8 cores) that formula gives **min(16, 6) = 6 concurrent agents per run**.
- **Opt-in gate:** en-masse Workflow orchestration is **not** a default behavior — it activates only on explicit user request ("ultracode"-style opt-in). So a habitual fleet posture still needs BR's standing authorization per mode, which is exactly what [[cue-use-fleet]] + spending-mode gating provide.

*Verification caveat:* the Workflow tool is not loaded in this investigation session, so the min(16, cpu−2) / ~1000 / 4096 figures are taken from the tool's documented semantics as briefed, not exercised locally. Treat them as the harness contract; re-verify empirically before designing anything that depends on the exact numbers.

**Box-side cap (the FIRST-CLASS local ceiling — blixten is flaky):**
- Blixten **hard-crashes GNOME under memory pressure** (crashed within the last week; re-flagged by BR 2026-07-09, now pinned as durable memory). Research/047 records the box already memory-pressured mid-study: **6.1 GB swap in use, a single 8.3 GB process**, BR freeing RAM by hand mid-dig.
- Each *local-tool-heavy* sub-agent spawns **its own scala-cli/JVM processes** (compile server, bloop, test JVMs). A JVM toolchain per agent plausibly costs on the order of 0.5–2+ GB each (estimate, not measured — measuring this is a cheap follow-up). The box has **31 GiB total**, of which a large share is already committed to desktop + browser + the main session.
- Consequence: **a concurrent fleet of compiling agents can OOM-crash the box**, killing the main session and (per 047) corrupting any in-flight study data. This ceiling is **independent of and stricter than** the API-side caps: the harness would happily run 6+ agents that blixten cannot survive.
- **Rule of thumb (recommendation, not a measured threshold):** on blixten, cap *JVM-spawning* fleets at **2–3 concurrent**, and reserve larger fleets (4–6+) for **read/grep/write-only** sub-agents that touch no compiler. Text-only agents cost API tokens, not local RAM.

## (b) WHEN IT HELPS vs HURTS

**Helps — parallelizable, independent, write-to-file work:**
- **Fan-out finders:** N agents each sweep a declared slice of a codebase/corpus (per [[cue-use-fleet]]: declared-focus slices).
- **Self-Q&A batteries:** independent probe questions answered in isolation (the 047 fresh-restart pilot's 4 proxies are the existing in-project instance).
- **Multi-lens review:** the same artifact examined per lens (correctness / security / style / pedagogy), each lens blind to the others — independence is the point.
- Common trait: sub-tasks share **no state**, need **no mid-flight coordination**, and each result compresses to a short conclusion or a file.

**Hurts — synthesis-bound or coordination-heavy work:**
- **Synthesis is serial and lands on the parent.** N reports must be read, reconciled, and deduplicated by one context; past ~5–8 substantial reports the parent's context fills with other agents' prose and quality dilutes (a context-rot vector, the very thing the [[delegation-dance]] exists to avoid).
- **Briefing-fidelity risk:** a sub-agent knows *only its brief* — none of the session's accumulated corrections, memory nuances, or half-spoken constraints. A subtly wrong brief is multiplied by N, at N× the token cost (RT049/RT050 territory; the brief must constrain the tool-lane and forbid memory-index writes).
- **Token cost is roughly linear in fleet size** with overlap waste on top (agents re-reading the same files). Fine in spending-mode, poison in saving-mode.
- **The box, again:** anything where sub-agents compile/test locally converts fleet size directly into OOM risk (see (a)). "Helps vs hurts" on blixten is partly just "text-only vs JVM-spawning".
- **Interactive/creative work** (design dialogues, anything needing BR's taste mid-stream) doesn't decompose; a fleet returns N confident divergent answers and the reconciliation costs more than doing it once well.

## (c) MAKE IT A HABIT (spending-mode default)

1. **Mode gate first:** the token-usage posture ([[token-budget-modes]]) is the switch. Spending-mode (use-it-or-lose-it headroom) → default to a **small fleet (2–4)** for any decomposable question. Normal-mode → solo unless the task is obviously parallel. Saving-mode → no fleets.
2. **Decomposability test before spawning:** can the question be split into slices that (i) need no shared state, (ii) fit in a one-paragraph brief each, (iii) produce a conclusion ≤ ~10 lines or a file? If any test fails, do it solo.
3. **Sub-agents WRITE results to files** (scratchpad or a declared results dir), returning only a short pointer + summary — never thousands of items through the parent's context (this is the standing [[cue-use-fleet]] rule and the main anti-dilution lever).
4. **Compose with the existing mechanisms:** background tasks (SM011 repertoire) for long-running solo jobs; the self-Q&A mechanism as a ready-made battery shape; the delegation-dance briefing discipline (constrained tool-lane, no memory writes) as the per-agent contract.
5. **Blixten guard as a standing checklist line:** before any `go` on a fleet, classify each slice as *text-only* vs *JVM-spawning* and apply the (a) caps. Check `free -h` first if in doubt.

## (d) RECOMMENDED PATTERNS + when to graduate to Workflow

- **find → verify:** a cheap wide finder pass (fan-out greps/reads, high recall, low precision) feeds a second, smaller verifier pass that confirms each candidate against source. Two waves, each independent internally. Already half-proven in-project (the verify agents).
- **judge-panel:** 3–5 agents independently assess the same artifact against the same rubric; the parent aggregates votes/findings and only reads *disagreements* in detail. Best for review/quality questions where independence beats depth. Odd panel sizes make majorities cheap.
- **loop-until-dry:** repeat a fixed sweep (e.g. prose-leak hunting, spell/jargon sweeps) in waves until a wave returns zero new findings. Each wave is a small fleet; the dry wave is the stopping rule. Matches the existing --prose-leaks grind method.

**Graduate from ad-hoc Agent fan-out to a structured Workflow when** any of:
- the run needs **more agents than one message comfortably fans out** or must survive across many waves (loop-until-dry over a large corpus) — the Workflow's per-run concurrency cap + ~1000-agent lifetime budget are built for this;
- you need **pipelining** (stage B consumes stage A outputs mechanically, e.g. find→verify at scale via `parallel()`/`pipeline()`);
- you want **resumability/bookkeeping** the parent shouldn't hand-manage.
Stay with plain Agent fan-out for ≤ ~4–6 one-shot slices — the Workflow's structure isn't worth its setup cost there, and it's opt-in anyway.

**Bottom line:** en-masse is a real spending-mode lever, but on blixten the binding constraint is local RAM, not the harness. Habit = small text-only fleets by default, JVM work capped at 2–3, results to files, synthesis kept deliberately thin. If a recurring Workflow-scale use case emerges (a real loop-until-dry corpus job), spin off an RT to design it; nothing found in this investigation requires one yet.
