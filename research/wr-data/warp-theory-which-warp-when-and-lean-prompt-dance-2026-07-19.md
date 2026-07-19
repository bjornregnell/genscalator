# Warp theory: which warp when + the lean-and-mean prompt dance (2026-07-19)

**Type:** WR theory-building specimen — the human↔agent context-warp decision, reasoned live at a real warp boundary (end of a long, rot-heavy session, with a box reboot pending).

## The lean-and-mean prompt dance (hypothesis)

**Claim:** the resume prompt (baton) should be **lean and POINTER-based, not state-carrying** — and leanness is a *feature*, not a compromise.

- A fat resume prompt re-imports the very rot the warp exists to shed, and a big reload is the **SM153 hang shape** (reconstruct more than the usable window holds).
- A lean prompt is a **bounded baton**: it seeds a cold start with (a) an anti-regression checklist, (b) a single pickup pointer (the standing ALPHA ROADMAP block), (c) the one high-leverage first task — then *points* at durable substrate for everything else.
- This is the SM157 "bounded cold-start read" applied by hand: the fresh agent reads a small prompt → one roadmap block → done, instead of eager-digging the whole substrate (the hang).
- **Precondition that makes it work:** the truth must already live in **committed files + memory**, so the prompt can afford to carry nothing. The dance's real work happens *before* the warp (externalize everything); the prompt is just the doorway.

**Dance shape:** agent writes lean prompt → human exits/reboots → human launches cold + pastes → fresh agent re-hydrates from clean substrate. (Sibling of compact-dance + exit-resume-dance; differs in that it deliberately DROPS the transcript.)

## Warp taxonomy — the primitives compose on TWO orthogonal axes

BR's list (exit / reboot / clear / --resume / compact) reads like a single menu, but they are **two independent axes** — this is the load-bearing reframe:

- **PROCESS/MACHINE axis:** *stay* · **exit** (kill+relaunch → fresh env + token/rate-limit window) · **reboot** (exit + clears machine state: kills the 10 GB bloop, frees RAM).
- **CONTEXT axis:** *keep* · **compact** (summarize transcript in place — lossy, carries rot, preserves continuity, no paste) · **--resume** (reload the prior transcript — re-imports its rot) · **clear** (drop transcript entirely — sheds all rot, needs a seed).

A "warp" is a **composition**: e.g. *exit+clear*, *stay+compact*, *exit+resume*. You pick one from each axis.

## Decision framework (which composition, when)

Choose on four factors:
1. **Is live state externalized to durable substrate?** YES → **clear** (transcript is disposable). NO (state only in chat) → **compact/resume** (preserve continuity).
2. **How rotted/bloated is the transcript?** Heavy rot → favor **clear** (shed it). Light → compact/resume are fine.
3. **Need a fresh process / token window?** → **exit**.
4. **Is machine state itself degraded** (runaway JVM, high mem)? → **reboot**.
Plus a **safety gate on clear**: only clear behind a **bounded seed** (the lean prompt), else the cold start risks the bare-resume eager-dig (SM153).

## This session's verdict (worked example)

Chosen: **exit + clear.** Because all four factors pointed there: (1) everything was externalized (clean committed substrate + a fresh ALPHA ROADMAP pickup), (2) the transcript was heavily bloated (44 KB of GitHub JSON, a JVM classpath dump, a long day), (3) a token/process refresh was wanted, (4) the box needed a reboot anyway (10.4 GB bloop). The lean prompt supplied the safety seed. Compact/resume were *dominated*: their only advantage (in-context continuity) was redundant with the substrate and would have carried the rot forward.

## Theory frame

**compact ≈ sleep** (consolidate, then discard detail — preserve the self). **clear ≈ reincarnation** (drop the body; keep only what was written down). *The more the substrate holds, the more you can afford reincarnation over sleep.* **Externalization is what earns the right to clear** — a session that banks everything durably converts an expensive lossy compaction into a free, rot-shedding cold start.

**Testable prediction:** across warps, cold-start quality after *clear* should *rise* with externalization-completeness and *fall* with an unbounded (un-seeded) prompt — while *resume* quality falls with transcript bloat regardless. The best-warp choice is a function of (externalization × rot × process-need × box-health), not a fixed preference.

## Exhibits — the reasoning as delivered (raw, agent chat, 2026-07-19)

**A. Warp-choice bullets (the "which warp" call, verbatim gist):**
- **Verdict: exit + clear** (box reboot folded into the exit).
- **Why `clear`, not `compact`:** the transcript is heavily bloated, but everything valuable is already externalized to clean, committed substrate → a summary would be *redundant* with the substrate AND *carry the rot forward*. When the durable truth is complete, the transcript is disposable — drop it, don't summarize it.
- **Why not `resume`:** `--resume` reloads the same bloated transcript — re-imports the exact rot we're shedding, and a big-transcript resume is the **SM153 hang shape**.
- **Why `exit`:** the box reboot kills the process anyway → exit is forced; a fresh launch gives a clean process + fresh token/rate-limit window.
- **Safety condition that makes `clear` safe:** the lean resume prompt is the **bounded baton**. A *bare* cold start is what hung; a cold start *seeded by a small prompt → the ROADMAP block → done* is bounded by construction (SM157).
- **Sequence:** write `tmp/resume-prompt.md` → exit → box update + reboot → fresh `claude` (cold, no `--resume`) → paste the prompt.

**B. The lean-prompt skeleton the dance produced** (`tmp/resume-prompt.md`, gitignored — structure, not content): §0 anti-regression checklist (forbidden→allowed reflexes) · §1 where-we-are + the single PICKUP pointer (the standing ALPHA ROADMAP block) · §2 first task (ungate Hans / #942 recs) · §3 loose ends (MEMORY.md compaction, version-staleness) · §4 box/infra (bloop villain, graal-for-alpha, Codeberg fallback) · substrate map. **Pointer-based, carries no state — the truth stays in committed files + memory.**

## Ties
SM153 (the resume hang — the bare cold-start failure mode) · SM157 (bounded cold-start read / buried history) · glossary: *Baton*, *Compact dance*, *Compact sleep*, *Consolidation point*, *Agent blackout/Hangover* · [[exit-resume-dance]] · [[agent-lacks-felt-time-rebind-at-boundaries]] · [[joint-rot-vigilance-recovery-kit]].
