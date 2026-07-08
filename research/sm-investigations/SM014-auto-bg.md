# SM014 — Auto-bg in TSM: investigation + recommended design

- **Task:** auto-launch an AFK-safe **bg task** when (token-mode == **spending**) AND (BR silent > **X** min).
- **Status:** investigation REPORT (agent-drafted 2026-07-09, CF5 sub-agent). **Recommends only — enables nothing.**
  Enabling auto-bg is a **BR-review autonomy escalation** (the AUTO version of the SM011 repertoire).
- **Grounding:** PIN-BOARD SM014 spec; `research/039` (wall-clock blind spot);
  `wr-data/session-logging-existing-capture-2026-07-08.md` (SM013a transcript map); the live tool schemas
  probed from this session (Monitor verified; ScheduleWakeup/Cron NOT found here — flagged below).

## (a) The hard part: no idle / wall-clock awareness

The agent cannot sense elapsed time or "human silent for X min" (`research/039`: no clock between tool
calls; even an 11-minute crunch and repeated guard-stalls were perceptually invisible). Every candidate
trigger below fires on **elapsed time**, not sensed idleness — so the design question is which mechanism
best *approximates* idleness by checking for intervening user input around a timed fire.

| Mechanism | Fires on | Can it approximate idle? | Verdict |
|---|---|---|---|
| **Monitor tool** (verified available in this session; schema loaded) | a **condition** — its script's stdout lines become in-chat events that re-invoke the agent | **YES, best.** A poll loop can `stat` the session transcript (`~/.claude/projects/<slug>/<uuid>.jsonl`, appended on every user/assistant/tool turn — SM013a) and emit ONE event when its mtime is stale > X min. "Transcript quiet" = *nobody* (human or agent) produced a turn = exactly the joint-idle condition auto-bg wants. Near-zero memory (sleep+stat), safe on the flaky box. | **RECOMMENDED** |
| **ScheduleWakeup** (semantics per task brief: delaySeconds clamped [60, 3600], fires a prompt back; **not found in this sub-agent's tool list** — verify in the super-agent session) | timer only | **Partially** — "wake-and-check": schedule X min out; on fire, inspect the conversation/transcript for intervening user input; if BR spoke, stand down; else run one bg unit; reschedule. Wakes even when BR is active (wasted fires + context noise). Prompt-cache TTL of 5 min: any delay > ~300 s lands as a cache miss (full context re-write cost per wake) — spending-mode absorbs this, but it argues against very frequent long-delay wakes. | Fallback #1 |
| **/loop skill** (verified in skill list) | fixed/self-paced interval, re-prompts every tick | Same wake-and-check as ScheduleWakeup but **noisier**: it keeps the session hot continuously, injects a prompt every interval regardless of BR's presence, and each tick risks landing while BR types. | Fallback #2, not preferred |
| **Cron (CronCreate/List/Delete)** — **not found in this environment** (ToolSearch: no match); the /schedule skill (cloud routines) exists | wall-clock schedule | **No.** Wall-clock fires regardless of idleness; /schedule routines run in a **remote cloud environment**, not on blixten where the SM011 repertoire's substrate (repos, box health, wr-data) lives. Wrong substrate + wrong trigger. | Reject |
| **Hooks** (SessionStart / Stop / UserPromptSubmit; via settings.json = BR-approved change) | harness lifecycle events | **Measurement only.** A Stop + UserPromptSubmit hook pair could timestamp "agent stopped" / "user spoke" (true idle *measurement*), but no hook can deliver a **delayed wake back into an idle session** — that channel is exactly what Monitor/ScheduleWakeup provide. Hooks could later harden the design (external idle ledger), not bootstrap it. | Not a trigger |

**Crux resolution.** Monitor is the least-bad by a clear margin because it is the only mechanism whose fire
is **condition-gated** (transcript staleness), not purely elapsed — it wakes the agent *only when* the
joint-idle condition already holds, so the "check for intervening input" step becomes a cheap re-verify
instead of the primary filter. Known caveats: (1) transcript **flush-lag** (SM013a) — the latest turn lags
the jsonl write; harmless at minute granularity. (2) **Unverified in the wild:** whether a Monitor event
actually re-invokes a fully quiescent session waiting on user input — tool docs strongly imply yes
("notifications arrive in the chat", background exits "re-invoke you"), but run a cheap 2-minute BR-present
probe before relying on it. (3) The wake itself appends to the transcript, resetting the mtime — so the
monitor must emit **once and exit**, and the agent re-arms it explicitly after each cycle.

## (b) Guardrails

1. **Repertoire whitelist, re-verified at fire time.** Only SM011 items that pass the **AFK-strict** filter
   (bare allowlist-matchable commands, no compound shells, no guard-trippable steps — a stalled permission
   prompt is invisible to the agent per `research/039` and would sit stealing BR's input focus). Safety
   bands are stale snapshots: re-check each item against *current* state at wake (the `go afk` rule),
   value-gate it (value ≥ tokens), and skip if in doubt.
2. **Prompt-race-free.** No AskUserQuestion / modals ever (standing rule). On wake: check the kill-switch,
   check token-mode is still **spending** (mode is board/context state, not sensed), and check no user
   message raced in; any check fails → disarm silently and log one line.
3. **Box-crash containment (blixten is flaky; OOM = unrecoverable until BR power-cycles).** Auto-bg units
   must be **lightweight** — read/analyze/report tasks (consistency sweeps, index-rot checks, wr-data
   mining, box health reads); **no scala-cli/JVM builds, no sub-agent fleets** in auto mode. One unit per
   wake. **Commit+push via `tt git commit` after every completed unit**, working tree clean between units —
   a crash at any instant loses at most the in-flight unit, never leaves half-done state.
4. **Not-dead-proof.** The unit runs as a background command with a completion notification; the monitor's
   filter covers failure states, not just success (silence is not success). A wake that finds a dirty tree
   or a failed prior unit does **recovery-only** (commit or revert to clean), then disarms and flags.
5. **BR can disable — default OFF.** Kill-switch = a one-line flag on the PIN-BOARD (`auto-bg: OFF/ARMED`),
   checked at every wake; BR saying "stop" or TaskStop on the monitor kills it instantly; the monitor dies
   with the session anyway (no orphan schedulers — an advantage over cron).
6. **Audit + report on return.** Every auto-run appends one line to a bg-log (what/why/commit-sha); the
   session jsonl is the full audit trail (SM013). On BR's first message after any auto-runs, lead with a
   one-screen summary before resuming his topic.

## (c) Recommendation

**Design: the "armed idle-watch" (session-scoped, per-arming BR-gated).**

1. **Arm** only when BR has declared spending-mode and ratifies arming for this session (explicit `go`;
   later, if trusted, arming could ride the `go afk` cue). Agent starts ONE persistent Monitor:
   poll the session transcript mtime every 60 s; when stale > X min, emit one event and exit.
2. **Wake** → run the guardrail checks (kill-switch, mode, no raced input, clean tree).
3. **Execute** the single cheapest green SM011 unit; commit+push; append to bg-log.
4. **Re-arm** the monitor; go quiet. Disarm permanently for the session on any failed check, on
   mode-change, or after a configurable max (suggest 4 auto-units/session — bounds token burn and blast
   radius on the flaky box).

**Default X = 10 minutes.** Rationale: below ~5 min the trigger fires during BR's normal think-pauses (his
"hmmmm" deliberations must not summon background work); the 5-minute prompt-cache TTL means any wake after
>5 min silence pays the cache-miss cost anyway, so 6 vs 10 min costs the same per wake — 10 min buys a much
cleaner away-vs-thinking separation for free. Spending-mode absorbs the per-wake cache re-write; in normal/
saving mode the feature is off by definition (mode-gated).

**Escalation ladder (explicit):** this report (enables nothing) → BR-present probe of the Monitor-wake
mechanics (2 min) → per-session arming on explicit BR `go` (trial period, BR reviews the bg-log) →
only then discuss standing auto-arm. Each rung is BR's call.

**Open uncertainties:** (i) ScheduleWakeup/Cron availability in the super-agent session — not present in
this sub-agent's tool list; semantics above are from the task brief, verify before choosing the fallback. **[Super-agent confirms 2026-07-09: ScheduleWakeup IS available to the main agent (delaySeconds clamped [60, 3600], fires a prompt back on wake); Cron (CronCreate / CronList / CronDelete) is available as a deferred tool. So the fallback analysis holds and Monitor remains the recommended condition-gated trigger.]**
(ii) Monitor waking a quiescent session — probe before trusting. (iii) Whether monitor poll traffic
survives a laptop sleep/suspend — untested; if blixten suspends when BR leaves, the whole feature is moot
until wake (which is itself a safe failure mode).
