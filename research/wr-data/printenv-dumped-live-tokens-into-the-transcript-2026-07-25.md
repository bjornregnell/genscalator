# Hazard specimen: a bare `printenv` put live credentials into the transcript (2026-07-25 ~15:0x)

**Severity: this is the worst outcome the toolbox-gap family has produced.** The earlier specimens in
this family cost a missed abstraction or a stalled run. This one disclosed two live access tokens into a
durable transcript. No token values appear in this note, and none should ever be added to it.

## The event

While reasoning about SM208 (per-session modes), the agent wanted to know whether Claude Code exposes a
session identifier to a shell. It ran a bare **`printenv`**, which printed the ENTIRE environment into
the conversation. That environment contained two live secrets: a GitHub OAuth token and a Codeberg API
token, both used by `tt forge`. They are now in the session JSONL on disk and in the model context.
BR was told immediately and both are being rotated.

**The agent needed exactly ONE variable and took all of them.** That gap between what was needed and
what was taken is the whole specimen.

## Why nothing stopped it, including the fix built the same morning

Three layers had a chance and all missed:

1. **guardcheck HIGH/MED**: `printenv` is not a `cd`, not a compound, not a pipe, not a redirect. It
   trips no syntax check. Same structural blind spot as SM228.
2. **The guardcheck NOTE tier — built THIS MORNING for exactly this blind spot (SM230) — also misses
   it.** Its interpreter checks name `python|perl|ruby|node|jq`. `printenv` and `env` are not in that
   list. So the freshly-built defence was too narrowly scoped: it caught the shape that had already
   bitten and not the class.
3. **RT056 predicted it and the agent did not apply its own framework.** The coverage sweep, written
   hours earlier, says the failure mode is "a noun with no verb". `env` is a noun with no verb. The
   prediction HELD. Having the model did not produce the behaviour.

Point 3 is the uncomfortable one and the reason this is worth filing: a correct predictive model, freshly
written by the same agent, did not fire at the moment of action. That is the same failure the SM228 note
identified for prose rules ("rules that require correct self-classification fail when classification is
what is broken") — and it now applies to the agent's OWN research output, not just to inherited rules.

## The property that makes this different from a private terminal

`printenv` on a human's terminal is unremarkable: the output scrolls past and is gone. The same command
from an agent writes into a transcript that is **durable, copied, quoted and mined** — this project's own
WR workflow exists to copy transcript material into committed files. So:

> **Read-only is not the same as safe.** For an agent, the hazard of a read is set by where the OUTPUT
> lands, not by whether the command mutates anything.

That reframing generalises past this case: any bulk read of a credential-bearing surface (environment,
`~/.netrc`, `~/.git-credentials`, CI config, `.env` files, `settings.json` with tokens) is a disclosure
operation even though every one of them is technically read-only.

## Counter-evidence for the DumbZone specimen filed one hour earlier

`dumbzone-chip-changed-behaviour-n1-2026-07-25.md` records the chip apparently making the agent more
careful. **The DumbZone chip was ACTIVE when this happened.** So that note now has a paired negative:
the chip preceded a careful act (writing missing tests) and also preceded the worst slip of the day.

n=1 either way. The pairing is the useful part: it means the chip does not produce blanket caution, and
any real effect has to be measured on a specific behaviour rather than on a general impression of
carefulness. Both notes should be read together or neither should be cited.

## Where the exposure actually was (BR, 2026-07-25, and it changed the remediation)

The reflex after an incident like this is to remove the token from the environment. BR's framing, pinned
here because it redirects the fix:

> The real exposure wasn't "token in env" — it was "agent dumped the whole env into a transcript", and
> that's now closed structurally by `tt env` plus widening the guardcheck NOTE tier to bulk env reads. So
> keeping the ambient export is defensible.

This matters because the two remediations are not equivalent:

- **Remove the ambient token** treats the credential's PRESENCE as the fault. But the token was in the
  environment for a reason (`tt forge --gh` reads it there, deliberately, so that the human's shell — not
  the agent — decides whether the agent has credentials at all). Removing it costs a working tool and a
  trust boundary, and it does not stop the next bulk read of the next credential-bearing surface.
- **Remove the dangerous REQUEST** treats the bulk read as the fault. `tt env` has no whole-environment
  verb, so the shape that caused this cannot be asked for. That generalises to `.env` files, credentials
  files and CI config, and it survives the next careless moment, which is the property vigilance lacks.

The second is the structural fix and it is the one that was built. Note the asymmetry it rests on: a
wrongly-withheld value costs one explicit `--reveal`; a wrongly-revealed one costs a rotation.

**Follow-on, same day, and recorded because it cuts the other way:** BR then authorised `tt forge --gh`
to fall back to `gh auth token` when no env token is present — which DOES remove the ambient credential,
at the cost of letting the tool mint one instead of being handed one. The agent flagged the
trust-boundary move before implementing it, and it is documented at `ghCliToken` in `forge.scala` with
the mitigations (env still wins; token still pairs only with the fixed API host; never silent — it prints
to stderr when the fallback fires). Whether that should be gated behind an explicit opt-in is an open
SM073 question, so this specimen ends with a decision deliberately left open rather than closed.

## The candidate (SM231)

`tt env`, shaped so the safe answer is the default:

    tt env list [regex]     names only, never values
    tt env has <NAME>       exit 0/1, prints nothing
    tt env get <NAME>       ONE variable, redacted when the name or value looks secret

Redaction should reuse the existing `tt harden` secret detector rather than growing a second one. The
design principle: "what is in the environment" is almost always a question about NAMES, so names is what
the tool should return by default — which is precisely what was needed here.

Also worth doing, and cheaper: widen the SM230 NOTE tier from a list of interpreter names to include bulk
environment reads (`env`, `printenv`, `set` with no arguments), since that layer already exists.

Ties SM228, SM230, RT056, SM208 (the question being answered when this happened),
[[missing-tool-verb-causes-invisible-interpreter-reach]], [[never-allowlist-interpreters]], `tt harden`.
