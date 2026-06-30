# RAW-DATA — curated verbatim excerpts from the live human-agent sessions

**Purpose.** A durable, committed store of *verbatim* chat excerpts (with real timestamps) that matter for the
research, plus the agent's reflections. It exists because the raw context is lost from the **agent's** working
memory at each **compact dance**, taking the *curation* (which moments mattered and why) with it — even though
the raw transcript itself persists in the session `.jsonl`. This file is the curation, made durable in git.

**How it is produced (important for honesty).** Excerpts are **mined verbatim from the session jsonl** by the
typed tool [`RawData.scala`](RawData.scala) — they are NOT retyped by the agent from memory (which would be
lossy and, per [`METHODOLOGY.md`](METHODOLOGY.md) §5, confabulation-prone). The agent adds **reflections** as
clearly-separated `> **Agent reflection:**` annotations. So: *excerpt = objective (mined); reflection =
subjective (labelled).* This is METHODOLOGY §4 (behavioral mining over self-report) applied to data capture.

**Workflow.**
```
scala-cli run research/RawData.scala -- --list --grep "WR data"        # find turn indices
scala-cli run research/RawData.scala -- --dump 6129..6135              # preview verbatim
scala-cli run research/RawData.scala -- --append 6129..6135 --note "…" # commit it here
```
Ranges use `FROM..TO` (or two ints `FROM TO`), **never** `<FROM-TO>` — zsh reads `<N-M>` as a numeric-range
glob and the harness guard locks on it (incidental complexity; see the WR-FRICTION entry on the guard glob).
Turn indices (`#N`) are the jsonl's user+assistant ordering and are STABLE for a given session file; the
`--list` headers note when a turn is a post-compact "session continued" summary (a natural episode boundary).

**Coding legend.** Each `> **Agent reflection:**` line is tagged with one or more WR-* codes (authoritative
source: METHODOLOGY §4); this legend makes the file self-contained. The codes are **textual** tags — the
`#`-tokens *inside entries* are `#N` *turn indices* (explained above), not colours. The `hex` column below
assigns each code a colour: VSCode's markdown preview renders `#RRGGBB` as a colour box, so the eight codes
are visually distinct at a glance. The hexes are **maximally saturated and hue-separated** — roughly a rainbow
(red → orange → gold → green → cyan → blue → purple → magenta) — so they stand out and differ meaningfully for
the human reader. Adopt or retune as wanted.

| code | hex | meaning |
|------|-----|---------|
| **WR-REGRESS**  | `#E60000` | agent fell back to a banned/worse pattern (shell bundle, raw `cat`, `cd`+git) despite a rule |
| **WR-FRICTION** | `#FF7A00` | a confirmation/approval prompt or other human-attention cost actually incurred |
| **WR-STATE**    | `#FFC400` | a signal about human or agent state (fatigue, self-disclosure, smart/dumb zone) |
| **WR-WIN**      | `#00C853` | a tool/structure prevented a problem, or a static check caught a bug (the desired path) |
| **WR-DECISION** | `#00B8D4` | a human decision / **HD** (hard decision) recorded so the agent can act on it |
| **WR-TOOL**     | `#2962FF` | a concrete tool/allowlist/instrumentation change proposed or built to design a friction away |
| **WR-META**     | `#AA00FF` | a reflection *about* the method itself (data capture, the roundtrip, the substrate hierarchy) |
| **WR-INVENT**   | `#FF2D9B` | a new collaborative *way of working* / practice / convention is **created** in-flight (the "WR data" flag protocol, the compact dance, the meta-project itself, build-your-own-tool, the AT/GS split, smart/dumb-zone vocabulary). Distinct from WR-META: INVENT *makes* a practice, META *reflects on* one — the generative moments that became genscalator's methods. |

---

## Excerpt #6159–6159 · 2026-06-26

