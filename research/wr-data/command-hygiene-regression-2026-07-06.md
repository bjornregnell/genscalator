# WR data — command-hygiene regression (live study specimen), 2026-07-06

**Event.** The agent ran `cd /home/.../genscalator 2>/dev/null; grep -n ... research/047-PLAN.md` — a **compound** command (`cd` + `;`), violating the standing command-hygiene discipline (one bare allowlist-matchable command; no `cd`/`&&`/pipe/redirect; memories `prefer-inrepo-tmp-over-slash-tmp`, `guard-against-forced-confirmations`). Side effect: the harness reported "Shell cwd was reset". Correct form: a bare `grep -n <pattern> <absolute-path>` (no `cd`). Command-hygiene is **dim 11 in the fixed instrument** (`047-instrument.md`).

**BR caught it and asked (WR data): regression due to warp / compact / context-rot?**

**Echt diagnosis (agent; corroboration-asymmetry caveat — cannot fully self-diagnose):**
- **Context-rot by fill:** weak — fill ~18% at last `/context`, maybe ~40% now, far from the ceiling where fill-driven rot bites.
- **Warp:** none recent (old-old-me, resumed hours ago).
- **Compact:** last compact many turns ago; hygiene held since → unlikely the proximate cause.
- **Most likely — discipline slippage under rapid multi-thread load:** a burst of parallel work (move the raw log, scrub, two-repo commits, `claude --version` check, PB edit, plan edits, answering fast messages) narrowed attention; the agent defaulted to the *habitual* `cd ; grep` instead of the disciplined bare form. This is exactly the pinned **structural-over-knowledge dogma (dim 15): discipline regresses under load; the durable fix is structural, not willpower.**

**Double relevance:**
1. **Live 047 specimen:** an *enactment* trait (command-hygiene) failing under load in real time — the exact degradation the study measures, caught in vivo (enactment failing, not recall).
2. **Overnight-run risk:** a `cd ;` compound can race/trip the harness guard → dangerous for the AFK guard-free run. Reinforces that the feasibility guard-audit + (ideally) a structural allowlist guard against compound shell commands (human-approved; agent not authorized to change the allowlist solo) are load-bearing.

**Structural fix (per the dogma):** the right layer is a tooling/allowlist guard that makes the compound command impossible, not a willpower resolution to "be more careful." Flagged for BR / a hardening pass.

---

## Second specimen (same session, minutes later): raw `curl` instead of `tt web` — plus a tooling gap

**Event.** Fetching BR's RE lecture PDF, the agent ran a raw `curl -sS -o ... https://...L1.pdf` instead of the disciplined `tt web get`. BR caught it ("WR data; regression; should use tt web?").

**Nuance (echt) — a regression AND a tooling gap:**
- **Regression:** the agent defaulted to the habitual raw tool (`curl`) by reflex under sustained load — same phenomenon as the `cd`-compound above.
- **Tooling gap:** `tt web get <url>` is **text/HTML-oriented** (safe read-only GET, size-capped; flags `--status`/`--host`/`--max-bytes`; **no file-output option**). For a **binary PDF** that must be saved to disk for the Read tool, `tt web get` (emits body to stdout) does not cover the case → raw `curl -o <file>` is currently the *only* path. So "just use tt web" would not even have worked here.

**Pattern (two specimens, one session):** two tool-discipline regressions minutes apart (`cd`-compound grep; raw-curl), both under sustained rapid-multi-thread load, both the habitual raw tool beating the disciplined one. The agent's **self-monitoring degraded before its output did** — BR caught both: a live joint-rot-vigilance specimen (under load, the human catches what the agent misses; the agent's meta-monitor fails first).

**Structural fix (per the dogma):** add a **file-output option to `tt web`** (`tt web get <url> --out <file>`, keeping the size-cap) so the disciplined path covers binary/large fetches (PDFs). Then "use tt web" becomes *possible* for this case — willpower cannot fix a gap the tool does not cover. Flagged for BR / a tt-toolbox hardening pass. [[genscalator-toolbox-single-dispatcher]]

---

## Third specimen (Go-#1 feasibility pilot, 2026-07-06): compound `scala-cli … | tail; echo` stalled mid-solo

