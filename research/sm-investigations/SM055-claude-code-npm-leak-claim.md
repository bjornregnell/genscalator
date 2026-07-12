# SM055 — INVESTIGATE the claimed "Anthropic Claude Code source-code leak on npm" (2026)

**Status:** pinned 2026-07-12 (BR). NOT started. **Web-surf job → NOT AFK-safe** (needs BR-present egress-ack; treat the source domain with caution). Report before drawing any conclusion.

## The claim to investigate
BR pinned a single URL asserting a source-code leak:

    https://tech-insider.org/anthropic-claude-code-source-code-leak-npm-2026/

That is the CLAIM, not a fact. `tech-insider.org` is an unknown-to-us domain; the headline has the exact shape of SEO-bait / misinformation, and this whole session has been running on a "disinformation keeps comin'" theme (confabulated harness "Next:" hints, a fabricated compaction hint). So the investigation's first job is to decide whether the claim is **real, exaggerated, or fabricated** — grounded in PRIMARY sources, never in the article's own say-so.

## Goal
Answer, with checkable evidence:
1. **Is there an actual leak?** If yes, WHAT leaked (full source? a build artifact? already-public minified bundle? a stale cache?), WHERE (which npm package/version), and WHEN.
2. **Severity / novelty.** Claude Code ships to npm as a distributable; a published npm package already contains its shipped (often minified/bundled) JS by design. "Source on npm" may be nothing more than "the package you install is on npm" dressed as a scoop. Distinguish: (a) genuinely-secret source or secrets exposed vs (b) the ordinary published artifact vs (c) a supply-chain event (a malicious or typosquat package impersonating `@anthropic-ai/claude-code`).
3. **Any secrets exposed?** API keys, signing keys, internal endpoints — the only part that would be a real security incident.
4. **Verdict on the source itself** — is `tech-insider.org` a credible outlet or a spam/misinfo/lure site?

## Why it matters to BR (why this earns an SM, not just a glance)
- BR **pays for Claude Code privately** and holds a deliberate **sovereignty / portability** stake ([[br-funds-claude-privately]]); a real leak or supply-chain event around the tool he depends on is directly load-bearing for his security posture and his "friction is the sovereignty" thesis.
- Ties the session's **disinformation-vigilance** thread and the genscalator **security-gap / hardening** threads (SM040 confabulation-while-blind, SM041 allowlist fleet, SM042 secret-scan, `tt harden`, BHH anti-goals). A verify-the-claim-against-primary-sources exercise IS the genscalator method applied to a live rumour.

## Method (deep, primary-source-first — NOT trusting the article)
- **Vet egress BEFORE surfing** ([[web-surf-not-afk-safe-front-load-and-vet-egress]]): `tech-insider.org` is almost certainly NOT on the WebFetch allowlist → needs BR's domain-ack. Front-load the surf list. Do NOT run/download/execute anything the article points to (no `npm install` of any package it names — a lure could weaponise exactly that).
- **Triangulate the CLAIM against authoritative sources**, and weight those over the article:
  - the npm registry itself (the real `@anthropic-ai/claude-code` package page + version history + published files list),
  - Anthropic's own channels (status/security/changelog), GitHub Security Advisories, CVE/GHSA, the Node security WG,
  - reputable security press, only as corroboration.
- **Sub-agent shape (when run):** a small multi-modal sweep — one agent on the npm/registry facts, one on official-Anthropic + advisory sources, one adversarial "is this article misinfo?" pass — then synthesize + an honest verdict with a confidence level. (Mirrors the RQ0 sweep + SM040 grounding method.)
- **Deliverable:** a short report in this file — verdict (real / exaggerated / fabricated), what actually happened if anything, severity for BR, and a source-credibility note on `tech-insider.org`.

## Constraints / guardrails
- ⛔ **Do not execute or install anything** referenced by the article. Read-only investigation.
- Egress only to allowlist-vetted domains after BR-ack; log any domain that stalls.
- If it resolves to misinfo/spam, say so plainly (a clean "fabricated / non-story" verdict is a valid, valuable outcome — and itself a disinformation-vigilance specimen).

Ties: [[br-funds-claude-privately]], [[web-surf-not-afk-safe-front-load-and-vet-egress]], SM040/SM041/SM042, `tt harden`, the disinformation-vigilance thread.
