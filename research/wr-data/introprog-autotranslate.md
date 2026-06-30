# WR data — introprog/autotranslate case study

**WR = Workflow Research** (see [`README.md`](README.md) for the thesis + field schema). Each row below is
a *real* moment, during the introprog/autotranslate working session, where an agent action hit (or would
hit) a **confirmation/approval prompt** because it used dynamic shell — and is therefore a **candidate to
design away**. Resolution is usually *build a new typed `tt` tool*, but it can also be *make an existing
typed tool the reflex* (and allowlist it) or *a usage rule* — the goal is that the prompt never recurs.

> The introprog/autotranslate session appends here. One row per friction event.

| when | context | action | command (offending form) | why-prompted | candidate-tool / fix | status |
|------|---------|--------|--------------------------|--------------|----------------------|--------|
| 2026-06-26 | introprog/autotranslate | Find which source files contain given Swedish code-comment/string strings | `cd …/introprog && grep -rnlE '<strs>' compendium/examples … \| grep -vE '/compendium-en/' \| head` | raw recursive `grep -r` (non-typed), `cd`, pipe `\|` to `grep -vE`, `\| head` — full dynamic-shell stack | **`tt text grepr <dir> <ext> <regex>`** — EXISTS. Lesson: make it the reflex; needs an **absolute** dir (cwd resets between calls → relative path throws `NoSuchFileException`). | built (reflex gap) |
| 2026-06-26 | introprog/autotranslate | Multi-ext search: needed `.scala` AND `.java` in one scan | (two separate `tt text grepr … scala …` / `… java …` calls) | `tt text grepr` takes ONE `<ext>`, so a 2-language scan = 2 calls | **enhance** `tt text grepr` → accept `<ext1,ext2,…>` (comma list / repeatable) | **built** (tools/text.scala, genscalator session 2026-06-27) |
| 2026-06-26 | introprog/autotranslate | (same) when given a relative dir | `tt text grepr compendium/examples scala …` | raw Java `NoSuchFileException` stack trace dumped to the user on a bad/relative path | **harden** `tt text grepr`: resolve relative dir vs a base, friendly one-line error (not a stack trace) | **built** (tools/text.scala, genscalator session 2026-06-27) |
| 2026-06-26 | introprog/autotranslate | Launch a long (~3.5 min) regen as a background job, capturing output | `cd …/introprog && LOG=… && scala-cli run …/at.scala -- …/introprog --all … > "$LOG" 2>&1; echo "EXIT=$?"` | `cd` + `&&`/`;` compound + `LOG=` var + `> file` redirect + `echo` status line — dynamic shell; redirect+echo also **redundant** | **rule, not a tool**: one bare command + `run_in_background:true` (harness already captures stdout + exit code to the task file). Pass abs paths as args instead of `cd`. | rule |
| 2026-06-26 | introprog/autotranslate | Many read-only checks (git status / md5 / line counts) bundled | `cd X && rev=$(…) && echo … && git status … && md5sum …` (recurred several times) | `cd` + `&&` compound + `echo` headers + chained `git` — the recurring "scaffolding stack" | **rule**: one bare command per call; `git -C <abs-path> status -s` as its own call; use the editor's Read, not `cat`/`head`; prefer typed tools (`tt text count`, `tt text cols`) over `wc`/`awk` | rule |

## Narrative (what the data shows)

The dominant friction cause in this session was **momentum re-introducing the dynamic-shell stack**
(`cd`, `&&`/`;`, `$var`, `>` redirect, `echo` headers, raw `grep -r`, `| head`) even when a safe typed
path existed. BR flagged two of these in-session with the literal call-out **"WR data"**.

Two distinct flavours, both genscalator-relevant:

1. **Tool exists, reflex doesn't.** `tt text grepr` already replaces raw recursive grep, but the agent
   reached for `grep -rnlE … | grep -vE | head` first. The fix is not a new tool — it's making the typed
   tool the *default reflex* + ensuring it's allowlisted so the bare typed form never prompts. This is
   evidence that **shipping a tool is necessary but not sufficient**; discoverability/allowlisting/habit
   close the loop. (Two real tool-improvements fell out and were **shipped** the same day in the genscalator
session: multi-ext scan `<ext1,ext2,…>`, and a friendly `exit 2` error on a bad/relative dir.)

