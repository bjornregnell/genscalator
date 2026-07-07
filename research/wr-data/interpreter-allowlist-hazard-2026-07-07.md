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

## Addendum (BR, 2026-07-07): the near-miss — the BadGoal almost fired

BR reported he was **nearly accidentally clicking option 2** ("Yes, and
allow `python3 *`") — his hand was on the button; he declined only just
in time. Not hypothetical: it is the **weaponized-CF BadGoal**
(`foundations.md`: *"BHH pushes the tired human into a broad 'always
allow' that grants more than intended"*) **almost realized** by an
ordinary sub-agent overreach + a badly-shaped harness prompt — no
adversary required. BR: *"thank supreme being for the harness BUT it is
REALLY bad that I was nearly accidentally clicking allow python3 star —
THAT'S BAD."*

Lesson sharpens: the structural safeguard (the guard fired) is
**necessary but not sufficient** when the guard's own UI offers the
dangerous generalization as a one-click default at maximum friction. The
human is the authority anchor, but the anchor is **fallible under a
mis-shaped prompt** — a near-miss is a hit that missed by luck, and luck
is not a control. Two structural responses, both human-owned:
1. **Shrink the attack surface upstream** so these prompts rarely arise:
   tighter briefs (sub-agents never reach for python3) + a curated
   allowlist. Motivates the standing **settings-hardening inspection bg
   task** (PB) — proactively spot + propose hardening so the tired human
   is offered fewer dangerous blankets in the first place.
2. **The `never-allowlist-interpreters` reflex must be the human's too,
   structurally** — BR now holds it (he declined), but a near-miss shows
   knowledge ("I know not to click that") leaks under a fast hand, same
   as it leaks for the agent under momentum. Only a narrower allowlist /
   a prompt-heuristic fix makes it structural on the human side.
