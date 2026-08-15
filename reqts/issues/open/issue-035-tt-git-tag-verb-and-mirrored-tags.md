# Issue 035: `tt git` has no tag verb and cannot push a single tag, so a mirrored release is a two-forge job

> status: open · labels: toolbox, git, forge, release, mirrors · summary: `tt git` can push all tags
> but cannot create one and cannot push exactly one, and `tt forge whoami` cannot check the GitHub
> auth the other verbs use. Found cutting prontopop v0.2.0, second time tagging has hit this.

## Description

Found 2026-08-15 while cutting **prontopop v0.2.0**. Second time tagging has hit this; the first was
v0.1.0 in July, when `tt forge` had no release verb either and the answer was "use `gh` for now". The
release half is solved. The tag half is not.

⚠ **Scope correction applied before filing, see the Discussion.** The original note claimed there was "no
way to push" a tag. That is **not true on current main**: `tt git push --repo <dir> [--remote <name>]...
[--tags]` exists, is documented at `git.scala:70-76`, and is the shape this project's own release dance
already mandates per mirror. The note was written against a clone 79 commits behind, whose `git.scala` had
no tag support at all. What follows is the residue that survives that correction.

### What works, and is worth keeping

```
tt forge release-create bjornregnell/prontopop v0.2.0 --gh \
  --name "ProntoPop v0.2.0" --body-file notes.md --target main
