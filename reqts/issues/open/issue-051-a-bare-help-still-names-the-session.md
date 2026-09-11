# Issue 051: a bare `help` still NAMES the session, because issue 037 fixed the seven words it had met rather than the shape of the rule

> status: open 2026-08-29 · labels: toolbox, session, agent-trust, ux · measured against: v0.10.2
> (from `tt --version`: git checkout, bash launcher, Linux) at `c51a728`, scala-cli 1.15.0,
> Scala 3.9.0-RC4 · summary: `tt session help` does not print help — it sets the session's human name
> to "help". So does `tt session version`. Issue 037 reserved seven read-shaped words
> (`list ls show status current get name`) so a query could not write, but left the *default* intact:
> any word not on the list is a name. That makes the protection a **denylist over an open vocabulary**,
> and the two most likely words a newcomer or an agent types at an unfamiliar verb — `help` and
> `version` — are not on it. Both are answered elsewhere in the same CLI (`tt session --help`,
> `tt version`), and both sibling tools reject them harmlessly (`tt mode help`, `tt git help` → usage,
> exit 2). `session` is the outlier that writes.

## Description

Issue 037 is the direct ancestor: `tt session list` used to rename the live session to "list". Its fix
(closed 2026-08-16, `7add972`) reserved a set of read-shaped words (`session.scala:94-95`) and added an
audit line on stderr so a write can no longer pass as a read. Both halves work. This issue is about
what the fix did **not** change: the default.

`session.scala:220` still reads, in effect, *any remaining non-flag word is a name*. The reserved set
is therefore a **denylist against an open vocabulary** — it can only ever cover the words someone has
already been surprised by. Two words that were not on the list:

```
$ tt session help
session: named 260829-18h12m -> 260829-18h12m-help      # stderr
260829-18h12m-help                                       # stdout, exit 0

$ tt session version
session: named 250829-18h00m -> 250829-18h00m-version
```

Neither is an exotic guess. They are the first two things anyone types at a verb whose usage they do
not know, and both are *answered* elsewhere in this very CLI: `tt session --help` prints the full help
(`session.scala:169`), and `tt version` / `tt --version` / `tt -v` is a documented toolbox-level alias
(issue 028). So the CLI already has a meaning for both words; `session` assigns them a third one, and
that one writes.

**The sibling tools disagree with it.** Measured, same tree, same launcher:

| command | result |
| --- | --- |
| `tt session help` | **names the session "help"**, exit 0 |
| `tt mode help` | `mode: usage: tt mode [ add <label> \| rm <label> \| clear ]`, exit 2 |
| `tt git help` | prints the eight-verb usage, exit 2 |
| `tt text help` | prints the tool's usage, exit 0 |

`mode` is the closest comparison: it is session's sibling, it writes to the same store, and it is the
other half of the statusline. It takes an explicit verb (`add`/`rm`/`clear`), so no bare word can ever
mutate it. `session` is the one tool in this group whose *setter is the default*, and it is
consequently the only one where a wrong guess is a write instead of a usage message.

### Honest bound: this is not silent, and that is issue 037's doing

The rename announces itself: `session: named <old> -> <new>` goes to stderr, and the statusline shows
the new name. So the 037 audit line does its job, and this is a weaker defect than the one 037 fixed.
It is filed anyway for two reasons.

First, the stderr line is only as good as its reader. An agent that runs `tt session help` to learn the
verb has, at that moment, no reason to read stderr carefully — it asked a question and got a plausible
answer shape (a name on stdout, exit 0). The failure mode is not "the user is not told", it is "the
user is told in the one channel they are not attending to". That is the same reasoning issue 018
records about the absence of bad news.

Second, and the actual point: the residual defect is evidence about the *rule*, not about these two
words. Adding `help` and `version` to the denylist would close today's report and leave the next word
open — `usage`, `verbs`, `-?`, `info`, `?`. A protection that must enumerate an open vocabulary is
never finished, and each round of it is filed by someone who got surprised first.

## How to reproduce it

Against an isolated store, so a live session is not touched:

