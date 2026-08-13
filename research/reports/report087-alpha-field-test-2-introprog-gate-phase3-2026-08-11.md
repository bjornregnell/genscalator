# Report 087: alpha field test #2 — the toolbox as the working surface for one long task (2026-08-11)

- **Question:** does the `v0.10.1` bug-fix release hold up when the toolbox is not *swept* but *used* — as
  the only sanctioned lane for search, git and process execution — across a single multi-hour feature
  implementation on the same third-party Scala repo?
- **Why it matters:** report 085 enumerated the surface; report 086 replicated it on Windows. Neither ran
  the toolbox as the **working substrate** for a long task with a real deliverable. 085's own method note
  says a surface sweep and a use-it-for-real pass find different defect classes and that an alpha wants
  both — this is the second arm. It is also the first field use of the release that fixed 085's findings.
- **Status:** shipped as issues 024–028.
- **What shipped:** nothing in genscalator. The deliverable was in the *target* repo — phase 3 of the
  introprog mirror compile gate. The toolbox was the instrument, not the subject of the work.

## Environment

| | |
|---|---|
| Date | 2026-08-11 |
| Platform | Ubuntu 24.04.2 LTS, `VERSION_ID=24.04` |
| Kernel | Linux 6.8.0-136-generic, x86-64 |
| `tt` under test | `v0.10.1` native, `~/.genscalator/bin/tt` (ELF, 42.3M, 2026-08-11T10:23) |
| Installed via | `tt update --native --write`, in-session upgrade from `v0.10.0` |
| Release asset | `genscalator-linux-x86_64.zip`, 14 767 868 B, sha256 `1c03c12fff83e8dcca2e66a64c71a9444ebdfb8efeaed5d700f19bffe6e09b45`, 30 files |
| Plugin cache | `0.10.1`, updated in-session via `/plugin marketplace update bjornregnell` + `/reload-plugins` |
| Skills | 12 active (`serverless-spa-seed` is new since the `0.9.1` that was loaded at session start) |
| Target repo | `lunduniversity/introprog` @ `a19a3a46`, clean checkout |
| scala-cli | 1.x at `/usr/local/bin/scala-cli`, two further copies shadowed on PATH |

**Version note, and it is good news.** The binary and the plugin now *both* report `0.10.1`. Issue 021's
core symptom — a release declaring the wrong version in its in-repo carriers — is **fixed**, and this
report could name its artifact by version rather than by mtime, which 085 and 086 could not. Two residues
survive: the binary's `VERSION.txt` says `v0.10.1` while the plugin's says `0.10.1` (the `v`-prefix
mismatch 086 predicted any version-agreement gate would have to settle), and there is still no
`tt --version` / `tt version` verb — both exit 2 with the usage line — so identifying the build still
means reading a file.

## Method

**Not a sweep.** One real engineering task, start to finish: implement the parked "phase 3" of a LaTeX/Scala
translation compile gate (cross-environment context so REPL transcripts that reference a neighbouring
code block can be compile-checked). Multi-hour, iterative, with genuine verification runs.

The constraint is the experiment. The human enforced the tool lane explicitly and repeatedly, rejecting
three tool calls mid-flight:

- raw `git log` / `git remote -v` → *"I want to use tt git as well as described in the genscalator plugin"*
- raw `git remote -v` again → *"use tt git for effectull ops"*
- raw `scala-cli run …` → *"use tt ..."*

That forces the toolbox to be **sufficient** rather than merely available, and every place it was not
shows up as friction with a timestamp. It is a different instrument from a help-text sweep: a sweep asks
*does this tool work*, this asks *can you get a day's work done inside the lane*.

Deliverable outcome, recorded only to establish that the task was real and completed: gated code bodies
37 → 52 (+41%), 0 regressions, whole gate 316 s, landed as one commit. The new gate's *failure* path was
proved separately with an injected break invisible to the three pre-existing gates — which matters here
only because it means the task included a real verification loop, not just a green run.

## Findings

Pointers only — as in 085 and 086, the issue files are the single source of truth and this table does not
restate them.

| issue | severity | one-line |
|---|---|---|
| 024 | docs / defect | `tt-toolbox` and `avoid-guard-stall` give **contradictory** git guidance; observed cost: two raw-git calls, two human corrections |
| 025 | gap | `tt verify` discards the child's stdout — a long run is unobservable, and there is no `--tee` / `--out-file` |
| 026 | gap (2 of 3) | `tt git` can finish a PR workflow but cannot start one: no branch verb, no `--remote` on `fetch` (which also reports a false "up to date"); the `--set-upstream` refusal turned out to be deliberate policy, and is filed as such |
| 027 | defect | `tt text` prints a raw Java stack trace for a missing file instead of a clean error and exit 2 |
| 028 | polish | no `tt --version` / `tt version` verb; the two carriers still disagree on the `v` prefix (021 residue) |

