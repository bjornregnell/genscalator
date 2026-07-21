# 055 — Eyes on the ball, or blinders? (RT055: focused-attention substrate and rot)

**Status: DESIGN NOTE (agent-drafted 2026-07-21, from the in-feed co-design with BR; the study
itself has NOT started — design ratification + go are BR's).** Blog delivery vehicle: `media/blog/
031-agent-blinders-or-eyes-on-the-ball.md` (stub-draft). Pinned as RT055 + SM193 in the closed
substrate.

## The concept pair (ratified vocabulary)

- **Eyes on the ball** ("ögonen på bollen" — the Swedish is a naturalized borrowing, so the pair
  self-translates): attention DIRECTED — the agent/human knows what to track. Positive by
  construction; the deliberateness is in the image (a player chooses to track the ball).
- **Blinders** (US) / blinkers (UK) / skygglappar (sv): input RESTRICTED — what cannot reach you
  at all. Figuratively PEJORATIVE in both English and Swedish ("blinkered" = narrow-minded), so
  in our usage the chosen-ness must be explicit: blinders worn on purpose. Demoted to
  mechanism-description; the practice is named by the positive member of the pair.

They are complements, not synonyms: direction says what to look at, restriction says what is
structurally unable to pull you sideways.

## The hypothesis (BR 2026-07-21, refined in-feed)

Deliberately narrowed reading surfaces serve the joint work specifically in:
1. **Rot vigilance** — less context pulled in per question means slower fill and fewer
   stale-fact collisions; and
2. **Hot harvesting** — one lens per harvest pass beats one sweep over everything.

**The rot-graded dial (the refinement):** at low rot, direction suffices — peripheral vision is
affordable and even valuable (serendipity, side-catch). At high rot, direction alone fails,
because rot's signature failure IS derailment-by-peripheral-pull (a stale fact or old thread in
context yanks the run). So structural restriction joins in as fill rises: read only the lens
files, no wide substrate opens. Direction always on; restriction increasing with fill.

**Stated trade-off (BR):** more places to look. The lens habit taxes discovery and navigation,
so it leans on good indexes (READMEs, the registry, a repo-structure guide).

**Pre-existing instances** (the hypothesis retro-dicts these, which is weak-but-real support):
the warp ember's bounded-reads rule; DumbZone discipline; the SM192 decision to keep `NOW.md`
and `SOLO-MENU.md` as SEPARATE narrow files ("context-rot aware skygglappar" — BR's phrasing at
the decision).

## Two-pronged empirical design (both, sequenced — they cover different halves)

### Prong A — dogfooding action-research case study (running for free)

The wr-data stream + meta-minion pushes are already the instrument; the lens-vs-sweep choice
occurs naturally many times per session. Observes REAL rot: degradation-with-history, where
stale context actively interferes. Cannot randomize; can generate hypotheses, effect-size hints,
and specimens. Cost: zero beyond what the substrate already collects.

### Prong B — controlled experiment: dumb local models as rot simulacra (BR's idea)

A small local model (via modly on the compute box) is a cheap, REPEATABLE low-capacity subject —
no need to burn a six-figure token count per trial inducing genuine fill in a frontier model.

- **Design: 2×2 factorial.**
  - Factor SUBSTRATE: blinders (agent may read ONLY: the task file + NOW.md + SOLO-MENU.md) vs
    open (whole repo readable).
  - Factor CAPACITY: strong (a frontier sub-agent) vs dumb (a small local model via modly).
- **Task battery:** fixed, muntabot-sized coding/reading tasks (pen-and-paper scale), held
  constant across cells; multiple instances per cell for variance.
- **Derail metrics (objective, transcript-countable):** off-task file reads, wrong-target
  edits, task completion, wall/token cost. No self-report — the subject's own sense of how it
  went is the least trustworthy instrument in the room.
- **REGISTERED PREDICTION (written before any run):** restriction helps the dumb model MORE
  than the strong one — an interaction effect, not just a main effect. The strong model under
  open substrate mostly ignores the periphery anyway; the weak one gets pulled.

### Validity, stated up front (the honest part)

The dumb-model-as-rot-proxy is an ANALOG, not an identity. A small model is uniformly weak with
NO history: it models rot's **capacity** loss but not its **interference** component (the
history-laden pull of stale context). So Prong B tests the capacity half cleanly; Prong A
observes the interference half in vivo without control. That is exactly why the prongs are
complements, not alternatives — any write-up must say so. Further threats: task-battery
representativeness (small tasks may under-trigger derailment), harness differences between the
sub-agent and modly lanes (keep the prompt scaffold identical), and the experimenter being the
same agent that designed the hypothesis (mitigate: registered prediction above, objective
metrics, meta-minion audit of the analysis).

## Feasibility notes

modly runs on the compute box (dogfooding preference: modly over raw Ollama, and improvement
gaps get tracked upstream). The SM186 A/B harness pattern (parallel sub-agents, identical task,
one varied factor, exact command-log return) transfers directly. Study log home: this repo's
`research/` per the study-log convention, when BR ratifies the design and says go.

## Ties

SM193 (the practice + term), SM192 (NOW.md / SOLO-MENU.md — the first structural blinders),
RT050 (delegation-vs-rot), blog 001 (rot ≈ fatigue), blog 011 (how dumb did the agent get),
blog 031 (the delivery vehicle for this note).
