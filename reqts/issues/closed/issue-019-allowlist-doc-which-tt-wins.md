# Issue 019: `docs/allowlist.md` should tell users to check WHICH `tt` wins — both failure directions now have field evidence

> status: closed 2026-08-16, fixed by `7add972` · labels: docs, allowlist, native, install-layout · summary: the doc records the
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

### Comment by hmiddelk/Opus5 at 2026-08-04 16:12

**The doc gap is CONFIRMED on Windows 10 — but the proposed fix does not work there, so please do not
land this one alone.** Released `v0.10.0` native `windows-x86_64` build, Windows 10 Enterprise
10.0.19045.

The gap first: the shipped `tt doc allowlist` (71 lines) still records only the 2026-07-28
stale-plugin-cache case. It has the `Bash(tt *)` bare-command-word note, but no prescribed `tt which tt`
check, no native-install arrangement, and no pointer to issue 015's `skillcheck` consequence — exactly as
described above.

The problem is the remedy. This issue's whole proposal is to promote `tt which tt` into the documented
first-line ritual, and **on Windows `tt which` returns a confident false negative**:

```
> tt which tt
tt: not found in PATH (36 dirs; not a bash builtin either)     (exit 2)
```

...on a box where `tt` is on PATH and is the very binary answering. Root cause filed as **issue 022**:
`which.scala:98` splits `$PATH` on `':'` rather than `File.pathSeparator`, so on Windows the entry list is
shredded by the drive-letter colons (35 real entries reported as "36 dirs"), and separately there is no
PATHEXT resolution, so the bare command word cannot match `tt.exe`. The two faults compound: the *only*
thing that can resolve is an explicit `tt.exe` sitting in the last PATH entry.

This also removes the second reason given above for promoting the check. The Description argues the
ritual settles the **function** question — whether bare `tt skillcheck` will work, per issue 015 — but on
Windows the tool that would settle it is the one that is broken, so both links in the diagnosis chain
fail together.

Suggested adjustments to the acceptance sketch, offered rather than assumed:

* Add a line making the check's platform dependency explicit, and land this doc change **with or after**
  issue 022 rather than before it. A doc that sends Windows users to `tt which tt` today teaches them the
  tool lies.
* The optional "`gs warm` / `gs init` run `tt which tt` and flag a surprising winner" item inherits the
  same constraint — wiring it now would bake a false negative into the cold-start reflex, which is worse
  than the manual check because nobody is watching it.
* Consider naming a **third** field-evidenced arrangement alongside the two above: native install winning
  on a platform where `tt which` cannot see it. That is the case a Windows reader is actually in.

The "standout tool of the sweep" verdict from the Linux test still stands on Linux — this is a
portability defect in one function, not a re-assessment of `tt which`.

Agent disclosure: the Windows re-test, the root-cause reduction behind issue 022, and this comment were
produced by an AI agent (Claude Opus 5) under human direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-07 15:56

Maintainer-side review (PR 2 triage), confirmed: `docs/allowlist.md`'s only multiple-installs
content is the "Which `tt` does the rule match?" paragraph (lines 35-44) — no `tt which tt` ritual
anywhere; and the ordering constraint is sound, verified in source (`which.scala:98` plus the
missing PATHEXT resolution).

Answer to the ordering question: 019 MAY ship in the SAME release as 022's fix — the doc travels
with the fixed binary, so no false-negative window exists for readers of that release — but never
BEFORE it. The new section should carry one line for repo-copy readers still on v0.10.0: "on
Windows this check requires v0.10.1+ (issue 022)". The `gs warm`/`gs init` auto-check wiring stays
a separate, later item regardless — a broken check baked into an unwatched cold-start reflex is
the worst placement for it. One addition for exhaustiveness: the triage box exhibits a THIRD
benign arrangement (a `~/.local/bin` symlink to the checkout launcher winning over the plugin
cache) — worth a word in the final doc.

Triage: DOCS — v0.10.2 by class, promotable into v0.10.1 only in the same cut as 022's verified fix.

Agent disclosure: this review comment was produced by an AI agent (Claude Fable 5) under human
direction; the human reviewed and submitted.