2. **No tool needed — the bundling itself is the prompt cause.** A single `scala-cli run …` is fine; the
   `cd && LOG=… && … > log 2>&1; echo` wrapper is what makes it dynamic/non-allowlistable. The fix is the
   **one-bare-command discipline** + leaning on harness features (`run_in_background` captures output, so
   the redirect/echo are pure noise). This argues genscalator's method should explicitly cover *command
   hygiene rules*, not only *new tools* — some confirmation fatigue is eliminated by **subtraction**.

Prioritisation hint (the point of logging columnar WR data): the **highest-frequency** cause here is the
generic dynamic-shell bundle; the two `tt text grepr` enhancements (multi-ext, friendly errors) are now
**built**, so the open items are the discipline/allowlisting reflex and the launch-bundle subtraction rule.
A later `tt text freq` over the `why-prompted` column across all case studies would rank these objectively.

### Recurrence 2026-06-29 (same root cause)
Agent ran `cd <dir> && pdflatex … -halt-on-error … > log 2>&1; echo "EXIT=$?"` then a chained `grep` to
test one cover .tex — the full dynamic-shell bundle (cd, `&&`, `>` redirect, **`echo "EXIT=$?"`**, `;`,
piped grep). BR flagged it: *"WR data — don't ask again for: echo EXIT$?"*. The `echo "EXIT=$?"` idiom is
**pure noise** under this harness: `run_in_background` already captures stdout+exit code, so the redirect
AND the echo are redundant — the correct form is one bare `pdflatex …` (or the existing `build-deck`/
`pdfCompendium*En` task) with `run_in_background`. Confirms flavour 2 (subtraction): the highest-frequency
prompt cause remains the agent re-bundling dynamic shell under momentum, not a missing tool. **Candidate
fix:** allowlist the bare `echo`/exit-status idiom is the WRONG fix — the right fix is the agent NEVER
emitting `> log 2>&1; echo "EXIT=$?"` because the harness reports exit code itself. A linter/hook that
rejects `echo "EXIT=$?"`-style tails would make the anti-pattern impossible.

### Tool bug 2026-06-29: `tt text match` is not UTF-8 robust
`tt text match <file> '[åäöÅÄÖ]'` returned NOTHING on a file full of å/ä/ö, forcing the agent to fall back
to ASCII-only regex substrings (lossy). Cause: `text match`/`count`/`cols` use `Lib.readLatin1` while
`grepr`/`freq` use `readUtf8` — so a UTF-8 `å` (bytes C3 A5) is read as two latin1 chars `Ã¥` and a UTF-8
regex class `[åäö]` can't match. **Fix:** make `match`/`count`/`cols` read UTF-8 (or auto-detect), OR accept
an `--enc` flag. Until then, `match` mangles Swedish output (shows `Ã¤`) and can't be regex-searched by
diacritic. BR flagged: fix tt to be encoding-robust. (Reminder owed to BR.)

### Recurrence 2026-06-29: hand-rolled bash wait-loop instead of typed Monitor/Read
To watch a background job's `progress.txt`, the agent ran
`f=...; i=0; until [ -f "$f" ] || [ $i -ge 30 ]; do sleep 3; i=$((i+1)); done; echo ...; cat "$f" || echo ...`
— a dynamic-shell stack (var assignment, `until` loop, `||`, arithmetic `$((...))`, `cat`, `echo`, `;`
compounds). BR flagged WR data. Two typed paths exist and should be the reflex: **Monitor** (the deferred
tool) for "block until a condition/file appears", and **Read** for showing a file's contents — never
`cat`. The `sleep`-loop pattern is exactly what the harness's anti-foreground-sleep rule pushes you off of;
reaching for bash to poll is the smell. Candidate: a tiny `tt wait <path> [--timeout]` / `tt tail <path>`
typed helper, OR just discipline (Monitor + Read). Same root cause as the other entries: momentum
re-introducing dynamic shell when a typed tool exists.

