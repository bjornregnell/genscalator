# WR data: `2>/dev/null` guard trip on a scratch link-checker; no `tt` tool validates markdown anchors (2026-07-13)

Tags: #tool-candidate #reflex #model:fable-5 (delegated general-purpose subagent, `claude-fable-5`)

**What happened.** During the delegated README restructure (rename/move the Usage section, renumber, fix
anchors), the subagent verified the result with a scratch scala-cli program (per the scala-scratch-over-bash
discipline) that recomputes GitHub heading slugs and checks every internal `](#...)` link. The command was
`scala-cli run <scratchpad>/checklinks.scala 2>/dev/null`. The `PreToolUse:Bash` guard flagged it:
*"[MED] stderr suppression (2>/dev/null): let the tool self-report to a file and Read it; tolerate benign
stderr"* — costing BR a confirmation.

| field | value |
|---|---|
| when | 2026-07-13 (evidence = the flagged Bash call; pinned same day on BR's `WR data` cue) |
| context | genscalator README.md section move/renumber (delegated subagent from the muntabot-synch session) |
| action | verify all internal markdown anchor links match the post-edit heading slugs |
| command | `scala-cli run .../scratchpad/checklinks.scala 2>/dev/null` |
| why-prompted | `2>/dev/null` stderr suppression (guard: tolerate benign stderr instead) |
| candidate-tool | `tt md links <file>` — see below |
| status | idea |

**Analysis — two separable causes.**
1. **The bash reflex (agent-side, avoidable now).** The `2>/dev/null` was pure reflex to hide scala-cli's
   benign compile-progress chatter on stderr. A bare `scala-cli run <abs-script>` is allowlist-matchable and
   guard-clean; the chatter is a few lines and should simply be tolerated (exactly what the guard message
   says). Same lesson-family as the `| tail` / `> file` trips: a *suffix* on an otherwise-trusted command is
   what costs the confirmation.
2. **The tool gap (toolbox-side).** BR asked "should use tt, perhaps `tt text`?" — checked: `tt text
   match/grepr` can *extract* headings (`'^#'`) and internal links (`'\]\(#'`), but no `tt` tool does the
   actual validation (compute GitHub slugs from headings, check each `#anchor` for membership). That logic is
   why a scratch program was the right lane this time. But link-checking a README after restructuring is a
   recurring, pure, project-agnostic job.

**Candidate tool: `tt md links <file>`** (or fold into a `tt md` markdown family with `md-fmt`): list all
internal `](#...)` links, compute the GitHub-slug set from the headings, report OK/STALE per link, non-zero
exit on any stale anchor. Optional later: `--fix-renumber` awareness, relative-file link existence checks.
Safe by design (pure read + report), so the whole verify step becomes one bare allowlisted `tt` call — no
scratch compile, no stderr chatter to be tempted to suppress.
