# The status line at a glance REPLACES the slash-command dances — a self-built instrument the harness did not ship

**2026-07-17.** BR, unprompted, mid-session: *"the status line IS really marvelous; human can just with a glimpse
see ctx-fill and rot? and lim etc; saves me a lot of chatting and slash-commands and what not (no more slash-cost
dance and what not). REALLY GOOD UX that anthropic didnt give us!!! BR happy!"*

## The datapoint — a positive one, which the corpus under-collects

⚠️ **Note the sampling bias this note corrects.** `wr-data/` is dominated by **failures** (stalls, false claims,
rot) because failures demand a fix. **A tool that just quietly works generates no incident, so it is under-recorded**
— and that silence reads, wrongly, as "nothing good happened." **This is a deliberate positive specimen: an
externalisation that PAID OFF, logged because it did.**

## What it replaced — three dances collapse into a glance

The status line (`tt statusline`, rendered every turn) surfaces **measured** state continuously:
`ctx-fill · rot? · tot · silent Ns · 5h-lim · wk-lim · cost`. Before it, each of those was a **human-initiated
ritual**:

| before (a dance) | after |
|---|---|
| **context dance** — BR runs `/context`, agent narrates fill-vs-ceiling | **glance** — fill is always on screen |
| **token-usage / "slash-cost" dance** — BR runs `/cost`, pastes it, agent reads | **glance** — cost + both limits always on screen |
| **"are we rotting?" chat** — BR asks, agent estimates from felt length | **glance** — `rot?` is a rendered number |

⭐ **The mechanism of the saving is the load-bearing part: it moved state from a PULL to a PUSH.** A dance is
**pull** — the human must *remember to ask*, spend a turn, and read a reply. The status line is **push** — the state
is *already there*, at zero turns and zero attention-initiation cost. ⇒ **the dance's real cost was never the
slash-command; it was the HUMAN having to decide to run it**, which is exactly the faculty that degrades under
fatigue (the §2 security-model axis). **A glance has no initiation cost, so it does not degrade.**

## ⇒ Why this is a genscalator thesis specimen, not just a nice UX win

- **"Anthropic didn't give us this" is the point, not an aside.** It is a concrete instance of the standing frame:
  the substrate we build **extends the pair's capability past what the vendor shipped** — smarter/safer/faster by
  externalisation. **The harness exposes the data (`tt statusline` reads `cost.total_cost_usd`, ctx, etc.); we built
  the instrument that renders it continuously.** The raw capability was latent; the *dashboard* was ours.
- **It is the same move as every dance** (005): externalise fragile state out of the fragile place. Here the fragile
  place is **the human's willingness to keep asking**, and the durable substrate is **one always-rendered line.**
  ⇒ **the status line is a dance that fully AUTOMATED itself** — the human step ("run the command") dissolved
  entirely, leaving only the glance. **That is the end-state a good dance aspires to: to stop being a dance.**
- **It is `code beats prose` in the UX register.** The old way was the agent *telling* BR the state in prose (which
  can rot, mislead, or confabulate — "we're probably fine"). The new way is a **measured, rendered number** BR reads
  himself. **Line 1 is the one self-report in this system that is not confabulation** (it is instrumented) — and the
  status line puts it where it can be *glanced*, not narrated.

## ⇒ Actionable / ties forward

- **This is direct motivation for SM139** (sample the status/mode line into an append-only research log): the same
  line that saves BR's attention live is **the natural DV for the rot/heat theory** if captured over time. **The UX
  win and the research instrument are the same artifact seen from two sides.**
- **Positive-specimen discipline:** when a built tool visibly saves the human effort and the human *says so*, log it
  — the corpus needs the numerator, not only the denominator of failures. *(This note is the first deliberate one of
  its kind; cf. the "report the null" discipline — here, "report the WIN.")*

## Honest limits

- **This is BR's SUBJECTIVE report of relief, not a measured attention saving.** It is real testimony (first-person,
  unprompted, specific) but it is **not** a stopwatch on dances-avoided. A measurable version would count
  `/context`+`/cost` invocations per session before vs after the status line shipped — **buildable, not built.**
- **Self-subject:** BR is the tool's author, its user, and the one praising it. Genuine enthusiasm, and a conflict
  of interest worth naming. The claim that survives independent of taste is the **mechanical** one: pull→push
  removes an initiation cost, and initiation cost is what fatigue attacks.
- ⛔ **NOT claimed: that the status line is complete or optimal.** SM134 carries open defects on it (mode-line
  provenance #3, whose-state #6). **Marvellous AND unfinished.**

## Ties

**SM139** (sample the line into a research log — the same artifact as instrument) · **SM134 #3/#6** (open status/mode
defects) · **blog/005** (dances — the status line as the dance that automated itself) ·
[[code-beats-prose-a-rule-fires-only-when-it-governs-the-object-of-attention-2026-07-17]] (measured glance vs
narrated prose) · **SM118** (what `tt statusline` can actually read — the feasibility ground) ·
[[propose-compact-dance-at-trigger]] · [[token-budget-modes]] (the cost/limits half of the line).
