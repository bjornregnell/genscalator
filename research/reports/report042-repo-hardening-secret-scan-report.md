# SM042 — repo-hardening / secret-scan: `harden.sc` (deterministic) vs in-session (judgment) — report before building

**Status:** investigate→report (report before building — the script-vs-in-session decision is the crux). Nothing
built. Author: agent solo, post-compact AFK run, 2026-07-10. Grounded in a read-only survey of the actual repo.

## TL;DR — the decision, and why
**BOTH, LAYERED — and the layering is forced by evidence, not taste.** A deterministic scanner surfaces *candidates*;
the agent/human *triages* them. This is the only shape that works, because on this very repo a deterministic scan
produces **33 hits, of which 100% are false positives** clearable only by semantic judgment (see Evidence). So:
- **Layer 1 — `tt harden` (deterministic, allowlistable leaf):** signature-regex + entropy gate, respects
  `.gitignore`. Fast, reproducible, a clean `tt` unit. Surfaces candidates.
- **Layer 2 — semantic triage (in-session, agent/human):** name/comment/keyword vs a real secret *value*. Exactly
  what the two CF5 pre-upload ZIP audits did (2026-07-10), correctly clearing the credential-handling code as FPs.
- **The border BR named is real** (deterministic execution vs semantic context-judgment) and the resolution is
  **composition** — genscalator's own pattern: the typed tool is the mechanical, allowlistable unit; the agent/human
  is the judgment. Same split as guardcheck (mechanical checks + human gate). It also answers the round-4
  security-gap in one move: the real boundary today is *human review + convention*, and a scanner makes that review
  **tractable** instead of replacing it.

## Evidence — the survey (read-only, this repo, surf-free)
A prototype of Layer 1's regex (via `tt text grepr` over `.sc`/`.sh`: PEM headers, `password/secret/api_key`,
`Bearer`, AWS `AKIA…`, OpenAI `sk-…`, `TOKEN`, `LUCATID`) found **33 matches — every one a false positive:**
- All are **comments, documentation, variable names, or the `.netrc` tokenizer's `"password"` parse-keyword.**
- `deployblog.sc` self-documents *"SAFE TO COMMIT / PUBLIC: this file contains NO host, username, or password"* and
  reads the secret from `~/.netrc` **at runtime** (username-in-URL; nothing on the command line / process list).
- **No PEM private keys** anywhere; **no secret values** committed. The repo follows secrets-in-`.netrc`/env
  discipline by design.
Three design conclusions fall straight out:
1. **Deterministic-alone cries wolf** (~100% FP here) → Layer 2 triage is essential, not optional. Thesis confirmed.
2. **An entropy gate is needed:** every FP is a low-entropy English word ("password", "secret"); a real secret
   *value* is high-entropy. Signature **AND** a Shannon-entropy threshold on the captured value cuts most FPs before
   triage ever runs.
3. **Respect `.gitignore`:** half the hits were in `tmp/genscalator-clean/` (a gitignored build copy) and
   `tmp/test-sftp-netrc.sh` — a scanner must skip ignored paths or it double-reports build artifacts.

## Layer 1 design — `tt harden` (deterministic scanner)
- **Signatures (pure-JDK regex):** PEM (`-----BEGIN [A-Z ]*PRIVATE KEY-----`), cloud/API key shapes (AWS `AKIA…`,
  GitHub `ghp_…`/`github_pat_…`, OpenAI `sk-…`, Slack `xox[baprs]-…`), generic `((api[_-]?key|password|secret|token))
  \s*[:=]\s*["']?<value>` where `<value>` passes the entropy gate, and a bare high-entropy-blob detector.
- **Entropy gate:** Shannon entropy over the captured value (e.g. > ~4.0 bits/char and length ≥ 20) — this is what
  separates "password" (the word, low entropy → drop) from an actual key (high entropy → flag). The survey shows
  this single gate would have dropped all 33 FPs.
- **Scope hygiene:** honor `.gitignore` (skip `tmp/`, build dirs); scan tracked text files.
- **Dependency call ([[dependency-preference-cascade]]):** **pure JDK first** (regex + a ~10-line entropy fn) — no
  `gitleaks` dependency for a first cut; the signature set is small and hand-maintainable. `gitleaks` is the
  *escalate* option only if signature coverage/maintenance becomes a real burden. Ships as an allowlistable `tt`
  leaf (mechanical unit), consistent with [[never-allowlist-interpreters]].
- **Output:** candidate list (file:line, matched signature, entropy) → feeds Layer 2. Exit non-zero on any
  above-threshold candidate (so it can gate a pre-commit / pre-deploy step later).

## Layer 2 design — semantic triage (in-session)
A reusable **review-fleet brief** (lifting the CF5 ZIP-audit briefs): for each candidate, decide *real value* vs
*name/comment/keyword/test-fixture*, with the repo context. Cheap, and it is exactly the judgment a regex cannot do.
Output: a triaged report (confirmed / FP / needs-human), the confirmed set escalated to BR.

## Egress-hardening — scan what LEAVES, not just the repo (the round-4 / ZIP-audit pattern)
The round-4 gap and the 2026-07-10 pre-upload audits were about **what crosses the boundary**. `tt harden` should
have an **egress mode**: scan a directory or archive *destined to leave* (a ZIP before upload, a deploy payload, a
research export) with the same Layer-1+Layer-2 pipeline — because a secret that is fine at rest in a private repo is
a leak the moment it is bundled and sent. This is the higher-value half: repo-at-rest is already disciplined here;
the risk is egress. (Cf. the `credential-leak-in-deploy-verbose` incident — a `--dry-run` that printed the SFTP
password: an *egress* leak, not a repo leak.)

## Open questions for BR
1. **Build `tt harden` Layer 1 now?** (pure-JDK signature + entropy + gitignore-respect; a `tt` leaf.)
2. **Entropy threshold** default (~4.0 bits/char, len ≥ 20) — tune to taste after a first run?
3. **Egress mode in v1, or repo-scan first and egress in v2?** (I'd argue egress is the higher-value half.)
4. **The FP-triage fleet brief** — want it captured as a reusable artifact (like the delegation-dance briefs)?

Ties: [[hardening-dance]], SM041 (allowlist deep-mine — the sibling hardening arm), the round-4 security-gap +
ZIP-audit pattern, guardcheck (SM007c — the mechanical-check + human-gate precedent),
[[credential-leak-in-deploy-verbose-2026-07-08]] (the egress-leak incident), [[dependency-preference-cascade]].
