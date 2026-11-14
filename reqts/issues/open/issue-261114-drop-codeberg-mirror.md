# Issue 261114-drop-codeberg-mirror: drop the Codeberg mirror, after Codeberg's July 2026 LLM policy

> status: open 2026-11-14 · labels: hosting, docs, deploy · measured against: genscalator v0.10.3, main at `6481b7a`
> · summary: Codeberg's members voted in July 2026 that projects written with heavy use of LLMs, or heavily tied
> to the LLM ecosystem, are no longer welcome there. genscalator is both. We stop mirroring to Codeberg, remove the
> live references to it, and delete the Codeberg repo. GitHub stays the primary host, gitlab.com stays as a
> public mirror, and coursegit.cs.lth.se stays as a closed backup.

## Description

Codeberg's members voted on a Terms of Use change, which passed 358 to 144 on 2026-07-22. The announcement is
https://blog.codeberg.org/protecting-our-floss-commons-from-llms.html and the change itself is
https://codeberg.org/Codeberg/org/commit/71149c7fc95ccfeae36109b5cddca339e4aa1473. Among the projects the post says
are no longer welcome are those "written and maintained with heavy use of LLMs" and those "heavily tied to the LLM
ecosystem". genscalator is developed together with a coding agent and ships as a Claude Code plugin, so it fits both.

Codeberg says enforcement is case by case, with no scanning and no mass deletion, and that small experiments are
likely to be tolerated. We leave anyway. The policy is an explicit decision by Codeberg's community about its own
commons, and genscalator argues for human control and respect for the people who run shared infrastructure.
Staying because nobody enforces the rule would not match that.

What we give up: the Codeberg copy was kept as the EU-hosted one (SM035), and it was the only public one.
coursegit.cs.lth.se is a Swedish university server, but it is closed hosting with no public access, so it is a
backup, not a mirror anyone else can read. After this change the public hosts are GitHub and gitlab.com, both
US-based; coursegit keeps an EU-hosted copy of the history. If public EU hosting matters again, it needs a new
host whose policy fits the project.

State at filing: Codeberg's `main` is at `649943e` (2026-10-11), 20 commits behind; its newest tag is v0.10.2.

## Acceptance sketch

Live references, to edit or remove:

- `README.md:386`, the paragraph naming the mirrors.
- `docs/manual-src/getting-started.md:23` and `docs/claude-plugin.md:33`, the "Codeberg works too" plugin
  marketplace URL.
- `reqts/issues/open/issue-000-how-to-make-issues-on-genscalator.md:43`, the Codeberg issue link.
- `deploy/mirror.sc`: the `"codeberg"` target (line 84) and the comments naming it (lines 17, 33, 40, 41).

Left as they are, because they record what was true at the time: research case studies and logs, closed issues
(e.g. 036), and the comment in `deploy/mirror.sc` about the 2026-07-26 direction flip.

Then:

- remove the `codeberg` git remote from the local clone;
- the maintainer deletes https://codeberg.org/bjornregnell/genscalator in the Codeberg web UI (irreversible,
  done by hand);
- `tt text grepr <repo> md,sc,scala 'codeberg\.org/bjornregnell/genscalator'` hits only historical records.

Out of scope: `tt forge` keeps its Codeberg/Forgejo support, which is a general tool feature. The
scala-common-style repo on Codeberg is human-written prose about Scala style and is not affected.

## Discussion

2026-11-14: open question, delete the Codeberg repo or keep it as a frozen archive. The case for deleting: an
archived repo is still hosted there, and the cost is a few broken old links, mostly in research records.

2026-11-14, fix: two more live places turned up while doing it. `docs/RELEASING.md` steps 8, 10 and 12 used
`tt forge` without `--gh`, i.e. against its Codeberg default, with the Codeberg token; they now use `--gh` and
`GENSCALATOR_GITHUB_TOKEN`, and step 11 names gitlab + coursegit. `docs/EMBER-TEMPLATE.md` called Codeberg a batched
mirror. The plugin install docs now name the GitLab mirror as the alternative URL. Not changed: `tt forge` still
defaults to Codeberg; whether the default should move is a separate question. RT004 keeps its 2026-06-30 snapshot of
BR's instructions verbatim; the live instructions are outside the repo.
