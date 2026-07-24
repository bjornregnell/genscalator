# gs help / gs status are slow — static-render latency (2026-07-24)

**Specimen.** Live, pre-Odersky-demo: `gs help` and `gs status` each "took ages" — the transcript
shows `gs status` at **Crunched for 43s**. Both are near-STATIC lookups (a help file; a status-line
legend), so 43s is a UX failure exactly where snappiness matters (a shared-screen demo).

**Where the time goes (hypothesis, to confirm in the investigation).**
1. **Skill load.** The first `gs` loads the WHOLE `gs-dwim/SKILL.md` (~250 dense lines) into context —
   heavy, and unnecessary for a static help render that needs zero DWIM reasoning.
2. **Doc read.** `gs help` cats `gs-help.txt` (instant), but `gs status` READS the 150-line
   `statusline-manual.md`, then COMPOSES a fresh table from it.
3. **Agent re-emit (the big one).** Because users do NOT see raw Bash tool-output, the gs-dwim skill
   requires the agent to PASTE the result as visible text — reversing the "let the subprocess render"
   optimization. Re-emitting ~40 lines (help) or composing a big table (status) is LLM-latency-bound;
   the 43s is dominated by this generation, not by the tools (each `tt` call is ~0.01s).

So the irony: the doc tool exists to avoid token-by-token re-emission, but the hidden-tool-output
constraint forces re-emission anyway — that is the latency.

**Fix directions to investigate.**
- **Pre-bake render-ready static docs.** Make legend/gist commands cat a READY artifact instead of
  reading-a-manual-then-composing: e.g. generate `docs/gs-status-legend.txt` (a finished table) from
  `statusline.scala`/the manual, so `gs status` becomes `tt doc gs-status-legend` — agent pastes a
  finished block, no compose step. Same for any "explain X" that is deterministic.
- **Shrink the mandatory paste.** Only the agent-visible re-emit costs latency; keep canonical outputs
  tight (the smallest faithful form), and prefer a link-plus-gist over a full dump where acceptable.
- **Harness/UX — the biggest win.** A reliable way for the USER to see `tt doc` stdout directly would
  remove the re-emit entirely (restoring the original optimization the hidden-output constraint broke).
  Worth a harness feature ask / a `gs`-render channel investigation.
- **Split the DWIM skill.** A tiny hot-path core (dispatch + the static commands) loaded cheaply, with
  the full spec lazy — so simple commands don't pull ~250 lines.

**Demo mitigation (now):** for the meeting, prefer pre-rendered/pasted-ahead content over live `gs`
calls, or accept the latency knowingly. Ties SM219 (pinned), [[gs-lists-need-ready-to-grab-substrate]],
the doc-tool "don't re-emit" rationale (now in tension with hidden tool-output).
