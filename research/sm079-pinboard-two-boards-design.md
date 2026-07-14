# The pinboard as super-substrate: two boards, one source (DESIGN NOTE)

**Status:** agent-authored DESIGN NOTE, 2026-07-14 (SM079), for BR to develop. NOT a rewrite — the actual
migration is JOINT (BR's board is his window, so he co-owns the split). Model: Opus 4.8 (1M). Ties
[[humans-md-agent-sole-writer]], RT052, the `reqt-lang` skill, the `toolbox-single-dispatcher` arch, the
asymmetry study (`research/asymmetry-study-plan.md`).

## 1. The problem: the pinboard is a blob because it serves two readers at once

`PIN-BOARD.md` is a single Markdown artifact that today does two incompatible jobs:
- it is **BR's window** — a curated, scannable `## NOW` he reads read-only to re-sync fast (RT052: "NOW is the
  human's window into it"); and
- it is the **agent's working state** — the full set of SM/OD/CD/RT items with IDs, bands, statuses, deps, and
  history the agent queries and mutates every session.

One artifact, two consumers → it drifts toward a blob: prose the agent must re-parse by eye, no enforced syntax,
IDs and statuses that can only be checked by reading. BR's standing critique ("it needs syntax") and his new
take ("it is super-substrate") are both right, and they point the same way once you add the asymmetry.

## 2. The lever: the ape⟷anthro asymmetry says the two readers want DIFFERENT representations

The asymmetry thread holds that human and agent have non-symmetric needs from shared substrate. Applied here:

| | **Human board** | **Agent board** |
|---|---|---|
| Job | a *window* — re-sync in one glance | a *state store* — query, mutate, verify |
| Form | narrative, curated, low-syntax, scannable | structured, typed, machine-parseable |
| Optimised for | human fast-read, low cognitive load | deterministic lookup + edit, no eyeball-parsing |
| Cadence | the present-tense NOW + a little context | the full backlog with IDs/status/deps/bands |
| Writer | agent (sole) | agent (sole) |
| Reader | BR (read-only) | agent (primary), BR when he wants depth |

Forcing both into one file means every change is a compromise between "scannable for BR" and "parseable for the
agent." Splitting them lets each be optimal.

## 3. The design: ONE canonical structured store, the human board is a rendered VIEW

The trap in "two boards" is **two sources of truth that drift**. Avoid it: make the **agent board the single
canonical store**, and **generate the human board as a projection of it** — never hand-maintain both.

- **Canonical store = the agent board, in `reqT-lang`.** genscalator already has the substrate: a reqT-lang
  parser, the `PRD.md` precedent, and `gs reqt` (parse + lint). Model each item as an entity with typed
  attributes (`SM082 has Status "open"; SM082 helps Goal ...; SM082 band "green"`). Now "is the numbering
  monotonic?", "what's open + green?", "what depends on SM077?" are QUERIES, not eyeball scans — and the lint
  catches malformed state (no more blob-drift). This is the `toolbox-single-dispatcher` philosophy: structure the
  data, let a tool answer questions.
- **Human board = a rendered view.** `tt pb render` (or `gs where`) projects the canonical store into the
  scannable `## NOW` window BR reads — exactly as `gs help` renders `docs/gs-help.txt` and `tt prd summarize`
  renders the PRD roadmap. BR never edits it; he reads the projection and conveys changes via chat
  ([[humans-md-agent-sole-writer]]), the agent updates the store, the view regenerates. Single source, two
  projections, zero drift.

A neat cross-check from the SM080 methods reading: [CS]'s **case-study database** prescribes exactly this
separation — raw/canonical evidence stored apart from the analysis/presentation layer, with a followable chain
between them. The two-boards split is the same discipline applied to project state: one canonical store, derived
views, traceability between.

## 4. Why this is genscalator-shaped (and cheap)

- reuses existing investments (reqT-lang parser, `gs reqt`, the `tt doc`/render pattern) — little new machinery;
- "structure over willpower": correctness of state is enforced by the lint, not by the agent remembering to keep
  prose tidy;
- token-efficient: the agent queries the store (`what's open+green?`) instead of re-reading a long blob each
  session — the same big-in/small-out logic as delegation and `tt prd summarize`.

## 5. Risks / open questions (for BR — the migration is his to co-own)

- **reqT-lang expressiveness:** can it carry everything the current PB holds (bands, HISTORY, freeform rationale,
  the wall-sticker)? Likely a hybrid: typed core (IDs/status/deps/bands) in reqT-lang + a freeform notes field
  per item. Scope the schema before committing.
- **Migration cost:** the current PB is large; converting it is real work and must not lose history
  ([[pinboard-history-workflow]], [[no-number-reuse-across-substrate]]). Stage it: stand up the store alongside
  the blob, render + diff against the current NOW until trustworthy, then cut over.
- **BR's editing habit:** he reads read-only already, so the "view is generated" model fits — but confirm he
  never wants to hand-edit the window directly (if he does, that reintroduces a second source).
- **Is `tt pb render` worth building, or does `gs where` already cover the human-window need** without a full
  reqT-lang migration? Cheapest first step: prototype the render from a SMALL reqT-lang state file for a few
  items and see if the projection reads as well as the hand-curated NOW.

## 6. Smallest next step

Not a rewrite: model ~5 current SM items in a `reqt-lang` state file, run `gs reqt` to lint, and prototype a
`tt pb render` that projects them into a NOW-style window. If the projection reads as well as today's hand-curated
NOW and the queries feel better than eyeballing, that validates the split before any large migration.
