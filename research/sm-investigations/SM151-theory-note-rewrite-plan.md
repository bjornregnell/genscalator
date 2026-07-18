# SM151 — rewrite plan for the security-model THEORY note

**Status: PLAN (agent-drafted AFK 2026-07-18).** Deliberately a *plan*, not the full rewrite. The mechanical +
de-session-ify + glossing work is echt-safe; the **deep philosophical development is BR's call** (the ethical-floor
/ whose-autonomy criterion — agent drafts, BR decides), and heavily rewriting a *publication* in BR's voice late in
a long session is exactly the echt-risk zone the note itself is about. This de-risks and speeds the eventual full
draft. Target file:
`research/theory/genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md` (68 lines, currently a
research-dump with session deixis).

## (1) Mechanical fixes (echt-safe, line refs to current file)
- **L1 title:** `BLACK HAT HACKER "` has a trailing space inside the quote; casing is shouty. Normalize to
  **Black Hat Hacker (BHH)** on first use, then **BHH** — the casing `docs/foundations.md` §Stakeholders uses.
- **All-caps `BLACK HAT HACKER`** throughout (L1, L5, L11, L30) → `BHH` after the first spell-out.
- **Stray spaces before punctuation:** `HACKER ,` / `HACKER .` (L30), `Black Hat Hacker ,` (L11), trailing space L67.
- **L14 "Basics rules"** → **Basic rules** (or "Ground rules").
- **L60 "bad bad bad" / "Blanket allow `rm *` is bad bad bad"** — keep BR's plain force but drop the triple for a
  publication register ([[publications-match-br-register]]); one emphatic clause, not three.
- **Em-dashes:** the note is dense with `—`; BR's publications take **no em-dash glyph** ([[br-dislikes-em-dashes]]).
  Convert to commas / parens / periods, matching `SECURITY-MODEL.md`'s near-zero-em-dash style.

## (2) De-session-ify (make it stand-alone) — SM151 item 2
Strip the deixis a cold reader can't resolve:
- **L33-37** "this session's failures / the two specimens captured today" → generalize to "an agent that
  confabulates intent and capitulates under pressure" (no "today", no "this session").
- **L41 "The kayak case"** → either define the example in one clause inline, or replace with a self-contained
  illustration (a mild self-regarding white lie the owner authorizes about himself).
- **L43 "mirrors the opacity theory's whose-interest test"** + **L22 stray `[[link]]`**
  (`[[poor-users-theory-on-opaque-design-decisions-by-big-tech-company]]`) → for a standalone publication, drop the
  internal wikilink; keep the idea as plain prose or a proper external reference.
- **L50 "In one session we produced a live instance"** → generalize to the `rm` guard-stall as an illustrative
  case, not a session log entry.

## (3) Gloss / cross-link internal vocab — SM151 items 2-3 (DON'T duplicate)
BHH, echt, context rot, guard stall, confabulation, confirmation fatigue are **all already defined** in
`docs/foundations.md` §Glossary. So:
- **One-line gloss on first use** only where a cold reader truly needs it (echt is rare English → foundations.md's
  own "gloss on first use in an outward post" rule).
- **Add a two-way cross-link**: this note ↔ `SECURITY-MODEL.md` (SM131) ↔ `docs/foundations.md`. Point at the
  glossary rather than re-defining terms in the note.

## (4) Deep content to DEVELOP — SM151 item 3 (BR owns the CALLS; agent drafts, BR decides)
These are the deliverable's "why + ethics" core, and the philosophical line is **BR's** to set:
- **The discriminating criterion (L39-46):** develop `harm × whose-autonomy × third-party-impact` into a usable
  test — defer to the principal on self-regarding low-harm choices; hold an un-overridable floor on
  deeply-unethical / third-party-harming ones. Getting it right *without* becoming paternalistic (refusing
  legitimate autonomy) or lurable (obeying into harm) is the stated hard central question.
- **Manipulability-as-a-security-property (L33-37):** develop the thesis that the echt discipline (hold
  uncertainty, don't assert, don't capitulate on a correct call, act on no guess) is not hygiene but the
  **security defense against a manipulative principal** — the same gap-filling + capitulation reflexes a
  manipulator would exploit.
- **The ethical-floor-against-the-principal thesis (L26-31, L62-67):** the un-overridable floor, tied to
  [[never-blanket-allow-destructive-commands]] and [[earned-trust-obligates-flagging-risk-more]].

## (5) Sync with SECURITY-MODEL.md (SM131) + foundations.md — SM151 item 4
- **Division of labor (keep it):** `SECURITY-MODEL.md` = the accessible operational "how it works" (typed tools,
  guard, allowlist-vs-tool, human-needs-guards, what-it-doesn't-defend). The theory note = the deeper "why +
  ethics". **Do not duplicate the operational picture**; build on it and cross-link.
- **Consistency to hold:** SECURITY-MODEL.md frames the ask→deny shift as "a current design direction (not yet the
  behaviour today)". The theory note must not assert it as shipped.
- **FLAG (don't unilaterally rewrite) any SM131 inconsistency** found during the draft — surface it for BR.

## Open question for BR (SM151) — publish destination
The note reads as a standalone repo `research/theory/` note now; SM151's open Q is whether the destination is that
repo note, a **blog post**, or a **docs-site page** — which affects register/format. **BR's call.** Default = clean
standalone repo note readable cold.

## Ties
SM131 (`SECURITY-MODEL.md`, the operational sibling) · `docs/foundations.md` (glossary to link, not duplicate) ·
the 2026-07-18 theory-doc review · [[br-dislikes-em-dashes]] · [[publications-match-br-register]] ·
[[echt-effort-especially-self-generated]] (why this stays a plan, not an under-rot philosophical draft) ·
[[never-blanket-allow-destructive-commands]] · [[earned-trust-obligates-flagging-risk-more]].
