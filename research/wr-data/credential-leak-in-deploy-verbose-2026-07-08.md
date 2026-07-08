# WR data: a credential leak in the deploy tool, caught by an agent-run dry-run (2026-07-08)

During the first live production test of `deployblog.sc` (SFTP deploy to bjornregnell.se), an **agent-run
`--dry-run` surfaced BR's SFTP password in cleartext** in the tool output. No real upload happened, and the
dry-run is exactly what caught it before either a real deploy or BR running it himself. Recorded here because it
is a clean, general security lesson (and a point in favour of agent-run dry-runs).

## What happened
`deployblog.sc` shells out to `lftp` to mirror the rendered site over SFTP. Two things combined:
1. **`lftp` in verbose mode reconstructs the full `sftp://login:PASSWORD@host/...` URL for every operation** it
   logs (mkdir, get, chmod). The password is embedded in each URL.
2. **`deployblog.sc` piped `lftp`'s output straight through** (`ProcessBuilder.Redirect.INHERIT`), so those
   credential-bearing URLs went verbatim to stdout, i.e. the terminal scrollback, the session `.jsonl`
   transcript, and the model's API context.

The design had been careful about the *command line* (creds travel on `lftp`'s stdin, never on argv, so nothing
leaks via the process list) but missed the *output* channel. Secret handling has more than one hole to close.

## Blast radius + remedy
Exposure = local terminal + session transcript + model context/logs. Not posted anywhere public. Remedy =
**rotate the SFTP password** (one.com email-based reset), which invalidates the leaked value outright. BR is doing
this. Rotation is the clean fix; scrubbing a transcript is not reliable.

## The fix
`deployblog.sc` no longer inherits `lftp`'s output. It captures the stream and redacts the password to `***` on
every line before printing, so the dry-run plan stays readable (you still see files + paths) while the secret
never reaches any sink. Verified: post-fix dry-run shows `sftp://login:***@host`.

## General principle (transfers beyond this tool)
**Any tool that runs a credential-bearing subprocess must not inherit its output raw — capture it and redact known
secrets first.** Verbose modes of network tools routinely echo credentials inside URLs: `lftp`, `curl -v`, `git`
with a userinfo URL, `rsync` over ssh, `wget`. The secret can leak on the *output* side even when the *input* side
(argv, env, stdin) is handled correctly.

## Method note: agent-run dry-run as a security net
The dry-run was run by the agent, not just as a correctness check ("are these the right 5 files?") but it doubled
as a **security check** — it exposed the leak in a harmless, reversible run before any live deploy. This is a
concrete argument for agent-driven dry-runs before outward-facing actions, and another instance of a
human-present (not AFK-strict) run surfacing data that pure inward work would not (see
[[not-afk-safe-solo-yields-wr-data]]).

## `tt` / genscalator angle
A future typed `tt deploy` should bake **output redaction in by construction** (declared secret-handling on a
typed tool), rather than relying on a hand-rolled script remembering to capture-and-redact. This is the
genscalator thesis again: a general executor (`lftp` piped raw) leaks by default; a typed tool with declared
narrow semantics can guarantee it does not.

## Disposition
Feeds the [[hardening-dance]], a future `tt deploy`, RT052 (deploy friction/throughput), and the deploy-odyssey
record ([[shell-blob-fallback-regression-2026-07-07]] is a sibling process-slip log). Action on BR: rotate the
SFTP password; then the real production push can proceed with the fixed (redacting) tool.
