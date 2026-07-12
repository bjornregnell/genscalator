# WR specimen: the live-edit dance (do-what-I-mean, agent holds the pen) - 2026-07-12

**Context.** Reviewing blog 021, BR stopped opening the file to edit it and instead threw a stream of review
comments and new text fragments into the CHAT feed - often cue-less, not even naming the doc - and the agent
applied each edit **live** to the file while BR watched it update. BR, delighted: "the text emerges in my flow of
review commenting ... a LOVELY do-what-I-mean editing mode that the emacs/vi creators would have loved."

**What happened, concretely.** In one burst the 021 text was shaped entirely this way: passive-voice fix, softened
"aaargh", the pull-quote, a self-attribution added then removed on humbleness, the cruelty-corrupts-the-human
caveat woven in - BR directing in natural language, the agent editing and committing, BR never touching the buffer.

## Why it matters (SM054)

- **A new collaboration MO, and an escalation beat (RQ2).** Human = intent + judgment held in flow; agent = precise
  mechanical editing + session-context inference. The document co-emerges faster and more fluidly than either party
  could manage alone. The human stays in flow (no context-switch to editing mechanics), which is exactly the
  condition blog 021's Opening names as peak human productivity.
- **The enabling safety invariant: ONE writer.** BR drew the line sharply: **edit-BUFFER racing is forbidden**
  (two writers clobbering the file), but **session-FEED racing is tolerated** ("we can tolerate session msg feed
  racing"). By editing *indirectly through the agent* and never touching the buffer, BR removes the dangerous race
  and keeps only the benign, handled one. A nice inversion of the usual no-clobber rule: here the human cedes the
  pen on purpose.
- **DWIM, realized.** The do-what-I-mean dream (emacs / vi / Interlisp) was always brittle because it guessed;
  here it works because the agent understands intent from context. A concrete "what agents newly make possible"
  datum for the broader genscalator thesis.

Ties: [[cue-we-are-racing]], [[harness-double-post-edit-race]], [[no-clobber-human-owned-files]] (inverted), the
flow/productivity point in blog 021, and [[live-edit-dance]] (the memory).
