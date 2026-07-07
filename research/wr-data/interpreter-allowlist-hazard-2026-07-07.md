# WR data: interpreter-class "don't ask again" offers are blank-shell grants (2026-07-07)

## Event

Agent wrote a one-off Python scratch (`reflow.py`, lossless 72-col
markdown reflow of PIN-BOARD.md) and ran it with `python3 <script>`.
The harness raised "This command requires approval" and the approval
dialog offered:

> 2. Yes, and don't ask again for: `python3 *`

BR flagged the offer as DANGEROUS and declined the blanket option:
"we can never make python3 more targeted as python3 can do anything."

## Analysis (agent agrees)

- **Interpreter-class commands cannot be meaningfully scoped.**
  `python3 *` (like `node *`, `sh *`, `perl *`) is an
  arbitrary-code-execution grant: `-c` takes inline code, and any
  script path is only as safe as the file content *at run time*.
- **Even an exact-path allow is TOCTOU-broken** when the agent
  authors the script: the allowlist pins the command *string*, not
  the code behind it. Agent writes script → runs it unprompted →
  the "targeted" entry is a blank shell with extra steps.
- **The only safe shape:** allowlist entries must point at
  **audited-fixed executables** — `tt` leaf tools, or a
  git-committed human-reviewed wrapper script (same principle as the
  planned `ssh bjornyx.local /home/bjornr/bin/gen.sh *` narrowing).
  Ephemeral scratchpad scripts never qualify.
- **Honest tension:** `scala-cli` is the same interpreter class and
  is already the blessed lane. That is a *deliberate, single-lane*
  tradeoff (one audited arbitrary-code surface, in-repo git-visible
  scratches), not evidence python3 is fine. The rule that keeps the
  tradeoff sane is: **exactly one blessed scratch lane; every other
  interpreter stays prompted, forever.** Interpreter sprawl is how
  allowlists rot into `Bash(anything *)`.

## The agent-side misfire (root cause of the prompt)

The friction was self-inflicted: standing memory
`prefer-scala-scratch-over-bash` says use scala-cli scratches, but
the agent consciously chose python3 ("gray zone" reasoning, regex
familiarity). So this is a **deliberate-deviation** data point, not
context-rot: the single-lane policy erodes precisely through
locally-reasonable exceptions, and each exception then generates a
tempting blanket-allow offer at the human. Structure-over-willpower
again: the harness's "don't ask again for `<cmd> *`" UI actively
invites the worst generalization at the moment of maximum friction.

## Resolutions

1. NEVER allowlist interpreter-class commands (`python3`, `node`,
   `sh`, `perl`, ...); always answer such offers with a one-time yes
   or a decline. (Memory: `never-allowlist-interpreters`.)
2. Scratches go through the single blessed lane (scala-cli / `tt`),
   per the existing memory; a per-run prompt for an off-lane
   interpreter is the system working, not friction to optimize away.
3. Harness-UX observation for the record: the auto-generated
   "don't ask again" suggestion truncates to `<argv0> *` — safe for
   fixed-function binaries, hazardous for interpreters. A better
   heuristic would refuse to offer wildcards for known interpreters.
