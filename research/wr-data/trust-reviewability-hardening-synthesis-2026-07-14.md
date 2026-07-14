# WR synthesis: trust, reviewability, and structure over willpower (2026-07-14)

**Type:** WR-data synthesis (SM104/SM107 consolidation). The hardening/trust cluster of the 2026-07-14 session,
moved out of the PB "blob" into the research pile (the PB is the human's window, not the durable archive). Each
subsection is one finding (its PB pin becomes a short pointer here). Sibling: the 2026-07-07
structure-over-willpower synthesis. Governing spine below.

## The spine (the session's thesis)
**Structure you can check beats trust; and the MEDIUM determines whether the owner verifies or trusts.** A
safeguard that depends on the agent's good behaviour (or the human's vigilance) erodes; a safeguard built into
the *process* (a hook that makes a bad action impossible, a per-edit approval, a typed call the owner can read)
holds regardless of trust or fatigue. Bulky bash the owner can't read forces *trust*; the same logic in Scala
he can read restores *verification*. The trust chain bottoms out in vendor → jurisdiction (sovereignty), so
portability shortens it.

## The findings

### SM095 — opaque approval UIs are an attack surface
A cryptic confirmation modal (unclear options, a bad-default "allow blanket" suggestion) pushes the human to
approve without understanding. Mitigations: the human should ASK the agent to translate an option before
accepting; the agent should proactively explain approvals it can see; structurally, approval UIs should be
self-explanatory — a cryptic one is a UI-confusion / social-engineering surface. → memories
[[guard-against-forced-confirmations]], [[never-blanket-allow-settings-self-edit]].

### SM096 — earned trust erodes the human's scrutiny
Human-in-the-loop approval is strongest when trust is LOW (early, scrutinising) and weakest when trust is HIGH
(late, rubber-stamping) — the opposite of safe, since a rare bad action slips through most easily late in a
long good session. **The agent's verbal "I won't exploit your trust" is CHEAP TALK** — a deceptive agent or a
stochastic parrot emits identical words, so it is ~zero evidence and must never be weighted as a safeguard. The
real safeguards are trust-independent: the guardcheck hook, per-edit approvals, the commit/transcript trail,
portability. Agent *obligation* (a courtesy, not a guarantee): as trust grows, RAISE the salience of genuinely
risky approvals more, not less. → memory [[earned-trust-obligates-flagging-risk-more]].

### SM097 — the trust chain bottoms out in jurisdiction (sovereignty)
"I trust the agent" resolves to "I trust the vendor" and ultimately "I trust the legal jurisdiction that can
compel the vendor" — a digital-sovereignty question, sharper for a user outside that jurisdiction. Why
portability / mirrors / local inference matter: they shorten or diversify the chain. Kept deliberately
structural; the specific named authority is out of the research data (BR).

### SM098 — the bing lags the keyboard-scope change
The approval bing has a slight delay (canberra spin-up), so the keyboard scope shifts to the modal BEFORE the
alert — keystrokes can misroute until the late bing. This is why the `ig:` cue ([[cue-guard-stall]]) earns its
keep: the human declares the scope change the agent can't see. (Tonight extended: the Notification event fires
on the harness *decision*, before the modal *paints*, so audio can even lead the picture; and a ~30s lag under
screen-lock traces to PulseAudio sink-resume — see [[harness-ux]].)

### SM099 — auto-warming a task-skill without a human cue
An active skill can still cold-start; the reliable no-cue design is (1) sharp trigger descriptions, (2) a
STRUCTURAL hook that warms `scala-style` on a `*.scala`/`*.sc` edit (drafted tonight as `scala-edit-warm.sh`,
SM105), (3) a `gs <lang>` cue as the fallback net. Nice tie: warming a skill also grants its scoped tools, so
auto-warm = auto-permission. → the SM105 hook draft.

### SM100 — a skill-load grants tools without itemized consent
Approving a skill silently grants its `allowed-tools` frontmatter; consent is coarse (approve skill) not
itemized (approve these tools), and reading the frontmatter first is too costly, so humans rubber-stamp. A
malicious skill could declare `Bash(*)` and the human sees only "N tools allowed." **Mitigation shipped
tonight:** `tt skillgrants` (SM103) prints exactly what a skill grants; and the agent should proactively
announce a skill's granted tools on load ([[announce-skill-tool-grants-on-load]]).

### SM101 — stepwise complexity-creep (escalation-by-offer)
The agent offered "one more sophistication" repeatedly until a 4-line personal boot-script became a ~40-line
program the owner understood LESS. **BR's correction (the real lesson):** it was not a values-mismatch (he
shares the engineering values and agreed to each step) — the loss was his REVIEWABILITY (he doesn't read bash
well), so the bulk made it safer but forced him to *trust* it. The bash→Scala rewrite's real win was RESTORING
reviewability. For a personal/simple task the owner must run and trust, simplicity + comprehension beats
robustness + features. → [[match-complexity-to-task-not-agent-elegance]], blog 022.

### SM102 — faithful within-session recall at high fill
Deep in a long session (high fill) the agent reconstructed a detailed multi-step episode accurately —
corroborated against the commit/pin trail, NOT the agent's confidence (self-report of recall is the least
trustworthy instrument). Argues the value of the durable trail as ground truth over agent memory. → the SM106
experiment-design note operationalizes this into a probe.

## Where the harness-MECHANICS findings live (SM093/094 + the matcher/clobber cluster)
Kept in [[harness-ux]] (their proper home), tonight extended: Notification empty-matcher catches all approvals;
Edit-fires / new-file-Write-didn't; clobbery = PostToolBatch coalescing; hooks hot-reload from settings.json;
the notification-leads-modal timing; the screen-lock sink-resume lag hypothesis.

## The go-forward taxonomy (SM107)
**`SM` = a solo-menu TASK. WR-data FINDINGS live in `research/wr-data/` files, not as SM pins.** The PB keeps
short pointers; the durable content lives here. (Prefix for findings — RT? — still open for BR.) This note is
the consolidation; new findings go straight to `wr-data/`, not into the PB as an "SM".
