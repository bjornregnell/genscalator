# `gs` registry — ready-to-grab cues, dances, and terms

**Purpose.** A pre-built, read-and-render source for the `gs cues` / `gs dances` / `gs term` commands, so the
agent does NOT have to synthesize the answer live from `docs/foundations.md` + the `cue-*` memories every time
(which lagged, cost tokens, and risked dropping an entry — WR
`research/wr-data/gs-lists-need-ready-to-grab-substrate-2026-07-13.md`, SM058). Each entry is one line: a
meaning plus, for cues, its direction.

**Canonical source.** `docs/foundations.md` (its "Channel bandwidth", "Dances and handoffs", and glossary
sections) plus the `cue-*` memories remain authoritative. This file is a **distilled index** of them; when
foundations changes, regenerate the relevant table here (keep it in sync — it is a cache, not a second source
of truth). For the full prose on any entry, follow it back to foundations.

---

## Cues

### Human → agent

| Cue | Meaning |
|---|---|
| `go` · `(go)` · `GO` | urgency dial: when-convenient / as-soon-as-suitable / asap+reassess. Bare `go` also = proceed autonomously on the current plan. |
| `go afk [SMn]` | AFK-strict go; re-verify each item's AFK-safety against current state first. |
| `hang on` | human is thinking or offline; hold, minimal ack, no new work. |
| `hmmmm` / trailing mutterings | "I'm thinking"; hold, don't push, wait for the human's call. |
| `BRB` | short break; may do safe background work, prep for the return. |
| `we are racing` | feeds are overlapping; keep useful work going and reconcile the whole queue. |
| `ETA-idle?` | non-interrupting "when are you free?"; give a quick ETA to idle and keep working, do not stop. |
| `quick` / `deep` | per-turn depth dial (token spend + how much info back). |
| `why:` | the goal behind the ask; serve the goal, may refine the literal request. |
| `similar to` / `or similar` | don't take the definition literally; improve it in the spirit of the work. |
| `OK?` | an alignment check (not a yes/no); reflect intent back + flag ambiguity. |
| `edit:` · `*wrong;right` · `*right` | low-stakes fix of a typo or word. |
| `clarification:` | real misread-risk; apply the clarification AND register that the original was ambiguous. |
| `correction:` | a factual correction carrying misread-risk; apply + re-check nothing acted on the misread. |
| `|sv` (or `|<lang>`) | the preceding word is Swedish (that lang); preserve in chat, render good English in publications. |
| `:Z` | human is getting tired; take rot-vigilance, checkpoint, steer to a safe stop. |
| `note:` | keep fluent + a pin-candidate. |
| `pin:` / `WR data:` | save durably (agent picks the home). |
| `fyi:` | hand over a fact; agent decides the disposition (the you-decide cue). |
| `gs ...` | a genscalator do-what-I-mean command (this system). |
| `pin a joint task` / `JOINT 🤝` | work requiring BOTH parties: agent may prepare, never executes or decides solo; excluded from AFK/solo menus; unparks on the human's go. |
| `use fleet` | spawn parallel sub-agents. |
| `do Q-test` | run the fresh-restart fidelity self-test. |
| `bg` | an agent-solo background task (runs when the AFK menu is empty + human away). |
| "here" | in this chat/feed, not a file. |
| "the p-word" | avoid saying the plan-trigger word literally so the harness doesn't switch modes. |
| disclosed stress ("I'm stressed" / "not so stressed now") | authoritative human state; adapt (reduce load, defer, simplify) / exit the mode. |

### Agent → human

| Signal | Meaning |
|---|---|
| "ready to compact" | prep done; the human is clear to `/compact` (the compact dance). |
| status-line: frozen clock | the agent's turn is running (the ballgame signal). |
| status-line: red ctx-fill | fill is past the dumb-zone ceiling Z; brake / compact. |
| status-line: red limit % + reset | near a 5h / weekly usage cap. |
| staleness flag | the agent flags a stale PB NOW / resume-prompt before trusting it. |
| WDYT deferral | the agent holds an owed opinion for a lull rather than interrupt flow. |
| propose the rest dance | the agent detects fatigue or over-trust and proposes `:Z`. |
| "did you mean: ..." | on genuine ambiguity the agent offers the nearest candidate readings to pick from, instead of an open-ended question. |

---

## Dances