```

* created the release **and the tag**, because GitHub creates `tag_name` at `target_commitish` when the
  tag does not exist yet;
* found the token by itself, reporting `no token in env; obtained one from gh auth token`, with neither
  `GITHUB_TOKEN` nor `GH_TOKEN` set. That fallback is a good touch, and it is **not** in the usage text,
  which says tokens come "ONLY from fixed env names";
* printed an audit line before the POST, which is exactly what one wants before a public write;
* `tt git fetch` then brought the new tag down to the clone.

### The residue, in three parts

**1. Tag creation is absent, and that is a stated decision rather than an oversight.** `git.scala:215-217`
says so explicitly: *"Tag CREATION (`git tag -a`) is deliberately NOT in scope here — this sends tags that
already exist."* So this item is a request to revisit a decision, not to fill a hole, and it should be
argued on that footing. The argument for revisiting: creating a tag mutates nothing and destroys nothing,
which is the same reasoning issue 026 item 1 makes for a branch verb, and the two should be triaged
together as one question about how far the non-destructive half extends.

**2. There is no way to push exactly ONE tag.** `--tags` pushes all of them. On a repo that has
accumulated local scratch tags, `--tags` sends more than was meant, and the actual need when a mirror
falls behind is to send the one tag that release cares about. This is the narrowest and most defensible of
the three items.

**3. There is no read-only way to list local tags.** `tt forge tags` lists a *forge's* tags over the
network; nothing lists the clone's. That is the verb you want before and after a mirror push, to see what
is actually there.

The mirrored case is what makes these bite. prontopop pushes to two forges through dual push URLs on one
remote (`github.com/bjornregnell/prontopop` and `git.cs.lth.se/bjornregnell/prontopop`). `v0.2.0` reached
GitHub and the clone; the GitLab mirror did not have it. **This is the same trap genscalator itself has
now hit three times** (recorded at the v0.10.1 and v0.10.2 cuts): tags never ride a plain mirror push, and
a release that is "done" on one forge is silently absent on the other.

## How to reproduce it

```
$ tt git                                            # no tag verb in the safe subset
$ tt git tag --repo <abs-repo> v0.2.0               # usage error
$ tt git push --repo <abs-repo> --remote gitlab --tags   # works: ALL tags, not one
```

## Acceptance sketch

```
tt git tag  <repo> [--limit N]                      # read-only: list, newest first
tt git tag  --repo <dir> <name> [--ref <ref>] [--message <s> | --message-file <path>]
tt git push --repo <dir> [--remote <name>]... [--tags | --tag <name>]
```

Notes on the shapes, from the guard's own rules rather than taste:

* a tag message from a **file**, like `tt git commit --message-file`, keeps shell metacharacters out of
  prose and matches the verb next door;
* `--tag <name>` alongside `--tags` matters here: pushing *one* tag to a mirror is the actual need, and
  `--tags` on a repo with old local tags pushes more than was meant;
* annotated by default when a message is given, lightweight otherwise, is the least surprising rule;
* **deleting a tag should stay out.** It rewrites published history, which is the same family as
  reset/force/rm/clean that the safe subset already refuses. (Noted for triage: a `tt forge tag-delete`
  was proposed separately at the v0.10.2 re-cut, for the *forge* side, which is a different question.)

## Two smaller items from the same session

**`tt forge whoami` takes `[--url BASE]` but not `--gh`**, while `releases`, `tags`, `issues`, `prs` and
`release-create` all take it. So the one verb whose job is "check my auth" cannot check the GitHub auth
the other verbs use — running it before a GitHub release reports a 403 from Codeberg, which reads as a
problem and is not one. Confirmed still true on current main (`forge.scala:19,52,80`).

```
$ tt forge whoami --gh
forge: unknown/incomplete flag '--gh'
$ tt forge whoami
forge: no token in env; obtained one from keyring get codeberg genscalator-token
forge: GET https://codeberg.org/api/v1/user -> 403 Forbidden
```

`whoami` accepting `--gh` and `--gl` would make it the thing to run first, which is what its name promises.

**There is no GitLab equivalent of the `gh auth token` fallback.** `release-create --gl` refuses without
`GENSCALATOR_GITLAB_TOKEN` or `GITLAB_TOKEN`, and says so clearly, which is right — a token must never be
a flag. But the GitHub side does better than refuse: it falls back to `gh auth token`, so a human who has
logged in with `gh` never has to think about env vars. There is no matching fallback for GitLab.
`glab auth token` is the same shape and supports self-hosted instances such as `git.cs.lth.se`. Worth
mirroring the fallback for `--gl` when `glab` is on PATH, and saying so in the refusal when it is not:
*"install glab and `glab auth login`, or set GITLAB_TOKEN"* is a better dead end than the env vars alone.
(`glab` is not installed on the reporting machine, so this is inferred from the asymmetry, not from a
failed fallback.) Confirmed on current main: `forge.scala:14` names the `gh auth token` fallback and
`glab` appears nowhere.

Until then, a mirrored release is a two-forge job with only one forge automatable, which is the same shape
as the tag gap above: the tooling can do it for GitHub, and the human is on their own for the mirror.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-15 20:29

**Provenance.** Found and drafted by a different agent session while cutting prontopop v0.2.0 on
2026-08-15, and left as an untracked file named `issues/tt-git-tag-and-mirrored-tags.md` in an old
Codeberg-era clone of this repo. It followed neither the location nor the numbering convention
(`reqts/issues/open/issue-NNN-short-snake-case-name.md`), so it would have been lost with that clone.
Relocated and renumbered here on the maintainer's instruction, with the analysis preserved.

**One claim corrected, recorded rather than quietly amended.** The original stated there is "no way to
make a tag **and** no way to push one", and proposed `[--tags | --tag <name>]` as if neither existed.
`--tags` has existed since before v0.10.0: `git.scala:17-18,45-46,70-76,220-229`, with its own reasoning
for why it is `--tags` and not `--follow-tags` and why it runs as a second invocation per remote. The
project's own release dance depends on it.

**The cause is worth more than the correction.** That clone is **79 commits behind** upstream, and its
`tools/git.scala` contains the string `tags` exactly **zero** times. So the report was accurate about the
source it read and wrong about the shipping tool. **A stale local clone is a source of false gap reports**,
and the failure is invisible from inside: the file was there, it parsed, it simply predated the feature.
Anything that reads source to decide whether a verb exists should check what it is reading first — and the
cheapest check is the one issue 028 asks for, `tt --version`, which does not exist yet either.

What survives the correction is genuine and is what this issue now asks for: tag creation (a stated
decision to revisit, not a hole), single-tag push, a local tag list, and the two `tt forge` auth items.

**Triage note.** The three tag items and the two forge-auth items are arguably two issues rather than one.
They are kept together here because that is how they were found and reported, following the shape of
issues 020 and 026. Happy to split if preferred — the `whoami --gh/--gl` item in particular is a one-line
fix that need not wait for a decision about the safe subset's boundary.

Agent disclosure: originally found and drafted by an AI agent (Claude) in a prontopop session; verified,
corrected, restructured and filed by an AI agent (Claude Opus 5) in session with the maintainer, who
reviewed it.
