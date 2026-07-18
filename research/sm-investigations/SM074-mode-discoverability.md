# SM074 — mode discoverability for new users: plan + draft copy

**Status: DRAFT (agent-drafted AFK 2026-07-18), content only.** The actual wiring into shipped surfaces
(`docs/gs-help.txt` welcome, `tools/README.md`, the `gs-dwim` skill) is **BR-present** — it touches the alpha
welcome banner (version-gated, `docs/RELEASING.md:23`) and the shipped command list. This is the discoverability
plan + ready-to-place copy.

## The gap

`tt mode` works and the mode line renders, but a **new user has no on-ramp**. What exists today:
- `tt mode --help` lists the label vocabulary (tok-spend, token-saving, hot-harvest, high-context, solo,
  human-stress, rot-vigil, racing) but **not when/why** to reach for each.
- The `/genscalator` welcome (`SM056` draft §A, later `docs/gs-help.txt`) has one paragraph on the mode line —
  but it is **turn-it-on mechanics only** ("append `--mode-line`", "add/remove with `+`/`-`"), no pedagogy.
- The `gs-dwim` skill exposes `gs cues` / `gs cue <what>` and `gs dances`, but **no `gs modes` sibling**.

So a newcomer can *see* a mode label appear but has no path to learn that (a) it is a **declared, shared
state-of-mind**, (b) **either party** may declare or clear it, (c) declaring **changes how the agent works**, and
(d) which label to reach for when.

## The plan — surface modes at four touchpoints (least to most work)

1. **`tt mode --help`** (smallest, already the reference): add a one-line *why* after each label. Cheapest win;
   it is the doc a curious user hits first.
2. **A `gs modes` / `gs mode <what>` DWIM command** — the missing sibling of `gs cues`. `gs modes` lists the
   vocabulary with the one-liner; `gs mode <what>` explains the mode nearest in meaning. Agent-facing, so it
   works the moment the plugin is active (no settings), exactly like `gs cues`. Spec below.
3. **A short "Modes" section in `tools/README.md` / the manual** — the example-use-cases table below. This is
   the "example use cases showing when/why" the SM074 pin asks for.
4. **Extend the welcome's mode paragraph** (`docs/gs-help.txt`) with two sentences of *why* + a pointer to
   `gs modes`, keeping the mechanics it already has.

Grounding note (do not lose in the wiring): modes are **declared, not derived** — the whole point is that the
state is *stated* rather than guessed. The discoverability copy must lead with that, or a newcomer will expect
the agent to infer modes automatically.

## Draft copy A — the example-use-cases table (for the README/manual + `gs modes`)

| mode | declare it when… | what changes |
|---|---|---|
| `afk` | you step away from the keyboard | agent does only safe-solo work; won't route a guard prompt to an absent you |
| `solo` | you want the agent to proceed independently | fewer check-ins; agent uses sensible defaults + reports |
| `tok-spend` | you have token headroom for this stretch | agent goes deeper, spawns help, is less terse |
| `token-saving` | tokens are tight / near a reset | agent is terse, defers heavy work, batches |
| `racing` | your messages are overlapping the agent's replies | agent reconciles the *whole* queue before acting, so it doesn't get lost |
| `high-context` | the context window is filling | agent watches for rot and proposes a compact at the smart zone |
| `rot-vigil` | you sense drift / it's late | agent double-checks mechanical edits (whitespace, arg-order, anchors) first |
| `human-stress` | you tell the agent you're stressed | agent reduces load, defers, steers to a safe stop |
| `hot-harvest` | *(verify wording with BR)* you're actively mining workflow-research from the session | agent captures WR notes liberally |

*(Each row's "what changes" is grounded in the matching cue memory; `hot-harvest` is my inference — confirm the
one-liner before shipping.)*

## Draft copy B — the `gs modes` DWIM command (for `skills/gs-dwim/SKILL.md`, Tier 1)

Add alongside `gs cues`:

```
gs modes             list the mode labels (the declared joint state-of-mind) and one line on each
gs mode <what>       explain the mode nearest in meaning to <what> (e.g. gs mode afk)
```

Behaviour (mirrors `gs cues`): render the table above; `gs mode <what>` fuzzy-matches to the nearest label and
gives the "when + what changes" plus the `+<label>` / `-<label>` shorthand. Keep a `docs/modes.txt` current when
the vocabulary changes (same pattern as `docs/gs-help.txt`). *Disambiguation:* a leading `gs mode …` is the DWIM
explainer; the **operation** is still `tt mode add/rm <label>` or the `+`/`-` shorthand.

## Draft copy C — two sentences to add to the welcome's mode paragraph

After the existing "turn it on" mechanics in `docs/gs-help.txt`:

> A mode is a **declared, shared state-of-mind** — not something the agent guesses. Either of you can add one
> (`+tok-spend`) or clear it (`-afk`); it changes how the agent works (an `afk` agent sticks to safe-solo work; a
> `token-saving` agent gets terse). Run **`gs modes`** to see them all and when to reach for each.

## Ties
SM056 (the welcome body — extend, don't duplicate; §A already has the mechanics) · SM064 (mode colour/sort UX —
a visual half of the same discoverability) · SM120 (the `+`/`-` shorthand the copy references) · the
declared-not-derived / disinfo thread (the framing to lead with) · the cue memories that supply each "why".