One observation that belongs here rather than in any single issue: **024 is the finding a surface sweep
structurally cannot produce.** It is not a broken tool — every tool involved works. It is two pieces of
correct documentation that disagree, and the only detector is an agent acting on them under real
conditions. Reports 085 and 086 both read the same two skills and neither noticed, because neither had a
task that made the git lane load-bearing.

## Coverage — what was NOT tested

**Invoked for real: 8 of 45** — `files` `git` `gitinfo` `text` `update` `verify` `which` `wr`
(`wr` help-only, see below), plus the bare dispatcher for its tool list.

**Help-only, no real invocation: 3** — `bloop` `scala` `wr`.

**Not tested: 34.** No claim of any kind is made about them.

The denominator is worse than 085's 26/45 and that is inherent to the method: a sweep chooses tools, a
task does. What this arm buys instead is *depth* — `tt verify` alone was invoked a dozen times across
runs lasting from 2 s to 40 min, which is how 025 surfaced at all. A sweep would have called it once,
seen `PASS`, and recorded it as working (085 did exactly that, and was not wrong).

Notable near-misses: `tt bloop` was consulted for its status/restart contract but never needed, because
the compile daemon behaved; `tt scala` was read but not used, because its verbs take a project
*directory* and this task compiles thousands of throwaway single files.

## What worked well

Recorded for the same reason 085 and 086 record it.

| thing | verdict |
|---|---|
| `tt update --native` | Issue 003 shipped, and it shows. Preview-by-default, sha256 against the published hash, CRC32 on every archive entry, staging dir beside the install, two renames instead of a write-through — then `installed v0.10.1`, with the line "the binary you just replaced kept running", which is exactly the reassurance the moment needs. Staging and backup dirs were cleaned up on their own. An in-session self-upgrade of the tool under test, with no drama. |
| `tt update` (non-native) | Correctly refuses to actuate the plugin update and prints the two exact `/plugin` commands only a human can run. The "the name above is the MARKETPLACE name, not the plugin name" clarification pre-empted the obvious mistake. |
| `tt verify` | Notwithstanding 025: the audit line turned "did it pass?" into a record. `316483 ms` against the earlier cold-path timings is *how the bloop speedup was established at all*. The allowlist (`scala-cli`, `tt`, `scalex`) happened to cover this task exactly, so no widening was needed — a good sign for the no-flag, human-only `TT_VERIFY_ALLOW` design under real load. |
| `tt which` | Best in show again, as in 085. One call surfaced three `scala-cli` binaries and the plugin-vs-native `tt` shadowing, with kinds, sizes and mtimes. It is the tool that makes an environment table honest. |
| `tt text grepr` | The BRE-escape warning ("pattern has grep-BRE escape(s) `\{` — this tool uses Java regex") fired twice on a genuine reflex error. A warning that catches a habit rather than a typo is worth more than its line count. |
| `tt git log --grep` | Searched the target repo's history with no shell and no pager, and was the right tool the first time. |

Also worth stating: **the guardcheck hook never fired once** across roughly sixty Bash calls. The three
interventions in this session were all *human* tool-lane corrections, not guard stalls. Following the
`avoid-guard-stall` reflexes (`grepr` not `grep`, `run_in_background` not `| tail`, the Write tool not
`>`) appears to work as designed — with the sharp exception of 024, where the reflex the agent had
absorbed was the wrong one because two skills disagreed.

## Corrections made during the session

Both were claims the agent made to the human and then had to withdraw against measurement. Recorded
because each one influenced a decision before it was checked.

- **Overstated the value of the REPL continuation-prompt fix.** The agent argued that phase 3 would be
  materially "starved" without repairing the transcript reader (Scala 3's `|` continuation prompt was
  being classified as compiler output, truncating multi-line entries to nothing). The human questioned
  the term "compiler diagnostic", then scoped the fix, then asked for it. Measured afterwards: it moved
  exactly **one** transcript from "no code" to "has code", and contributed **zero** additional gated
  bodies. The reasoning was sound and the defect is real; the impact estimate was not evidence, and it
  was presented in a way that helped drive a scoping decision.
