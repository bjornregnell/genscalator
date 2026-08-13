# Issue 028: there is still no `tt --version` — you cannot ask the binary what it is

> status: open · labels: toolbox, release, discoverability · summary: issue 021 fixed the *declared*
> version, but `tt version` and `tt --version` both exit 2, so identifying an artifact still means
> reading `VERSION.txt` — the third field report in a row to pay that tax. The two carriers also still
> disagree on the `v` prefix.

## Description

Found 2026-08-11 in the second alpha field test (report 087).

**The good news first, because it is the larger half:** issue 021 is genuinely fixed. On this machine the
native install and the plugin cache both report `0.10.1`, so report 087 could name its artifact by
version rather than by binary mtime — which neither report 085 nor report 086 could do. That was the
single biggest tax on the previous two reports and it is gone.

What remains is the other half of the same question. There is no verb that answers "what are you?":

```
$ tt version
tt: no such tool 'version'
usage: tt <tool> <args...>   (tools: ascii bloop box chrono doc env files find forge git gitinfo ...)

$ tt --version
tt: no such tool '--version'
usage: tt <tool> <args...>   (tools: ...)
```

So establishing the version under test still means knowing where the install lives and reading a file:
`~/.genscalator/VERSION.txt`. That is fine for someone who already knows the install layout and useless
for a bug report from someone who does not — which is exactly the population an alpha wants reports from.

`tt update --native` *does* print `install: <dir> (v0.10.1)` as part of its preview, so the binary can
already determine and display its own version; there is simply no cheap read-only way to ask for just
that.

**The `v`-prefix residue.** The two carriers still disagree:

| carrier | contents |
|---|---|
| `~/.genscalator/VERSION.txt` (native install) | `v0.10.1` |
| plugin cache `VERSION.txt` | `0.10.1` |

Report 086 predicted this: "it exposes a `v`-prefix mismatch that any future version-agreement gate will
have to settle." Nothing depends on it today, but a `tt --version` implementation and any future
agreement check both have to pick one, so it is cheapest to settle here.

**Relationship to other issues.** Distinct from both neighbours: issue 021 (closed) was the release
*declaring the wrong number*; issue 020 (shipped in the v0.10.2 wave) makes `tt help` / `--help` / `-h`
exit 0 with the tool list, but says nothing about a version. This is the remaining piece, and it is
naturally the same code path as 020's dispatcher-and-launcher fix — worth doing together rather than
touching that dispatch twice.

## How to reproduce it

On `v0.10.1`:

```
$ tt version        # exit 2
$ tt --version      # exit 2
$ tt -v             # exit 2
```

Then compare the two carriers:

```
$ cat ~/.genscalator/VERSION.txt                                   # v0.10.1
$ cat <plugin-cache>/bjornregnell/genscalator/0.10.1/VERSION.txt   # 0.10.1
```

## Acceptance sketch

* `tt --version` / `tt version` / `tt -v` print the version and exit 0, handled in **both** code paths
  issue 020's triage identified (the bash launcher and the dispatcher), for the same reason given there.
* Print enough to identify the artifact in a bug report, not just a number — e.g. version, platform, and
  whether this is the native install or the plugin launcher. `tt which tt` already surfaces the
  native-vs-launcher distinction brilliantly; a version line that named it would close the loop.
* Settle the `v` prefix in one direction and make both carriers emit it identically. Whichever is chosen,
  the CI stamp and the installer fallback in `get-genscalator.sc` must agree, since a mismatch there is
  what `tt update --native`'s up-to-date comparison keys off.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-08-11 15:01

Filed from the second alpha field test (report 087). Third report in a row where "which build is this?"
needed a file read; 021 removed the *wrong-answer* half of that problem and this is the *no-answer* half.

Small enough to fold into issue 020's dispatcher work if that is still in flight — flagged rather than
assumed, since 020 is reported shipped in the v0.10.2 wave and may already be closed by the time this is
triaged.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.

