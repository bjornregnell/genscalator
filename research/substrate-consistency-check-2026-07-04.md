# Substrate consistency check — status (genscalator, 2026-07-04)

A four-agent read-only audit of the written substrate (blog · research+docs · skills+tools · cross-cutting links), then
a fix pass. This doc tracks **what the agent has done** vs **what still needs BR**. Updated live.

## ✅ Actions taken by the agent (done + pushed)
- **10 unambiguous auto-fixes** — 4 dead internal links (off-by-one `../`), 1 wrong `[[pointer]]`
  (`model-capability-and-leverage.md` → `foundations`), the lingering `safe-zone`→`smart-zone` drift, forge-token +
  `text context` doc↔code drift, and `*.bak` in `.gitignore`. *(commit `a3fa6d1`)*
- **Glossary gaps filled** — added **AT**, **ballgame**, **rest dance**, **corroboration asymmetry**, **authority
  anchor**, **thriller state**; reframed the roles/cases group per your *Case Study Research in SE* §3.2.3. *(`51cc088`)*
- **Dance definitions completed** — the **hardening dance** had no foundations entry (only a passing mention); added it
  with steps, plus the new **go dance** + the **≥2-step dance-bar** criterion. *(`7898dbf`)*
- **note/pin cue split** (was briefly "etch") propagated across foundations + memory + the 005 catalog + research notes. *(`92faef7`, `34e5c7f`)*
- **foundations made findable** — group map + A→Z index + 7 themed subsections (was one flat bullet wall). *(`1d7b840`)*
- **WR-data logged** — the AARGH allowlist-defeat episode, the "too-broad-allow-suggestions" category, the cue-word
  motor-ergonomics finding. *(`2ae1e5d`, `f2f2573`)*

## 🟠 Agent can do on your `go` (no decision needed — just greenlight)
- **Refresh `research/README.md` investigations index** — it misses ~13 notes + `experiments/` + `papers/` (the
  index-rot dropped-ball machine). I can regenerate it from the file tree.
- **Fix the three stale "not-yet-run" statuses** — `blog/003`'s H1 title + closing line, and the experiment
  `README.md` §Status (they contradict the finished run). *(The scala-style-evolution findings-backport needs your
  judgment — see below.)*
- **Write the missing `tt git` / `tt box` cheat-sheet sections** in `tools/README.md` (surfaces are in the file headers).
- **Fix README status-label drift** — 005 (stub→scaffold), 007 (drafted→scaffold), 008 (initialized→stub).
- **Full within-subsection alphabetical sort** of the foundations glossary + **convert dance steps to sub-lists**
  (your two review comments — deferred as polish; say `go` and I'll do the reorder).

## 🔴 Actions needed by BR (judgment / authorial / your call)
- **Name the agent role** (you asked). `CF` for Claude-Fable **collides** with `CF` = Confirmation Fatigue → won't
  work. Proposal: keep **"the agent"** as the role; model handle **`CO`** (Claude-Opus) now, and for Fable avoid CF —
  **`F5`** or **`Fab`**. Pick one and I'll pin it into the Roles group.
- **Books-in-context architecture** (your question) — how to make your two methodology books usable as selective
  reference without bloat + without copyright leak. My recommendation below (in chat).
- **`scala-style-evolution.md`** — backport the finished-experiment findings (the safety headline was overturned); a
  judgment edit, not a status flip.
- **`blog/002`** — the Odersky/Regnell/Kerr note is cited 3× but isn't in `References.scala` (deliberate, as a living
  self-authored doc? your call).
- **`006` title vs arc-role** — titled *"Building a theory…"* but the arc assigns 006 = Method, 007 = Theory. Retitle or
  adjust the arc labels.
- **`[[wiki-link]]` policy** — many `[[…]]` point at agent-memory files outside the repo; a public reader can't follow
  them. Glossary/footnote-expand, a resolver, or strip them from published-track blog posts?
- **`pull-log.tsv` ignore pattern** — a blanket `*.tsv` would over-ignore real data TSVs; path-specific ignore vs
  leave-untracked is your call.

## Notes
- Cross-repo coupling (research notes → muntabot-synch / introprog files) is inventoried, no action — just a map.
- Meta: a subagent reproduced the raw-`grep`/`for`-loop reflex during the audit → subagents also need the
  bare-allowlist-matchable discipline in their prompts (logged in `wr-data/harness-ux.md`).
