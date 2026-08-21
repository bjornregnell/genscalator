# Issue 044: `get-genscalator.sc` silently ignores unrecognised arguments, so a mistyped or unsupported `--uninstall` installs instead

> status: open · labels: installer, cli-contract, uninstall, safety, good-first-issue · summary: the
> bootstrap parses flags with `argv.contains(...)` and validates nothing, so any unknown `--flag` is
> discarded and the script proceeds to install. `--uninstal` (one `l`) installs; `--uninstall` on a carrier
> that predates the feature installs. A request to REMOVE software is answered by ADDING it, with no error.

## Description

Found 2026-08-21 in a Windows field test of the update lifecycle (report 088), while establishing whether
issue 043's symptom was an artefact of release timing or a defect in its own right. It is the latter.

`get-genscalator.sc` reads its command line as a set of membership tests:

```scala
val argv       = args.toList
val dryRun     = argv.contains("--dry-run")
val noPath     = argv.contains("--no-path")
val uninstall_ = argv.contains("--uninstall")
val force      = argv.contains("--force") && !dryRun
def flag(n: String): Option[String] =
  val i = argv.indexOf(n); if i >= 0 && i + 1 < argv.size then Some(argv(i + 1)) else None
```

Nothing ever asks whether a token was *understood*. Every argument the script does not recognise is
discarded in silence, and control falls through to the install path — which is the one destructive-adjacent
default the file has, since it writes ~30 files and edits a shell config.

Two ways this bites, both observed:

**1. A typo inverts the operation.** On `main`, where `--uninstall` exists:

```
$ scala-cli run get-genscalator.sc -- --uninstal --force --dry-run
genscalator bootstrap  (DRY RUN: nothing will be written)
  ...
  would unpack 30 file(s) into ~/.genscalator
```

One missing letter turns *remove everything* into *install everything*. `--force` is discarded on the way
past, so nothing in the output acknowledges that a destructive operation was requested.

**2. An older carrier answers a flag it has never heard of.** This is how it was actually met — see
**issue 043**: the `get-genscalator.sc` served by `releases/latest/download` has no `--uninstall`, so the
documented uninstall command runs the installer. 043 is the reason a user types the flag; this issue is the
reason they get no error.

**Why it wedges.**

1. **The failure direction is the worst available one.** A silent no-op would be recoverable, an error
   message would be free. Instead the tool performs a *different* operation than the one requested, and
   the operation it performs is the one that writes to disk and edits the user's shell config. The
   `--force` in `--uninstall --force` shows the caller had already accepted a destructive outcome; the
   script discards that signal too.
2. **It contradicts the file's stated premise.** The header argues that this script exists in this shape
   because *"genscalator argues against curl-into-shell precisely because it hides what it does"*, and asks
   to be read before it is run. A CLI that quietly discards half its input is hiding what it does, in the
   one artifact whose whole job is to not do that.
3. **It is out of step with the rest of the project.** `tt` exits 2 on an unknown tool — verified on this
   box, `tt nosuchtool` → `tt: no such tool 'nosuchtool'`, exit 2, and issue 020 deliberately kept that
   invariant while adding `tt help`. The bootstrap is the *first* genscalator artifact a newcomer runs and
   it is the one place where an unrecognised token is free.
4. **It will silently absorb every future flag.** Any flag added on `main` is, from the perspective of an
   older fetched copy, an unknown flag — so this defect converts every future capability gap into an
   unwanted install rather than a clear "your script is too old". That is precisely the diagnosis a user
   in 043's position needs and cannot get.

Scope note: this is argument *validation*, not argument *parsing*. `argv.contains` is fine for a file that
resolves no dependencies; the missing piece is a closing check that every `--`-prefixed token was consumed.

## How to reproduce it

With any copy of `get-genscalator.sc` (the typo case needs the `main` copy, which has `--uninstall`;
`--dry-run` keeps it non-destructive):