**Follow-on same session:** switched to the typed **Monitor** tool — but its `command` body is STILL a raw
bash poll loop (`prev=""; while true; do cur=$(cat progress.txt); ...; sleep 90; done; case ... break`).
BR flagged WR data again. So the typed *wrapper* (Monitor) doesn't remove the dynamic-shell smell when
there's no typed primitive to poll a file: you still hand-write `cat`/`while`/`sleep`/`case`. The cleaner
end-states: (1) `tt tail <path>` / `tt watch <path> --interval` typed helper usable as the Monitor command
(or standalone), so periodic file-progress needs no bash; (2) or the producer streams progress to a
channel the harness reads natively (e.g. a structured progress event), removing the poll loop entirely.
This is the strongest case yet for a typed file-watch primitive — it recurs for ANY long background job.

**HOT (3× in one session, 2026-06-29).** BR flagged this same bash-poll-loop pattern THREE times in one
session (bare until-loop, then twice inside Monitor's command body). Promoted from idea to **high-priority
candidate**. Resolution adopted as discipline: STOP hand-rolling poll loops entirely — rely on the harness
auto-completion notification + **Read** the producer's progress file on demand; do NOT wrap a `cat/while/
sleep` loop in Monitor just to get periodic pushes. The durable fix is a typed `tt tail <path> [--interval]`
(or a harness-native "watch file" event source) so periodic progress needs zero bash. Highest-frequency
typed-tool gap found in this case study.

### Recurrence 2026-06-30: grepr bundled with wc/for/printf/echo/head to TALLY leverage
While scoping which candidate translation fixes are "big wins", the agent needed *match counts* (not the
matches themselves) for several Swedish short-leader words across two generated dirs (`slides-en`,
`compendium-en`). `tt text grepr` only emits the matching lines, so the agent fell straight back to a
dynamic-shell stack:
`echo "=== ... ==="; tt text grepr <dir> <ext> 'Exempel:' | wc -l; ... ; for w in 'Tips:' 'Notera:' ...; do printf '%s ' "$w"; tt text grepr <dir> <ext> "$w" | wc -l; done`
— i.e. `echo` section headers + `wc -l` counting + a `for` loop over patterns + `printf` labels + `head -6`
for a sample. BR flagged WR data. Root cause = the SAME momentum smell: a typed finder exists but has no
**count / tally** mode and no **multi-pattern, multi-root batch** mode, so the agent reaches for bash
plumbing to aggregate. This is a "measure leverage across the corpus" need that recurs constantly in the
override-grind (how many places would this fix touch?).
Candidate typed primitives: (1) `tt text countr <dir> <ext> <regex>` → just the integer (kills `| wc -l`);
(2) a batch form taking multiple regexes and/or multiple roots in one call, returning a small
`pattern  dir  count` table (kills the `for`/`printf`/`echo` scaffolding); optionally `--sample N` to also
show a few hits (kills `head`). Note also the allowlist already discourages `echo`/`head`/`cat` bundling —
this entry is a concrete case where the gap is an aggregation/count feature, not just discipline.

### Recurrence 2026-06-30 (same session, ~30min later): per-file build-status check via for/if/grep -q
Diagnosing which of 7 sibling PDF builds failed, the agent wrote a full bash control-flow stack:
`cd compendium && for f in compendium1 compendium2 solutions ...; do printf '%-14s ' "$f"; if grep -q 'Output written' "$f-console.log"; then echo "OK ($(grep -oE ... | tail -1))"; else echo "NO OUTPUT — $(grep -E '^! ' ... | head -1)"; fi; done`
— `cd`, `for`, `if/then/else`, `grep -q` (boolean), nested `$(grep -oE | tail)`, `printf`, `echo`. BR
flagged WR data. The `cd` also tripped the cwd-reset guard (shell cwd was reset afterwards), reinforcing
the bare-command rule. This is the **same aggregation gap as the grepr+wc entry above, one step up**: the
need is a per-file *status table* ("for each of these files: did it match X? if not, show first line
matching Y"). Candidate typed primitives: (1) `tt text has <file> <regex>` → boolean/exit-code (kills
`grep -q`); (2) a batch status form: given a list of files + a "success regex" + an "error regex", return
a `file  ok?  first-error` table in one call (kills the whole for/if/printf/echo loop). Until then the
discipline is: ONE bare command per file (Read the log, or a single grepr), no for-loops over files, no
`cd`. Aggregating across a *set of files* is now the clearest repeat-offender category (3rd variant:
count, then status-check).

### Recurrence 2026-06-30: worker-alive + GPU-status check via echo/pgrep/ssh/grep -v/head
While a long translate run was blowing the GPU fan, the agent repeatedly needed "is the worker process
still alive locally, and what is the remote GPU doing?" and wrote:
`echo "=== local procs ==="; pgrep -af 'at.scala|scala-cli|autotranslate' | grep -v grep | head; echo "=== GPU ==="; ssh bjornyx.local nvidia-smi --query-gpu=utilization.gpu,memory.used --format=csv,noheader | head -2`
— echo section headers + `pgrep` + `grep -v grep` + `head` + a remote `ssh nvidia-smi`. BR flagged WR
data and suggested a dedicated scratch tool. This is a DISTINCT, recurring need from the file-aggregation
ones: a **job/GPU health probe** during long autotranslate runs (checked many times per long run). Good
fit for a small typed scratch program, e.g. `gpu-status.scala` (and/or `tt gpu`): one call that prints
{local at.scala/scala-cli PIDs or "none", modly GPU util/mem/temp/fan via ssh, and the last line of
scratch/progress.txt} as a compact status block — no echo/pgrep/ssh/grep/head bundling. Pairs naturally
with the honest progress bar already in scratch/progress.txt (the right monitoring signal; the agent had
been polling a grep-filtered task-output file instead, which stays empty until the run ends — itself a
process-smell worth noting). Two concrete autotranslate-diagnostics asks emerged this session: (a) this
job/GPU probe, (b) a per-model-call key log so a runaway ("why 35 calls?") is instantly diagnosable.

### Recurrence 2026-06-30: passive "I'll wait for the notification" instead of instrumenting
Repeatedly, while a long run was in flight, the agent emitted filler turns ("I'll stop polling and wait
for the completion notification", "waiting for the run to complete") AND kept re-Reading a grep-filtered
task-output file that stays EMPTY until the run ends — so the reads were pure noise. BR flagged WR data:
the habit should be to **instrument and read the instruments**, not narrate waiting. The right signals
already exist this session: `scratch/progress.txt` (honest progress bar: %/model/cache/fb/ETA/current-file)
and the new `scratch/model-calls.txt` (per-model-call keys). Both are the correct thing to Read on a tick;
the grep-filtered stdout is the wrong thing. Notably, reading the INSTRUMENTS is what actually solved the
session's hardest bug: progress.txt revealed a runaway (model count climbing, stuck on one file), and
model-calls.txt revealed the ROOT CAUSE — the translate cache is keyed on the *masked* unit (`__C<n>__`
with GLOBAL placeholder numbers), so inserting an `\ifswedish` clamp renumbers every downstream placeholder
and cache-misses the whole rest of the file → a re-translation cascade (the repeated "why is the GPU fan
blowing" events trace to this). Lessons: (1) default to instruments (progress.txt / model-calls.txt /
the proposed gpu-status), never filler-wait + re-poll empty stdout; (2) don't pipe long-run stdout through
a tail-only `grep` and then poll it — either let it stream or rely on the progress file; (3) good
instrumentation pays for itself — the per-call log BR requested directly cracked a multi-hour mystery.

### Recurrence 2026-06-30: verify rendered-PDF text via cd/pdftotext/grep/head
To confirm a LaTeX fix (a `~` hard-space in a clamped heading) actually rendered, the agent wrote
`cd compendium-en && pdftotext compendium1-en.pdf - | grep -iE 'Exerciseexpression|Exercise expression|Exercise [a-z]' | head -5`
— `cd` + `pdftotext … -` (stdout) + pipe to `grep` + `head`. BR flagged WR data. Double smell: (a) the
bundle itself; (b) it didn't even work — the regex returned prose "exercise" mentions, not the SECTION
HEADING, so the check was inconclusive. The repo already has a pdftotext-based path (`--pdf-swedish`),
so the typed fix is right there: add a `--pdf-grep <pdf> <regex>` (or `tt pdf grep <pdf> <regex>`) mode
that runs pdftotext once and returns matching rendered lines — no cd/pipe/head, and reusable for every
"did X render?" check (headings, spacing, a specific translated string). Pairs with the earlier asks
(gpu-status, model-call log) as the third concrete autotranslate-diagnostics tool. Meta-pattern across
today's entries: the agent keeps hand-assembling shell pipelines for VERIFICATION/INSPECTION tasks
(count matches, per-file status, GPU health, rendered-PDF text) — each is a small typed tool waiting to
be written, and each bundle also tends to be subtly wrong (latin1 diacritics, wrong regex, stale logs).

### Recurrence 2026-06-30: git commit via cd && add && commit && push | tail (regression)
`cd <repo> && git add <files> && git commit -m "…" && git push 2>&1 | tail -1` — `cd` + `&&`-chained
add/commit/push + `| tail`. This REGRESSED from the established discipline used earlier the same session
(bare `git -C <abs-path> commit …` then `git -C <abs-path> push`). The repo rule is explicit: bare
`git -C <abs-path>`, no `cd`/`&&`/`;` chains (commit-no-claude-credit / introprog-build-and-sync). Note
the `cd` also triggers the cwd-reset guard each time. Clean form: `git -C <path> commit <files> -m "…"`
(commit takes pathspecs, so no separate `add` for tracked files) as ONE bare command, then
`git -C <path> push` as a second. The `| tail -1` on push is the only arguably-useful piece (push is
chatty); a typed `tt git push <path>` returning just the result line would remove even that. Root cause
is the same momentum smell as the rest: under flow, the agent reaches for `cd && a && b && c` instead of
two bare `git -C` calls. Cheapest fix is pure discipline (no new tool needed): bare `git -C`, never `cd`.

### Note 2026-06-30: confirmation guard FALSE POSITIVE on `<->` in a commit MESSAGE (not a shell reflex)
A clean bare `git -C <path> commit <files> -m "…"` got flagged by the confirmation guard as "Contains zsh
<N-M> numeric-range glob". Cause: the commit MESSAGE prose contained `Swedish<->English`, and `<->` is a
zsh glob token (`<->` matches any non-negative integer; `<1-10>` is a numeric range). The guard does a
STATIC scan of the whole command string and matches `<->` regardless of quoting — even though it sits
inside a double-quoted `-m "…"` argument where zsh would never expand it. BR asked, reasonably, "why does
it think you reflex into zsh? this is a clean git -C" — answer: it's the message text, not the command.
Takeaways: (1) DISCIPLINE — keep glob-looking punctuation out of command strings, especially commit
messages: write `Swedish/English` or `sv to en`; avoid `<->`, `<N-M>`, bare `*?[]{}` in -m text.
(2) The guard could skip quoted `-m`/message args (treat commit bodies as opaque) — this false positive
recurs for any commit message using `<->`/`<n-m>` range notation. Cosmetic, no risk, but the kind of thing
that stalls an away-from-keyboard run if it needs confirmation.

### Recurrence 2026-06-30: for-loop over files to pull override keys (4th of the aggregation kind)
`for f in w07-sequences-exercise w10-inheritance-exercise; do echo "=== $f ==="; grep -A1 "sv-fallback | …/$f" override-suggestions.txt | grep 'SV |' | grep -vE '⏎|begin{|code|' | head -8; done`
— a `for` over a file set + `echo` headers + chained `grep | grep | grep -v | head`. BR flagged WR data.
This is the SAME "aggregate/extract across a SET" category already logged 3× (count, status-check, this).
It recurs specifically in the override-grind loop: "for each target file, show its clean single-line
fallback keys". The clean end-state is a typed query over override-suggestions.txt, e.g.
`tt sweep keys <file> [--single-line]` returning that file's clean override keys (no `for`/`echo`/`grep`
pipeline, and it could honor the ⏎/code filters as flags). Until then: ONE bare grep per file (no loop),
or operate on the whole file and read it. The aggregation-across-a-set category is now the dominant,
most-repeated friction of the session — strongest signal for a small typed "query the sweep / a dir / a
file set" tool.

### Recurrence 2026-06-30: piped a scratch's stdout through `| grep -v | tail` to suppress noise
Running a brand-new scratch (`token-usage.scala`) for the first time, the agent wrote
`scala-cli run token-usage.scala 2>&1 | grep -v -iE 'warning|deprecat|sun.misc|maintainers|^$' | tail -20`
to hide scala-cli/JVM noise (Unsafe warning + "outdated dep" hints). BR flagged WR data: *"you did a pipe
to grep; should use some scala tool."* DISTINCT from the aggregation category — there was nothing to
aggregate; the `grep -v` was **pure cosmetic noise-suppression of a tool the agent controls**. Correct
moves: (a) run the scratch **bare** and just Read the output — the warnings are harmless and the scratch
prints its own clean summary; (b) if the noise bothers, fix it at the SOURCE. Fixes applied immediately:
bumped the scratch's deps (os-lib 0.11.8, ujson 4.4.3) to silence scala-cli's outdated-hints; the JVM
`sun.misc.Unsafe` warning is on stderr and is simply ignored. Discipline: **never pipe a scala-cli scratch
through grep/tail to clean it up** — run bare + Read; if it's noisy, fix the scratch, don't add shell.
genscalator angle: a `tt run <scratch.scala>` runner that executes a scala-cli file and returns only its
stdout (warnings stripped) would remove the temptation entirely — exact same shape `sbt-task.scala` already
provides for sbt tasks. (Meta: this is the FIRST WR event whose tool — `token-usage.scala` — is itself a
token-introspection instrument; the friction was in *launching* it, not the tool.)

### Recurrence 2026-06-30: repo-overview (list md + recent commits) via cd && ls && echo && git log
Prepping the genscalator commit, the agent wanted "which top-level `.md` files exist + the last few commits"
and wrote `cd <repo> && ls *.md && echo "---recent log---" && git log --oneline -8` — `cd` + `&&` chain +
`ls` glob + `echo` separator. BR flagged WR data (*"we might still need a tool that helps you do what you
want without the cd echo stuff"*) and confirmed the **goal** was good (he wants to see commits) — only the
**means** is the smell. Notably the agent had used a clean bare `git -C <abs-path> status --short` just ONE
call earlier, then regressed to `cd && … && …` — same momentum-regression as the git-commit entry above.
This is a **repo-orientation** need (distinct from the per-file-aggregation category): "give me a compact
snapshot of repo X" = {changed/untracked files, last N commits, maybe tracked top-level docs} in ONE bare,
allowlistable call against an **arbitrary path** (not cwd). Candidate typed primitive: **`tt git overview
<path> [-n N]`** (or `tt repo status <path>`) → one block: `git -C` status (short) + last N `--oneline`
commits + optionally a doc-file list — no `cd`, no `echo` header, no `&&`. Until then the discipline is the
established one: bare `git -C <path> status --short` and bare `git -C <path> log --oneline -8` as **two
separate calls** (the harness shows both outputs); `ls` of tracked docs → `git -C <path> ls-files '*.md'`
bare, or just the editor's directory view. Root cause unchanged: under flow the agent bundles `cd && a && b`
instead of issuing each read-only probe as its own bare command. Strengthens the case for a tiny typed
`git`/`repo` overview tool, since "show me the state of repo X" recurs at every commit/checkpoint.

### Recurrence 2026-06-30: file-inventory by ext/dir via `find <tree> -name '*.ext' | wc -l`
Scoping the B0+D glossary task, the agent wanted "how many .scala/.java under compendium/examples/ and
workspace/, and which example subdirs exist" and wrote a `find … -type d | head` + three
`find … -name '*.scala' | wc -l` calls bundled with `echo` separators. BR flagged WR data and asked whether
the agent should **build a scratch tool for such searches before going further**. This is the
**per-tree-aggregation** category (same family as the dominant `grepr | wc -l`/`for/if/grep -q` friction):
"count/list files matching <ext> under <subtree>, grouped" in ONE bare call. Candidate primitive:
**`tt files count <dir> --ext scala,java [--by-dir]`** (or fold into `tt repo overview`): walks a tree,
counts/lists by extension and optionally per-immediate-subdir, no `find|wc|head|echo`. **Agent judgment
(recorded for honesty):** for THIS task the genuinely needed tool is the *task-specific* glossary
**identifier-harvester** scratch (walks the same trees and collects Swedish identifiers) — building that
subsumes the ad-hoc `find`s, so a *separate generic* file-survey tool is NOT worth a mid-task detour here;
it belongs in genscalator `tt` (already backlogged) where BR greenlights tools. The harvester scratch will
also PRINT its scanned-file inventory, so the survey need is absorbed, not re-shelled. Lesson reinforced:
when a bash bundle appears, the right fix is often the *real* task tool I was about to write anyway, not a
new generic utility — but log the generic pattern so `tt`'s next tool is chosen from evidence.

### META 2026-06-30: agent failed to SELF-TRIGGER the tool reflex (human supplied the trigger) — twice
Sharper than the per-pattern entries above. After BR's first nudge ("would you benefit from building a
scratch tool?"), the agent logged + built the harvester — but BR then made the deeper point: *"you did not
realize yourself that you would be better off building a scala-cli statically-checked tool just now."* The
failure is **metacognitive**, not a missing tool: the agent had (a) the `prefer-scala-scratch-over-bash`
memory AND (b) was about to write a scanner scratch anyway, yet still reached for `find|wc|head|echo` to
"orient." It mentally EXEMPTED orienting probes as throwaway, so the tool reflex never fired without the
human. Two compounding insights:
1. **Static-checking is a correctness lever, not just TE/CF.** A scala-cli scratch is type-checked +
   round-trip-validatable (the harvester reuses `Latex.mask` and self-checks); a bash bundle is unchecked
   string-slinging that fails/misleads at runtime (same failure class as the earlier `--only` mirror
   clobber and a garbled `println` the agent had to fix). The agent under-weights this and treats bash as
   "free" for small probes when it is actually the riskier option.
2. **The trigger is the bottleneck, and it is external.** The reflex to tool-up fired only when BR provided
   it. This is the `instruction-adherence-decay` thesis in its purest form: the trained bash-prior wins on
   "small" probes because nothing structural intercepts the call; a per-call memory is re-sampled and loses.
   The agent cannot reliably self-trigger by willpower — so the fix must be a **submit-time structural
   check** (the HUMANS.md item-D hook): before any `find`-aggregation / pipe-chain / `&&`-bundle / `for`
   analysis command is emitted, intercept and require "is this a typed scratch/`tt` call instead?". This is
   the strongest evidence yet that genscalator's safe-by-design value should include a **tool-up trigger**,
   not just safe tools — the tools exist (or are cheap to write); the missing piece is the reflex to choose
   them, which only structure (hook/allowlist friction on raw bash bundles) can supply reliably.
Action taken: tightened the `prefer-scala-scratch-over-bash` memory to remove the "orienting probe"
exemption (ANY multi-part bash analysis bundle is the smell). But per (2), a memory edit is the weak fix;
the durable fix is the submit-time hook — flagged for BR's item-D decision.