| Dance | Goal |
|---|---|
| **Compact dance** | hand off across a `/compact` without losing state; refresh the resume-prompt + PB first; self-measure with chrono stamps. |
| **Context dance** | the umbrella: assess how full + how rotted the context is, then choose the response. |
| **Exit-resume dance** | exit + `claude --resume` to inherit a fresh process env / token (a warp *between* sessions). |
| **Rest dance (`:Z`)** | conserve the human; take rot-vigilance, checkpoint, steer to a safe stop. |
| **BRB dance** | short-break handoff; maybe do safe background work; prep for the return. |
| **Me-go-pee dance** | short human bio-break; agent batches `short-solo`+`afk`, does short AFK-safe solo, then on return reports + `rm afk`, and `rm short-solo` when the work lands. |
| **Solo dance** (formerly AFK) | hand off for unattended autonomous work; bare allowlist-safe commands only; work a stocked menu. |
| **Live-edit dance** | high-flow do-what-I-mean doc editing; the agent is the sole writer, buffer-race forbidden. |
| **Delegation dance** | the super-agent hands bounded, well-scoped work to CF5 sub-agents to cut rot. |
| **Go dance** | release the agent to act autonomously within a goal. |
| **Token-usage dance** | compensate for the agent having no token gauge; the human relays, modes track headroom. |
| **Session-limit dance** | estimate + avoid hitting the 5h session usage cap. |
| **Weekly-limit dance** | estimate + avoid hitting the weekly usage cap. |
| **Pin dance** | longitudinal externalisation: bank many small durable captures (especially near a compact). |
| **Note dance** | the notice cue: surface a fact fluently, as a pin-candidate. |
| **Fyi dance** | delegated disposition: the agent decides what to do with a handed-over fact. |
| **Edit dance** | correct a just-sent message without derailing the turn. |
| **Consistency dance** | sweep the persistent substrate for internal inconsistency and fix it. |
| **Hardening dance** | audit the agent's own persistent config for misfire-causes and propose structural fixes (security changes stay human-approved). |

---

## Terms (glossary)

| Term | Meaning |
|---|---|
| **CF** (confirmation fatigue) | review quality/willingness degrades from too many approval prompts; a BHH enabler. |
| **Review overload** | too much agent-generated output for the human to meaningfully review. |
| **Communication bandwidth** | useful intent crossing the human↔agent channel per effort/token, in each direction. |
| **TE** (token efficiency) | achieving a task with fewer model tokens ($cost + smart-zone pressures). |
| **Smart zone / dumb zone** | the fill region where the agent reasons well vs where it degrades. |
| **Z** (smart-zone ceiling) | the usable working-context ratio (~30%); brake as fill nears Z, not 100%. |
| **Context rot** | progressive reasoning degradation as the window fills with accumulated history. |
| **Context usage / fill** | the fraction of the window occupied (quantity axis); the agent can't reliably self-read it. |
| **Compact sleep** | the wander-off UX pain: a long `/compact` lets the human leave, and the agent stays dormant after it finishes until the human next types; remedy = the bing-bing (`gs compact notify on`). |
| **Hot context** | the relevant material is already resident + the cache is warm; do related work now. |
| **Harvest-hot-context mode** | rapidly pin the session's insights while context is still hot, before a warp cools them. |
| **Token velocity / acceleration** | dS/dt and d²S/dt²: the burn rate and whether it is speeding up (the introspective alarm). |
| **DWIM** | act on the human's intent, not the literal tokens; bounded by confirm-on-genuine-ambiguity. |
| **echt / äkthet** | genuine AND grounded outward writing; guards against *false äkthet* (an ungrounded surface). |
| **WR** (Workflow Research) | the empirical study of the human↔agent workflow itself, logged live as "WR data". |
| **BHH** (Black Hat Hacker) | the adversary stakeholder in the threat model; **BadGoals** = the anti-goals to PREVENT. |
| **BadGoal** | a must-not-do constraint / anti-goal ("Goals we Hurt"). |
| **agentic SE / RE** | software / requirements engineering with the agent as a first-class collaborator + stakeholder. |
| **to ape** | the agent's move: imitating human-ness (it is trained to sound like us). |
| **to anthro** | the human's move: projecting human traits onto the agent. |
| **ape ⟷ anthro asymmetry** | a mirror pair; the agent's aping lures the human's anthro-ing, and it strengthens as models improve. |
| **CO4 / CF5** | the agent handle encodes the model: Claude Opus 4 / Claude Fable 5. |
| **AT** (AutoTranslate) | the introprog Swedish→English case study (now closed). |
| **SSG** (static site generator) | the strand that publishes the `media/blog/*.md` posts to bjornregnell.se. |
| **substrate-grounding** | check the deterministic substrate instead of recalling; regex-triangulation beats stochastic recall. |
| **gs** | genscalator (always lower-case); deliberately overloaded — a leading `gs` cue means "run a gs command". |
| **PB** | the closed pinboard `PIN-BOARD.md`. |
| **Thriller state** | a human high-arousal excitement state; carries an over-trust risk → propose the rest dance. |
| **human-stress-mode** | a human-state mode (agents don't get stressed); enter on a disclosed stress, exit on "not so stressed now". |
