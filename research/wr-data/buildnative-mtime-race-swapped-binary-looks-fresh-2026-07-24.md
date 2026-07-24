# buildnative mtime race: a swap landing AFTER an edit masquerades as fresh (2026-07-24)

Logged 2026-07-24 ~14:2x by CF5, BR-present. BR handed in the anomaly live: "colors still
the same; is the updated status line lagging?"

## The specimen

1. ~14:08 a buildnative run started (motivated by an earlier help-text edit).
2. ~14:1x, WHILE it built, the agent edited `statusline.scala` again (the w-cluster hue
   174 -> 99, BR's color-separation call).
3. The ritual finished green (its parity suite has no pin on the base hue) and SWAPPED.
4. The swapped binary's file mtime is NEWER than every `tools/*.scala`, so the launcher's
   staleness check (`find -newer`) declares it FRESH — but it was COMPILED from the
   pre-edit source. Result: statusline renders the OLD hue from a "fresh" native binary,
   with no STALE warning and no fallback. The human saw it before the agent did.

## Why the guard missed it

The staleness check compares FILE TIMES, which measure when the binary was WRITTEN, not
WHAT it was built from. A build racing an edit inverts the invariant the check assumes
(binary-newer-than-source == binary-built-from-source). The parity suite cannot catch a
difference nobody pinned (the hue had no test — deliberately, it is an eye matter).

## Fix applied now + candidate structural fix

- Now: rebuild AFTER the edit settled (this note's commit rides that rebuild).
- Agent discipline (cheap): do not edit tools/ while a buildnative run is in flight; if it
  happened, rebuild again unconditionally — never trust the no-STALE silence after a race.
- Structural candidate (SM-material, BR decides): buildnative records a SOURCE FINGERPRINT
  (e.g. sha256 over sorted tools/*.scala) next to the binary; the launcher compares
  fingerprint, not mtime — the check then measures what it claims to measure. Cost: one
  hash per launch (~ms native) vs the current find -newer.
