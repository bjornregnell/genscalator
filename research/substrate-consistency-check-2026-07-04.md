# Substrate consistency check — genscalator (2026-07-04, AFK, Opus 4.8)

A four-agent read-only audit of the written substrate (blog · research+docs · skills+tools · cross-cutting links),
run while BR walked. **Auto-fixable findings are already fixed + pushed** (commits `2ae1e5d`, `a3fa6d1`); the
**surface-for-BR** items below need your judgment. Grouped, most-valuable first.

## ✅ Already auto-fixed (unambiguous — no action needed, just FYI)
- **4 dead internal links** (all the same "needs one more `../`" off-by-one in nested files): the two parent-context
  links + the `[[METHODOLOGY]]` wiki-link in `research/experiments/indent-vs-braces/README.md`, and the foundations
  link in `skills/blog-assistant/SKILL.md`.
- **1 wrong pointer:** `research/model-capability-and-leverage.md:36` cited `[[scala-style-evolution.md]]` for the
  "structural vs knowledge safeguard" glossary — that file has no such entry; repointed to `[[foundations]]` where the
  definition actually lives.
- **1 term drift:** a lingering "safe-zone" in `blog/004` → "smart-zone" (the rest of 004 + the corpus already use
  *smart zone*).
- **Tool-doc drift → docs now match code:** `forge.scala` header + `tools/README.md` now name
  `GENSCALATOR_CODEBERG_TOKEN` first (matches `TokenEnvNames`); the `text context` subcommand (implemented in
  `text.scala:20`) added to `tools/README.md` + `skills/tt-toolbox/SKILL.md`.
- **`.gitignore`:** added `*.bak` (the partial-run backup no longer shows as untracked).

## 🔴 Surface-for-BR — highest value

**1. `research/README.md` investigations index is STALE (the dropped-ball machine).**
The durable index exists but misses **~13 investigation notes + `experiments/` + `papers/`** now on disk (unlisted:
`agent-affective-analogs`, `agent-psyche-literature-review`, `cross-model-psyche-comparison`,
`experiment-prioritization`, `guardcheck-hook-proposal`, `kyo-ai-inspiration`, `model-capability-and-leverage`,
`recommended-plugin-settings`, `references-summary-enum-design`, `scala-style-recommendations`, `ssg-scoping`,
`steering-doc-design-tension`, `subagent-genscalator-propagation`, + `experiments/framing-as-arousal`,
`experiments/indent-vs-braces`, `papers/paper1-…`). This is *exactly* the "topics get forgotten because the index rots"
failure you keep worrying about. **Highest-value durability fix.** Options: refresh by hand, or a `tt` tool that
regenerates the Investigations list from the file tree (candidate tool).

