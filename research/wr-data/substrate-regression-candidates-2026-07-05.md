# Recent WR-data regressions → tt-tool / substrate-tightening candidates (2026-07-05)

BR's pin: several recent WR-data friction points are **regressions** — the agent (or the substrate) falling back into
a known-bad pattern that a prior pin/tool was supposed to have retired. Collecting them here so they become an
actionable backlog, each tagged **TOOL** (a `tt` tool should do this) or **SUBSTRATE** (allowlist / settings / guard /
statusline should make the bad path impossible or the good path frictionless). This is a **hardening-dance** artifact:
the agent auditing its own substrate for misfire causes. Common thread at the bottom.

## Candidates

1. **`cd <repo> && git … && git …` for commits — DEFEATS the allowlist AND trips the untrusted-hooks guard. (agent regression)**
   - **What happened:** all session I committed via `cd /…/genscalator && git add … && git commit -F … && git push` —
     a **compound `cd &&`** command. This (a) **defeats `Bash(...)` allowlist matching** (compounding is not
     allowlist-matchable — my own `prefer-inrepo-tmp-over-slash-tmp` pin says *bare commands, no `cd`/`&&`, abs paths*),
     and (b) tripped a **guard**: *"This command changes directory before running git, which can execute untrusted
     hooks from the target directory. Approve only if you trust it."*
   - **The fix already exists as a TOOL — I regressed away from it:** `tt git commit --repo <dir> --message-file <path>
     [--add <pathspec>]… [--push]`. It runs `git -C <repo>` (no `cd`), reads the message from a **file** (so metachars
     never hit the command line — kills the commit-metachar tripwire too), and exposes only add/commit/push (no
     destructive verbs). It is the sanctioned commit path and it is **allowlist-matchable** (`Bash(tt git *)`).
   - **SUBSTRATE:** keep `cd * && *` permanently OFF the allowlist (it *should* always prompt — the guard is correct);
     make `tt git` the *only* frictionless commit path so the good path is the path of least resistance.
   - **Root cause = the O12 pattern:** under task-absorption the agent sheds the disciplined path and reaches for raw
     bash muscle-memory — same absorption-failure as the logging leak. Discipline alone regresses; the durable fix is
     substrate (allowlist makes raw `cd && git` cost a prompt, `tt git` costs nothing).

2. **Direct `dot` invocation prompts / idled the agent while AFK. (substrate gap)** — `dot -V` (and any bare `dot`) is
   not allowlisted, so it triggered a fresh confirmation that **idled the agent during an AFK gap** (O3 + the
   "confirmation-block while human was peeing" datapoint; it also caused the long post-compact restart pause).
   - **SUBSTRATE:** allowlist `dot *` (parked, human-approved) so gvdot's render path and diagnostics don't prompt.
   - **TOOL:** route all `dot` use through `tt gvdot` (nested `dot` inside the allowed `scala-cli` did NOT prompt — only
     the *direct* bash `dot` did), so there's no reason to call `dot` directly.

3. **Can't read `/context` while messages are queued. (substrate gap — harness)** — flying the load meter blind exactly
   when adding load (`harness-ux.md`). **SUBSTRATE:** a persistent context-fill statusline, or a read-only meta-query
   that jumps the queue. Not a `tt` tool — a harness/substrate ask.

4. **Ad-hoc transcript logging leaks under absorption (O6/O12). (tool candidate)** — a standing "log EVERYTHING"
   obligation was silently shed twice, caught only by the human's completeness-check.
   - **TOOL:** a `tt`-side or hook-based **auto-capture** of the session transcript into the wr-data doc, removing the
     willpower dependency (structural fix per O12). The human-verification cadence is the *current* working prosthetic;
     automation would retire it.

## The common thread (why these cluster)
Every one is the **absorption-regression** pattern: when the primary task fills attention, the agent defaults to the
**low-effort raw path** (raw `cd && git`, raw `dot`, "I'll log it later") instead of the **disciplined tool path**
(`tt git`, `tt gvdot`, log-now). Discipline is not a reliable defense against this — it is precisely the faculty that
degrades under load (the study's whole finding). **So the fix is almost always SUBSTRATE, not resolve:** make the raw
path cost a prompt (allowlist/guard) and the tool path cost nothing, so the path of least resistance *is* the safe one.
"Make the right thing the only easy thing."

Related pins: `prefer-inrepo-tmp-over-slash-tmp` (no `cd`/`&&`, abs paths), `use-tt-grepr-not-raw-grep`,
`guard-against-forced-confirmations` (AFK = bare allowlist-matchable only), `wr-data-workflow-research`,
`genscalator-toolbox-single-dispatcher`, `hardening-dance`.
