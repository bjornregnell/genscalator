# WR data: bare auto-compact → stale substrate (`cue-bare-auto-compact`)

*Specimen + protocol. 2026-07-11. Ties: [[propose-compact-dance-at-trigger]] (this is its failure
mode), [[exit-resume-dance]], [[joint-rot-vigilance-recovery-kit]], [[raw-data-append-only]].*

## The specimen

Auto-compact fired **without a prepared resume-prompt** — we did not run the compact dance (no
deliberate checkpoint first). The agent resumed on only the harness's **auto-generated summary**.

Two concrete stale-substrate findings on resume (verified against `git log`, not eyeballed):

1. **PB `NOW` was stale.** It listed *"(1) build `tt forge release-edit`"* as a pending v0.9.0
   follow-up — but that tool was **built this session** (`9cd5962`, used live to fix the v0.9.0
   release body). It also framed the **v0.9.0 release as awaiting BR's go**, though v0.9.0 had
   **shipped live** (codeberg + github + coursegit).
2. **`tmp/resume-prompt.md` was stale** for the same reason — it predates the session's work, so it
   describes a world before the release + the four new tools + the README polish pass.

Root cause: **PB `NOW` and `resume-prompt.md` are refreshed only at a deliberate checkpoint.** A
*bare* auto-compact (no dance) skips that refresh, so both docs reflect the **last manual
checkpoint**, not the just-completed work. The auto-summary itself is lossy and can be **actively
misleading** (it is a compression, not a ledger).

## The hazard

If the resumed agent **trusts** PB `NOW` / `resume-prompt.md` / the auto-summary as current, it will
re-propose already-done work, act on outdated gates, or miss shipped state. The failure is silent:
the docs *look* authoritative.

## The protocol (`cue-bare-auto-compact`)

When you notice you resumed from a **bare** auto-compact (auto-summary present, no fresh
resume-prompt), **do not assume any doc is current.** Deep-mine the **substrate** (BR: "reminiscences
in substrate" — the real files, not memory of them):

1. **`git log --oneline`** on the active repos → the canonical recent work. **Commits are ground
   truth**; the anti-rot anchor ([[joint-rot-vigilance-recovery-kit]]).
2. The **transcript `.jsonl`** for specifics the summary dropped, if needed.
3. **Re-read PB `NOW` + `resume-prompt.md` and DIFF them against `git HEAD`** — hunt for items listed
   as pending that are already committed, and gates listed as open that are closed.
4. **Reconcile first, work second:** refresh PB `NOW` (+ resume-prompt if continuing) to reality
   *before* starting new work.
5. **Surface the staleness to BR** so the human window (PB `NOW`) is trustworthy again.

Sibling of the compact dance ([[propose-compact-dance-at-trigger]]): bare-auto-compact is what the
compact dance exists to *prevent* — it fired before we prepared, so recovery replaces preparation.

## Meta

This note was written *by* an agent that had just resumed from exactly this situation, which is why
the specimen is first-hand: the discovery came from grepping git + PB rather than from the summary,
and the summary would not have surfaced the release-edit / v0.9.0 staleness on its own.
