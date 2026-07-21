# gh vs tt forge: a capability-gap specimen with an honest process finding

2026-07-21 ~20:1x (regen/gauge window 20:08-20:12; BR's guard question arrived mid-unit).
Context: SM194 introprog release assessment; agent needed the open-PR and open-issue state
of github.com/lunduniversity/introprog.

## What happened

1. Agent ran `gh pr list` and `gh issue list` (two calls) against the GitHub repo.
2. Both tripped the guard; BR one-off allowed them from the approval TUI, then asked:
   "do we have gh-capabilities in tt forge and why did you not use that if so?"
3. Capability check (`tt forge help`, usage dump): tt forge surface is exactly
   `whoami | releases | tags | release-create | release-edit`, Gitea-flavored API, BASE
   defaulting to codeberg.org. There are NO pr-list or issue-list verbs on any forge.

## Findings

- **Outcome-correct:** gh was the only available tool for the job; tt forge could not have
  answered the question. No capability overlap existed.
- **Process-lucky (the honest part):** the agent did NOT check the toolbox before reaching
  for gh — gh came from base-model salience, not from a consult-the-registry step. Had a
  `tt forge pr` verb existed, the raw-gh reflex would have bypassed it. The tt-toolbox
  skill triggers on text/file/search shapes; forge operations have no trigger in it, so
  nothing re-surfaced the typed alternative at the decision point.
- **Stall cost:** two guard prompts landed on a present human (cheap here, a wall in AFK
  mode — the standing not-afk-safe pattern).

## Candidates (BR decides)

1. **Grow tt forge**: `pr list` / `issue list` verbs are natural Gitea-API extensions and
   would serve codeberg (the origin) first-class. For GITHUB specifically, gh is an
   audited, platform-blessed executable — per the dependency-preference cascade it may be
   RIGHT to keep gh for github rather than reimplement its API surface; the gap is then an
   allowlist/ergonomics question (which read-only gh verbs are guard-clean), not a build
   question.
2. **Skill fix regardless of 1:** the avoid-guard-stall / tt-toolbox skills could gain one
   line: "forge/PR/issue/release operations: check `tt forge help` first; gh only for
   GitHub-specific reads" — closing the no-trigger hole that made the right choice lucky.
3. Guardcheck hint candidate: raw `gh` -> "is there a tt forge shape?" (SM143 family).

## ADDENDUM ~20:3x same evening: the reflex missed AGAIN, and this time the verb EXISTED

Within the hour, the agent ran `gh api repos/lunduniversity/introprog/releases` to read BR's
draft release — BR guard-caught it again ("could be tt forge"). Sharper than the first
specimen: `tt forge releases <owner>/<repo>` EXISTS (the earlier "no capability overlap"
claim was about pr/issue verbs only), and the agent did not check before reaching for gh —
the exact no-trigger hole recurring after it was WRITTEN UP in this very file. Empirical
test run immediately: `tt forge releases lunduniversity/introprog --url https://api.github.com`
-> 404, because tt forge builds Gitea paths (`/api/v1/repos/...`) while GitHub wants
`/repos/...` on api.github.com. Also: GitHub DRAFT releases are invisible without auth, and
tt forge reads a token only for create/edit, not list. Net: gh remained necessary in
practice for this call, but (a) the check-first reflex failed twice in one hour despite
fresh documentation — documentation-in-context does not arm reflexes (the carried-not-armed
pattern, same evening as the raw-curl specimen), and (b) the 404 is a concrete SM196
requirement datum: a `release-all` needs per-forge API adapters (Gitea vs GitHub vs GitLab
paths + auth models), not just a BASE-url parameter. Ties SM196 (pinned minutes before).