- **Filed a deliberate policy as an oversight, and caught it only at the last check.** Issue 026's second
  item was drafted as "`tt git push` has no `--set-upstream`, so the first push of a new branch falls out
  of the lane". Reading `tools/git.scala` on `main` before committing showed the help text states the
  opposite of an omission: *"set the upstream once with `git push -u`; the tool never sets one behind your
  back."* It is an intended, well-argued design decision. The item survives in narrowed form — an
  explicitly typed flag is arguably a request rather than something done behind the caller's back, and the
  refusal currently surfaces as raw git's `fatal:` rather than as a tool-level message — but the original
  framing would have had a maintainer arguing against a position nobody holds. The lesson is procedural:
  the duplicate check was run against the issue *titles* early and against the *source* only at the end,
  and the source is where the design intent lives.
- **Mis-framed the cost of phase 3.** The agent projected ~75 min for the gate and "phase 3 adds ~50%",
  which made the new phase look expensive. After switching the compile helper from `--server=false` to
  the persistent bloop daemon (measured: 19.3 s cold, **2.2 s warm in a fresh directory**), the entire
  gate ran in **316 s**. The ratio was about right; the absolute figure was anchored on an artefact of
  how the existing helper spawned compilers, and framing it as phase 3's cost was misleading.

## Duplicate check

Searched `open/` and `closed/` before filing. 024 is the next free number (highest existing 023, across
both directories). The check changed the shape of the batch twice, so it is worth recording rather than
just asserting.

- **Merged three findings into 026.** The branch verb, `push --set-upstream` and `fetch --remote` were
  first written up as three issues. They were hit in one unbroken sequence while opening a single PR and
  share one triage question — *how far does the non-destructive half extend?* — so three files would make
  the maintainer answer it three times. Filed as one issue with three items, following the shape of 020,
  with a note that item 3 is a one-line parity fix that need not wait for the other two. Commit `--amend`,
  the other half of the report-085 note, is deliberately excluded: amend rewrites a commit, so unlike the
  three above it is genuinely arguable against the safe-subset principle.
- **Rescoped 028 after finding 020 had shipped.** It was drafted as "no version verb, and `tt help` is
  awkward"; 020's discussion shows `help` / `--help` / `-h` already fixed on both code paths in the
  v0.10.2 wave. Since v0.10.1 is the latest *release*, this box still hits the old behaviour — but filing
  it would have re-reported a solved problem, so 028 is scoped to `--version` alone and cross-references
  020's dispatcher work as the natural place to add it.

Relationship to the existing git issues, stated because it is easy to mistake for overlap:

- **004** is the parent of this family and is NOT duplicated. It is a *missing* verb producing a justified
  raw reach (`tt gitinfo` gives a count, not paths), and it names the tripwire rule: reaching for a raw
  shape IS the signal that a typed verb is missing. **026** is four more instances of that same rule
  firing. **024** is its inverse — a *present* verb with documentation pointing away from it.
- That inversion has an operational consequence worth flagging to the maintainer: mis-instruction adds
  **noise to 004's signal**. If an agent reaches for raw git because a skill told it to, the reach no
  longer reliably indicates a gap — so 024 is worth fixing partly to keep 004's tripwire diagnostic.
- **008** (`tt git log` search) is closed and shipped; it was used successfully in this test and is not
  implicated.

No existing issue covers `tt verify` output handling (025) or `Lib.readUtf8`'s error path (027).

## Threats to validity

- **n = 1 task, one repo, one day.** Tool selection was driven by what the task needed, so the coverage
  gap is not random — it is systematically biased toward search, git and process execution.
- **Not independent of 085 or 086.** Same human, same agent model, same target repo, same conventions.
  On the judgement-call findings (025, 026) a different pair would be worth more than this one.
- **The deliverable's success is not evidence about the toolbox.** Phase 3 landing says nothing about
  `tt`; only the friction does. Resist reading the +41% as a toolbox result.
- **"The lane was sufficient" is really "the lane plus three human corrections was sufficient."** An
  unattended agent would have used raw `git` and never generated 024 or 026 — which is an argument for
  human-in-the-loop field tests, and a caution about what a solo run would have reported.
- **025 is confounded with the target program's design.** The gate spawns one compiler per snippet, which
  is what made runs long enough for verify's buffering to hurt. A tool that finishes in ten seconds would
  never expose it. The finding is real but its severity is task-shaped.
- **Effectful and outward-facing tools were again mostly untested** (`forge` `ssg` `serv` `web` `zip`
  `harden`), on the same grounds as 085 — so three field tests now share one blind spot.

## Notes for the next field test

- **Fix 024 before the next test**, or the same correction gets spent again. It is a one-line
  documentation change and it is currently teaching the agent the opposite of the intended reflex.
