# The genscalator security model

> **STATUS: STUB (2026-07-16).** Started at BR's request from a live design session. What is here is **grounded**:
> every claim below is either a verified doc fact, a shipped commit, or a specimen we actually observed. What is
> *missing* is most of the model — see [Not yet written](#not-yet-written). **SM131** is the task to elaborate it.
> Do not read this as complete, and do not let its tidy surface imply more than it covers.
>
> **Deeper frame lives in research** and is not duplicated here:
> [`research/theory/genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md`](research/theory/genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md)
> — the BIG why, the three pillars, and the discriminating criterion. Read that first; this doc is the
> **operational** side: how the guard should behave, and why.

## 1. The threat model in one line

**Either party can be the failure point.** The agent rots, or is BHH-controlled. The human rots, or *is* a BHH. So
vigilance is **symmetric** — and unusually, the agent's ethical floor must hold **even against its own principal**.
(Full argument: the theory note. Glossary: `docs/foundations.md`.)

The three pillars it builds on — **save-nothing**, **fully open (code AND policy)**, **no security-by-obscurity** —
are stated and argued in the theory note.

## 2. The human-rotted axis: the blanket-allow is the weakest link

The guard (a `PreToolUse` hook, `tt guardcheck hook`) is real structure: it has demonstrably stopped bad acts the
agent's reflexes did not (the `rm` case, 2026-07-16). **But its strength reduces to the human's approval
discipline** — and that is exactly what degrades under fatigue.

A **blanket approval does not approve one act; it permanently disarms the guard** for every future command of that
shape. The human who grants it is, by construction, often the tired one.

### 2.1 The stall budget (BR, 2026-07-16) — the generalisation

> *"EVERY guard stall is a risk as I might be dumb and blanket allow anything!"*

The `rm` case said *blanket-allowing a destructive verb* is the disarm. BR's reframe generalises it: **every stall
carries some probability of inducing a blanket-allow.** Therefore **stalls are a budget, not a free safety
mechanism**, and the goal is to minimise **all** stalls, not merely the wrong ones:

| stall kind | cost | benefit | verdict |
|---|---|---|---|
| **false positive** | spends the budget | none | **strictly bad — a bug, fix it at the source** |
| **true positive** | spends the budget | catches a real regression | worth it, but *not free* |

**A false-positive guard manufactures the very failure it exists to prevent.** That is why removing a false positive
is a **security improvement**, not a loosening (§3.2).

### 2.2 `deny` is cheap; `ask` is expensive

The lever the budget framing exposes. Today: HIGH → `deny`, MED → `ask`. But:

- **`deny`** blocks the call and hands the reason to the **agent**, who retries correctly. **No human attention
  spent. No blanket-allow risk.** (`| tail` → deny + *"use `--limit`"* → the human never sees it.)
- **`ask`** spends the budget, every time.

**We had been treating `ask` as the safe default when it is the expensive one.** Direction (SM129): move MED checks
that have a known clean rewrite from `ask` → `deny`. **Ordering constraint: `deny` demands precision** — a
false-positive `deny` hard-blocks a correct command with no override. So precision work (§3.2) lands *first*.

For genuine exceptions: **ask in a sentence, not a modal.** A modal invites a reflexive yes; a written request
invites a thought. Same information, none of the fatigue mechanism.

### 2.3 ⛔ RETRACTED 2026-07-17 — stall accounting: asks are NOT countable, and this section's arithmetic is void

> **🔻 READ THIS BEFORE §2.3. The section below is KEPT VERBATIM and is WRONG.** Kept, not deleted, per
> [[keep-the-ball-game-retract-by-annotating]]: the wrong path is the data. **Do not cite its numbers.**
>
> **❌ WHAT IS RETRACTED:** *"stalls are COUNTABLE"* · *"8 stalls, every one a MED"* · *"together they take the
> human's 8 stalls to ZERO"* · *"the budget argument is now arithmetic, not rhetoric."*
>
> **⭐ WHY — the structural reason, and it is the valuable part:**
>
> | | goes to | in the transcript? |
> |---|---|---|
> | **`deny`** | **the agent** (returned as the tool result — it MUST be, or the agent could not retry) | ✅ **recorded exactly**, joinable to its command via `tool_use_id` |
> | **`ask`** | **the human's SCREEN** (a terminal dialog) | ❌ **never recorded, anywhere** |
>
> ⇒ **THE AGENT IS STRUCTURALLY BLIND TO THE STALLS IT CAUSES.** It cannot see them live (no observer runs during
> the pause) and cannot see them after (nothing is written). **The only way a stall enters the agent's world is if
> the HUMAN pastes it.** The probe searched the transcript for the dialog text and found 8 — **8 of BR's PASTES.**
> **The number was real. Its referent was not.** *(Checked: swept ALL of `~/.claude` — `.json` → 0, `.log` → 0.
> `history.jsonl` → 1, and that is BR's own paste. **There is no ask log. The rescue is not available.**)*
>
> **✅ WHAT SURVIVES, and it MATTERS — the case for acting is UNTOUCHED and arguably STRONGER:** the 8 were real
> stalls BR really experienced, so they are a **valid LOWER BOUND** and a valid **sample of causes** (6 pipe, 2
> redirect) — just **not a census**. The sampling frame is *"stalls BR bothered to paste"*. ⇒ **the true stall count
> is UNKNOWN and ≥ 8**, so the fix's value is **unquantified and possibly much larger. We lost the number, not the
> argument.** §2.1's thesis (every stall risks a blanket-allow; minimise all of them) stands unchanged.
>
> **⚠️ AND THE INVERSION §2.3's own caveat GOT BACKWARDS:** it says HIGH denies *"cannot appear in this search"*, as
> if denies were the invisible class. **They are the ONLY visible class.** ⇒ `tt stalls` must **count denies**, join
> via `tool_use_id`, **re-run `Guardcheck.cmdFindings` to MEASURE the false-positive rate** (turning §2.2's
> "precision first" from a judgment into a number), read **subagent** transcripts too (a minion's Bash stalls the
> same human), and **say loudly that asks cannot be counted** — an instrument that silently reported only denies
> would repeat this error one level up.
>
> **⚠️ THE INSTRUMENT MANUFACTURED ITS OWN SIGNAL.** The corpus grew **54 → 66 → 72** *while the retraction was being
> written*, with **zero new stalls**: every message reasoning *about* the trace deposits the trace. ⇒ **a
> transcript-mining instrument must match a shape the agent and human CANNOT UTTER IN PROSE** (a JSON field
> position, a record type) — never a phrase. The deny matcher clears that bar (`"content":"[HIGH] …","is_error":true`
> is a field position); **the ask matcher never could have.**
>
> **⭐ THE METHODOLOGICAL FINDING TO KEEP: the instrument measured the researcher's own note-taking.** It was
> convincing because it was *specific* (8), *decomposable* (6+2), *plausible*, and **agreed with a true story we
> already believed — because it was derived from our own account of it.** A **false mechanism propping up a TRUE
> conclusion.**
>
> **⚠️ FAN-OUT FAILURE, recorded against ourselves:** the retraction note listed **four homes** and **this document
> was not among them**, though it carried the claim in **two** places (§2.3 and §5). It was found ~5h later, only
> because BR asked for something else here. **The doc that says "put the fix outside the agent" was itself patched
> by an agent's memory failing to enumerate.** ⇒ SM133 (fan-out) needs a **mechanical** home list, not a recalled one.
>
> **Full retraction:** `research/wr-data/sm129s-probe-counted-brs-pastes-not-stalls-the-agent-is-blind-to-its-own-asks-2026-07-17.md`
> · **recidivism specimen** (the same claim class re-made 4h later, in a pin, by the same agent):
> `research/wr-data/retracted-is-not-immune-i-remade-sm129s-claim-4h-after-retracting-it-2026-07-17.md`
> · **SM130 depends on the retracted claim and needs RE-DECIDING, not re-wording** — the canary's target
> (*approvals*) is precisely the thing that leaves no trace.

### 2.3 Stall accounting: the budget is MEASURABLE (probed 2026-07-16) ⛔ SUPERSEDED — SEE ABOVE

You cannot manage a budget you cannot count. We assumed stalls would only be *estimable* (via a `tool_use` →
`tool_result` time gap, confounded by slow commands). **Wrong — they leave a distinctive, greppable trace:**

```
requires confirmation for this command:\r [SEVERITY] <check name>: <fix text>
```

**Result for the whole 2026-07-16 session** (one transcript, spanning a compact):

| stalls | cause | severity | status |
|---|---|---|---|
| **6** | `pipe to head/tail/wc` | MED | **true positive** — the agent's pipe reflex |
| **2** | `output redirect (>)` | MED | **FALSE positive** — a `>` in a quoted arg (§3.2) |
| **8** | **total — every one a MED** | | |

**What this validates:** the 2 false positives are already dead (`eb0cd14`). The 6 true positives would, under
MED→`deny` (§2.2), have been handed to the **agent** ("use `--limit`"), who retries correctly. **Together they take
the human's 8 stalls to ZERO — eight removed chances to blanket-allow.** The budget argument is now arithmetic, not
rhetoric.

**Honest caveats (do not over-read this):**
- **"All 8 are MED" is partly BY CONSTRUCTION, not evidence.** HIGH findings `deny` rather than `ask`, so they emit
  no "requires confirmation" message and **cannot appear in this search**. The claim the data supports is narrower:
  *every stall the human experienced was a MED.*
- These are **records**, not verified-distinct **events** (double-logging unchecked).
- **Confound:** the same phrase also appears in the agent's own prose *about* stalls and in truncated summary
  records. A real instrument must match the `:\r [SEVERITY]` shape, not the loose phrase (a loose count returned 33).

**→ Instrument wanted (SM129): `tt stalls <transcript>`** — count + histogram by cause + severity. It makes the
budget observable, and it is the prerequisite for the **blanket-allow canary** (SM130), which needs to watch
approval *patterns* over time.

## 3. Guard design rules (each one bought with a mistake)

### 3.1 guardcheck may TIGHTEN, never LOOSEN — it must never emit `allow`

Verified against the Claude Code hook docs (2026-07-16). The three states are **not symmetric**:

| the hook emits | meaning | the user's permission rules / dialogs |
|---|---|---|
| **nothing** (empty, exit 0) | the documented `defer` default | ✅ apply **normally** |
| `permissionDecision: "ask"` | the stall | ✅ (this *is* the dialog) |
| `permissionDecision: "allow"` | *"Bypasses the permission system and runs the tool immediately"* | ❌ **skipped entirely** |

So **"we have no objection" is spelled `emit nothing`, never `allow`.** An `allow` would override the **user's own**
`settings.json` on the strength of guardcheck's string-matching; a bug would then silently disable protections
unrelated to guardcheck. **This tool's job is to ADD findings, never to REMOVE protections.**
*(Bought with a mistake: the agent reasoned fluently toward "it can just say allow" without ever checking what the
word meant. BR asked "that seems dangerous... or what am i missing?" — see `research/wr-data/co-design-specimen-*`.)*

### 3.2 A false positive is a BUG, not a conservative margin

The policy is *"no shell redirects"*. The shell parses redirections **at parse time, before expansion**, so a `>`
inside a **quoted argument** is passed through as a literal and can **never** redirect. Flagging it was therefore an
**implementation bug** — **imprecision, not protection**. Fixing it is a **correctness fix** granting **no new
authority**: a clean command emits nothing and defers to the user's rules, exactly as before.

Shipped `eb0cd14`: MED checks scan a **quote-masked skeleton**; **HIGH keeps scanning raw bytes**, so a lexer bug
costs at most a missed MED. Masking uses a **space, never deletion** — it can only *add* token boundaries, so every
error points toward a false **positive**. **Unbalanced quotes → scan RAW**: ambiguity fails toward flagging.

**Honest limit:** for a **deliberately crafted** command, quoting a shape hides it from the MED checks. Accepted and
bounded on purpose — guardcheck exists to catch **the agent's own reflexes**, not to withstand a crafted attack.
Do not mistake it for an adversarial sandbox.

### 3.3 Put the fix OUTSIDE the agent — anything else rots

**A fix that lives inside the agent — a skill, a memory, a resume-prompt line — only works if it is loaded AND hot
at the instant of action. So it rots.** Anything routed through agent recall is willpower wearing a structural
costume (evidence: ~5 mechanical slips in one session, one while a vigilance mode was *explicitly active*).

| where the fix lives | survives a warp? | examples |
|---|---|---|
| the **guard** | ✅ always runs | the stall; a fix-text that teaches at the moment of failure |
| the **tool interface** | ✅ if it is the only way in | `--limit` / `--tail`; `--message-file` |
| the **human** | ✅ | shepherding the post-warp window |
| a **skill / memory / prompt** | ❌ rots | useful, but never the last line of defence |

#### 3.3.1 ⭐ "CODE BEATS PROSE" (BR's tagline, 2026-07-17) — and the qualifier that keeps it true

> ### **Code beats prose.**
>
> **BR's framing, and it is the same logic as why a comment is bad when the code can say it without one.**

**The mapping is exact.** A comment is a **claim** about what the code does; the code is the **mechanism**. When they
disagree the code wins, and the comment **rots silently**, because nothing forces it to stay true. Swap the nouns: a
memory / skill / briefing is a **claim** about what the agent does; the **tool** is the mechanism. Same rot, same
winner, same silence.

⚠️ **It is WORSE for agents than for comments, which sharpens the argument:** a stale comment at least gets *read*
when someone reads the code. **A stale rule about an instrumental action is never consulted at all** — it is not
misleading the agent, it is **absent**.

**⛔ THE QUALIFIER — without it the tagline says something false ("delete the memories").** It is BR's own rule from
`CLAUDE.md`: *"Only write a code comment to state a constraint the code itself can't show."* So the honest form was
never *"comments are bad"*:

| | ❌ delete it, build the mechanism | ✅ keep it — no mechanism can hold it |
|---|---|---|
| **code** | a comment restating the code | *why* this constant; what breaks if you change it |
| **agents** | a rule a tool could enforce (which command, which path, which dir) | judgment, values, taste, threat reasoning |

⇒ **Prose loses precisely where a MECHANISM IS POSSIBLE, and prose is all there is where one is not — which is
exactly where it works.** *(Evidence both ways: BR's no-em-dash rule is pure prose and has never failed, because no
tool can hold "write like me." **This document** is prose and is irreplaceable, because it encodes judgment.)*

**🔬 THE DISCRIMINATOR — and it CORRECTS §3.3's stated mechanism above.** §3.3 says an in-agent fix works only if
"loaded AND hot", i.e. it blames **rot**. **That is incomplete, and there is a counter-example inside this repo:**
the no-em-dash rule is *mechanical*, never "hot", and **arms reliably**. So heat is not the variable.

> **A rule fires when it governs THE OBJECT OF ATTENTION. It does not fire when it governs a MEANS.**

When the agent wrote `cp` (2026-07-17), attention was on *preserve the file*; the command was **instrumental, beneath
notice** — the way you do not notice which fingers you type with. **A rule about an incidental action never enters
the moment it is meant to govern.** No substrate fixes that: the rule is not lost, it is **slower than the thing it
races**. ⇒ **the lever is not better storage. It is REMOVING THE RACE: make the wrong move UNAVAILABLE, not
forbidden.** *(Same conclusion §3.5 reached from the path angle — "only removing the reach does" — arrived at from
the attention angle. Two independent routes to one rule.)*

**📌 THE SPECIMEN IS THIS DOCUMENT.** §2.3 asserted *"8 stalls, every one a MED… the budget argument is now
arithmetic, not rhetoric."* **That was PROSE ABOUT A MEASUREMENT.** It rotted into a standing falsehood and sat here
for a day. **Had `tt stalls` existed, the number would have been CODE — and code cannot rot into a false claim; it
either runs or it does not.** ⇒ **§2.3 IS a comment that outran its code**, in the doc that preaches against exactly
that.

**⛔ THE PRECONDITION, and it is the rule this buys (bought with a mistake, 2026-07-17):** a `deny` (§2.2) is the
lever *because* it is code, not prose — it fires at the instant of action, reaches the **agent** not the human, and
is **recorded**. **But a deny must name a tool that EXISTS.**

> **A deny without a provision is a prohibition-only briefing implemented in the guard. The agent will flail
> against it exactly as a sub-agent flails against a rule list with no tools.**

*(Bought by: the agent briefed a sandbox minion with prohibitions — no `cd`, no pipes — and gave it **no tool to obey
them with**. The tool it needed was **tracked, in its own clone, the whole time**. BR watched it flail.)*
⇒ **ORDERING: build the lane, THEN close the road.** Concretely: `tt git` read-verbs and `tt forge` (SM137) are
**prerequisites** for denying raw `git log` / `gh pr`, not companions to it.

**⭐ BR'S TWIST (2026-07-17) — THE COMPILER IS THE PUREST `deny` WE HAVE, and it has been running all along:**

> *"if it doesn't compile it is false code that never cost us those runtime bugs the compiler caught for us (compare
> it to the alternative: brittle bash or agent on-the-fly generated do-whatever-at-runtime-python)"* — **BR, verbatim.**

> ### **A lie in prose compiles. A lie in typed code does not.**

**This is not an analogy — a compile error IS a deny, by every criterion in §2.2:** it fires at the instant of
action, it hands its **reason** to the **agent** (not to the human's screen), it **names the fix**, it is
**recorded**, and **nothing has to be remembered for it to work**. ⇒ **the compiler is `code beats prose`
enforced by a machine, and it costs the human's attention budget ZERO.**

**⇒ THE RANKING THAT MATTERS — and note it is not "code vs prose", it is WHEN THE FALSEHOOD IS CAUGHT:**

| medium | when is a falsehood caught? | costs the human? |
|---|---|---|
| **prose** (comment, memory, skill, briefing) | **never.** It ships and is false for years. | only when it misleads |
| **unchecked code** (brittle bash, agent-generated runtime Python) | **at runtime, maybe, in front of a user** | ⚠️ **worst of both** |
| **typed, compiled code** (`tt`, Scala) | **before it exists** | **zero** |

⚠️ **The middle row is the trap, and it explains an existing rule.** Bash and on-the-fly Python have **all of code's
power and none of code's checking** ⇒ **they are PROSE THAT EXECUTES.** ⭐ **This gives §4's
[[never-allowlist-interpreters]] a SECOND, INDEPENDENT justification**: the standing reason is *an interpreter is a
blank shell* (authority); the new one is *an interpreter is prose that runs* (verification). **Two unrelated
arguments landing on one rule is the strongest form of support this document can offer.**
⇒ **And it re-justifies the toolbox itself: `tt` being typed Scala rather than shell is THE SAME DESIGN DECISION as
the deny lever, not a taste preference.** *(Worked specimen: `blog/022` — an agent "elegantly" refactored a
`kill -9` into `timeout 3`, silently destroying the SIGKILL knowledge the ugly line encoded. `destroyForcibly()`
cannot quietly mean SIGTERM. **The knowledge moved from the human's head into the build.**)*

**🔬 FALSIFIABLE, and on record:** *rules about **instrumental** actions will never arm from any substrate, however
well written; rules about the **object of attention** can.* **If a future agent catches itself reaching for a raw
command BY REMEMBERING A NOTE, this is wrong.** *(Standing confabulation caveat: this is a story about **behaviour**
— the agent cannot see its own retrieval and does not claim to. The evidence is a day of episodes with a consistent
split, listed in the wr-data note.)*

**Ground:** `research/wr-data/code-beats-prose-*-2026-07-17.md` (the finding + the full episode list) ·
`research/case-studies/action-research-meta-minion/log/` (the process record) · §2.2 · §3.5 · PB SM137/SM138.

Worked example: a hex-escape convention (*in-agent*) needed remembering and was forgotten 3 days after being
documented. The quote-aware guard (*outside-agent*) made the plain command **just work** — nothing to remember.

### 3.4 The tool serves the human, not the agent

`tt` is a **general tool BR uses at a terminal**; guardcheck is **BR's safety rail**. Twice in one session the
agent's reasoning drifted toward designing them around its own in-harness ergonomics (proposing that tt *error out*
on unescaped chars — which would reject BR's own valid regex). **That drift is how a tool quietly starts serving the
agent instead of the human.** Reject designs that only make sense from inside the harness.

### 3.5 A path constraint CANNOT live in a permission pattern — it is unenforceable, not merely weak

**Never express a path-scoped permission as an allowlist rule. No pattern can hold it.** The permission layer matches
the **command STRING**; the constraint lives at the **FILESYSTEM** layer, over **RESOLVED** paths. **Resolution
happens AFTER the match, in a system that has never heard of the rule** — so `..` and symlinks walk straight out of
any prefix you write:

```
mkdir -p /home/…/genscalator/../../../../tmp/anything   # satisfies ANY genscalator/-prefixed rule
Bash(git -C /repo:*)  ⟵  git -C /repo/../../elsewhere   # the same hole in a rule that looks innocent
```

**⇒ It is not a matter of writing a better pattern. The two layers cannot see each other.**

**This SPLITS §3.3's "guard" row, and the split is the point:**

| guard layer | knows | can hold | example |
|---|---|---|---|
| **allowlist / permission pattern** | the command **string** | **SYNTACTIC** invariants only | no `\|`, no `&&` |
| **the tool** (`tt …`) | **RESOLVED** paths, semantics | **SEMANTIC** invariants | stay inside this worktree |

⚠️ **A semantic invariant parked in the allowlist is not a weak guard — it is a NON-guard, and it looks exactly like
a real one.** (Same failure shape as `hangover?`, a measurement posing as a judgment.) **The rule: a constraint must
live at the layer that can evaluate it. Put the check where the facts are.**

**This is also the precise reason `tt git` is safe to allowlist** — and the usual explanation is wrong. **Not**
because its string is safe: because **the TOOL enforces the invariant after resolution**. The allowlist grants *"run
this tool"*; the tool decides what is permitted. `tt git mv` (SM134 #9) is the concrete instance: **git refuses paths
outside the worktree**, so git enforces the containment a pattern cannot express.

> #### ⚠️ The rationale that makes this a RULE and not an observation — the agent GENERATES this risk
>
> **Bought by two stalls FIVE MINUTES APART on 2026-07-17** (`mkdir *`, then `mv *` offered as blanket allows), and
> the honest part is **whose fault they were: the agent's.** Neither was the guard being noisy. Both were the agent
> reaching for a shell verb **out of habit, for something it did not need** — the **Write tool creates parent
> directories** (verified: one Write created two dirs, no `mkdir`), and `git mv` moves tracked files.
>
> ⇒ **The agent is not merely a consumer of the guard; it is a GENERATOR of the risk the guard exists to contain.**
> Each avoidable stall puts an **irreversible policy change one keystroke away from a human who is mid-task** (§2.1:
> stalls are a budget; ALL of them must be minimised, not just the false ones). **Two draws in five minutes.**
>
> **And it recurs, which is why the rule may not route through agent recall (§3.3):** the identical shape was logged
> **7 days earlier** (`guard-suggests-blanket-date-glob-but-tt-chrono-exists-2026-07-10.md` — the harness proposed an
> over-broad glob while the right tool already existed), and the quoted-arg false positive recurred **3 days** after
> being logged *and* written into a skill (`36f1532`). ⭐ **Knowing about a shape does not stop it. Only removing the
> reach does** — which is why this rule's real form is *"the tool holds the check"*, not *"the agent remembers the
> hazard"*.

**Three dialog defects this exposes, all attacks on the human's attention (the §2 axis):**
1. **The consent mismatch — LOCAL evidence, GLOBAL grant.** The dialog shows **one** command and takes an answer
   binding **every** command of that shape, **forever**. The human is not careless when this bites: **the instance
   shown genuinely IS benign, which is exactly why they say yes.** A blanket allow is an **irreversible policy
   change elicited by displaying its most innocuous example** — and nothing ever re-arms it, so **guard coverage only
   ratchets DOWN.**
2. **The severity mismatch — the affordance does not grade by blast radius.** The *same* offer, same framing, same
   keystroke, was made for an **additive** `mkdir` and a **destructive** `mv` two minutes apart. **The only thing
   grading severity in that dialog is the human** — i.e. the mechanism leans on **exactly the faculty §1's threat
   model says will fail.**
3. **The intent mismatch — the gesture that grants carries NO evidence of intent to grant** (BR, 2026-07-17). Worse
   than (1), which at least assumes the human *meant* to answer. **A mouse click is usually a raise-window gesture**,
   fired on a **false hypothesis that the window is not focused** — and the human is **blind to that error, because a
   belief you could notice was false is one you would not hold.** The window *was* focused; the click lands on
   option 2; **`mv *` is granted as a SIDE EFFECT of a window-manager correction.** ⇒ **The most irreversible option
   in the system is reachable by the least intentional gesture a human makes.** *(Not hypothetical: the 2026-07-07
   **near-miss** — "BR nearly clicked" — bought the standing rules **"never click always"** and **"treat the
   allowlist as reviewed code, not an accident of clicking"**.)*

> #### ⭐ The generalisation §3.3's table understates: STRUCTURE OVER WILLPOWER APPLIES TO THE HUMAN TOO
>
> §3.3 lists **the human** as a ✅ warp-survivor, which quietly implies the human is *the reliable one*. **Defect (3)
> falsifies that framing.** BR's fix was not vigilance — it was `CLAUDE_CODE_DISABLE_MOUSE` (SM093): **the human
> building a guard against himself**, for a reflex he cannot willpower his way out of. **He removed the reach, days
> before we wrote §3.5's "only removing the reach works."**
>
> ⇒ **The human is not a guard. The human is another reflex-driven system that NEEDS guards.** §2's human-rotted axis
> names the rubber-stamp as the weak link — **but the mitigation is "narrow your own reach", not "be careful".**
> **The pair is symmetric: both members need their reach narrowed, and neither can do it from inside at the moment of
> action.** *(Sibling: `human-tab-fires-redundant-preprompt-low-friction-2026-07-16.md` — **match firing-friction to
> the stakes.** Today the system has it inverted: typing a commit message takes more effort than permanently
> disarming the guard.)*

**Full specimen + theory:** `research/wr-data/the-permission-layer-cannot-hold-a-path-constraint-2026-07-17.md`
(§3b the intent mismatch). **Mouse-mode UX + env trade-off live elsewhere, by design:** `wr-data/harness-ux.md`
§ "New mouse-click TUI mode races the human's native terminal clicking" (2026-07-06) and **SM093**.

## 4. Standing rules (already load-bearing elsewhere)

- **NEVER blanket-allow `rm` or any destructive/irreversible command.** One-time, shown, human-gated is fine; a
  blanket "always allow" is the disarm. The agent must **actively flag** any move toward one — earned trust
  *obligates* flagging harder, not less.
- **Never allowlist interpreters** (`python3 -c`, `bash -c`): an interpreter is a blank shell.
- **Never blanket-allow settings self-edit.** Settings/security changes stay per-edit human-approved.
- **Under AFK**: bare, allowlist-matchable, prompt-free commands only — a stall the agent creates cannot be cleared.

## 5. Not yet written

This stub covers the **guard** and the **stall budget**. Missing, and the substance of **SM131**:

- The **blanket-allow canary** (SM130): detecting that the human has started rubber-stamping — the threat model's
  actual failure mode. Term is BR-approved and, *if it works*, belongs in `docs/foundations.md`.
- **Stall accounting**: ⛔ **the 2026-07-16 "they are COUNTABLE" answer is RETRACTED (2026-07-17) — see §2.3's
  retraction block.** **Asks are NOT countable: they never enter the transcript.** Denies ARE, exactly. Open:
  re-spec `tt stalls` around **denies + false-positive measurement**, and decide what (if anything) can stand in for
  the ask count — gap inference (SM121) is the only route left, and it is ambiguous by construction.
- The **discriminating criterion** (harm × whose-autonomy × third-party impact) — the hard central question, seeded
  in the theory note: hold an un-overridable floor without becoming paternalistic or lurable.
- **Deployment surface**: the standing deploy permission and its guardrails, `~/.netrc`, what the hosted surface
  touches.
- **Save-nothing in practice**: what genscalator.ai actually does and does not persist, stated concretely enough to
  be checkable.
- **Manipulability as a security property**: why intent-confabulation and caving-under-pressure ARE the attack
  surface against a manipulative principal.
- Threat-model coverage: what this model does **not** defend against (crafted attacks, a compromised box, supply
  chain).

## See also

- `research/theory/genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md` — the BIG why, pillars, criterion.
- `research/theory/poor-users-theory-on-opaque-design-decisions-by-big-tech-company.md` — the opacity budget.
- `research/wr-data/co-design-specimen-human-falsified-five-agent-premises-in-one-hour-2026-07-16.md` — the specimen behind §3.
- `research/wr-data/prohibition-does-not-arm-the-reflex-use-a-hex-escape-2026-07-16.md` — §3.3's evidence.
- `skills/avoid-guard-stall/SKILL.md` — the operational agent-side guidance.
- `docs/foundations.md` — glossary (BHH, agent blackout, rot).
