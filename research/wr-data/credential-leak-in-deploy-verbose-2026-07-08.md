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
SFTP password; then the real production push can proceed.

## Update (2026-07-08): the contained-def resolution + the capture-checking lesson
BR's follow-up reframed the fix as a *containment boundary*, which is both cleaner than redact-and-relay and the
point where capture checking (CC) becomes the right tool.

**The contained-def shape (shipped).** `deployblog.sc` now runs lftp through a `runContained(script): (Int, String)`
def that reads lftp's merged stdout+stderr into a LOCAL string and NEVER prints it. deployblog prints only its OWN
summary, synthesized from values it controls (localDir / remoteDir / the local file list). The credential-bearing
lftp output dies inside the def; it is surfaced only on `--check` (where the remote listing IS the point) or on a
failure, and even then redacted. This removes the reliance on a redaction regex on the normal path: the secret is
never printed at all, rather than printed-then-scrubbed.

**Why this is the CC-relevant framing** (two corrections to the first-pass "CC can't help here"):
1. *Capturing, not inheriting, is the enabling move.* With `Redirect.INHERIT`, lftp writes straight to the
   terminal and the bytes never enter the program, so no type system can reach them. CAPTURING pulls the
   secret-bearing bytes back INTO the typed world, where the program's handling of them is governable.
2. *"A def that never itself prints" = "a def that does not capture the console/print capability."* In a CC +
   safe-mode world (the SM017 PoC), that is not a discipline you hope holds -- CC can PROVE at compile time the def
   cannot print, because it does not hold the capability. It captures the *network* capability (to do sftp) but
   not the *console* one, and CC distinguishes them.

So CC governs *containment of the print effect* (capability lens), which is distinct from and complementary to
labelling the secret as data (a `Secret`/`Classified` IFC wrapper). Airtight version: CC contains the print
capability inside the runner; if secret-bearing data must cross the def boundary, wrap it as `Secret` so callers
inherit the constraint. Caveat: Java IO (`ProcessBuilder` / `BufferedReader`) is not capability-typed, so a real
CC proof needs capability-typed facades over the Java IO -- which is exactly why the CC-verified version belongs in
a nightly experiment, NOT in this production deploy tool (kept on stable so nightly/CC churn never breaks a deploy).
Pinned as **SM033**.

## Timestamp retrofit (2026-07-10, evidence-timestamp enhancement)
Git-anchored bracket (git dates are NON-perishable, unlike the transcript): the leak event was **before** this
note's add-commit **2026-07-08 20:57:08 +0200**; contained by fix commit `0012bec` ("deployblog: contained-def
shape…") **2026-07-08 21:21:54 +0200**; SM033 follow-up `75392cd` **2026-07-09 12:34:13 +0200**. So leak <
20:57:08+02 on 07-08, contained < 21:21:54+02 — about 25 min from record-to-containment on the git record. The
exact transcript stamp of the dry-run is recoverable by grepping an `lftp` / `sftp://` output fragment if wanted.
([[raw-data-append-only]].)