**2. Stale "not yet run" statuses contradict the COMPLETED experiment.** The indent-vs-braces big-run is done (56
models, 3024 cells, preregistered null p≈0.59, written up as blog/003). Three places still say it hasn't run:
- `research/scala-style-evolution.md:72-75` — "open / not yet measured / nothing shipped yet." Needs the findings
  *backported* (a judgment edit, not a one-word flip — the thesis' safety headline was overturned).
- `research/experiments/indent-vs-braces/README.md:3-4` — "harness SKETCH … not yet run at scale," contradicting its
  own sibling `RESULTS.md`/`RUN-LOG.md`.
- `blog/003` — the **H1 title still says "STUB / preregistered, not yet run"** while the banner + populated Results say
  complete; also `003:176` closing line and `README:14` ("results pending") are stale. *(Left for you because you're
  mid-voice-pass on 003.)*

**3. Glossary gaps in `docs/foundations.md`** (terms used ≥2× across research, no entry). Priority order:
**`AT`** (AutoTranslate — a *core coined acronym*, yet WR + SSG have entries and AT doesn't), then **`ballgame`**
(its partner `ralph loop` is glossed), **`rest dance`**, **`corroboration asymmetry`**, **`authority anchor`**,
**`thriller state`**.

**4. `tt git` and `tt box` are undocumented in `tools/README.md`.** Two shipped, working tools with no cheat-sheet
section and no Files entry. Needs prose written — the surfaces (`tt git commit --repo/--message-file/--add/--push`,
`tt box models/df/gpu/freegb/pull`) are ready to lift from the file headers.

**5. `tt git commit` is NOT atomic to `--add`** (`git.scala:63` commits everything staged, not just the `--add` paths).
Already captured in `DESIGN-single-dispatcher.md:218-225` as pending (cites the `a91f764` mislabel), tied to the tt
single-dispatcher refactor (status: **PLAN, not started, awaiting your joint go-ahead**). *I worked around it this
session by running `git -C … status` before each commit* — but the real fix (`git commit -F <msg> -- <adds…>`) is small
and known.

## 🟡 Surface-for-BR — medium

**6. `blog/002`'s load-bearing citation isn't in `References.scala`.** The Odersky/Regnell/Kerr *"Towards a Common Scala
Style Recommendation"* note is cited 3× in 002 but lives only as a Google-Doc URL, not an entry in the bibliography that
bills itself as "the genscalator blog" bib. Maybe deliberate (self-authored living doc) — your call.

**7. `006` title vs arc-role mismatch.** File is titled *"Building a theory of agent psyche"* (reads *Theory*), but the
arc uniformly assigns **006 = Method, 007 = Theory** (004:6, 005, README). Invites "which post is the theory?"
confusion. Authorial call (retitle 006 toward *Method*, or adjust the arc labels).

**8. README status-label drift + model mismatch.** README lags the posts: **005** (README "stub" → file "SCAFFOLD"),
**007** (README "drafted" → file "SCAFFOLD, not ship-ready"), **008** (README "initialized" → file "STUB"). Also the
README's status vocabulary (initialized→drafted→published→deployed→updated) doesn't include the informal
*stub*/*scaffold* sub-states the posts actually use — reconcile the model.

**9. The `[[memory-file]]` wiki-links don't resolve for a public reader.** Many `[[…]]` across research/ (and now
`blog/005`, which I drafted today) point at **agent-memory files outside the repo** (`[[propose-compact-dance-at-trigger]]`,
`[[exit-resume-dance]]`, `[[hardening-dance]]`, `[[joint-rot-vigilance-recovery-kit]]`, …). They exist on your disk but
a public genscalator reader can't follow them, and there's no wiki-link resolver — they render as literal `[[text]]`.
This is the very *"dangling pointer to session-specific context"* hazard `foundations.md` defines. **Policy needed:**
glossary/footnote-expand on first use? a resolver tool? or accept them as private-notes-only and *strip them from
published-track blog posts*? (This now touches blog content, not just notes — my 005 draft added several.)

## 🟢 FYI / decisions parked
- **`pull-log.tsv`** left untracked — a blanket `*.tsv` ignore would over-ignore the real data TSVs
  (`results-bigrun.tsv` etc.), so the pattern choice (path-specific ignore vs leave-untracked) is yours.
- **Cross-repo coupling** (inventory only): several research notes reference `muntabot-synch-introprog` + `introprog`
  files (br-todo, wr-inbox, plan-tt-monolith-client, autotranslate/scratch) and one external reqT clone — no action,
  just a map of the coupling.

## Meta (WR data, already logged in `harness-ux.md`)
The audit itself reproduced the reflex it was checking for: the **research+docs subagent** reflexively used raw `grep`
and a `for`-loop that tripped the guard (more approval prompts) — so **subagents also need the bare-allowlist-matchable
discipline**, which I can't fully enforce on their internal tool use. A candidate reason to prefer typed `tt` tools the
subagents *can't* bypass, or to pass them the discipline explicitly in their prompts. This contributed to the
prompt-storm that pinned you to the keyboard.