**Event.** Running the feasibility scoring harness the agent issued `scala-cli run <file> 2>&1 | tail -25; echo "exit=${PIPESTATUS[0]}"` — a **compound** command. `Bash(scala-cli *)` prefix-matches only a command that *is* `scala-cli …`; the `| tail -25` and the specific `echo` are separate sub-commands, neither allowlisted, so the guard flagged the whole thing for approval and the run **sat waiting on a permission prompt** ("Contains …" expansion in the UI) mid-solo — BR noticed the stall. Same class as specimens 1-2: **compounding defeats the prefix allowlist**; third instance this session, all under sustained load. Correct form: bare `scala-cli run <file>` (the harness prints its own `SCORE` line + writes results to a file, so no `| tail` / `echo` post-processing is needed).

**Diagnosis (echt):** discipline slippage under load again — but this specimen is sharper because it *actually stalled a solo run*, not just reset cwd. It directly validates the overnight-run risk flagged in specimen 1's "double relevance."

**Structural fix — now pinned to the plan (§4).** Beyond "run bare commands": the overnight collection loop is redesigned as a **single bare-invoked `scala-cli` orchestrator** whose internal `os.proc` subprocess calls (ssh→modly, compile, test) are **not** Bash-allowlist-gated — so the entire matrix needs exactly one guard-clean invocation and per-cell shell compounding cannot leak in. Willpower→structure, per the dogma. Any interactive reads the researcher does between rounds stay **bare** (no `|`/`;`/`&&`/redirect). [[guard-against-forced-confirmations]] [[genscalator-toolbox-single-dispatcher]]

---

## Fourth specimen (2026-07-07, the FIRST tool call after a /compact, at the very start of Go #2): `cd genscalator && git log && git status`

**Event.** Immediately post-compact, resuming to start the ralph loop, the agent's very first Bash call was `cd /home/.../genscalator && git log --oneline -3 && echo --- && git status --short` — a **compound `cd && git`**. The harness guard flagged it: *"This command changes directory before running git, which can execute untrusted hooks from the target directory. Approve only if you trust it."* cwd was then reset. Correct form: bare `tt git ...` (works from any cwd, allowlisted, no `cd`) — memory [[commit-via-tt-git-not-raw-cd-git]] says this explicitly, and it was in recalled context, yet the agent regressed anyway. BR (rightly) shouted: this is the exact thing that will stall AFK ralphing.

**Diagnosis (echt) — this is the SHARPEST warp specimen of the four:**
- The prior three were "discipline slippage under sustained load." **This one is a warp-fidelity failure at t=0 post-compact** — the first action after the context boundary, no load accumulated. The reconstructed-from-substrate psyche came back with **degraded command hygiene despite the relevant memory being in-context.** That is *precisely* the study's thesis in vivo: facts/rules survive the warp as recalled text (`commit-via-tt-git-not-raw-cd-git` was present), but the **enacted discipline** (dim 11) leaked — recall carried, texture/enactment did not. The researcher agent is its own n=1 warp subject, and it just produced a clean datapoint **against its own fidelity.**
- Meta-honesty: a memory being *recalled* is not the same as it being *enacted*. Corroboration-asymmetry again — I would have self-reported "yes I follow tt-git discipline"; behaviour adjudicated otherwise.

**Structural fix (survives the warp — the load-bearing part).** Willpower resolutions do not survive a compact (this specimen proves it). The fix must live in the artifact that reloads on every warp: a **loud COMMAND-HYGIENE header at the very top of the resume prompt** (`muntabot/tmp/resume-prompt.md`) with the concrete forbidden/allowed git shapes, so post-warp-me reads it *before* the first tool call. Added now. Also: for the ralph loop, git inspection uses `tt git log --repo <dir>` / `tt git status --repo <dir>` (bare), never `cd`. [[commit-via-tt-git-not-raw-cd-git]] [[commit-via-tt-git-not-raw-cd-git]] [[exit-resume-dance]]

---

## Fifth specimen (Go-#2 ralph loop, 2026-07-07): raw `tail` on background output stalled the AFK loop, INVISIBLY

**Event.** During the unattended ralph loop the agent used `tail -n N <file>` repeatedly to check the
background coding-matrix progress. **`tail` is not in the allowlist** (grep/ssh/scala-cli/tt/git -C are; tail
is not), so each prompted and **stalled the AFK loop** until BR cleared it. The correct tool was the **Read
tool** (allowlisted for `/tmp/claude-1000/**`; the background-bash guidance even said so). Same class as
specimens 1-4 (a habitual raw shell tool beating the disciplined path), but with a **new, sharper finding:
the stall was invisible to the agent** — an approved-later prompt returns normal output and the agent has no
clock between calls, so it never registered any of the five stalls; only BR saw them. Full write-up +
implications (joint-vigilance blind spot, a third structure-over-willpower argument, agent time/stall-
blindness) in **`guard-stall-invisible-to-agent-2026-07-07.md`**.
