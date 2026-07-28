# WR data: compacting the MEMORY.md index — the harness read-limit it surfaces, and does compaction lose anything? (2026-07-13)

Two linked, BR-flagged items from a live MEMORY.md compaction.

## (1) Agent introspection surfaced a harness behaviour
Mid-task, the agent reported the harness's memory mechanics: **MEMORY.md** (the memory INDEX the recall system
loads each session) has a **~24.4KB read limit**, and a PostToolUse hook fires when it approaches (at 19.5KB it
asked to compact under 17.1KB). BR flagged this as worth logging — the agent, doing normal work, **exposes
harness internals the human cannot otherwise see.** Sibling of the frozen-clock / `refreshInterval` finding: the
**agent as a probe of the harness it runs inside**.

## (2) RQ (BR): does compacting an over-large MEMORY.md risk losing something? If so, what?
Grounded in what the compaction actually is:
- **Index vs content.** MEMORY.md is an INDEX — one line per memory (`[name](file.md) — hook`). The actual
  memory CONTENT lives in the per-topic files, which the compaction **does not touch.** Shortening a hook loses
  **no content** — the hook was only ever a summary; the file is the source of truth.
- **The real risks, ranked:**
  1. **Dropping an entry** — removes the pointer, orphaning its topic file so recall never surfaces it =
     effectively lost. *Mitigation used: keep EVERY entry* (all 117 kept; none dropped/merged).
  2. **Over-tightening a hook** — recall matches on the hook to decide relevance; a hook that no longer signals
     its topic stops being RECALLED when it should = a **discoverability / recall-precision** loss, not a content
     loss. Mitigation: keep each hook specific enough to trigger on its subject.
  3. **Transcription error in a full rewrite** — mangling/dropping a line by accident. Mitigated by care + the
     topic files remaining the durable source (a lost index line can be regenerated from its file).
- **The COUNTER-risk (why compact at all).** If MEMORY.md exceeds the read limit, the WHOLE index becomes
  unreadable → the recall system sees **no** hooks → **total recall failure** (every memory undiscoverable at
  once). So NOT compacting risks losing *everything's discoverability*; compaction trades a small
  recall-precision risk for avoiding a total-index-read failure.
- **Net.** The safe compaction (the one done) = **keep every entry, shorten hooks only, never drop, topic files
  untouched.** Content loss ≈ zero; residual risk = slightly reduced recall precision on the most-tightened
  hooks. The DANGEROUS compaction would be *dropping/merging* entries to save space — avoid that; tighten hooks
  first, and drop an entry only when its topic file is genuinely dead.

## (3) TWO different "compactions" — the word is overloaded (BR caught it)
BR, startled: *"agent said IT is compacting !?! Does that mean the agent controls the compacting algorithm and
can inspect it / report its pseudocode?"* and *"are you inventing the compaction algorithm as you go?"* The
answer turns on a distinction the shared word **hides**:
- **Harness context-compaction (`/compact`)** — the HARNESS summarising the transcript to reclaim window space.
  The agent does **NOT** control it, cannot inspect its algorithm, and is **PAUSED** during it (the compact
  sleep). The agent is the **OBJECT**.
- **MEMORY.md compaction** — the agent manually **rewriting a file** (Read + Write). Fully **agent-authored**:
  the agent chooses the procedure, can inspect + report it, and **invents it on the spot** from the constraint.
  The agent is the **AUTHOR**.

So "the agent is compacting" is **TRUE for the file, FALSE for the context** — same word, opposite
control-relationship. A real source of confusion worth naming.

**The MEMORY.md compaction pseudocode (agent-invented, reportable — BR asked):**
```
read MEMORY.md (the index of one-line hooks)
for each entry:
    keep the [name](file.md) link EXACTLY        # never drop a pointer
    rewrite the hook -> shortest phrase that still signals the memory's topic
        # cut redundant words / examples / parentheticals; keep the trigger + the rule
drop or merge an entry ONLY if its topic file is genuinely dead   # dropped none this time
write MEMORY.md back
invariant: total size under the read limit, with headroom; topic-file CONTENT untouched
```
It is **ad-hoc agent judgment**, not a fixed algorithm — devised from the constraint (under 17.1KB) + the
principle (don't lose discoverability). That the agent can state its own procedure as pseudocode is exactly the
transparency the harness `/compact` lacks — a nice contrast between agent-as-author and agent-as-object.

**The crux (BR flagged the phrase *"almost nothing, the way I did it"*).** Because the agent is the AUTHOR, the
outcome is **method-contingent**: the same instruction ("compact MEMORY.md") loses ~nothing under a careful
method (keep-all, shorten-hooks) and a LOT under a careless one (aggressive drop/merge). *With authorship comes
responsibility* — the agent's chosen procedure IS the risk variable. The opposite of the harness `/compact`,
whose method is fixed and out of the agent's hands (there the risk is the harness's; here it is the agent's
judgment). So "does it lose anything?" has no method-independent answer for an agent-authored compaction — the
honest answer must say *how* it was done.

Ties: [[agent-lacks-felt-time-rebind-at-boundaries]] (agent-as-harness-probe), the frozen-clock/refreshInterval
finding, [[raw-data-append-only]] (don't destroy durable records), [[echt-effort-especially-self-generated]].
