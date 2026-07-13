# Releasing genscalator — the release checklist (SM067)

A step-by-step so **no manual step is forgotten** (research 047 flagged *"repeated manual-step omissions in our
release process"*). The **cut itself is BR's hand** — outward-facing and token-gated. This is a checklist to
follow AND to refine; **verify each item against the actual repo** before a cut (paths/anchors may drift).

Draft (SM067, 2026-07-13); candidate to wire later as a `release-genscalator` skill or a `gs release` command —
but kept a doc for now (a rarely-fired skill would bloat the skill listing; see SM069).

## Before the cut
1. **All committed + pushed**, both trees clean; the full toolbox **suite green**:
   `scala-cli test <repo>/tools --java-prop tt.tools=<repo>/tools`.
2. **Bump the version anchors** in lockstep (the git tag is the source of truth — keep the rest matching it):
   - `.claude-plugin/plugin.json` → `"version"`
   - `.claude-plugin/marketplace.json`
   - `AGENTS.md` (the version line near the top)
   Grep to confirm all read the new `X.Y.Z`. *(Verify this anchor list against the repo — it can drift.)*
3. **CHANGELOG.md** — fold `## Unreleased` (+ everything since the last tag) into one `## vX.Y.Z — <date>` entry,
   reverse-chronological, in the established tool-centric style (New tools · Enhancements · Docs/foundations ·
   Research/blog). Note any operating-rules change → version bump convention.
4. **Release notes** (`tmp/vX.Y.Z-release-notes.md`, the `--body-file`): **MUST link to the CHANGELOG**, and
   **check CHANGELOG ↔ release-notes are consistent** (same set of shipped items) before the cut. (BR pin.)
5. **WELCOME version banner** — update the `Version vX.Y.Z - ready for alpha testers` line in the welcome content
   (`research/sm-investigations/SM056-welcome-content-draft.md`, later the shipped welcome) to the release being
   cut. (BR pin — a forgettable step.)
6. **PRD.md** — add reqT Features for anything shipped-but-unspecified; move the released block FUTURE →
   PAST/IMPLEMENTED. Verify: `tt parsereqt parse PRD.md` and `tt parsereqt lint PRD.md` (clean, at/under the
   known lint baseline).
7. **Consistency sweep** — every shipped tool has: a `tools/README.md` entry, a PRD Feature, a CHANGELOG line,
   and (where testable) tests in `tools/test/`. Flag any tool missing one.
8. **Dry check** (read-only, no token): `tt forge releases bjornregnell/genscalator` confirms the previous tag is
   still the latest before the cut.

## The cut — BR's hand (token-gated, outward-facing; the agent PREPARES, BR CUTS)
9.  `git tag vX.Y.Z`
10. `tt forge release-create bjornregnell/genscalator vX.Y.Z --name "vX.Y.Z: <title>" --body-file tmp/vX.Y.Z-release-notes.md`
    (needs `GENSCALATOR_CODEBERG_TOKEN` in the env; this is the `ask`-gated command).
11. **Mirror** via `mirror.sc` (github + coursegit; gitlab if wired).
12. Verify: `tt forge releases bjornregnell/genscalator` now shows the new tag.

## After the cut
13. Ensure the PRD `## PAST / ### IMPLEMENTED` release block mirrors the CHANGELOG entry.
14. Reset `## Unreleased` afresh for the next cycle.

Ties: SM067, research `047` (the manual-step-omission pain), `genscalator-released-v020` (tt forge +
`GENSCALATOR_CODEBERG_TOKEN`), the v0.9.0 release-prep plan (the process this generalises), SM069 (skill vs doc).
