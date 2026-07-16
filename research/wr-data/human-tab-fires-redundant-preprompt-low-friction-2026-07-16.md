# Human TAB-fires a redundant preprompt — low-friction accidental action (2026-07-16)

**Type:** WR data — a **human-side** attention/friction specimen + an open UX question BR raised. Benign here
(the fired action was idempotent). BR flagged it live: *"preprompt was redundant but just pressed TAB and fired
it... specimen of uncareful reading by human (here unproblematic). BUT i'm not sure if i like those preprompts...
they should not be so very easy to just fire, but i am not sure if it is a bug or a feature."*
**Threads:** [[joint-rot-vigilance-recovery-kit]], the friction-as-safety theme (guard stalls add friction on
purpose), [[never-blanket-allow-destructive-commands]] (rubber-stamp risk), the post-compact mechanical-regression
note (both are low-attention failure modes — one agent-side, one human-side).

## What happened (observable, as reported)

BR pressed **TAB** and it **fired a preprompt** (a suggested / carried-over prompt — "+afk +solo go on with safe
solo menu") that was **redundant**: the modes were already set and `tt mode add` is idempotent, so nothing
duplicated and no harm followed. BR named it himself: a **specimen of uncareful reading by the human** — he fired
without fully re-reading. (I only know what BR reported; I do not see his keystrokes or the harness UI —
[[agent-blind-to-input-channel-and-timing]] — so this is his account, not my observation of the mechanism.)

## The specimen (why it's WR data even though it was harmless)

It is the **human-side analog** of the agent's mechanical slips: a **low-attention, low-friction action** that
fires more readily than the human intended. Here the target was idempotent so it was unproblematic — but the same
low-friction reflex on a **non-idempotent or destructive** target is exactly the rubber-stamp / blanket-allow
hazard (the tired human who fires a confirmation without reading). So joint-rot-vigilance is symmetric: the human's
attention lapses are as real and as worth designing-for as the agent's.

## The open design question BR raised (unresolved — bug or feature?)

Should a **preprompt be that easy to fire** (a single TAB)? The tension is **convenience vs accidental firing**:
- **Feature reading:** low friction is the point — fast to accept a good suggestion, keep the human in flow.
- **Bug reading:** firing a *whole prompt* with one un-deliberate keypress invites exactly the uncareful-reading
  misfire above; a prompt is a higher-stakes unit than a keystroke and might deserve a beat of friction
  (a confirm, a visible preview, a two-key accept) proportional to what it can trigger.

BR is **genuinely unsure** — recorded as an open question, not a verdict. The genscalator-relevant angle: this is
the **friction-calibration** question the whole project keeps meeting (guard stalls add friction where it protects;
here the worry is *too little* friction on prompt-firing). A candidate principle: **match the firing-friction to
the stakes of what the prompt can do** — trivial/idempotent prompts fire freely; a prompt that could trigger an
effectful or irreversible action earns a beat. (Not acted on; BR's call, and it is largely a harness-UX matter.)

## Corroborating specimen (same session): a WRONG guess that was also a LUCKY strike

While BR asked what "preprompt" means, the harness's autopreprompt suggested **`add podcast stubb "PC-003-..."`** —
a **bad guess** on intent (BR was asking a *definitional* question, not queuing another stub; it over-fit the
PC-000 / PC-001 / PC-002 pattern). BR: *"aha; bad guess!"* Then, crucially: *"but the bad guess was a lucky strike
cause it triggered my imagination"* — the wrong suggestion **sparked a real idea**, and BR promptly created an
actual PC-003 (a blacksmith tool-bootstrapping interview).

Two things this adds (keep-the-ball-game, both framings held):
1. **Hazard sharpened.** The first specimen fired something redundant-but-harmless; this shows the suggestion can be
   **wrong about intent**. Easy-to-fire × wrong-guess is strictly worse than easy-to-fire × redundant: had BR tabbed
   it, it would have injected an unwanted instruction the agent might then act on. The only things that stopped the
   bad guess becoming a bad action were the friction of BR **reading** it and the agent **flagging** it rather than
   auto-acting. Friction-to-stakes, again.
2. **Serendipity upside.** A *wrong* guess is not purely a cost — it can be a **creativity trigger**. The
   autopreprompt's mis-prediction worked like a random prompt / oblique-strategy card and produced genuine value (a
   new episode). So the design question is richer than "minimise misfires": a system that only ever suggests the
   *expected* next move forgoes this serendipity, and some **productive wrongness** may be a feature. The likely
   reconciliation mirrors the friction principle: let **low-stakes** suggestions run a bit wild (cheap to ignore,
   occasionally sparks), gate **high-stakes** ones. A small instance of a recurring project theme: **noise /
   wrongness as a source of value, not only risk.**
