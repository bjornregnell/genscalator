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

Worked example: a hex-escape convention (*in-agent*) needed remembering and was forgotten 3 days after being
documented. The quote-aware guard (*outside-agent*) made the plain command **just work** — nothing to remember.

### 3.4 The tool serves the human, not the agent

`tt` is a **general tool BR uses at a terminal**; guardcheck is **BR's safety rail**. Twice in one session the
agent's reasoning drifted toward designing them around its own in-harness ergonomics (proposing that tt *error out*
on unescaped chars — which would reject BR's own valid regex). **That drift is how a tool quietly starts serving the
agent instead of the human.** Reject designs that only make sense from inside the harness.

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
- **Stall accounting**: can we even *count* stalls? Probably estimable, not countable (a `deny` leaves a trace; an
  ask-then-approved may leave only a `tool_use`→`tool_result` time gap, confounded by slow commands). Ties the
  blackout/hangover work. **A real stall specimen exists in the 2026-07-16 transcript — probe it, do not theorise.**
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