```
$ scala-cli run get-genscalator.sc -- --uninstal --dry-run        # one 'l'
genscalator bootstrap  (DRY RUN: nothing will be written)         # ...it installs

$ scala-cli run get-genscalator.sc -- --this-is-not-a-flag --dry-run
genscalator bootstrap  (DRY RUN: nothing will be written)         # same

$ scala-cli run get-genscalator.sc -- --uninstall --dry-run       # with a pre-v0.10.3 asset
genscalator bootstrap  (DRY RUN: nothing will be written)         # issue 043's path
```

Expected in each case: a non-zero exit naming the token it did not understand.

Contrast with the sibling contract in the same toolbox:

```
$ tt nosuchtool
tt: no such tool 'nosuchtool'
usage: tt <tool> <args...>   (tools: ascii bloop box chrono ...)     # exit 2
```

## Acceptance sketch

* **Reject unknown `--`-prefixed tokens**: collect the known flag names (`--dry-run`, `--no-path`,
  `--uninstall`, `--force`, `--home`, `--tag`), subtract the values consumed by `--home` / `--tag`, and
  `die` naming the offender if anything `--`-prefixed remains. Roughly four lines, using the `die` helper
  that already exists.
* **Suggest the nearest known flag** if it is cheap — a `--uninstal` that prints
  `unknown flag '--uninstal'; did you mean '--uninstall'?` turns the worst case into the best one. Optional,
  and only worth it if it stays a few lines.
* **Name the script's own vintage in the error**, so 043's case is self-diagnosing: when an unknown flag is
  rejected, print the release the copy came from (or that it does not know) plus a pointer to fetch a
  current one. The user in 043's position then learns *"this script is older than the flag you typed"*
  rather than watching an install scroll past.
* **A bare positional argument is also currently ignored** and should get the same treatment, for the same
  reason.
* Test: a CLI-contract case per rejected shape (unknown flag, typo'd known flag, stray positional)
  asserting non-zero exit and that **nothing was written** — the last part matters, since the bug is that
  the fall-through path writes.

## Discussion

### Comment by hmiddelk at 2026-08-21 17:00

Filed from report 088 (`research/reports/report088-windows-update-lifecycle-2026-08-21.md`), which carries
the method, coverage and threats to validity for this batch.

Baseline measured against: released `v0.10.2` native `windows-x86_64` (install `VERSION.txt` = `v0.10.2`,
zip sha256 `7b5fcae61f2cae8da84decd82c7a2420a8a078aef08cf03b14cf730896ae9b7d`) for the `tt` comparison, and
`get-genscalator.sc` at `main` `7f03345` (360 lines, sha256
`beeb62ffb119804d1185b8a957789adac256b14f8dce8c0f1ec35b0cfbe33ba4`) for the typo case. Named by hash rather
than `tt --version`, which still exits 2 at this release (**issue 028**). Windows 10 Enterprise
10.0.19045, PowerShell 5.1, Scala CLI 1.16.0.

Numbering note: `main`'s highest is 039, but PRs #4, #5 and #6 claim 040-042. This takes **044** assuming
those land; the later PR renumbers per `reqts/issues/README.md`.

**Split from issue 043 deliberately, and the split is the argument.** They were met as one event — the
documented uninstall command performing an install — but they are independent in cause, fix and lifetime.
043 is why the flag gets typed and disappears when v0.10.3 publishes a script that has it; this one is why
no error is printed, is one-line, and is true on `main` today. Fixing only 043 leaves a bootstrap that
answers every typo with an install. Filing them together would have let the cheap durable fix wait on a
docs/release-sequencing decision.

Worth recording that the typo case was **run, not reasoned**: the first draft asserted this was a
release-timing artefact of 043 on the strength of having read the argument block. `--uninstal --force
--dry-run` against `main`'s script settled it in one command and promoted a footnote to an issue. The
project's own lesson, again — a claim about behaviour that has not been executed is a guess.

Not verified: macOS or Linux (pure argument handling, so it should reproduce anywhere); whether the
fall-through is intentional tolerance for forward-compatibility, which is the one reading under which this
is a docs issue rather than a defect — though even then the silence, not the tolerance, is the problem.

Agent disclosure: an AI agent (Claude Opus 5) found this while checking whether issue 043 was
release-timing-specific, ran the typo case, and drafted this issue under human direction; the human
reviewed and submitted.
