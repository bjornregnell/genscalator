# SM152 — a sanctioned way to download a BINARY file to disk

**Status: DESIGN (agent-drafted AFK 2026-07-18).** No build here — the tool is a small git/http wrapper the agent
can draft, but it is **egress behaviour**, so the wiring and any allowlist entry stay **BR-reviewed**. This note is
the interface + safety model to react to.

## Why (the gap, grounded)

Surfaced 2026-07-18 self-hosting Fira Sans on the media page: the page needs the `.woff2` font files shipped
alongside it, so the fonts had to be **fetched to disk**. But the audited egress tool, `tt web`, is a read-only
**fetch-and-PRINT** — it does GET and writes the body to **stdout**, size-capped, with no `--out` flag
(serv/web help: "It can ONLY fetch-and-print"). A binary written to stdout is garbled, and steering it to a file
needs a `>` redirect, which the guard also stops. So the **only** path left was raw `curl -o` — two gated
approvals for the two extra weights (400 came through, 500/600 each prompted).

That is a real capability gap: the toolbox can **read** the web safely but cannot **save a fetched asset** without
falling back to the exact raw `curl` the model tells us to avoid. It also leaves **SM143 incomplete** — that note
adds a guardcheck hint steering `curl`/`wget` toward `tt web`, but if `tt web` cannot do the download-to-file case,
the hint points at a tool that can't finish the job. Closing this closes the **last** raw-`curl` use case, so the
toolbox is self-sufficient for egress ([[dependency-preference-cascade]] — JDK's built-in HTTP client already backs
`tt web`, so no new dependency).

## Design fork (BR's call)

**(A) Add `--out <path>` to `tt web`** — extend the already-audited fetch tool:
```
tt web get <url> --out <path> [--host H] [--max-bytes N]
```
- Smallest new surface: one code path already owns the egress caps (GET-only, no credentials, size limit, `--host`
  lock). We only change the **sink** from stdout to a file when `--out` is given.
- Cost: `tt web` stops being purely read-only — it becomes a writer in the `--out` case. That is a real semantic
  shift worth naming, but a bounded one (write only to the one explicit path arg).

**(B) A dedicated `tt download <url> <path>`** — a separate tool with the same caps.
- Keeps `tt web` read-only (conceptually cleaner: `web` reads, `download` writes).
- Cost: duplicates the egress-cap plumbing, or factors it into a shared helper (`object Web`); two tools to audit.

**Recommendation: (A)**, a `--out` flag on `tt web`. One audited egress code path is easier to reason about than
two, and the caps are identical; the read-only-purity loss is contained to the flagged case. (If BR prefers the
clean read/write split, (B) over a shared `object Web` helper is the fallback.)

## Safety model (whatever the surface)

1. **GET-only, no credentials/cookies ever** — unchanged from `tt web`. A download tool must not gain POST/upload
   or auth headers; that is the property that stops it being an exfiltration channel.
2. **Write ONLY to the explicit path arg — never a shell redirect.** The whole point is to keep the egress-to-disk
   step *inside* the analyzable tool (`tt web get <url> --out <file>` is one literal), not behind a `>` the
   allowlist can't bound. Same principle as `SECURITY-MODEL` §3.5: syntax in the allowlist, semantics in the tool.
3. **Path-scope + no-clobber.** Resolve the canonical `--out` path; reject `..` traversal and `.git/`; **refuse if
   the file already exists** (overwrite is a separate explicit gesture, `--force`, never the default) —
   [[no-clobber-human-owned-files]], mirroring SM147's `tt move`.
4. **Size cap retained.** Keep `--max-bytes` (default sized for real assets — a font/image is tens–hundreds of KB;
   pick a sane binary default higher than the 5 MB text default only if needed) so a hostile URL can't fill the
   disk. Truncation must be a **hard error that deletes the partial file**, never a silent short write that leaves
   a corrupt asset (the `file` check tonight would have caught a truncated woff2, but the tool should self-guard).
5. **`--host` lock retained** — the SSRF-read mitigation; a download tool reaching internal hosts is the same risk.
6. **Integrity is out of scope (noted).** No checksum/signature verification; the caller still eyeballs the result
   (tonight: `file X.woff2` → "Web Open Font Format (Version 2)"). A `--sha256 <hex>` verify flag is a possible
   later hardening, not required for v1.

## What it must NOT do
- No POST / PUT / upload / auth headers — read-side egress only.
- No writing outside the explicit path; never touch `.git/` or escape via `..`.
- Never silently overwrite an existing file; never leave a truncated/partial file on a capped or failed fetch.
- Not a general `curl` — one URL, one output path, per call.

## The permission question (BR's call — do not pre-decide)
It is egress, so the allowlist entry is BR-reviewed. Because it only GETs and writes to one bounded path, it is a
better allowlist candidate than raw `curl` (which can also upload/exfil) — but "fetch a URL to disk unattended" is
still a capability worth a deliberate decision, not an assumed solo power ([[web-surf-not-afk-safe-front-load-and-vet-egress]]).

## Ties
SM143 (guardcheck hint `curl`/`wget` → `tt web`; **this completes it** — the hint needs `tt web` to cover
download-to-file) · SM146 / SM147 (toolbox self-sufficiency — the sibling gaps: native tools; safe delete/move;
this: safe fetch-to-disk) · the `tt web` audited-`curl`-replacement design · [[dependency-preference-cascade]] ·
[[no-clobber-human-owned-files]] · [[web-surf-not-afk-safe-front-load-and-vet-egress]].
