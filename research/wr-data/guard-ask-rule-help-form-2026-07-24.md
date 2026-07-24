# Guard stall on an effectful verb's --help form (2026-07-24 18:01)

Event: at cold-start verification the agent ran `tt forge release-create --help` to
verify the SM207 ship-claim (the ember's verify-mandate). The configured ask-rule
`Bash(tt forge release-create *)` (project settings.local.json, deliberate gate on the
forge WRITE verb) prefix-matched the read-only `--help` form and raised a confirmation.
The human was AFK-adjacent, answered from the approval TUI ("in guard:"), and initially
suspected a missing allow-rule because the dialog offered only Yes/No.

Mechanism (three observations, one per party):

1. Harness: when an EXPLICIT ask-rule matches, the dialog offers only Yes/No — the
   "always allow" shortcut appears only for commands no rule covers. Ask-rules also
   outrank allow-rules, so adding an allow for the `--help` form would not bypass the
   gate. Nothing is missing; the rule is doing its job, just prefix-broadly.
2. Agent (the reflex error): help text for a GUARDED effectful verb must come from
   files (`tools/README.md`, `tt doc`) — invoking the guarded binary for its help
   trips the gate by construction. New reflex: verify ship-claims of effectful verbs
   by reading the repo (usage text in source / README / docs), never by running them.
3. Human: the dialog names the matching rule, but under skim-reading it read as
   "some rule seems missing". The guard's own text was correct; the friction was
   readability under divided attention. Human verdict after inspection: expected,
   not surprising, approved.

Lesson: the gate behaved correctly; the cost was one stall + one confusion. The cheap
fix is agent-side (file-based help reflex). A candidate structural fix rides on SM219
(pre-baked render-ready docs): serve per-tool help via `tt doc <tool>` so no help
lookup ever touches a guarded verb.

Related: [[use-tt-gitinfo-not-raw-git]] (same class: reflex slips at cold start),
skills/avoid-guard-stall (consult BEFORE bash calls — this event is now an example).
