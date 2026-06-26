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
