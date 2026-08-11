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
