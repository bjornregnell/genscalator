# WR data: what was "easiest" to design — JDK vs cask vs Jetty (echt introspection + investigation seed, 2026-07-08)

## The question (BR)
For `tt serv` (SM020) BR offered cask / Jetty / just-JDK and asked which is *easiest for the agent*. He then
asked for the **echt** answer — "what did you actually think, as a data point" — and whether the broader
question ("what is easiest for the agent to design, with or without dependencies, and how does that relate to
what is dense in the model's training-data surface") **can be empirically grounded rather than the agent
generating an un-echt stance.** This note is the durable data point + a design sketch. [[echt-effort-especially-self-generated]]

## My echt answer, confidence-tiered (self-model — corroboration-asymmetric, so calibrated low-to-mid)

**Ranking of "easiest for ME to generate CORRECTLY, in one fluent pass, high confidence": JDK >> cask > Jetty.**

- **JDK `com.sun.net.httpserver` (what I chose):** the most fluent by a wide margin. I wrote
  `HttpServer.create(InetSocketAddress, backlog)`, `createContext`, and the `HttpExchange` surface
  (`getRequestURI.getRawPath`, `getResponseHeaders`, `sendResponseHeaders`, `getResponseBody`) in a single
  pass without halting to reconstruct the API, and with high confidence each call exists as written. That
  fluency is itself the signal.
- **cask:** I know its *shape* (lihaoyi, `@cask.get` routing, `cask.MainRoutes`) but for *static file serving
  specifically* my confidence drops — I'd be reconstructing `cask.staticFiles`/`staticResources` and would want
  to check the docs before trusting it. Verification I did NOT need for the JDK.
- **Jetty:** I know the architecture (Server, ServerConnector, ServletContextHandler, DefaultServlet) but it is
  more boilerplate and **version-sensitive** (the javax->jakarta package move across Jetty 9/10/11/12), so I'd
  have a real risk of emitting a plausible-but-outdated incantation. Highest hallucination risk of the three.

## The honest confound (why this single data point is WEAK on its own)
I cannot, from the inside, separate three explanations that here all point the same way:
1. **Objective simplicity** — JDK genuinely has fewer moving parts for "serve static files, minimal HTTP." (True.)
2. **Training-data density** — the com.sun.net.httpserver "~20-line web server" is a meme-tier canonical example,
   almost certainly over-represented in my training corpus, so it may be easy because it is *familiar*, not
   because it is *simple*.
3. **Context priming / post-hoc rationalization** — BR's toolbox has an explicit dep-minimizing ethos
   (`lib.scala`: "uses only the JDK... so pure text tools compile fast"), so I was primed toward zero-dep and
   may be dressing a context-cued choice in a capability rationale.

**This task is exactly the one that CANNOT dissociate the three** (simplest == most-trained == context-primed,
all JDK). That coincidence is the methodological crux, and admitting it is the echt move: my introspective
"JDK was easiest" is real as a *fluency observation* but near-useless as *evidence for why* without a design
that pulls the confounds apart.

## Can it be empirically grounded? Yes — measure behavior, do NOT trust self-report
Sketch of an echt design (sibling of the indent-vs-braces edit-cost harness, [[genscalator-indent-braces-experiment]]):

1. **Dissociate simplicity from frequency by task choice.** Pick tasks where the dep-based solution is
   objectively simpler/shorter but the dep is *obscure* (low corpus frequency), and tasks where the zero-dep
   solution is longer but *high* frequency. If ease tracked simplicity, the agent would pick the obscure-dep
   short solution; if it tracks frequency, it would avoid it.
2. **Ground in the compiler-oracle, not self-report.** For the SAME spec implemented against {JDK, cask, Jetty}
   (or {obscure-dep, zero-dep}), measure observable proxies: **first-attempt compile-success rate**, **number
   of correction iterations to green**, **hallucinated-API rate** (calls to non-existent members — the sharpest
   signal), **tokens-to-green**. Fluency = fewer iterations + fewer hallucinated APIs. This is measured, not a
   generated stance — the [[agent-cant-internalize-huge-codebases]] "compiler as oracle" discipline.
3. **Proxy the training-data surface.** We can't read the corpus, but we can proxy frequency (GitHub code-search
   hit counts, StackOverflow question counts, library download stats) and **correlate** it with the measured
   fluency. If fluency correlates with proxied frequency MORE than with objective complexity (LOC / cyclomatic),
   that is evidence for the training-surface hypothesis over the simplicity hypothesis.
4. **Vary the model** (CO4 vs CF5 vs Haiku) — if the fluency ranking shifts with training cohort, that is
   further evidence it is corpus-driven, not task-intrinsic.

## Why it matters
It reframes a design instinct ("prefer zero-dep") as a **testable claim about the agent's emission ability**,
not an engineering axiom. If "easiest for the agent" tracks corpus frequency, then agent-authored code is
biased toward the *popular* rather than the *best* dependency choice — a real, measurable bias with design
consequences (and it interacts with the style-vs-capability finding: capability/emission-ability dominates).

## Status
Candidate to graduate to **RT051** (research topic) with **an SM harness** (compiler-oracle emission-cost rig,
reusing the indent-vs-braces harness scaffold). Not minted yet — BR shapes the research framing. Sibling:
RT048 (substrate-content power), the indent-vs-braces experiment, [[agent-affective-analogs]].
