# Delegated web-surfing stalled an AFK run — the front-load-and-vet-egress lesson (2026-07-10)

BR-flagged, live. During a post-compact AFK solo run, the super-agent spawned a `claude-code-guide` sub-agent to
answer factual questions (statusline schema, ccusage). The guide has `WebFetch`/`WebSearch` and surfed to answer —
hitting domains **not on the WebFetch allowlist** (`code.claude.com`, `www.howdoiuseai.com`; note `claude.com` and
`support.claude.com` ARE allowlisted but exact-domain match means the `code.` subdomain misses). Those fetches
**blocked on a manual ack**, and BR — back for a quick check — had to **release a surf-ack**. BR: *"WR data on that
surf; it was not really afk safe; perhaps you could have checked your allowed settings first… a lesson is to do surf
jobs to prepare for afk solo BEFORE br actually goes afk."*

## The blind spot (echt)
The AFK-web rule is *known* (in the resume-prompt: "web-fetch to never-visited domains blocks on harness OK → not
autonomous"). I applied it to **my own** direct action — I explicitly declined a direct WebFetch earlier that turn as
AFK-unsafe — but I did **NOT extend it to a delegated sub-agent**. The rule needs to be **transitive**: a web-capable
sub-agent inherits the stall risk; delegating web work in AFK is exactly as unsafe as doing it directly, and I lose
the ability to see/prevent the stall because it happens inside the sub-agent.

## The two fixes (BR's lessons)
1. **Front-load surf jobs BEFORE AFK.** Do web-dependent lookups while BR is present, so his ack is a quick approval
   rather than an AFK stall. Sequence: *prep-while-present (surf, gather) → execute-guard-free-while-AFK.* This also
   fits [[keep-afk-menu-stocked]]: surf-dependent items are a "do-with-BR-present" class, not an AFK-solo class.
2. **Vet egress against the allowlist first.** Before any surf (direct or delegated) in AFK, read the
   `WebFetch(domain:...)` entries in `.claude/settings.local.json` and scope to allowlisted domains only; else defer.
   When briefing a web-capable sub-agent in AFK, forbid web tools or name the allowed domains.

## Why it matters
AFK is supposed to be **guard-free-by-construction** ([[guard-against-forced-confirmations]]); a surf-ack is precisely
the forced confirmation that races the human and defeats autonomous AFK. This is a fourth flavor of the AFK-safety
family alongside compounding-defeats-the-allowlist, the tail-stall (S3), and the settings-edit gate: **egress is a
guard surface, and delegation does not launder it.** Pinned to memory ([[web-surf-not-afk-safe-front-load-and-vet-egress]])
so it fires next AFK run, and added to the resume-prompt AFK pre-flight. Ties: [[delegation-dance]],
[[guard-against-forced-confirmations]], [[cue-go-afk]], [[not-afk-safe-solo-yields-wr-data]],
[[subagent-confabulated-off-task-settings-claim-2026-07-10]] (the same guide call also confabulated — two AFK
delegation hazards in one spawn).
