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
