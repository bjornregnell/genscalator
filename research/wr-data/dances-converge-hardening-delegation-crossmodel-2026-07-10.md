# Three dances converged in one move: outbound-hardening + delegation + a CO4->CF5 echt check (2026-07-10)

BR-flagged WR datum: before uploading a ZIP of the public genscalator repo to ChatGPT, agent+human reflected and
BR asked for a Fable-5 (CF5) sub-agent to audit it "so we are not doing anything stupid." That single move was
simultaneously THREE things, and the overlap is the interesting part.

1. **A hardening dance - in a NEW direction (outbound).** The standard hardening dance audits our OWN config /
   allowlist for misfire causes. This audited what LEAVES: outbound data about to be exposed to an external
   service (which may cache/index/train on it). Hardening applied to the *egress* boundary, not the config
   boundary. Worth generalizing: "before content crosses to an external service, audit it" is a hardening-dance
   variant (egress-hardening) distinct from the config-hardening variant.

2. **A delegation dance.** CO4 delegated the audit to a CF5 sub-agent with a Read-only, allowlisted-only brief
   (tt text grepr + Read + Glob, no writes/web). Scoped + verifiable + context-light = a textbook delegation
   target.

3. **A CO4 -> CF5 echt attempt (the sharpest part).** The verifier is a DIFFERENT model than the author. CF5
   audited what CO4 assembled - so the check is genuinely INDEPENDENT, not the author grading its own work. This
   is the "reliable self-measurement is impossible from inside a degrading system - use an external observer"
   principle (blog 001) realized via **cross-model delegation**: a differently-trained model with fresh context
   is a partial external observer for the agent's own work. Structurally stronger than CO4 self-review (which
   inherits CO4's blind spots). Result: CF5 verdict SAFE TO UPLOAD, thorough (108k tokens, 19 tool-uses;
   correctly rated the credential-handling code as false-positives - creds read at runtime from .netrc/env, only
   names present - and flagged two borderline-informational items: the settings-allowlist mirror reveals
   security posture, and one probe-line mentions the private Odersky correspondence exists, no content).

**Why it matters.** (a) Egress-hardening is a hardening-dance direction we had not named - protect what we SEND,
not just our own config. (b) Cross-model verification is a concrete, cheap way to get a MORE-independent check
than self-review - the external-observer principle applied at the model level. (c) The three dances co-occurring
in one move suggests the "dances" are not disjoint rituals but composable moves that stack on a single action.

**Echt caveat.** CF5 is not a fully-independent oracle - same model family, may share some blind spots (a
correlated-error risk); and it can only audit what it is pointed at. So "different model" buys real but not total
independence - better than self-review, weaker than a human or a truly-independent tool. Ties: hardening-dance,
delegation-dance, RT050 (does delegation lower rot), RT029 (cross-model), blog 001 (external-observer principle).
