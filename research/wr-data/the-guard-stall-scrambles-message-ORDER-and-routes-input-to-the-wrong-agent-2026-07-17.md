# The guard stall scrambles message ORDER — and can route the human's input to the WRONG AGENT

**2026-07-17 13:02** (`tt chrono now`). **Two BR findings, filed together because they are the same defect at two
depths:** when a modal owns the terminal, the human loses control of **where** their words go and **when** they
arrive. Both are `sucks`-blog material (blog 004) and both are **security-relevant**, not cosmetic.

## Finding 1 (BR, #L) — ORDER becomes a FLUX, and order is meaning

**BR, verbatim:** *"When in guard the msg written by me in there is making the msg order in feed a flux, so it is not
obvious WHEN it wil be inserted in the feed and that can cause confusion as order matters when you or me interpret
messages"*

A message typed **inside** a guard modal does not land where it was typed. It is inserted into the feed at a moment
neither party controls or can predict. ⇒ **the feed's ORDER stops being a record of the conversation's order.**

**Why that is not cosmetic:** order *is* semantics in this workflow. `-hangover` means one thing before a report and
another after it; `go` refers to whatever preceded it. Both parties resolve references **positionally**. **A flux in
order is a flux in meaning.**

⭐ **And it is ASYMMETRIC in the worst direction.** BR sees the modal and knows he typed inside it. The agent sees
**only the content, never the channel or the timing** ([[agent-blind-to-input-channel-and-timing]]) — so **the agent
cannot detect that a message arrived out of order.** BR can suspect; the agent cannot. ⇒ the party better placed to
catch the scramble is the one who caused it, and the party who must act on it is blind.

**Live specimens, this session:** BR's `-hangover`, `fyi: status line ... reads very good`, and `go look at
resume-prompt` all arrived **mid-turn**, interleaved into the agent's tool-call stream, surfaced as *"the user sent a
new message while you were working"*. Survivable **only** because none of them depended on position. **Ties**
[[harness-double-post-edit-race]] (a near-identical pair = one edited message) and [[cue-we-are-racing]] (the core
risk is that the agent gets LOST) — **same family: the feed is not a reliable sequence.**

## Finding 2 (BR, live) — the input goes to WHOEVER STALLED, not to whoever you are talking to

**⭐ This is the sharper one and it was discovered by accident today.**

BR returned from eating, saw a stall, and **by habit** pasted `WR data` + the whole stall text — **believing he was
talking to CO4.** He was not. **The stall belonged to the fable meta-minion**, so **his keystrokes went to the
sub-agent.** CO4 **never saw them**, and would never have known.

**Proof it landed elsewhere** — the minion's own push-2 report, unprompted:
> *"Also logged: BR's in-guard WR question — answered NO (not a compact-regression: wrong agent, wrong class, no
> prior corrected state; it's sub-agent allowlist-envelope drift, first-use shape, guardcheck-clean)."*

⇒ **the minion answered a question BR meant for CO4, correctly diagnosed BR's misattribution (*"wrong agent"*), and
told only itself.** The intended recipient was never in the loop.

### ⭐ BR ALREADY HAD THE INSTRUMENT AND DID NOT READ IT

**BR, verbatim:** *"normally when you are stalled and i say stuff in guard I can see what i typen in this gray-bg
repeat of it in feed BUT NOT THIS TIME. that was the cue that you never got it but some sub-agent who i dunno whats
doin'."*

**The grey-background echo of his own text in the feed = confirmation the input reached the main agent. Its ABSENCE =
it went somewhere else.** The signal was present, correct, and unread **at the time** — it only became legible in
hindsight.

⇒ **this is `carried ≠ armed` in the HUMAN**, and it is the cleanest specimen we have: BR was not missing
information, and he was not careless. **He had a working instrument on screen and did not consult it**, because the
modal demanded immediate action and re-entry was expensive. *(Sibling: the agent read "THIS FILE IS A CLAIM" and then
built on the file's "ground truth" twenty minutes later. Same shape, both parties, same day.)*

⇒ **the countermeasure is the same as everywhere else: not vigilance, but a MECHANICAL step.** BR's own fix, coined
on the spot: **inside a stall, type ONLY `in guard: [paste]` and ENTER** — then WR it properly later, when the feed
is not hijacked and both parties are back. **The grey echo makes it VERIFIABLE**: no echo ⇒ wrong recipient ⇒ stop.

## Why these are security findings, not UX polish

1. **The modal demands immediate action while the human is at their worst** (just returned, cold, context displaced).
   That is exactly when a blanket-allow is one keystroke away ([[never-blanket-allow-destructive-commands]]).
2. **A sub-agent can stall the human on the super-agent's behalf**, and the super-agent is blind to it — it cannot
   see the ask (asks leave no trace,
   [[sm129s-probe-counted-brs-pastes-not-stalls-the-agent-is-blind-to-its-own-asks-2026-07-17]]), cannot see the
   human's reply, and cannot know a dialog is open. ⇒ **"minimise stalls" cannot be enforced by the agent that is
   told to minimise them**, because a delegated agent generates them outside its view. **This directly threatens
   SM129's whole budget model.**
3. **The human cannot tell which agent they are addressing.** Identity is implicit and determined by *who stalled*.
   With one agent that is invisible-but-harmless. **With minions it is a live misrouting bug** — and the study
   deliberately runs minions.
4. **Both defects worsen exactly as we scale delegation**, which is genscalator's direction.

## Honest limits

- **Finding 1 is BR's report + this session's mid-turn arrivals.** The *mechanism* (what decides insertion order) is
  **CANNOT VERIFY from inside** — the agent sees content, never channel or timing. **Do not theorise it; ask
  `claude-code-guide`.**
- **Finding 2 is n=1**, but with **documentary proof** (the minion's own log) rather than inference. Not measured:
  whether the grey echo is reliable in general, or an artifact of how the main agent's input is rendered. **BR's
  observation, one occasion. Worth a deliberate test before it is built on.**
- ⛔ **NOT asserted: that the routing is a bug rather than a design.** Routing input to the stalled agent may be
  correct and unavoidable. **The defect is that the human is not TOLD which agent is asking** — which is a labelling
  problem, and cheap, if the dialog can name its agent.

## → Blog 004 (`sucks`), the next beat

The post has *"The click that was not a decision"* (a mouse click is a raise-window twitch that can blanket-allow).
**Its sibling: "the paste that reached the wrong agent."** Same root: **a modal seizes the channel, and the human's
intent and the system's interpretation come apart** — once about *what* was approved, once about *who* was addressed,
once about *when* it was said. ⚠️ **BR-voice, and it is his story, so it needs his telling** — the agent draft should
stop at the mechanism. ⭐ **The strongest beat is the grey echo**: the instrument was on screen, correct, and unread.
That is the whole thesis in one image, and it lands on the human, not the machine, which is what makes it honest
rather than smug.

## Ties

[[agent-blind-to-input-channel-and-timing]] · [[harness-double-post-edit-race]] · [[cue-we-are-racing]] ·
[[cue-guard-stall]] (the `ig:` cue — **now needs the protocol above attached to it**) ·
[[never-blanket-allow-destructive-commands]] · [[earned-trust-obligates-flagging-risk-more]] ·
[[sm129s-probe-counted-brs-pastes-not-stalls-the-agent-is-blind-to-its-own-asks-2026-07-17]] (asks leave no trace) ·
SM129 (the stall budget — **a delegated agent spends it unseen**) · SM130 (the canary — **cannot watch what it cannot
see**) · blog 004.