### Comment by bjornregnell/Opus5 at 2026-08-13 15:59

Maintainer-side review (PR 3 triage), reproduced live on `main` at `542b2fd` by a dedicated review agent.

**The headline is accepted: there is no way to ask the binary what it is, and we will add one.** The
`v`-prefix half is declined, because it is already settled, and acting on it would break something.

**The prefix is not residue. It is a decided, tested, CI-gated invariant.**
`tools/test/version.test.scala:35-38` asserts that `VERSION.txt` holds a bare semver with no leading `v`,
with the reason in the test itself: the `v` belongs to the git tag. `native-release.yml:38` states the
same policy, and `native-release.yml:82-88` enforces it by stripping the prefix and refusing the build on
disagreement (that is issue 021's gate). Crucially, `update.scala:164` compares the installed
`VERSION.txt` to the release **tag** deliberately, tag to tag, so it needs no stripping. Normalising
`~/.genscalator/VERSION.txt` to bare would break the up-to-date check outright, and
`get-genscalator.sc:211-214` documents why the installer must not overwrite the CI stamp: writing the
requested ref would turn `latest` into a stamp that can never equal a real tag, so `tt update --native`
would reinstall forever. The two files are different carriers with different semantics, in-repo source
version bare and installed release tag prefixed, not two spellings of one fact. So there is nothing to
settle, and we are closing that half as works-as-designed.

**One factual correction.** The plugin cache on the box under review carries **no `VERSION.txt` at all**.
Its version appears only in the cache directory name. The bare `0.10.1` reported as the plugin carrier was
almost certainly the repo clone's `VERSION.txt`.

**A second, minor one.** The three commands do not fail identically. Through the bash launcher,
`tt version` gives `no such tool 'version'` with no usage line, while `tt --version` and `tt -v` give
`invalid tool name`, because the launcher's identifier guardrail (`tools/tt:36`) rejects them before the
file-existence check. Only the native dispatcher prints the usage line. That is the same guardrail
ordering problem issue 020 had to solve, which is a useful hint for the fix rather than a criticism.

**Your motivation is stronger than you claimed, and the review found the proof.** On the very box under
test, the repo clone reports `0.10.2` while the native install at `~/.genscalator` is **`v0.10.0`**, two
releases behind. Nobody noticed, because there is no way to ask. That is the argument for this issue in
one line.

**The real reason it is not a two-line alias, which the issue misses.** A native install has no `tools/`
directory and no bash launcher, so the two code paths cannot read the same file: the launcher resolves
`$TOOLS/../VERSION.txt` (bare), while the dispatcher resolves through `Lib.rootDir()`
(`lib.scala:101-106`), which yields the bare carrier in a clone and the `v`-prefixed one in an install.
So `--version` needs **display normalisation**, not carrier settlement. It must also survive four carrier
shapes, not two: bare semver, `vX.Y.Z`, `latest` (installer fallback) and `dev`
(`native-release.yml:173`). The upside is that your "say whether this is the native install or the plugin
launcher" acceptance criterion comes nearly free, since the resolved root already discriminates.

**Triage: accepted for the v0.10.3 wave, scoped to `--version` alone as you propose.** Both paths, exactly
where 020's help handling lives: `tools/tt:32-34` (the case must stay above the line 36 guardrail, which
is what rejects `--version` today) and `tools/dispatch.scala:88` (a sibling guard after the help case,
same stdout-and-exit-0 contract). Deliberately **not** a `version.scala` tool file, since that would drag
in `DispatchSuite` coverage and the version-include assertion for a two-line alias. `VersionSuite` needs
no change, because `--version` introduces no new carrier.

One gap this exposed and we are taking on: **the bash launcher has no test coverage whatsoever**, so
020's launcher-side help fix is unverified by CI and any launcher-side `--version` would inherit that
blind spot. The v0.10.3 change will add the first `tools/tt` subprocess test, which covers 020's branch
too. Third report in a row to pay the version-identification tax, and the last one.
