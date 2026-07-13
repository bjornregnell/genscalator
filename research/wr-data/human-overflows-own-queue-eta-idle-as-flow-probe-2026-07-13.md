# WR data: the human overflows their OWN queue; "ETA idle?" as a flow-protecting probe (2026-07-13)

BR, introspecting on why he asked *"ETA idle?"*: he had pushed **so much onto the message queue** that he
believes it is "pretty full with stuff" but **cannot remember** what is in it — the queue he built exceeds his
own working memory. Rather than ask *"where are we?"* (which would **derail** a productive agent), he used the
**non-interrupting** [[cue-eta-idle]] probe — *specifically because* he read the agent as **in flow, doing good
work without regression or rabbit-holing** (in token-spending mode) and did not want to break that. Then: **BR
was surprised the ETA was only ~2 min** — his felt sense of the queue's size was far larger than the actual
remaining work.

## Findings
1. **The human overflows their OWN queue.** In a high-throughput feed the human posts faster than they can
   retain; the backlog *they built* exceeds their working memory. The substrate + the agent hold it, not the
   human — the "human can't hold all threads" thesis ([[humans-md-agent-sole-writer]]) applied here to the
   human's own recent MESSAGES, not just project state.
2. **"ETA idle?" is a flow-PROTECTION instrument, not just a scheduling query.** BR chose it over "where are
   we?" deliberately, to avoid derailing an agent he judged coherent + flowing. The cue's value is
   *low-interference status*: you read the state without paying the interruption cost. It is the human side of
   rot-vigilance — monitor, intervene minimally.
3. **Felt queue-size OVER-estimates actual remaining work** — the human-side twin of "felt context-length
   over-estimates actual fill" ([[propose-compact-dance-at-trigger]]). BR felt the queue was big; the real
   remainder was ~2 min. Both are over-estimation biases about an unmeasured backlog.
4. **The human reads the agent's behavioural proxies to calibrate intervention** — "seems in flow, no
   regression, no rabbit-hole" → don't interrupt. The very proxies the agent cannot self-certify (family-E),
   the human CAN observe externally — a live instance of the asymmetry the whole observability strand rests on.

Ties: [[cue-eta-idle]], [[propose-compact-dance-at-trigger]] (felt > actual), [[joint-rot-vigilance-recovery-kit]],
[[cue-we-are-racing]] (the feed-overlap the queue came from), [[ignore-joke-noise-when-heads-down]] (the flow
being protected).

## Generalization: the human can't hold the growing SYSTEM either (2026-07-13)
Same session, BR asked to "implement" `token-saving` — a mode that **already existed** — and earlier forgot the
restart's purpose and asked whether SM059 was pinned when it already was. BR named it: *"agent gets aware that
[the] human can't keep all of this in [his] head."* So the queue-overflow finding generalizes from the human's
own recent MESSAGES to the whole **system state** (modes, tools, SMs, features, pins). Consequences:
- The **agent (in context) + the substrate (PB, docs, memories)** are the human's memory; he increasingly asks
  *"is X already there / did we do Y?"* — and the agent must **answer from the substrate, not guess** (it did:
  "token-saving already exists", "that's SM059").
- **Substrate-check before build:** when asked to build/pin something, first check whether it already exists, to
  avoid duplicates.
- This is the direct motivation for the **query-tools** pinned this session — `gs prd find` (SM065) and
  `gs pinboard` (SM066) exist *because* the human can no longer navigate the blob by memory; he needs to ASK it.
Ties: [[humans-md-agent-sole-writer]], SM065/SM066 (the query-tools this motivates), [[cue-bare-auto-compact]]
(reconcile via substrate), the substrate-over-mechanism thesis.
