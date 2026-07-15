# Tool candidate: `tt web --head` / `--trace` (redirect-chain + header inspection) — 2026-07-15

**Type:** WR data — tool candidate, from a live dual-use-curl guard-stall. BR flagged it: *"don't we have a
tool for this?"* when `curl -sIL http://genscalator.ai` required approval.
**Threads:** the PRD `ttWeb` feature ("safe read-only HTTP … replacing the dual-use curl reflex"),
[[use-tt-grepr-not-raw-grep]] (the same typed-replacement pattern), [[never-allowlist-interpreters]].

## The gap
Verifying a domain redirect (genscalator.ai → bjornregnell.se/genscalator) needs the HTTP **status codes**,
**Location headers**, and the **redirect chain** — i.e. `curl -sI` (headers only) and `curl -sIL` (follow +
headers). `tt web` exists but does a **GET of the body** (size-capped, no creds); it does NOT expose status
codes, response headers, or the hop chain. So the only way to answer "is it 301 or 302, and where does each hop
point?" was raw `curl -sIL`, which is dual-use and correctly guard-stalled. The typed tool the PRD promises
(`ttWeb` replacing the curl reflex) does not yet cover this HEAD/trace use case.

## The candidate
Extend `tt web` (or a sibling verb) with:
- `tt web --head <url>` — a HEAD request (or a GET with body discarded); print the status line + response
  headers (at least status, `Location`, `Content-Type`, `Server`). The safe `curl -sI`.
- `tt web --trace <url>` — follow redirects and print **each hop**: status code + `Location`, then the final
  status. The safe `curl -sIL`. Cap the hop count (e.g. 10) and the total; loopback/private-address deny like
  the existing safe-web rules. Read-only, no creds, no request body — so it stays allowlistable (`Bash(tt web
  *)`) where raw curl cannot.

## Why it matters
This is the `ttWeb` thesis in miniature, surfaced by a real task: a genuinely-safe, read-only HTTP operation
(inspect a redirect) forced a dual-use `curl` because the typed tool lacks the mode. Adding `--head`/`--trace`
removes one more reason to reach for curl, and makes redirect/header checks a bare allowlistable command instead
of an approval each time. Feeds PRD `ttWeb`.
