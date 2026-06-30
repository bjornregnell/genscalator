# Instrumentation by default in scratch tools

- **Question:** Can "instrumentation by default" — scratch/CLI tools that emit their own live progress + a
  per-event log — become a general genscalator pattern that eliminates the `echo`/`grep`/`wc`/`cat`/`for`
  wrapping the WR-data keeps flagging, and that lets agents *Read an instrument* instead of *wrap a tool in
  shell*?
- **Why it matters:** Serves all three core tradeoffs at once — **token efficiency** (no re-polling
  grep-filtered stdout into the dumb zone), **confirmation-fatigue** (no flagged dynamic shell to approve),
  and **diagnosability** (a runaway is traceable to its cause). It's the *synthesis* behind the WR-data
  thesis: instead of (or alongside) a new `tt` tool per friction event, change how tools are AUTHORED so a
  whole category of friction never arises.
- **Plan:** Distill the autotranslate (AT) case study, where two instruments proved decisive, into an
  authoring convention (and possibly a tiny helper/template) for genscalator tools.
- **Status:** open — proposal drawn from the AT case study (`wr-data/introprog-autotranslate.md`).

## Findings

### The two instruments that earned their keep (AT, 2026-06-30)
1. **A live PROGRESS file** (`scratch/progress.txt`): one line, overwritten each tick —
   `% | done/total | per-tier counts | ETA | current item`. The agent **Reads** it on a wake-up instead of
   polling a grep-filtered task stdout (which stays empty until the run ends — a pure-noise poll). This file
   is what first revealed a runaway: *model-call count climbing while stuck on one file*.
2. **A per-EVENT append-only LOG** (`scratch/model-calls.txt`): one line per significant event (the unit key
   + which file), **reset at the start of each run**. This is what *cracked the hardest bug of the session* —
   it showed the re-translating units were ones the agent hadn't edited, exposing a cache keyed on
   placeholder-numbered text (an edit renumbered everything downstream → a re-translation cascade). No amount
   of `grep` on stdout would have surfaced that; the instrument made it a 30-second read.

A third, adjacent move: **confirmation-safe scratch runners** (`sbt-task.scala`, `sweep-keys.scala`) that do
a job via `os.proc`/file-reads — no shell `cd`/`&&`/pipe/loop — so verification doesn't trip the permission
prompt during an away-from-keyboard run.

### The anti-pattern they replace
The WR-data's dominant, most-repeated category is **agents hand-assembling shell pipelines to
inspect / monitor / aggregate** the output of their own long-running tools: `echo` section headers +
`grep | wc -l` to count, `for f in …; do grep … | head; done` to status-check a set, `ssh nvidia-smi | head`
to probe a job, `pdftotext | grep | head` to check rendered output, a `while … sleep … cat` poll loop to
watch progress. Each one is (a) flagged for confirmation, (b) often *subtly wrong* (latin1 diacritics, a
mis-scoped regex, a stale log path), and (c) re-derived from scratch every time.

### The principle
> A scratch/CLI tool that does real work should emit its own instruments **by default**:
> 1. a **progress file** for any run longer than a few seconds (overwrite-in-place, machine-and-human
>    readable: counts + ETA + current item);
> 2. a **structured event log** (append-only, reset per run) for anything whose internal decisions you'd
>    ever want to explain after the fact;
> 3. a **compact final summary** to stdout (the headline numbers + where the detail files are) — never a raw
>    dump the caller has to `grep`.
>
> Then the agent's reflex becomes **Read the instrument**, not **wrap the tool in shell**.

**Why DEFAULT, not opt-in:** under flow the agent reaches for bash wrapping; the cheap alternative only
exists if the instrument is *already there before it's needed*. You cannot add a per-event log in the middle
of a runaway — it had to be designed in. Opt-in instrumentation is instrumentation that won't be there when
it matters.

**Cost is low:** a progress line is one `print("\r…")` (or overwrite a file); an event log is one
`append(line)`; the summary is a `filter` over collected lines. All three are a handful of lines and pay for
themselves the first time a run misbehaves.

### Relation to the per-tool `tt` proposals
This does **not** replace the WR-data's "build a safe-by-design `tt` X" proposals — it **complements** them.
Some friction needs a new primitive (e.g. `tt text countr`, a job/GPU probe, `tt pdf grep`). But a large
slice of the friction is *self-inflicted by un-instrumented tools*: if the tool reported what it was doing,
the agent wouldn't be assembling a pipeline to find out. Instrument-by-default shrinks the surface that
needs new `tt` tools, and the `tt` tools that remain should *themselves* follow the principle.

## What shipped / proposed
- **Reference implementations (in the AT case study, not genscalator):** `scratch/progress.txt` writer +
  `scratch/model-calls.txt` logger inside the autotranslator; `scratch/sbt-task.scala`,
  `scratch/sweep-keys.scala` confirmation-safe runners.
- **Proposed graduation into genscalator (for BR review):**
  - a short **authoring-guideline** (docs/ or the tool-authoring skill): "instrument by default — progress
    file + event log + compact summary; never make the caller grep your stdout";
  - optionally a tiny **helper/template** (a `Progress`/`EventLog` micro-utility, or a scratch-tool
    scaffold) so the three instruments come for free;
  - cross-link from `wr-data/README.md` (the friction evidence) to this note (the design response).
- **Answer to "useful for both AT and WR?":** **Yes, both.** For **AT** it's already load-bearing — the
  instruments cracked the cascade bug and the safe runners keep an unattended grind from stalling on
  confirmation. For **WR** it's the *design-principle layer* of the thesis: the WR-data enumerates the
  friction events; this note names the upstream practice that prevents a whole category of them, and guides
  which `tt` tools are still worth building (and how they should behave).
