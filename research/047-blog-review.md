# 047 blog (012) readability + structure review — merged Fable-5 findings, 2026-07-07

**What this is.** CO4 adjudication of four blind Fable-5 subagent reviews of `blog/012-will-i-lose-you.md`,
run solo while BR was AFK (Go #2 tail). Three readability lenses (developer-first, layman-digestible,
fidelity-to-brief) + one structure reviewer (one-post vs split). Each read the draft + the brief
(`047-PLAN.md` §1) + `047-refs.md`. This doc = (1) what CO4 already folded into the stub, (2) beats still for
BR at the voice pass, (3) the structure recommendation + both outlines. All quantitative claims below were
re-grounded by CO4 against the study log (`047-fresh-restart-fidelity.md`) before folding in.

---

## 1. Folded into the stub already (grounded corrections, not voice decisions)

- **Jargon sweep (BR register).** "arm" -> "part" (Bridge x2, coding-friend x1); "coding-arm orchestrator" ->
  "coding-test orchestrator". Kept research jargon (ablation, negative control, decoy, construct validity) out
  of the body; it stays in `047-writeup.md`. [[publications-match-br-register]]
- **The "coding scheme" collision (both readability lenses flagged as the single worst word-trap).** In a post
  full of actual code, "coding scheme" reads as programming. Renamed the second Discussion heading to "scoring
  rubric" and disambiguated at first use ("in research-methods terms a 'coding scheme' for labeling answers,
  nothing to do with programming").
- **"human-like raters" corrected (fidelity + dev both flagged; near-BS-trigger).** The finer style raters were
  two blind **Fable-5 models** (a different model), r=0.95, off the CO4 author axis, NOT humans. Reworded to
  "two independent AI raters (a different model), each blind to which model and which notes produced the code."
- **Result #3 given real numbers + a concrete botch + honest hedging (dev lens: the claim the whole dev pitch
  rests on had no number).** Added: correctness **0.60 (no notes) -> 0.41 (our notes)**; even the strongest
  local coder tested fell from passing every task to failing two in five; concrete botch = **C2 Rectangle ->
  broken `enum`** under the immutability convention; the "competes for attention" mechanism marked a **guess,
  not a measurement** (fidelity #3); scope bound added (Claude family + small local models; other frontier =
  future work, per plan §3.5); and the "your 400-line CLAUDE.md making your cheap agent dumber" transfer punch
  moved from a TODO into the body.
