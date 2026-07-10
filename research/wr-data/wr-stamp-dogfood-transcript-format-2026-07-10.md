# Dogfooding `tt wr stamp` immediately exposed the transcript format (and the tool's next iteration) (2026-07-10)

Building the retrofit tool (SM044b) and using it on the first real target (SM044a, dating old WR utterances)
surfaced two transcript-format realities in one run - the dogfooding loop working as intended.

## What the first uses showed
- **`type:"user"` is NOT "human-typed prose".** A `--user` scan returned lines dense with `output_tokens` /
  `server_tool_use` metadata: user *turns* include **tool-result-carrying** turns, not just what a human typed.
  So `--user` alone does not isolate a human utterance.
- **Many entry types have NO top-level `timestamp`.** Matches came back as `[last-prompt]` and
  `[file-history-snapshot]` with `(no-ts)`. The precise human utterance (a `type:"user"` entry whose
  `message.content` is text) DOES carry a timestamp, but it is buried among un-timestamped **mirror** entries of
  the same text (`last-prompt` repeats the last prompt; snapshots repeat file contents).
- **Naive regexes are swamped.** `"etch"` matched `fetch` 700+ times; even `\betch\b` returned 1300+ hits,
  dominated by an embodied typing-test line ("etch etch etch ...") and file-history snapshots of the
  `cue-note-vs-etch.md` file.

## The tool's next iteration (spec'd by this dogfood)
A `--human` mode: keep only `type == "user"` entries whose `message.content` is human TEXT (not a `tool_result`),
and read THAT entry's `timestamp`; treat `last-prompt` / `file-history-snapshot` as un-datable mirrors. Then a
precise phrase query dates a human utterance to the second cleanly. (First cut `tt wr stamp` shipped as-is,
`1a9198f`; this is the refinement its own first use called for.)

## Silver lining: a second, durable retrofit channel
`file-history-snapshot` entries carry a **`backupTime`** (e.g. `memory/cue-note-vs-etch.md` shows
`backupTime:2026-07-04T2...`), a durable proxy for when a file existed - a second dating channel alongside the
`type:"user"` timestamp and the git-commit backstop.

## Disposition
SM044(a) bulk retrofits (S3/S5/S6/S7) DEFERRED pending the `--human` refinement (or careful per-target phrase
queries); they are low-urgency note-dating and now cheap once the filter lands. Ties: SM044, the
evidence-timestamp enhancement, [[prefer-scala-scratch-over-bash]] (the tool over grep+Read), SM013a
(the jsonl OMITS harness-injected content, another format caveat).
