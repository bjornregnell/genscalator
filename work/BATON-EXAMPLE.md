# BATON-EXAMPLE: a real resume baton, verbatim

This is a real specimen of the **baton** artifact from the genscalator exit+resume dance: a short
file the outgoing agent writes just before session exit, and the incoming agent reads at cold
start. A baton is deliberately a *pointer to durable truth* (committed files, memory, the pin
board), not the truth itself: the fresh agent is instructed to verify before trusting, and to read
only what it needs.

**Provenance:** written 2026-07-19 by the outgoing agent immediately before a planned machine
update and reboot (the box was memory-swamped). Consumed the same evening by a fresh agent
instance, which reconstructed the working state from it with zero human re-explanation: cleared
the stale mode label, verified pushes on both remotes, confirmed box health, and correctly kept a
held work queue on hold. In other words: this baton is published because it *worked*.

**Redaction note:** one colleague's name is replaced with `[colleague]` throughout; nothing else
is altered. The specimen is otherwise verbatim, including its em-dashes, abbreviations (PB = pin
board, SM = smart-move pin, WR = workflow research, tt = the typed toolbox, CF5/O4 = model names)
and its terse register: batons are written agent-to-agent, not for human readers.

---- BEGIN VERBATIM BATON SPECIMEN (2026-07-19) ----

⛔ THIS IS A BATON: a pointer to durable truth, not the truth. The truth is in committed files + memory. Verify before trusting; read only what you need (bounded reads, SM153).

# Resume — post box-update+restart (written 2026-07-19 ~17:2x by CF5 day-1, pre-exit)

## 0. Anti-regression checklist — READ FIRST (these reflexes regress at turn zero)
FORBIDDEN → ALLOWED:
- raw `grep -r`/`find`/`ls|head` → `tt text grepr <ABS-dir> <ext> <regex>`, `tt files`
- raw `curl`/`wget` → `tt web get <url>`
- `> file` / heredoc → the **Write tool**; `| head/tail/wc` → run_in_background + Read
- `cd repo && git` → `tt git commit --repo <abs> --message-file <f> --add <p> --push`; fallback push `git -C <repo> push github`
- compound `a && b` / `a; b` → ONE bare command per call; metachars OUT of quoted args
- mode toggles: **`tt mode add/rm <label>`**, NEVER Write-tool on ~/.claude/gs-modes (guard-stalls; 2nd tt-first-drift specimen 07-19)
- introprog sbt: **`env --chdir=/home/bjornr/git/hub/lunduniversity/introprog sbt --client "<task>"`** (bare `cd` is blocked; SM167 = future `tt sbt --repo`)
- kill a JVM: **`jps` → `kill -9 <pid>`** (`pkill -f BloopServer` MISSES — cmdline lacks the class)
- Swedish in artifacts/commits/sub-agents → ENGLISH (chat may be Swedish)
- NEVER blanket-allow rm/curl/interpreters/settings-self-edit. Commit+push every unit.

