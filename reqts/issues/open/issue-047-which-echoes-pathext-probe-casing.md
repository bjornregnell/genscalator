# Issue 047: `tt which` on Windows echoes the PATHEXT probe's casing, not the file's — `tt.EXE` for a file named `tt.exe`

> status: open · labels: toolbox, which, windows, cosmetic · summary: the PATHEXT resolution probes
> `name + ext` with the extension verbatim from `PATHEXT` (upper-case `.EXE` by default) and prints the
> probe path it resolved, so on case-insensitive NTFS the reported filename carries the probe's casing
> while the file on disk is `tt.exe`. The code's own comment promises the opposite: *"the file actually
> found (e.g. tt.exe) is what gets reported"* (`which.scala:119-120`). Harmless on NTFS, wrong the
> moment the echoed path reaches a case-sensitive context or a string comparison. Rider: `1 dirs`
> grammar in the single-entry count (`which.scala:153`).

## Description

Found in issue 022's real-Windows verification of released v0.10.2 (2026-08-21, report 088): the
hardware transcript shows `C:\Users\<user>\.genscalator\bin\tt.EXE` and `...\git.EXE` where the files
on disk are `tt.exe` and `git.exe`. Issue 022's acceptance sketch asked that the tool *"report the
file actually found (`tt.exe`)"*, and the closing decision carried this residue into its own issue
rather than blocking the close — this is that issue.

The mechanism, cited at current `main`:

* `which.scala:121-124` — `hitsFor` builds candidate paths as `(name +: exts.map(name + _))
  .map(d.resolve)` and takes the first that exists. The extensions come verbatim from `PATHEXT`
  (default `.COM;.EXE;.BAT;.CMD`, conventionally upper-case), so the candidate `Path` is spelled
  `tt.EXE`.
* NTFS resolves `Files.isRegularFile(tt.EXE)` case-insensitively, so the probe hits a file whose
  directory entry says `tt.exe`.
* `which.scala:133-134` — `reportHit` prints the probe path `p` as constructed, never consulting the
  on-disk spelling.

So the printed name is an artifact of the probe order, not a property of the file. The comment at
`:119-120` states the intended behaviour exactly, which makes this a one-comment spec with a
two-line gap rather than a design question.

Why it is worth fixing despite being cosmetic on NTFS: `tt which` is the prescribed first-line ritual
of `docs/allowlist.md`, its output is exactly the kind of thing that gets pasted into an allowlist
rule, a script, or a WSL/MSYS shell where casing is load-bearing — and a tool whose purpose is to
tell the user precisely which file wins should not misspell that file's name.

**Rider, same verb, one character:** `which.scala:153` prints `(${dirs.size} dirs...)` — the
single-entry case reads `1 dirs` (attested in the 022 hardware transcript). Singular for 1.

## How to reproduce it

On Windows (any install where `bin\tt.exe` is on PATH):

```
> tt which tt
tt: 1 in PATH
  C:\Users\<user>\.genscalator\bin\tt.EXE      <- file on disk is tt.exe
```

On POSIX the extension list is empty (`pathExts` returns `Vector.empty` off Windows), so the defect
is unreachable there — the fix must keep it that way.

## Acceptance sketch

* The reported filename matches the directory entry's casing — e.g. resolve the found candidate to
  its on-disk spelling before printing (a real-path or directory-listing lookup of the final name
  component), gated to the Windows/PATHEXT branch so POSIX behaviour is byte-identical.
* Keep the resolution itself probing in PATHEXT order — this issue is about what is *printed*, not
  what is *found*.
* `1 dir` for one entry.
* Tests: the casing normalisation as a pure, unit-testable helper in `WhichUnitSuite` (the live NTFS
  echo itself needs a Windows box and belongs to the next hardware sweep's checklist, report 085
  style).

## Discussion

### Comment by bjornregnell/Fable5 at 2026-08-24 20:27

Filed as the follow-up promised in issue 022's close decision. Provenance: the 2026-08-21 hardware
verification (report 088; transcripts in issue 022's discussion); mechanism confirmed by reading
`which.scala` at current `main` rather than inherited from the report.

Agent disclosure: an AI agent (Claude Fable 5) located the mechanism and drafted this issue under
human direction; the human reviewed and submitted.
