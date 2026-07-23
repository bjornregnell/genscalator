# Commit-message file written to the session scratchpad, not the in-repo tmp/ (2026-07-23)

**Specimen (BR-flagged, WR data, 15:29):** committing the forge extension (`ee0d7c2`), the
agent wrote the `--message-file` to the session scratchpad
(`/tmp/claude-1000/.../scratchpad/forge-commit-msg.txt`) instead of the repo's gitignored
`tmp/` — despite the project memory `prefer-inrepo-tmp-over-slash-tmp` (2026-06-26, hardened
twice) and despite an in-repo precedent sitting right there (`tmp/commit-msg-wr-sandbox-clone.md`).
BR caught it in the feed: a scratchpad path does not survive reboot and lives off-tree.

**Two observations:**

1. **The regression had no digest home.** The guard-clean digest's GIT block said "write the
   message file FIRST" but was silent on WHERE. The memory carries the where-rule, but memory
   is recall-weighted while the digest is re-read verbatim at every cold start and pasted into
   every sub-agent brief — a rule that lives only in memory loses to a rule in the digest
   (same mechanism as `code-beats-prose-a-rule-fires-only-when-it-governs-the-object-of-attention`).
   The system prompt actively pushes the other way ("always use the scratchpad for temporary
   files"), so the un-anchored preference lost to the prompt's gradient. **Fix applied in this
   commit: the digest GIT block now names the in-repo gitignored tmp/ and forbids scratchpad//tmp
   for message files.**

2. **The 2026-07-01 sandbox-invisibility claim did not reproduce.** That hardening layer says
   scratchpad files are invisible to Bash-run tools; today `tt git commit --message-file
   <scratchpad-path>` succeeded (commit `ee0d7c2` exists). So the current failure mode of a
   scratchpad message file is volatility/off-tree-ness, not unreadability. One observation,
   not a rule — the sandbox config may vary by command shape.

**Cross-refs:** [[prefer-inrepo-tmp-over-slash-tmp]] (project memory, updated with this
specimen), `SYNTHESIS-structure-over-willpower-2026-07-07.md`, `gh-vs-tt-forge-capability-gap-2026-07-21.md`
(same session closed that gap: forge issues/prs/protection verbs shipped in `ee0d7c2`).
