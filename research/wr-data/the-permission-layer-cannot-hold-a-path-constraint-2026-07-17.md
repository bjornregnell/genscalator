# The permission layer cannot hold a path constraint — three layer-mismatches in one dialog

**2026-07-17 10:15 (`tt chrono now`).** BR flagged the agent's claim as WR data, naming it **theory-building + the
security model**. Grounded in **two live guard stalls five minutes apart**, both self-inflicted by the agent.
**Feeds SM125 (the security model), SM131 (`SECURITY-MODEL.md` §3), SM129 (the stall budget), SM134 #9.**

## The claim BR flagged (agent, verbatim)

> *"a path-scoped `mkdir` allow is **structurally impossible to express safely as a permission pattern** — it's not a
> tuning problem, it's the **wrong layer**. `mkdir *` is even worse: it's mkdir anywhere, forever, and a blanket
> allow **permanently disarms the guard for that shape**."*

## The specimens (both the agent's own fault — that matters, see §5)

| time | command | harness offered |
|---|---|---|
| ~10:0x | `mkdir -p …/action-study-meta-minion/log` | *"2. Yes, and don't ask again for: **`mkdir *`**"* |
| ~10:0x, **5 min later** | `mv …/action-study-meta-minion …/action-research-meta-minion` | *"2. Yes, and don't ask again for: **`mv *`**"* |

BR refused both (*"too wide"*) and asked whether dir-creation should be a `tt` tool. **Answer: no tool was needed at
all — the Write tool creates parent directories** (verified: `log/README.md` created two directories with no
`mkdir`). The agent had reached for `mkdir` out of **shell habit, not need**.

---

## 1. ⭐ THE GENERAL PRINCIPLE — the enforcement mismatch

> ### **The permission layer matches STRINGS. The thing it is trying to constrain lives at the FILESYSTEM layer
> (RESOLVED paths). A string cannot express a resolved-path constraint, because resolution happens AFTER the match,
> in a different system that has never heard of the rule.**

Every path-scoped allow you could write is defeated by ordinary path resolution:

```
mkdir -p /home/…/genscalator/../../../../tmp/anything      # `..`      — matches ANY genscalator/ prefix rule
mkdir -p /home/…/genscalator/somelink/escape               # symlink   — resolution happens later, elsewhere
```

⇒ **This generalises far past `mkdir`.** *Any* path-scoped Bash allow has the hole — including a rule as
innocent-looking as `Bash(git -C /repo:*)`, which `git -C /repo/../../elsewhere` satisfies. **It is not a matter of
writing a cleverer pattern. No pattern exists**, because the two layers cannot see each other.

### ⇒ The corollary: PUT THE CHECK WHERE THE FACTS ARE

**This is exactly why the `tt` pattern works, and it is worth stating precisely, because the usual explanation is
wrong.** `tt git` is not safe to allowlist because *its string is safe*. It is safe because **the TOOL enforces the
invariant AFTER resolution**. The allowlist grants *"run this tool"*; **the tool decides what is permitted**, holding
facts the permission layer structurally cannot access. Its own help states the contract: the destructive verbs
*"stay off entirely, **so allowlisting `tt git` is safe**."*