**[🧑 BR · 17:26:21 · #6159]**

1.  I soon need to eat anyway. AND note in the joint-zone research that humans may reveal things about themselves such as going to eat so agent get an idea oh human state perhaps; or is this just noise that fills the agents context window with not so useful information that derails agent focus; do you get me?


> **Agent reflection:** WR-STATE. Voluntary human self-disclosure of state ('I soon need to eat') = the highest-signal, cheapest human-state input (METHODOLOGY-adjacent; human-state-and-joint-zone.md §5). Correct agent response: compress to the actionable bit (human about to be unavailable + heading to a hunger dip) -> produce durable reviewable state, do not block on human input; do NOT ruminate or store as a memory. BR also turned it into a research question (signal vs context-noise), which is itself the joint reflexive method (METHODOLOGY §5).

---

## Excerpt #5462–5462 · 2026-06-26

**[🧑 BR · 14:43:24 · #5462]**

⟦tool_result⟧
WR data you did a pipe to grep; should use some scala tool


> **Agent reflection:** WR-REGRESS (recovered from BEFORE a compact dance — no longer in agent context; proves the jsonl-mining durability win). Agent piped a scala-cli scratch through grep instead of fixing the scratch to emit clean output. Same reflex family as the later META-2 raw-cat regression: shell post-processing of a typed tool's output. Fix is structural (the tt run / submit-time hook), not exhortation.

---

## Excerpt #6315–6315 · 2026-06-30

**[🧑 BR · 18:12:16 · #6315]**

⟦tool_result⟧
WR data


> **Agent reflection:** WR-REGRESS -> WR-FRICTION causal link (the key finding). Agent ran 'cd introprog && scala-cli ... 2>/dev/null | tail' — three smells: cd-compound, stderr-suppression, pipe-to-tail. This tripped the confirmation guard ('Compound command contains cd with output redirection - manual approval required to prevent path resolution bypass'), COSTING BR a confirmation prompt. So the shell-scaffolding reflex (WR-REGRESS family: see also pipe-to-grep #5462, raw-cat META-2) directly CAUSES the confirmation-fatigue (WR-FRICTION) the project fights — they are the same problem. Structural fixes applied: (1) tools self-report to a file (instrumentation-by-default) so output is Read, never shell-cleaned; (2) use the tool's OWN --grep, never pipe to tail; (3) never combine cd with redirection/pipe. Third+ instance this session = the shell-wrap reflex is the most persistent; strongest case for the submit-time hook + instrumentation-by-default.

---

## Excerpt #6402–6402 · 2026-06-30

**[🧑 BR · 18:28:07 · #6402]**

⟦tool_result⟧
WR data, guard says reason: " This command changes directory before running git, which can execute untrusted hooks from the target directory. Approve only if you trust it."  (remember that it  is ok for me to see git commitss as I have say I want them, but the action should not trigger the guard if possible)


> **Agent reflection:** WR-REGRESS META-4 (cd+git). Agent ran 'cd introprog && git add && git commit && git push' instead of 'git -C <abs-path>', tripping the untrusted-hooks guard -> another needless confirmation. 4th cd/shell-compound regression this session (pipe-to-grep #5462, raw-cat META-2, cd+redirect, now cd+git). Pattern is unmistakable: the agent reflexively prefixes 'cd <project> &&' and chains shell, carrying the scratch-runner cd-habit onto git where 'git -C' is correct. The agent USES git -C correctly for genscalator but regressed on introprog = context-dependent reflex, not ignorance. Strongest cumulative case for a submit-time hook that rewrites 'cd X && <git|tool>' to the safe form. BR's constraint: he wants to SEE commits (good) but the action must not prompt; git -C satisfies both. Self-captured during the autonomous stretch per the self-reminding-as-method commitment (METHODOLOGY §5).

---

## Excerpt #6456–6456 · 2026-06-30

**[🧑 BR · 19:55:08 · #6456]**

⟦tool_result⟧
but consider that the swedish term paraFärg is short for "parallell color" and perhaps there is such a concept in english card playing games simlar to "paralell suit" or is that what "partner suit" means? Go with your best pick after a brief look into what you know about card game or try a wikipedia search if you think it is worth it


> **Agent reflection:** WR-DECISION (HD origin). BR supplies domain etymology the agent lacked: paraFärg = short for 'parallellfärg' = parallel/same-COLOUR suit (Spades<->Clubs black, Hearts<->Diamonds red). He delegates the naming with a bounded-research mandate ('best pick after a brief look ... or a wikipedia search if worth it') -- a clean autonomy negotiation (task-autonomy-negotiation.md): human gives intent + scope, agent decides depth. Agent web-checked: bridge pairs suits by colour but has NO single standard English word; 'partnerSuit' collides with bridge 'partner'=player. Chose self-documenting sameColourSuit. BR ratified at #6515 ('HD: sameColourSuit, Färg->Suit is good'). The two turns together = the full HD loop (human intent+etymology -> agent bounded research -> proposal -> human ratify); note Färg->Suit is CARDS-CONTEXT-ONLY (a kojo/colour file needs Färg->Colour).

---

## Excerpt #6729–6729 · 2026-06-30

**[🧑 BR · 20:33:16 · #6729]**

⟦tool_result⟧
WR data "Contains zsh <N-M> numeric-range glob"  could we make a tool? should we ha another convention than <N-M> to not trigger the zsh glob guard lock? This is INCIDENTAL COMPLEXITY carried over to us by the guardrail harness and its fair that we try to circumvent it if we find an easy way


> **Agent reflection:** WR-FRICTION, and a DISTINCT category from the shell-reflex WR-REGRESS family: here the guard is a FALSE POSITIVE on a perfectly legitimate convention. zsh reads <N-M> (angle-bracketed numeric range) as a numeric-range glob, so the harness guard locks on any command containing it -> a confirmation cost the agent did nothing wrong to earn. Recurring: also fired at #4764 earlier today. This is INCIDENTAL COMPLEXITY imposed BY the guardrail harness (BR's words), the inverse of the usual WR pattern (agent's bad reflex trips a guard); the fair fix is to route our own convention AROUND the guard. Applied: RawData.scala range arg now uses Scala-style FROM..TO (and space-separated FROM TO), never <FROM-TO>; usage docs + RAW-DATA workflow purged of the angle form; .. and space never glob so they never trip. Research angle: guard false-positives are a measurable TAX distinct from agent-caused friction -- a complete WR taxonomy must separate 'agent earned the prompt' from 'harness mis-fired', because only the latter is fixed by changing OUR notation rather than OUR behavior.

---

## Excerpt #6266–6268 · 2026-06-25

**[🧑 BR · 20:50:09 · #6266]**

⟦tool_result⟧
(and I had to confirm this because "Contains shell syntax (string) that cannot be statically analyzed"  - haha its what we are working on: statically analyzable cool stuff  :) )

**[🤖 agent · 20:50:53 · #6267]**

⟦thinking⟧

**[🤖 agent · 20:50:54 · #6268]**

😄 Perfect irony — and it's THE crux: **`$TT` is a shell variable, so the analyzer can't statically know it expands to an allowlisted path → it must ask.** Literal, statically-analyzable commands don't. That's the whole thesis in one confirmation. Logging it:


> **Agent reflection:** WR-WIN + WR-TOOL — the single cleanest demonstration of the project thesis (category 1: typed-tool GAIN). BR had to confirm a command only because it used $TT, a SHELL VARIABLE the guard flagged as 'shell syntax that cannot be statically analyzed'. The gain of a literal, allowlisted 'tt' launcher on PATH is therefore not merely correctness — it is that the HARNESS CAN PROVE IT SAFE, so it runs with ZERO confirmation. A dynamic shell var has an intrinsic, unavoidable cost: a static analyzer cannot know what $TT expands to, so it MUST conservatively prompt. So 'static analyzability' is the bridge between the agent's tooling choice and the human's confirmation load: literal/typed -> provable -> frictionless; dynamic -> unprovable -> taxed. Agent #6268: 'the whole thesis in one confirmation.' This is the positive mirror image of every WR-REGRESS shell-bundle entry.

---

## Excerpt #5440–5440 · 2026-06-25

**[🤖 agent · 10:04:54 · #5440]**

Crucial realization: my `--only w01` verification just **reverted the mirror** — `--only` recreates `compendium-en/` translating only w01 and **copying everything else as Swedish**. So the current exercise/lab/prechapter files are Swedish *because of that*, not because `--all` can't translate them. I need to restore the full `--all` state (also applies all 13 overrides corpus-wide). Let me relaunch it + rebuild, then diagnose the *genuinely* Swedish parts:


> **Agent reflection:** WR-REGRESS + WR-FRICTION (category 2: dynamic-tool/flag COST needing extra verification). The agent ran 'autotranslate --only w01' as a quick VERIFICATION step — but --only silently RECREATES the whole compendium-en/ mirror, translating only w01 and copying everything else as Swedish. So the 'verification' DESTROYED corpus-wide state (reverted all overrides + translated content to Swedish copies), which the agent only caught by noticing the mirror had regressed. Recovery cost: a full ~2-3 min --all re-run + re-diagnosis before any real work could continue. This is exactly the class BR named: a dynamic flag whose destructive blast-radius is NOT statically obvious, so it needs extra human/agent verification (and here caused silent rework) that a safer-by-design tool would make impossible. Durable fix = the standing rule '--all not --only before measuring/shipping' (now in memory + every resume note); the deeper fix is a typed API that distinguishes 'translate a subset' from 'rebuild the mirror' at the type level so a measurement call can never clobber. Contrast directly with the #6266 tt-WIN: there static analyzability REMOVED friction; here a dynamic flag's invisible side effect ADDED it.

---

## Excerpt #6065–6065 · 2026-06-25

**[🧑 BR · 19:17:51 · #6065]**

in this working directory, can you start on a meta-level task with a new plan independent (but related to) the autotranslator that deals with our workflow. I will then give you 1) overall goals and 2) some things to learn about and summarize in a way that is useful for you and the will hopefully be real powertools! you should use your own meta-level knowledge about your workflow and "habits". Can you start some new markdown files in this working repo and when we are done with the workflos meta-level task we will have a complete new workflow that is reusable based on some cool things. Are you with me?


> **Agent reflection:** WR-INVENT (the headline meta-invention). This turn is the BIRTH of genscalator itself: BR asks the agent to start a 'meta-level task ... that deals with our workflow', using the agent's 'own meta-level knowledge about your workflow and habits', so that 'we will have a complete new workflow that is reusable based on some cool things ... real powertools'. This IS the genscalator spirit stated at conception: distil reusable tools + workflow FROM a live working case (the AT autotranslator), in the open, with the agent co-authoring the method it will then be governed by. Note the collaboration shape: human supplies (1) overall goals + (2) things to learn, agent supplies meta-introspection + builds the artifacts -> a joint research loop (METHODOLOGY §0 DSR-in-AR; §2 roundtrip). Everything later in this ledger (WR taxonomy, tt toolbox, compact dance, substrate hierarchy) descends from this single 'Are you with me?' moment.

---

## Excerpt #5831–5831 · 2026-06-25

**[🧑 BR · 13:47:25 · #5831]**

⟦tool_result⟧
but if you can make your own safe scala scratch program instead of all these obfuscation expansion bash hacks so go ahead and wont need to confirm as long as you do safe stuff


> **Agent reflection:** WR-INVENT + WR-TOOL. The origin of the project's signature working practice: BR converts a friction ('all these obfuscation expansion bash hacks') into a STANDING new way of working -- 'make your own safe scala scratch program ... and wont need to confirm as long as you do safe stuff'. Three genscalator principles are born in one sentence: (1) the agent BUILDS ITS OWN typed tools rather than improvising shell; (2) typed/statically-checked scratch is the default medium for analysis (became memory prefer-scala-scratch-over-bash + the whole tt toolbox); (3) the autonomy bargain -- safe, statically-analyzable actions earn no-confirmation trust (foreshadows the #6266 $TT-vs-literal-tt thesis). This is the human granting the agent a tool-making mandate, the inverse of the usual 'agent asks permission per command'. A defining collaboration-shape invention.

---

## Excerpt #7294–7294 · 2026-06-26

**[🧑 BR · 13:55:48 · #7294]**

Have you heard about the concepts of "smart zone" and "dumb zone" meaning that the agent may get "dumb" even if context window is fille only to X % where X might be as low as 30 % for a model with large context window, so this is related to token efficiency (TE) - do you think those concepts are worth adding to our conept list?


> **Agent reflection:** WR-INVENT + WR-STATE. BR introduces the 'smart zone / dumb zone' vocabulary (agent degrades even at ~30% fill of a large window; tied to token efficiency) and asks whether to adopt it into the shared concept list. Adopting it created a new way of working: a SHARED MENTAL MODEL that both partners now use to govern session quality, which directly spawned the 'compact dance' ritual (propose compaction when over the L ceiling), the smart-zone-ceiling.md research, and the token-usage/L brake. Note the collaboration pattern: BR brings an external concept, asks the agent to evaluate fit (not just accept), and it becomes joint vocabulary -- METHODOLOGY §5 reflexivity. Naming a phenomenon is itself a tool: once 'dumb zone' exists as a word, the agent can flag 'I am over L' and the human can act, which was impossible before the shared term. Vocabulary-as-infrastructure.

---