```bash
# 1. the setter fires on a word that means "how do I use this?"
tt session help --sessions-root tmp/s51 --id t1 --cwd "$PWD" --now-ms 1756483200000
#    => stderr: session: named 250829-18h00m -> 250829-18h00m-help
#    => stdout: 250829-18h00m-help          exit 0

# 2. same for the toolbox's own version word
tt session version --sessions-root tmp/s51 --id t2 --cwd "$PWD" --now-ms 1756483200000
#    => stderr: session: named 250829-18h00m -> 250829-18h00m-version

# 3. the documented surface works, which is what makes the bare word a trap and not a gap
tt session --help                # full help, exit 0

# 4. the siblings, for contrast
tt mode help                     # usage, exit 2 — cannot be renamed by a guess
tt git help                      # usage, exit 2
```

Measured 2026-08-29 on Linux, v0.10.2 at `c51a728`, invoking `tools/tt` from the checkout (bash
launcher, per `tt --version`). Steps 1, 2 and 4 were run and their output is quoted above. Step 1 was
also hit **for real**, unprompted: the first command of an agent session in this repo was
`tt session help`, intended as a help read, and it renamed the live session to `260829-18h12m-help`;
`tt session --clear` restored it.

## Acceptance sketch

* **The narrow fix, and why it is not enough on its own.** Add `help` and `version` (and the obvious
  neighbours `usage`, `info`, `verbs`, `?`) to the reserved words in `session.scala:94-95`. The test
  at `session.test.scala:317` already iterates a word list, so this is a one-line change on each side.
  Cheap, correct, and it does not touch the shape of the rule — so it should be taken *and* not be
  mistaken for the whole answer.
* **`help` should print help, not the name.** The reserved read words currently print the display name
  with a stderr note. For `help` that is still the wrong answer to the question asked; it should print
  `Help` and exit 0, like `--help`. Worth deciding deliberately, because it is the one reserved word
  whose intent is unambiguous.
* **Decide what `version` means on a tool.** `tt version` is toolbox-level (issue 028) and
  `tt session version` currently means nothing coherent. Either delegate it to the same version line,
  or treat it as a read word — but not a name. Maintainer call; noted, not proposed.
* **The structural option: make naming explicit.** `tt session name <words>` (or `--name <words>`)
  would invert the default, so that setting is opt-in and **no bare word can ever write**, which
  retires the whole class instead of its current members. The cost is real and should be stated
  plainly: `tt session alpha prep` is the documented cold-start flow (the tool's help text,
  `tools/README.md:340`, and asserted at `session.test.scala:97` and `:354`), so this is a breaking
  change to a surface humans type.
  A deprecation path (accept the bare form, announce the new one) is possible. This is the option the
  issue exists to put on the table, not one it asks for.
* **Whatever is chosen, assert the property rather than the members.** A test of the form "no word that
  the CLI answers elsewhere — every dispatcher alias and every documented flag word — may set a name"
  fails when a *new* alias is added without thought, which a fixed `Seq` of seven or thirteen words
  cannot do. This is the same widening bullet issue 050 argues for on stderr: one enumerated check is
  a floor, not a total.
* **Out of scope:** the stderr audit line and the reserved-word mechanism from issue 037. Both work as
  designed; this is about the default they sit on top of, and the announcement is why the defect is a
  surprise rather than a silent corruption.

## Discussion

### Comment by hmiddelk at 2026-08-29 18:52

Found by tripping it. An agent session in this repo opened with `tt session help`, meaning to read the
verb's help, and the reply was that the session had been renamed to `260829-18h12m-help`. It read the
stderr line and cleared the name, so nothing was lost — but the sequence is a fair sample of how the
word gets typed, and it happened on the *first* command, before any file had been read.

Filed as the residual of issue 037 rather than as a new surprise, because that is the more useful
framing: 037 was reported for `list`, fixed for seven words on 2026-08-16, and the eighth arrived
thirteen days later by the same route — someone typing a word at a verb they did not know. The
interesting question is not whether `help` belongs on the list — it obviously does — but whether a
list is the right instrument when the setter is the default.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with me, from a defect it hit
itself, and reviewed by me. The agent verified BY RUNNING, against an isolated `--sessions-root`:
that `help` and `version` each set the name (stderr and stdout quoted above); that `tt session --help`
works; and the sibling contrast (`tt mode help`, `tt git help`, `tt text help`). It read the reserved
sets at `session.scala:94-95`, the setter branch at `session.scala:220`, and the existing test at
`session.test.scala:317`. NOT verified: any other unreserved word beyond `help` and `version`; the
statusline's rendering of the accidental name; anything on macOS or Windows; and whether the
`tt session <Name>` cold-start flow has users outside this repo, which is the fact the structural
option's cost turns on.
