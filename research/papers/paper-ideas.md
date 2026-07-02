## Paper ideas and brainstorming

This is our scratchpad for human-agent paper-writing.

### Decisions (BR 2026-07-02) — read this first
- **Target venue:** **Springer Nature Requirements Engineering journal (REJ)**.
- **Working title:** BR keeps his own **first attempt** (below) over the agent-brainstormed variants — he prefers
  it for reasons he will elaborate later. Do NOT re-litigate the title.
- **Scope / focus of the FIRST paper (keep it grounded, NOT "too philosophical"):** the **`tt` tools to achieve
  qualities** (safety, token efficiency, …), the **meta-level requirements-engineering angle**, and **what we
  leverage by using reqT-lang** (expressing genscalator's own requirements in reqT, the dogfooding recursion).
  That is the main line.
- **What is OUT of the first paper (or at most a speculative Discussion section):** the philosophical arc from
  this session — coupled-system capability, extrinsic-volatile plasticity, the prosthetic-habit / structural-vs-
  knowledge-safeguard thesis, inference-time-learning as "the" thesis. **BR's hunch: this is a SEPARATE (second)
  paper.** It may appear briefly + speculatively in the REJ paper's Discussion, but must not become the focus.
  The RAW-DATA capability excerpts + the foundations glossary hold that material for the second paper.
- **Method framing:** design-science / action-research case study (as below).

### First paper on genscalator: a case study in design science  action research mode


#### Working title:

*First attempt:*

**Teaching Agents to Develop and Use Typed Tools During Inference for Safety and Productivity -- a case study in meta-level agentic requirements engineering**

---
Review comments by agent on *first attempt*

Strengths: it names the distinctive contribution (agents building + using typed tools at inference time), the payoff (safety + productivity), and a genuinely novel angle in the subtitle — meta-level agentic RE (your field + the reqT dogfooding). Few agent papers have that RE framing; it's a real second contribution, not just method.

Three things to weigh:
1. Length — it's a mouthful (two full claims + subtitle). A punchier main title + the case-study subtitle would land harder.
2. "Teaching … During Inference" is a deliberate paradox (teaching happens at inference via tools/structure, not weight updates) — a great hook, but a reader may misread "teaching" as training-time. Consider foregrounding the mechanism (tools/structure) so the paradox reads as intended.
3. Which thesis leads? You earlier called inference-time-learning "one thesis, maybe not THE thesis," with the down-to-earth backbone being productivity via static tooling. This title leads with "During Inference." My recommendation: lead with inference-time (it's the more novel, citable scientific claim) but ground it in the abstract with the down-to-earth safety/productivity payoff so it doesn't read as hype.

The single most memorable thing to surface: the reflexive dogfooding recursion — the agent builds tools to build tools, expresses its own requirements in reqT-lang, and the method is the object of study. That self-reference is the paper's most distinctive feature.

Three alternative framings to react to:
- (tighten yours) "Typed Tools at Inference Time: Agents Building Safe, Productive Tooling — a meta-level agentic-RE case study"
- (lead inference-learning) "Inference-Time Tooling: How an AI Agent Learns to Work Safely and Efficiently by Building Its Own Typed Tools" (design-science case study)
- (lead down-to-earth) "Power Tools for Agents: Safe-by-Design Typed Tooling Agents Build and Use — an action-research study in agentic requirements engineering"
---

#### Other alternative titles brainstormed by agent

Ten variants spanning the three framings (inference-time / down-to-earth productivity / RE-method) plus a few
distinct hooks; each tagged with the angle it leads with, to make choosing easier.

1. **Power Tools for Agents: Safe-by-Design Typed Tooling that Agents Build and Use** (an action-research case study in agentic requirements engineering) - *leads with the product/slogan; RE as method*
2. **Inference-Time Tooling: How an AI Agent Learns to Work Safely and Efficiently by Building Its Own Typed Tools** (a design-science case study) - *leads with the inference-time-learning thesis*
3. **Typed Tools at Inference Time: Agents Engineering Safe, Productive Tooling for Themselves** (a meta-level agentic-RE case study) - *the tightened first attempt*
4. **Teaching by Tooling: Externalizing Agent Competence into Typed, Verifiable Tools During Inference** - *keeps the "teaching" hook but relocates it to the mechanism (tools), resolving the training-vs-inference paradox*
5. **The Agent That Builds Its Own Tools: Safety and Productivity through Inference-Time Typed Tooling** - *punchy; foregrounds the reflexive self-building*
6. **Structure over Exhortation: Safe-by-Design Typed Tools that Make AI Coding Agents Safer and More Productive** - *leads with the core empirical finding (structure beats telling)*
7. **Dogfooding the Agent: Requirements-Driven Typed Tools an AI Agent Builds, Tests, and Uses at Inference Time** - *foregrounds the reflexive dogfooding recursion + the reqT/RE angle*
8. **From Prompts to Programs: Escalating Human-Agent Code Generation with Typed, Safe-by-Design Tools** (a meta-level agentic-RE case study) - *the genscalator "escalate" framing*
9. **A Notebook and a Toolbox: Inference-Time Learning by Externalizing Agent Competence into Typed Tools** - *evocative (the anterograde-amnesiac / Memento framing); leads with the substrate-hierarchy idea*
10. **Meta-Level Agentic Requirements Engineering: An Action-Research Study of Agents Building and Using Typed Tools at Inference Time** - *leads with the RE/method contribution (your field first)*

*(agent picks, if forced to two: #2 for the novel/citable inference-time hook, or #7 for the reflexive-dogfooding
distinctiveness. #3 is the safest tightening of your first attempt.)*