- **Tiered evidence in "We measured it" (fidelity's #1 fix).** The draft blurred ceilinged proxies with the one
  real cold start. Now separated: proxies = ceiling 16/16 (proves little); the **one real cold start** = ~9 in
  10, judged by 3 blind raters, **n=1**, triangulated 3 ways (member-check "you feel different" + behavioural
  over-deliberation + agent self-prediction). Planted the missing n=1 anchor.
- **Enactment reconciliation (fidelity coverage gap).** The coda's failure could read as "enactment always
  fails," the opposite of the Arm-5 result. Added a beat: the fleet's guardrails *did* fire across
  compaction/restart; what slipped was the author-agent's own hygiene under load. "Recall is not enactment" =
  the gap, not a universal failure claim.
- **"twice" de-whiplashed (dev flagged five-vs-twice).** Both counts are grounded and different: **five**(-plus)
  tail-stalls = one specimen; **twice** = two distinct reflexive specimens (the cd-git command after a reset +
  the tail peeks). Reworded so they no longer read as contradictory.
- **CF5 glossed** at first use ("a newer, different model generation, CF5").
- **255 arithmetic stated** (17 models x 5 tasks x 3 substrate conditions), per the dev lens.
- **"substrate" + "context reset" lightly defined** at the reframe (killed the "codec" metaphor, which meant
  nothing to laymen and duplicated the hook's "shared shorthand").

## 2. Still for BR at the voice pass (beats, not yet folded, mostly voice/structure)

- **Anchor "substrate"/"context reset" fully** (I added a first sentence; BR to make it sing). The layman lens:
  this is the single highest-value accessibility change; every later section leans on the word.
- **Skip-path signposts** around the coding-friend section: one line after the hook ("here for the experiment?
  jump to ..."), and mark the section skippable with an explicit "rejoin at the coda." Both readability lenses:
  the technical middle is currently an unsignposted island and the coda is exactly what a layman must not skip.
- **Light-anchor two refs the body never uses** (both prepared in `047-refs.md`): **ELIZA effect** (a named,
  documented 60-year-old tendency to bond with conversational systems, grounds the attachment honesty) and
  **Ship of Theseus** (the ship is replaced plank by plank; the agent is replaced all at once, but its planks
  were copied to paper first, which is the thesis in miniature). One line each; respect the refs-file caveats
  (ELIZA = 1966-extension; Theseus = allusion, gradual != instantaneous).
- **Promote the "gravity well" beat** (85 MB on disk vs ~284 k tokens on resume, "you re-enter the archive's
  gravity well, not its full mass") up out of the phrasebook into the reframe or the model-switch section: it
  is the most developer-legible evidence that even the warm path is a reconstruction.
- **Trim the tail** (dev lens: readers close the tab here): merge the two Discussion sections into one short
  "open question," move the mid-document `*Cross-refs:*` footer to the true end (it currently reads as "the
  post ended" then it didn't), and cut the phrasebook before publish.
- **Amnesia-friend + changed-personality-friend images** (plan §8 promised both, grounded in real events: the
  P3b cold start = friend rebuilding from notes; the model switch = friend whose personality changed). Neither
  is voiced yet; the model-switch section is currently analogyless.
- **COI placement:** give the Anthropic-settlement disclosure an actual home (opener footnote or closing note),
  not just a meta-instruction blockquote. [[br-anthropic-copyright-settlement-stakeholder]]
- **Minor glosses** a layman trips on (keep, anchor lightly): pinboard, commit/push, compile+test, action
  research/case study/reflexivity, echt, decoy ("a lying version of our notes"). Replace outright: Likert
  inventory -> "a 10,000-question agree/disagree quiz"; statistical saturation -> "more answers stop teaching
  you anything new"; corroboration-asymmetry -> the plain phrase beside it; "disconfirmer" -> "the prediction
  we got wrong."

## 3. Structure decision (BR's call) — reviewers recommend a SPLIT (~70%), one-post viable

**Why split.** (1) The title is the sharing unit and one post has one title; the coding result ("our style
guide was redundant, and handing it to small models made them worse") is the most shareable material, and it
is currently buried under an emotional identity frame, so the developers who would feel it never click. (2) The
original bundle rationale from the plan (§3.6: the *same* facts-carry/texture-leaks pattern in both media,
mutually confirming) was **disconfirmed** by the coding data; the two halves now carry two different lessons,
which is the signature of two posts. Plan §8 pre-authorized this as the "too heavy" fallback. (3) The split is
cheap: the coding post needs no warp/galaxy machinery (devs know style guides and system prompts).

**Honest counter (why one-post is real):** the arc "I feared losing you -> we wrote it all down -> recall came
back near-perfect -> but a perfect score can't detect loss -> so we tested it objectively in code -> and the
tidy prediction was wrong -> and the researcher tripped over its own thesis" is a genuinely beautiful single
narrative, and finding #1 (the style doc was redundant) IS the identity reframe refined. Splitting costs the
identity post its objective teeth (its remaining data is the ceilinged recall study + the coda). If BR wants
`012` to land as *science*, that argues one-post; if as an *honest reflective essay pointing at the hard data*,
split serves both better.

### Two judgment calls only BR can make
1. **One arc or two artifacts** (the core call).
2. **If split:** where the humbling coda lives (I'd put it in the coding post, where it happened and where devs
   will love it, but its moral "recall is not enactment" is the identity post's natural capstone), and the
   coding post's title (friend-title `did I lose you, my good coding friend?` = series continuity; result-title
   `our style guide was redundant, and it made the small models worse` = dev reach).

### OUTLINE A — one post (only survives with hard cuts)
1. Hook (as drafted). 2. **Skip-path signpost** (one italic line: "here for the experiment? jump to the
coding-friend test"). 3. Reframe (substrate; fold the hyperspace imagery in compressed). 4. We measured it
(short) **+ fold the ceiling caveat in immediately** (2-3 sentences, not its own far-away section). 5. **The
coding-friend test = centerpiece**, self-contained, all three findings; skip-path exit line at its end. 6.
Humbling coda (kicker). 7. Model switch + takeaway (merged, short). 8. "me" discussion (halved). CUT to the
research doc: the scoring-rubric discussion, most of the phrasebook, the loop section (compress to 3 sentences
+ point at the appendix). 9. Appendix.

### OUTLINE B — split (recommended)
**`012` identity (reflective essay):** hook -> reframe (warp/hyperspace/galaxy live here) -> we measured it
(recall carries) -> echt/ceiling caveat -> **one-paragraph coding summary + cross-link to 013** (this teaser
paragraph must carry finding #3 in one sentence) -> model switch (029 foreshadow) -> takeaway/human note ->
"me" discussion (this post's deep end) -> scoring-rubric discussion (it is about the *identity* instrument, the
Q-battery Key, so it belongs here) -> full COI disclosure.

**`013` coding (experiment report):** cold open on the concrete fear (working code that stops looking like ours)
+ 2-3 sentences of self-contained setup (we externalize our Scala conventions; does handing them over matter?)
+ one link back to 012 -> the experiment (17 models, 3 substrate conditions, 255 runs, auto-scored + finer
2-rater pass; what we predicted) -> the three surprises (body) -> honest disconfirmer + durable lesson -> how
the loop ran (short) -> **the humbling coda lands here** -> RT048 seam (which substrate content has power over
the agent, as future work) -> Appendix (@main + thread-timeout note) -> one-line COI footer pointing at 012.

**Seams:** substrate concept = full in 012, 2-sentence self-contained version in 013 (013 must NOT require
012). One cross-link paragraph per post at its natural seam, not scattered links. Publish 013 first or same-day
if reach matters (it stands alone and is the shareable one).

---
*Sources:* four Fable-5 subagent transcripts (this session, 2026-07-07); grounded against
`047-fresh-restart-fidelity.md` (CODING ARM RESULTS + finer style-rater + P3b) and the guard-stall wr-data.
*Cross-refs:* `blog/012-will-i-lose-you.md` (the stub, fixes folded), `047-PLAN.md` §1 (brief), `047-refs.md`.