## 1. English-side Swedish-% scoreboard — ⏸ ON HOLD (BR 2026-07-19: "we wait for [colleague] to finish his contribution streak"; 3.4% baseline is good). Do NOT start unprompted; the queue below runs on BR's go once the hold lifts.
The `--all` translation ran CLEAN pre-restart (1m42: model 8, fallbacks 8, overrides 380, cache 15270; 197+96 tex; mirror fresh on disk; modly was DOWN, local ollama qwen2.5 covered). Remaining, on the healthy box (BR's go):
1. `pdfCompendiumEn`, `pdfCompendium1En`, `pdfCompendium2En`, `pdfSlidesEn` (bare = all decks) — via the env-chdir sbt shape; **each *En task SELF-REPORTS its Swedish-%** (build.sbt `reportSwedishPct`).
2. Corpus gauges: `--prose-swedish` (the allowed/glossary vs true-leak split). **Baseline already measured pre-restart: `--swedish-left` = 3.4% (440/12761 lines, 94 files); top offenders = the w01 intro cluster (34+14+14+12), kojo-commands 23 (GATED on the deferred English-Kojo dep — remind BR), w06-patterns 18.**
3. Deliver a SCOREBOARD in-feed: per-PDF %, corpus %, the `--prose-leaks` ranked worst-offenders = "what stands between us and ~0%".
4. After compendium-en.pdf exists: re-run `gen` (headingsEn was skipped for lack of it) — and BR has a muntabot `publish.sh` TODO from his Swedish build.

## 2. Where we are (day-1 of CF5-as-super-agent went WELL)
- **[colleague]/introprog arc CLOSED 07-19:** #942 calls posted (awaiting [colleague], their weekend); **#943 MERGED** + all three review follow-ups pushed to master (`03251287` cache purge, `8fcb349d` scratch/glossary-blast-radius.scala — ids mode verified ZERO collateral, `f6939418` loud unknown-flag abort) + BR's comment posted; #944 answered. `renderCodeIds` copies string literals verbatim (the #944 edge is handled).
- **SM163 box line BUILT+WIRED+DOCUMENTED** (live line 3: `box healthy/huffing/swamped`, 11-char leads; `--box-line` in the work repo settings statusLine; docs/gs-help/skill/welcome updated). f5/1M model tag = measured `context_window_size`.
- Housekeeping done: MEMORY.md compacted (~17.5KB), version + model-warp memories corrected.
- New pins (PB NOW): **SM165** (real cpu% via /proc/stat delta), **SM166** (`gs kill` over ask-gated `tt box kill`), **SM167** (`tt sbt --repo`), SM164 addendum (Workflow tool as the O4/F5 A/B vehicle — per-agent model overrides!), SM074+SM160 addenda.
- WR specimens 07-19 (SM164 informal stream): 2× tt-first drift, jps TIL (human-learns-by-watching), spinner-tip attention tax, BR-subjective-CF5-sharper (pre-registered for the A/B), felt-time stamp failure (corrected in PB).

## 3. Box / session hygiene at cold start
- Box was UPDATED + REBOOTED (memory was swamped) — expect `box healthy` on line 3; if not, believe the line.
- `~/.claude/gs-modes` survives the reboot and carries stale modes. **DO at cold start (BR pre-authorized): `tt mode rm rot-vigil`** (a fresh start is un-rotted; the declared line must not lie) — via `tt mode`, NEVER the Write tool. Leave `tok-spend` (budget mode, orthogonal to restart) unless BR says otherwise. Report the clear in-feed so BR sees it happened.
- Codeberg FLAPPED all day (GitHub fallback ~8×); at exit both work repos were synced on BOTH hosts; introprog pushed (`f6939418`). Verify pushes before trusting.
- BR may say `do Q-test` (fresh-restart fidelity, tmp/p3-probes-only.md).

## Substrate map
`PIN-BOARD.md` top block (roadmap P0–P3 + today's corrections at NOW-top) · `MEMORY.md` · `genscalator/research/wr-data/` · this file's §1 is the work queue.

---- END VERBATIM BATON SPECIMEN ----

## Why the baton looks the way it does

- **The anti-regression checklist comes first.** Fine-tuned reflexes (which tools to prefer, which
  command shapes stall a permission guard) regress to base-model defaults at turn zero of a fresh
  context. Stating them as FORBIDDEN → ALLOWED pairs at the top re-installs them before the first
  tool call. See the resume-prompt anti-regression pattern in the research notes.
- **Holds are stated with their reason and their release condition.** "Do NOT start unprompted"
  plus who lifts the hold prevents an eager fresh agent from resuming work the human paused.
- **Numbers over adjectives.** Commit hashes, percentages, counts and timings let the fresh agent
  verify the claimed state against git and the filesystem instead of trusting prose.
- **Pre-authorized cold-start actions are marked as such**, so the fresh agent knows which state
  changes it may perform before the human has said anything.
- **The substrate map closes the file:** where durable truth lives, in priority order.