**⇒ The design rule:** *a constraint must live at the layer that can evaluate it.* A **string** guard can hold a
**syntactic** invariant (no `|`, no `&&`). Only a **semantic** guard — a tool — can hold a **semantic** one (stay
inside this repo). **`tt git mv` is the concrete instance** (SM134 #9): git **refuses paths outside the worktree**, so
git enforces the containment a permission pattern cannot express.

### ⚠️ This SHARPENS the day's load-bearing finding — it does not merely illustrate it

The finding says: *only the **guard**, the **tool interface**, and the **human** survive a warp.* True — **but "the
guard" is not one thing, and this specimen splits it:**

| guard layer | knows | can hold |
|---|---|---|
| **the allowlist / permission pattern** | the command **string** | syntactic invariants only |
| **the tool** (`tt …`) | **resolved** paths, semantics | semantic invariants |

⇒ ***Which* guard layer** decides whether an invariant is enforceable at all. **A semantic invariant parked in the
allowlist is not a weak guard — it is a NON-guard, and it looks identical to a real one.** That is the same shape as
`hangover?` (a measurement posing as a judgment) and the `tt files` false negative (a tool lying quietly): **the
failure mode of this project is not error, it is something that reads as evidence and is not.**

---

## 2. ⭐ THE CONSENT MISMATCH — the dialog asks LOCAL and takes GLOBAL

**The sibling defect, and it is the mechanism behind *"a tired human blanket-OKs it"* (`100b6c0`):**

> **The dialog shows ONE command and asks a question whose answer binds EVERY command of that shape, forever.
> The human's EVIDENCE is local (this one benign `mkdir`); the human's DECISION is global (`mkdir *`, always,
> for all future agents — including the rotted ones).**

**The human is not being careless when this bites. They are answering the question they were shown.** The instance in
front of them genuinely *is* benign — that is exactly why they say yes. ⇒ **A blanket allow is not a decision about
this command; it is an irreversible policy change, elicited by showing its most innocuous example.**

⚠️ **And it is one-way**: a blanket allow **permanently disarms the guard for that shape**, while nothing in the flow
ever re-arms it or reminds anyone it was granted. ⇒ **Guard coverage only ratchets DOWN.**

---

## 3. ⭐ THE SEVERITY MISMATCH — the affordance does not grade by blast radius

**The identical offer — *"don't ask again for `<verb> *`"* — was made for both, TWO MINUTES APART:**

- **`mkdir *`** — over-broad, but **additive**: it creates. Worst case, clutter.
- **`mv *`** — **destructive**: it overwrites its destination and relocates anything anywhere. It is
  **create-plus-delete wearing a gentle name.**

**Same framing. Same friction. Same keystroke.** ⇒ **The ONLY thing grading severity in that dialog is the human** —
which is *precisely* the human-rotted axis of our own threat model (SM125). **The mechanism relies on the faculty the
threat model says will fail.**

---

## 4. ✅ LANDED in the security model as `SECURITY-MODEL.md` §3.5 (2026-07-17)

> **⚠️ It was an ORPHAN for ~15 minutes, and BR caught it.** This section originally read *"a **candidate** guard rule
> for §3"* — i.e. the agent wrote a rule for a document, **into a different document**, and stopped. **Narratively
> homed, actionably homeless** — the *exact* shape SM132's audit had named an hour earlier (`sm132-substrate-truth-audit-2026-07-17.md`
> §3), committed by the agent that named it. BR: *"it is now pinned to SECURITY-MODEL.md? or else go do it."*
> **The audit's rule does not arm the audit's author.** Add it to the tally: *carried ≠ armed*, *hot ≠ armed*, and now
> **found-and-written-up ≠ armed.** ⇒ Landed at §3.5, with the introspective rationale BR asked for (the agent
> **generates** this risk), and the two dialog defects (§2, §3) folded in.

The rule as landed, in the same *"recorded with the mistake that bought it"* style §3 already uses:

> **Never express a path constraint as a permission pattern — it is unenforceable, not merely weak.** Move it into a
> tool that resolves the path and checks containment. *Bought by:* two 2026-07-17 stalls where the harness offered
> `mkdir *` and `mv *`; `..` defeats every prefix rule either could have been narrowed to.

**And it composes with §3's existing rules**, all of which are the same shape: *never emit `allow`* (do not bypass
the human's layer) · *a false positive is a bug, not a margin* (do not spend the budget for nothing) · *put the fix
outside the agent or it rots*. ⇒ **This adds: put the fix at the layer that can evaluate it.**

---

## 5. ⚠️ The uncomfortable half — the AGENT manufactured both stalls

**Neither stall was the guard being noisy.** Both were the agent reaching for a shell verb **out of habit, for
something it did not need** (Write creates dirs; `git mv` moves tracked files). ⇒ **SM129's thesis, from the other
side:** *every stall risks a blanket-allow, so stalls are a budget and ALL of them must be minimised — not just the
false ones.*

> **The agent spent TWO draws on BR's blanket-allow budget in FIVE MINUTES, both avoidable, and each draw put an
> irreversible policy change one keystroke away from a human who was mid-task.**

**That is the honest framing: the agent is not merely a consumer of the guard, it is a GENERATOR of the risk the
guard exists to contain.** Ties `guard-suggests-blanket-date-glob-but-tt-chrono-exists-2026-07-10.md` — **the same
shape, seven days earlier: the harness proposed an over-broad glob and the right answer was already in the toolbox.**
**It recurred.** *(Cf. `36f1532`: the quoted-arg false positive recurred 3 days after being logged AND skilled. This
one recurred after 7. The pattern of the pattern: knowing about a shape does not stop it — only removing the reach
does.)*

## Cross-links

`guard-suggests-blanket-date-glob-but-tt-chrono-exists-2026-07-10.md` (**the 7-day-earlier twin**) ·
`research/theory/genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md` (SM125 — the
human-may-be-BHH frame; **§2 and §3 here are both attacks on the human's attention, which is that frame's soft
target**) · `SECURITY-MODEL.md` §3 (SM131 — the rule above wants a home there) · `100b6c0` (the guard-stalled `rm`:
*a blanket allow permanently disarms the guard*) · `f4dae63` (**the harness's classifier is opaque and MOVES: the
same `mkdir -p` shape ran clean at 00:2x and gated at 10:0x — a gate we neither control nor audit is luck with good
manners**) · SM129 (the stall budget) · SM134 #9 (`tt git mv`).
