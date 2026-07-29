# Issue 019: `docs/allowlist.md` should tell users to check WHICH `tt` wins — both failure directions now have field evidence

> status: open · labels: docs, allowlist, native, install-layout · summary: the doc records the
> stale-plugin-cache-wins case from 2026-07-28; a second field test found the inverse (a native
> `~/.genscalator/bin/tt` winning over the shadowed plugin-cache launcher) — and which case you are in
> changes both the allowlist rule you need and whether `tt skillcheck` works at all.

## Description

`docs/allowlist.md` currently warns, from the 2026-07-28 field test, that a stale plugin-cache `tt` can
run silently while an up-to-date checkout's `bin/tt` prompts — "so the quiet path was the stale one".

A second alpha field test on 2026-07-29 found the **inverse arrangement**, and it is benign:

```
$ tt which tt
tt: 2 in PATH (first wins)
  /home/<user>/.genscalator/bin/tt
    ELF binary (64-bit)  42.3M  rwxrw-r--  2026-07-29T12:21
  /home/<user>/.claude/plugins/cache/<marketplace>/genscalator/<version>/bin/tt (shadowed)
    script  #!/usr/bin/env bash  702B  rwxrwxr-x  2026-07-28T14:30
```

The newer native build wins, and because it is invoked as the bare word `tt`, the recommended
`Bash(tt *)` rule matches it — so here the quiet path is also the current path.

**Why it wedges.** Both directions are now attested, which means "which `tt` wins" is not a footnote but
the first thing a user should establish, for two independent reasons:

1. **Permissions.** If the winner is invoked by absolute path, `Bash(tt *)` does not match and every call
   prompts (the existing note). If it is invoked as bare `tt`, it does match — regardless of which install
   it is, including a stale one.
2. **Function.** Which install wins determines whether `tt skillcheck` works at all: the native install
   tree ships no sibling `skills/`, so bare `tt skillcheck` exits 2 there (issue 015). A user who has not
   checked cannot explain that failure.

The doc already has the perfect first-line check available — `tt which tt` flags shadowed entries, the
symlink chain, ELF-vs-script and mtime in one read-only call — it just is not prescribed.

## Acceptance sketch

* A short "which `tt` wins" section in `docs/allowlist.md`: run `tt which tt`, confirm the winner is the
  install you intend, and confirm it is invoked as the bare command word so `Bash(tt *)` applies.
* Both field-evidenced arrangements named, so the reader recognises whichever one they are in: stale cache
  winning (quiet but old) and native install winning (quiet and current, but no sibling `skills/`).
* A pointer to issue 015 for the `skillcheck` consequence.
* Optionally, `gs warm` and/or `gs init` run `tt which tt` and flag a surprising winner (e.g. a shadowed
  entry that is newer than the winner) — turning the manual check into a reflex, in the spirit of
  structure over willpower.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-07-29 23:27

Filed from the same alpha field test as issues 014–018. `tt which` was the standout tool of the sweep —
it surfaced this whole situation in a single call, including the shadowing and the build dates — so this
issue is less about a defect than about promoting an existing capability into the documented ritual.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.
