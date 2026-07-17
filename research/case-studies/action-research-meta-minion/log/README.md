# `log/` — this study's running record (NOT a second home for findings)

**Created 2026-07-17 on BR's `(go)`.** Written first, deliberately: writing this file is what **created both
directories**, with no `mkdir` — see the note at the foot, it is the reason this file exists before any other.

## The contract — one home per finding, and this is not it

> **`log/` holds the STUDY's PROCESS record. `research/wr-data/` holds the FINDINGS. The log POINTS; it never
> COPIES.**

BR's ask was *"a dir `log/` besides `minion-log`, as we also want to log things like our new findings here."* **The
findings part is the one thing this directory must not do** — and refusing it is not pedantry, it is the study's own
result applied to its own filing:

- **SM132's rule (2026-07-17): a claim's HOME is AUDIENCE-RELATIVE.** A finding about the *workflow* has the WR
  programme as its audience ⇒ it belongs in `wr-data/`, where the whole corpus is searched. A record of *what this
  study did* has a reader-of-this-study as its audience ⇒ it belongs here.
- **If both held findings, we would have built the exact defect we spent two days hunting**: two homes for one claim
  ⇒ they diverge ⇒ one becomes a **standing falsehood** nobody knows to correct. The retraction problem (SM133) is
  precisely *"a claim reaches several homes, its correction reaches one."* **Do not manufacture a second home.**

### What goes HERE
- **Session logs** — what was done, in what order, what changed mid-flight.
- **Protocol deviations** — anything off the protocol's §6 push triggers, logged as off-protocol.
- **Push results** — the verdict, the attribution, the coding (per protocol §7); the minion's own words stay in
  `minion-log/`, unedited.
- **An INDEX of the findings this study produced**, as **pointers** to `wr-data/` — never as summaries that can
  drift from what they summarise.

### What does NOT go here
- ❌ **Findings.** → `research/wr-data/`.
- ❌ **The minion's reports.** → `minion-log/`, which the minion writes itself and **nobody else edits** (protocol
  §5: it is its own scribe, so the agent under study cannot soften its own critique).
- ❌ **The protocol or the brief.** → the study root.
- ❌ **Anything the change log (protocol §10) already carries.** That log is the **reliability instrument** for a
  flexible design; splitting it would defeat it.

## Layout

```
action-research-meta-minion/
├── long-lived-meta-minion.md   the PROTOCOL (incl. §10 the change log = the reliability instrument)
├── meta-minion-brief.md        the BRIEF — the minion's ONLY behavioural channel; a file so it survives a warp
├── minion-log/                 the MINION writes these, unedited by anyone else (push-N.md)
└── log/                        THIS dir — the study's process record + an index of findings living in wr-data/
```

## 📌 Why this file exists before the directories did

**The `mkdir` that would have created these dirs hit a guard stall**, and the harness offered a blanket
**`mkdir *`** — mkdir anywhere, forever. **BR caught it** (*"too wide"*) and asked whether dir-creation should be a
`tt` tool.

**The answer was that no mkdir was needed at all: the Write tool creates parent directories.** Evidence, not
assertion — **`minion-log/` already existed and no `mkdir` ever ran for it**; the minion created it by writing
`push-1.md`. The agent had reached for `mkdir` out of **shell habit, not need**. ⇒ **"No tool needed" (SM134's §0.1
gate) is a real outcome, and this was one.**

**Why the blanket allow was structurally wrong, not merely wide:** a string-prefix allowlist **cannot** scope a path.
`mkdir -p …/genscalator/../../../../tmp/x` matches **any** `genscalator/` prefix rule — **the allowlist matches
STRINGS; `..` escapes at the filesystem layer.** So a path-scoped `mkdir` allow is not a tuning problem, it is **the
wrong layer**. *(Were a tool ever needed, `tt` is the right layer for exactly the reason `tt git` gives: it can
**resolve** the path and check containment, which a string match cannot — which is why allowlisting `tt git` is
safe.)*

**Ground:** `research/wr-data/` (the specimen note) · `research/wr-data/guard-suggests-blanket-date-glob-but-tt-chrono-exists-2026-07-10.md`
(**the same shape: the harness proposed an over-broad glob and the right answer was already in the toolbox**) ·
`100b6c0` (a blanket allow permanently disarms the guard for a shape) · SM134 §0.1.
