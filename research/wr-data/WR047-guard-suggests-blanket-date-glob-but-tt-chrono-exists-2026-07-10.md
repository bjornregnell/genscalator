# Guard offered a blanket `date *` allow for a timestamp - but `tt chrono now` already exists (2026-07-10 17:03)

BR-flagged WR datum. A clean two-layer specimen of the reflex-slip + over-broad-glob hazard, with a clean
resolution: the typed leaf was already there.

## What happened
Asked to timestamp a WR datum, the agent reflexively ran raw **`date -Iseconds`**. The permission guard's
suggested remedy was to allowlist **`date *`** - a blanket `*` glob over a raw system utility. BR caught it:
"we need a date tt - or do we have it already?" We do: **`tt chrono now`** prints `2026-07-10 17:03:08`
(local, `yyyy-MM-dd HH:mm:ss`). No allowlist change, no new tool: the fix was to USE the existing typed tool.

## The two layers (both are the datum)
1. **Reflex slip.** Reaching for raw `date` is the SAME family as raw-`grep`->`tt text`
   ([[use-tt-grepr-not-raw-grep]]): the muscle-memory shell reflex fires before the typed-leaf habit. The typed
   tool existed; the agent just didn't reach for it first.
2. **The guard's remedy is itself the hazard.** Broadening the allowlist with a `*` glob over a raw utility is
   exactly the over-broad-`*`-glob that **SM041** (fleet deep-mine to sharpen broad globs + lift raw-command
   allows to typed `tt` tools) targets, and it brushes [[never-allowlist-interpreters]]. Guards optimize for
   "unblock this call now", which pulls toward widening the allowlist; the sovereignty-preserving move is the
   opposite - encapsulate the capability in an audited typed leaf, allowlist only `tt`. **Never widen the
   allowlist over a raw utility when a typed `tt` leaf can encapsulate it** (and here, already does).

## Corollary spotted
`tt guardcheck` ALREADY EXISTS as a tool (partial realization of the SM007c guardcheck-hook idea - it flags
un-typed command shapes). Flag for the SM007c follow-up; not investigated here. Ties: SM041, SM007c,
[[dependency-preference-cascade]] (JDK `java.time` already vendored in `chrono.scala`, so no dep needed),
[[guard-against-forced-confirmations]].
