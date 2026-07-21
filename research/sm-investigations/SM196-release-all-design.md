# SM196 — releases across multiple mirrors: `tt forge release-all` design note

2026-07-22 00:2x, agent-drafted on the post-warp solo menu (NOW.md item 4, BR's "go 12345").
Status: DESIGN PROPOSAL for BR's ratification; nothing here is built. Evidence base:
`research/wr-data/gh-vs-tt-forge-capability-gap-2026-07-21.md` (the 11-prompt casefile,
one real release's plumbing paid one guard prompt at a time) + the CD13 x4 push routine +
the v0.9.1 release propagation (mirror.sc closed the tag gap by hand).

## The one-screen summary

With CD13, COMMITS propagate x4 automatically (origin, github, coursegit, gitlab), but a
RELEASE still lands per-forge by hand. The casefile measures the cost: 11 hand-approvals
for one GitHub release (2 reads, 3 draft-body edits, 6 asset uploads), every one a shape a
typed verb would have made a single allowlist entry. Proposal: grow `tt forge` from its
current 5 Gitea-flavored verbs into a per-forge-adapter design with a `release-all`
orchestrator on top. Four new verb families are evidenced by live specimens: list/read,
edit-by-id, draft flow, asset upload. The build splits cleanly into three stages, each
independently useful; stage 1 (adapters + single-forge verbs) removes most of the measured
pain on its own.

## Requirements, each traceable to a specimen

R1. **Per-forge API adapters, not a BASE-url parameter.** Empirically settled: `tt forge
    releases lunduniversity/introprog --url https://api.github.com` 404s because tt forge
    builds Gitea paths (`/api/v1/repos/...`) while GitHub wants `/repos/...` (casefile,
    addendum 1). Three dialects minimum: Gitea/Forgejo (codeberg), GitHub, GitLab.
R2. **Draft-release support + id-addressing.** GitHub drafts have an empty `tag_name`
    until publish; they are reachable only by numeric release id, a concept absent from tt
    forge's tag-addressed surface (addendum 2). The draft-first flow (create draft,
    iterate body, tag+publish last) is how a careful human releases; release-all must
    model it, not just the publish-in-one-shot case.
R3. **Edit-until-right must be one allowlist entry.** The draft body was PATCHed 3 times
    (n=3 measured, every iteration one hand-approval). A `release-edit --body-file` shape
    per forge amortizes the whole review loop.
R4. **Asset upload.** 6 hand-approvals for 6 PDFs (addendum 3). Per-forge upload models
    differ structurally, see the adapter sketch below.
R5. **Auth per host.** tt forge reads a token only for create/edit today, and GitHub draft
    releases are INVISIBLE without auth even for reads (addendum 1). Adapter-level auth:
    token env per host (naming scheme to decide, e.g. `TT_FORGE_TOKEN_<HOST>` with the
    current single-token env as fallback), applied to reads as well as writes.
R6. **Idempotency + partial-failure semantics, the CD13 rule.** One forge flaps (origin
    flaps regularly, see the codeberg status reflex) -> the others still land, the failed
    one is retryable next invocation, nothing half-lost. Concretely: `release-all` queries
    each forge first (does a release for tag T exist? in what state?) and converges toward
    the desired state rather than blindly creating; re-running after a flap is a no-op on
    the forges that already succeeded.
R7. **Tags-first split.** Tag propagation is ALREADY solved x4 by the CD13 push routine
    (`git push --tags` per remote); release-all owns only the release OBJECT (notes,
    draft state, assets). It should verify the tag exists on the target forge and fail
    that forge with "push tags first" rather than minting tags itself — one writer per
    concern, mirror.sc stays the deliberate force/repair path.

## Adapter sketch (verify each against current API docs at build time; the GitHub facts
below are casefile-tested, the Gitea facts match the shipped tt forge code, the GitLab
facts are from memory and UNVERIFIED)

* **Gitea/Forgejo (codeberg):** `/api/v1/repos/{o}/{r}/releases`; supports drafts +
  numeric ids; asset upload = `POST .../releases/{id}/assets` multipart on the same host.
  Closest to what tt forge already speaks.
* **GitHub:** `/repos/{o}/{r}/releases` on api.github.com; drafts id-only until publish
  (R2); asset upload goes to a DIFFERENT host (uploads.github.com, the `upload_url`
  hypermedia field) with explicit content-type — the adapter must not assume same-host
  uploads.
* **GitLab:** releases live under `/projects/{id-or-urlencoded-path}/releases`, addressed
  BY TAG (no draft concept known to this note — investigate; if confirmed, `release-all`
  degrades GitLab to publish-at-the-end, which the draft-first flow permits naturally);
  "assets" are LINKS to already-uploaded files (generic package registry or uploads), not
  a multipart release-asset endpoint. This is the structurally-different one and the
  reason the adapter interface must not bake in "upload bytes to the release".
* **coursegit (Lund GitLab):** same dialect as GitLab, behind auth. OPEN QUESTION for BR:
  do releases even make sense there (student-facing, auth-walled)? Proposal: config marks
  it `releases: skip` by default; release-all reports "skipped by config" per forge so the
  tally always enumerates all four.

## Proposed surface

Stage 1 — adapters + single-forge verbs (removes the 11-prompt pain):
  `tt forge --forge github|codeberg|gitlab <verb>` where verb set grows to
  `releases | release-create | release-edit | release-publish | release-asset-upload`,
  every verb accepting `--tag T | --id N` (R2) and `--body-file F` (R3). One allowlist
  entry per verb shape, guard-clean by construction.
Stage 2 — `tt forge release-all --tag T --body-file F [--assets G...]`: walks the
  configured mirror set (derived from the repo's remotes, the SM149 push-all principle:
  configuration IS the remote list + a small per-forge overlay like `releases: skip`),
  converges each forge per R6, prints a per-forge outcome table.
Stage 3 — draft-loop ergonomics: `release-all --draft` creates/updates drafts everywhere
  it can (GitHub, Gitea) and holds GitLab to the publish step; `release-all --publish`
  finishes the loop. Exactly the careful-human flow, typed.

## What this note deliberately does NOT decide (BR's calls)

1. Build-vs-wrap for GitHub: gh is an audited, platform-blessed executable; the
   dependency-preference cascade may favor keeping gh underneath the GitHub adapter
   (tt forge as the typed front, gh as transport) over reimplementing auth + uploads.
   The casefile supports either; the guard economics only require the TYPED FRONT.
2. Whether SM189 (what a version/release MEANS across plugin version, tags, tt update)
   must settle first. Recommendation: stage 1 is semantics-free (it only manipulates
   existing releases), so it can proceed; stage 2's "desired state" wants SM189 settled.
3. The token-env naming scheme (R5) — settings-adjacent, human-gated by standing policy.
4. Whether the avoid-guard-stall / tt-toolbox skills gain the one-line "forge ops: check
   `tt forge help` first" trigger now or with stage 1 (the casefile's check-first reflex
   failed twice in one hour; the skill fix is independent of any build).

Ties: SM196 (this), SM149, SM189, SM178e, CD13, the casefile. Next concrete step if BR
ratifies: stage-1 adapter interface as a `tools/` change proposal, test-first against
codeberg (whoami/releases are read-only probes).
