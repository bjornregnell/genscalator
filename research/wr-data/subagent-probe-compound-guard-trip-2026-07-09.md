# Sub-agent compound probe trips guard; `script --version` false positive

Date: 2026-07-09 (BR-flagged, `WR data` cue)
Agent: SM016a transport-survey sub-agent (CF5), first tool call of its run.

## Event

The sub-agent's very first Bash call bundled five harmless read-only probes:

```
tmux -V; which tmux expect script socat screen ttyd asciinema; expect -v 2>/dev/null | head -1; script --version 2>/dev/null | head -1
```

The guard fired. BR reports the analyzer message: `'script' runs its argument as a
command — cannot be statically analyzed`. BR approved; the command then ran and
returned the wanted data (tmux 3.4, script, socat present; expect/screen/ttyd absent).

## Analysis

Two independent causes, both known failure classes:

1. **Compounding defeats the allowlist** (existing specimen family:
   compound-command-approval-halt-2026-07-07, command-hygiene-regression-2026-07-06).
   `tmux -V` alone would likely have matched; five commands joined by `;` and `|`
   cannot. The standing rule (bare allowlist-matchable commands, one per call) was
   in the sub-agent's inherited memory context but not enacted on call one — the
   recalled-vs-enacted gap, here in a FRESH sub-agent, not a rotted context.

2. **Name-level heuristic false positive.** The analyzer flags `script` because
   `script CMD` executes CMD — correct in general, but `script --version` executes
   nothing. The heuristic keys on the executable name, not the argument shape.
   Specimen of: coarse command-name rules cannot express "safe flag-only invocation".

## BR's question: could a Scala scratch tool have done this?

Split answer:

- **Existence checks (`which ...`): yes, and better.** A scala scratch can scan the
  `PATH` directories for executable files — pure filesystem reads, zero process
  execution, no guard surface at all. This is the clean win.
- **Version strings (`tmux -V`): only by shelling out**, i.e. moving the exec inside
  the JVM where the guard cannot see it at all. That is *worse* for auditability,
  not better — same lesson as interpreter-allowlist-hazard-2026-07-07. The right
  shape for versions is bare single commands, one per Bash call (`tmux -V` alone).
- Also situational: this sub-agent's brief explicitly forbade heavy scala-cli/JVM
  runs, so the scratch lane was closed in this lane anyway; and for a one-shot
  3-binary probe, scratch-compile cost exceeds the value.

**Takeaway:** the scratch rule's real generalization is "replace *text-munging
pipelines* with Scala", not "replace *process execution* with Scala". Probing
external binaries should stay in the shell, but as bare single commands. A future
`tt env probe <names...>` (audited fixed executable, allowlisted) would make this
class of probe prompt-free.

## Delegation-relevant note

Sub-agent briefs currently constrain tool lanes (this one did: no commits, no heavy
builds) but did not restate the bare-command hygiene rule. Guard stalls inside a
background sub-agent are doubly bad: the sub-agent is blind to the stall AND the
super-agent is blind to the sub-agent (cf. guard-stall-invisible-to-agent-2026-07-07).
Candidate addition to the delegation-dance brief template: "one bare command per
Bash call; no `;`/`|`/redirects".

## Super-agent addendum (CO4, 2026-07-10)

Concrete cost of this stall: the sub-agent sat blocked on the guard for **~11 hours** (duration_ms 40919428) - the whole time BR was away overnight - and produced nothing until BR released it the next day. Throughout, the super-agent reported the sub-agent as "running" and told BR it would "finish soon." So the invisibility is not a minor annoyance: a single un-allowlistable command in a background brief can silently waste an entire idle period.

**Strengthened brief rule (supersedes CO4's separate draft, now deleted as redundant):** the fix is not only "one bare command per Bash call" but "**no non-allowlisted command at all** in a background / AFK sub-agent brief." Even a bare `tmux -V` (which would pass the compound analyzer) is still not prefix-allowlisted, so it still raises a prompt and still deadlocks an unattended agent. Background/AFK briefs must restrict shell use to the blessed allowlisted lanes (tt / scala-cli / git) or to pure no-shell knowledge+write work. A future `tt env probe <names...>` (audited, allowlisted) would make binary-probing prompt-free and is the proper tool-shaped fix. Ties: [[guard-against-forced-confirmations]], [[cue-go-afk]], [[delegation-dance]], SM014 (blocked-detection / not-dead-proof).
