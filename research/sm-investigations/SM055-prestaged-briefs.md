# SM055 — pre-staged sub-agent briefs (FIRE ONLY ON BR EGRESS-ACK)

**Do NOT run these yet.** SM055 is a web-surf job = NOT AFK-safe. These prompts are staged so that the
instant BR is present and acks the egress (and the target domains are vetted against the WebFetch
allowlist), the sweep can fire immediately with no re-drafting. See the main brief:
`SM055-claude-code-npm-leak-claim.md`.

## Pre-flight (super-agent, BEFORE firing) — BR-present, egress step
1. Confirm BR is present and has **ack'd egress**.
2. **Vet the domain list** against the WebFetch allowlist ([[web-surf-not-afk-safe-front-load-and-vet-egress]]):
   - `tech-insider.org` (the claim source) — almost certainly NOT allowlisted → BR domain-ack required.
   - `registry.npmjs.org` / `www.npmjs.com`, `anthropic.com` / `status.anthropic.com`,
     `github.com` (advisories) / `github.com/advisories`, CVE/GHSA — vet each; log any that stall.
3. Front-load the full surf list so no mid-run domain stall races BR.
4. Fire agents 1-3 in parallel (CF5, read-only lane), then the synth (agent 4) on their returns.

## Shared guardrails (paste into every agent brief below)
- READ-ONLY investigation. **Do NOT install, download, run, or execute ANYTHING** the article or any
  package references — no `npm install`, no fetching a tarball to run. A lure could weaponise exactly that.
- Tool lane: read-only web fetch + the file tools. NO python3, NO compound shell, one bare command per call.
- Egress only to the vetted/allowlisted domains; if a domain stalls or is not allowlisted, STOP and report
  it (do not try to route around the allowlist).
- Weight PRIMARY sources over the article's own say-so. A clean "fabricated / non-story" verdict is a valid,
  valuable outcome. Return raw findings (your final message is data, not prose-for-a-human). Do NOT write
  any memory file.

---

## Agent 1 — npm / registry facts (CF5, read-only)
> Investigate the FACTUAL npm-registry state behind a claimed "Claude Code source-code leak on npm (2026)".
> Do NOT install or run anything. Using read-only web fetches to `registry.npmjs.org` / `www.npmjs.com`
> only: find the real Anthropic Claude Code package (`@anthropic-ai/claude-code` and any official variant),
> its version history, publish dates, maintainers/owners, and the published files list. Determine: (a) does
> the published package contain genuine SOURCE, or the ordinary shipped (minified/bundled) JS a published
> npm package carries by design? (b) are there any TYPOSQUAT / impersonator packages (similar names,
> unofficial publishers) that could be the real "leak"/supply-chain story? (c) any package/version pulled,
> deprecated, or flagged around the claimed date? Return: the official package identity + owner, the files-
> list finding, any suspicious lookalike packages, and whether anything matches "a leak" vs "the normal
> published artifact". Cite exact package names, versions, dates, URLs. [+ shared guardrails above]

## Agent 2 — official Anthropic + security advisories (CF5, read-only)
> Investigate whether any AUTHORITATIVE source confirms a Claude Code source-code leak or supply-chain
> incident on npm (2026). Do NOT install or run anything. Check, read-only: Anthropic official channels
> (anthropic.com, status/security/changelog pages), GitHub Security Advisories (github.com/advisories) and
> the CVE/GHSA databases, and the Node security WG. Determine: is there any official advisory, CVE/GHSA,
> status post, or changelog note describing such a leak or a compromised/malicious package? Were any
> SECRETS reported exposed (API keys, signing keys, internal endpoints) — the only part that is a real
> security incident? Return: each authoritative source checked with its verdict (confirms / no mention /
> contradicts), any advisory IDs, and an explicit "no authoritative source confirms this" if that is the
> finding. Cite URLs + IDs. [+ shared guardrails above]

## Agent 3 — adversarial "is this misinfo?" pass (CF5, read-only)
> Adversarially assess the CREDIBILITY of the claim source and the article itself:
> `https://tech-insider.org/anthropic-claude-code-source-code-leak-npm-2026/` (fetch read-only ONLY if the
> domain is BR-ack'd + allowlisted; if not, assess from external signals without fetching it). Do NOT
> install or run anything it references. Assess: is `tech-insider.org` a credible outlet or a
> spam/SEO-bait/misinfo/lure site (domain age/registration, reputation, other content, whether it exists in
> reputable indexes)? Does the article cite checkable primary sources or only assert? Does the headline
> match the SEO-bait/misinformation shape? Is it possibly AI-generated filler? Default to SKEPTICAL: treat
> the claim as fabricated unless primary evidence supports it. Return: a source-credibility verdict with
> concrete signals, and whether the article gives any verifiable lead at all. [+ shared guardrails above]

## Agent 4 — synthesis + verdict (super-agent or a fresh CF5, on returns 1-3)
> Given the three returns (npm facts / authoritative sources / source-credibility), synthesize an HONEST
> verdict for the SM055 report: **real / exaggerated / fabricated**, with a confidence level. State plainly:
> what (if anything) actually happened, whether any secrets were exposed (the only real-incident case),
> severity for BR (who pays for Claude Code privately, sovereignty stake), and a source-credibility note on
> `tech-insider.org`. If it resolves to misinfo/spam, say so plainly and note it is itself a
> disinformation-vigilance specimen (candidate WR datum). Write the verdict as a "## Verdict (dated)"
> section appended to `SM055-claude-code-npm-leak-claim.md`; do not overwrite the brief.

---

**After the run:** the verdict lands in the main SM055 file; if fabricated, capture a short WR specimen in
`research/wr-data/` (disinformation-vigilance thread, ties SM040). Both are BR-present outputs, not AFK.
