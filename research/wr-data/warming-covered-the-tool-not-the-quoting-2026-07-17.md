# Warming the minion covered the TOOL, not the QUOTING — and CO4 mis-diagnosed it twice (2026-07-17)

**Context:** meta-minion push 3 (re-hydrated fable `general-purpose` sub-agent). CO4's push payload **warmed**
the minion with reflexes, including *"Search: `tt text grepr <ABS-dir> <ext> <regex>` … NEVER raw grep"* and
*"Bash: ONE bare command, no pipes."*

## What the minion ran (BR pasted it from the guard)

```
tt text grepr /home/bjornr/git/berg/bjornregnell/genscalator/tools scala rot..(↑|arrow)
```
- **BR observed:** a **"Parse error" + "Do you want to proceed?"** prompt (routed to BR — a sub-agent's prompt
  lands on the human, not on CO4).
- **The minion reports:** a **fast bash syntax error (exit 2), no blocking prompt, immediate self-recovery** by
  re-running the regex single-quoted; every subsequent call quoted.
- ⚠️ **Contested and left open:** the two accounts do not fully reconcile (CANNOT VERIFY the exact harness path
  without the transcript). The minion asserting *"no prompt was triggered"* about its own environment, against
  BR's direct observation of one, is itself a candidate specimen of *an agent is an unreliable witness to its
  own execution context*. **Certain shared root cause: the unquoted `|`/`()`.**

## The finding — warming has COVERAGE, and CO4's had a gap

1. **The warming WORKED where specific.** Right tool, right **arg order** (dir, ext, regex last). No raw grep.
2. **It failed where SILENT: quoting.** The regex was left **unquoted**, so the bare `|`/`()` broke on the
   shell (and, per BR, tripped the harness analyzer). This is **NOT** guardcheck's `pipe to head/tail/wc` MED
   check (an *output* pipe); it is the shell/analyzer on a **bare `|`** — precisely push-2's adjudication that
   *"the stalls that motivated `--any` for bare `|` … came from the harness's own permission analyzer, not our
   hook."*
3. **A generic warning cannot carry deep project lore.** The `|`-in-an-unquoted-regex trap and its workarounds
   (quote the arg; `\x7C`-escape a literal pipe — `prohibition-does-not-arm-the-reflex-use-a-hex-escape-
   2026-07-16.md`) are project-specific. **Warming must be as SPECIFIC as the traps it prevents** — "no pipes"
   reads as "don't `| head`," not "quote your regex metachars."
4. **Bounded severity:** the minion **self-recovered from the fast-error path** (saw the error, retried quoted)
   — the same robustness cycle 2 showed. A fast bash error is self-recoverable; a *blocking* permission prompt
   is not (it hangs under AFK). Which one this was is the contested part above; the self-recovery is not.

## ⇒ Actionable improvement (action-research goal: improve the pairing)

**Warm minions with the CANONICAL `docs/guard-clean-digest.txt` (`gs warm`, SM077), not a hand-rolled subset.**
CO4's push hand-wrote a warming block covering output-pipes but omitting regex-quoting. The digest exists so
the reflex set is complete and does not drift; the push should **point the minion at it** (same file-brief
discipline as the main brief) rather than paraphrase a partial version. *(TODO: verify the digest itself covers
the quote/`|` trap; if not, that gap is the thing to close — it then protects CO4 and minions alike.)* Ties
[[warm-delegated-subagents-lack-caller-skills]].

## ⚠️ Meta — CO4 reproduced the study's OWN subject failure TWICE this cycle, both substrate-caught

The meta-minion exists to catch CO4 asserting-before-checking (brief item 2). CO4 did it **twice in cycle 3**:

1. **"minion regressed to a PIPE (item 5)"** — written from BR's one-line paraphrase, before CO4 saw the
   command. Wrong: it was an unquoted-alternation regex. **Corrected by BR pasting the artifact.**
2. **"the SendMessage fix dogfooded live, the minion self-recovered after my guidance"** — a causal claim the
   timing refutes: the minion recovered *before* the message arrived. **Corrected by the minion's own report.**

**Same class both times: a diagnostic/causal claim from inference, refuted by substrate — and each time the
substrate (a human paste, a sub-agent report) was the corrector.** That is not the design failing; it is the
design *working* — the account is unreliable and the checkable record catches it. Logged in `case-studies/
action-research-meta-minion/log/2026-07-17-cycle-3.md`; annotated there, not erased
([[keep-the-ball-game-retract-by-annotating]]). The instrument's author is in scope (§0.2) and is here the
specimen.