- **025 is the one that changed the work.** Everything else cost an exchange; this cost two killed runs
  and a modification to the program under test. It is also the finding most likely to recur, because
  "drive a long build and watch it" is a normal thing to want.
- **Third report, third version-identification tax.** 085 asked for it, 086 asked for it, and while the
  *declared* version is now correct there is still no way to ask the binary what it is. `tt --version`
  would close it.
- **A task-shaped field test finds different things than a sweep — and a shorter task would have found
  none of these.** 024 needs a git workflow, 025 needs a run long enough to want progress from, 026 needs
  a PR. Worth choosing the next test's task for the *lanes* it will exercise rather than its subject
  matter; the obvious untried lane is the effectful/outward one.

---

Reported by hmiddelk. An AI agent (Claude Opus 5) did the engineering work, hit the friction, and drafted
this report under human direction; the human enforced the tool lane, arbitrated scope twice, and reviews
and submits. Two claims the agent made were withdrawn against measurement and are recorded above.

## Maintainer review round, 2026-08-13 (appended at merge)

PR 3 merged as `c3b271c` after a review round on `main` at `542b2fd`. Per-issue verdicts and triage live
in each issue's `## Discussion`; this section carries only the cross-issue and process points, per
`CONTRIBUTING.md`.

**Method.** Five independent review agents, one per issue, each briefed read-only and each told that a
confirmation earns little and the value is the delta: what the issue got wrong, missed, or over- or
under-stated. That brief clause is carried over from the PR 2 round, where it was what earned the fleet
its tokens, and it earned them again here. A sixth agent audited the session's own handover in parallel.

**Aggregate verdict.**

| issue | verdict | the delta that mattered |
|---|---|---|
| 024 | confirmed, understated | the contradiction spans **nine** carriers, not three, including `AGENTS.md`, the guard's own fix text, and a passing test that certifies the disputed shape |
| 025 | partly confirmed | "never echoed" is false (the FAIL path echoes a 20-line tail); guardcheck has no `tee` check; and there is no `--timeout` either |
| 026 | confirmed, mis-diagnosed | the false all-clear comes from the **empty-output fallback**, not remote selection, so the proposed `--remote` fix would not have fixed it |
| 027 | confirmed, understated | 7 verbs and 12 call sites, four of which bypass `Lib.readUtf8`, so the proposed single-point fix would have left a third still tracing |
| 028 | partly confirmed | the `v` prefix is a settled, tested, CI-gated invariant; unifying the carriers would break `tt update --native` |

**Three of five findings survived contact in a stronger form than filed, and two would have produced an
incomplete fix if implemented as written.** Both incomplete-fix cases (026 item 3, 027) share a shape: the
reporter correctly localised the *symptom* and inferred a cause from it, where the cause lay one level
down in shared code. Neither is a criticism of the filing, since both issues were explicit about their
evidence and one of them flagged its own uncertainty. It is an argument for the verify round existing at
all, and for keeping the "note anything the issue got wrong" clause in every review brief.

**Process points.**

* **Two findings were produced by reviewing the report rather than the toolbox**, and are now issues 029
  and 030: merging this PR required raw `gh`, because `tt forge` has read verbs for pull requests
  (`prs`, `pr`, `pr-files`, `pr-diff`) but cannot list a PR's commits and cannot merge. The commit-listing
  gap is the sharper one: `CONTRIBUTING.md` forbids assistant-credit trailers, and that rule could only be
  checked by leaving the lane. A rule we cannot check inside the lane is a rule the lane does not carry.
  This is the seventh name-the-gap specimen in this series.
* **One new defect was found while verifying another** and is filed as issue 031, credited to this report:
  `tt files` on a missing root prints `0 files` and exits 0, where `tt find` correctly exits 2. Silent
  wrong answers rank above loud ones on our list, so the smallest finding of the batch (027) turned out to
  be adjacent to the most serious.
* **The coverage argument is accepted as filed.** 8 of 45 for real, against 085's 26 of 45, is the correct
  trade for this arm, and the depth is what produced 025. No change to method is requested for the next
  test. The suggestion to choose the next task for the *lanes* it exercises is adopted: the untried lane
  is the effectful and outward one, which is precisely where 029 and 030 were just found by accident.
* **The report's self-corrections are the part to keep.** Three claims withdrawn against measurement, each
  recorded rather than quietly amended, is why the item-2 correction in 026 arrived as a narrowed ask
  instead of an argument against a position nobody holds. That practice is now the house expectation for
  field-test reports.
