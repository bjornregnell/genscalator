# Human learns by watching the agent: the jps TIL

**Stamp:** 2026-07-19 ~15:50 CEST (BR pin, in-feed).
**BR's observation (paraphrase of his pin):** "BR is watching what the agent
is doing and learns a lot; e.g. today I learned that there is such a thing
as `jps`."

## The specimen

During the bloop-kill sequence on the sick box, `pkill -9 -f BloopServer`
exited 1 while a 12 GB BloopServer was in fact running (cmdline pattern
miss). The agent needed to identify JVMs WITHOUT dumping their arguments
(the earlier `ps -o args` on a JVM had flooded the context with a 32k-token
classpath — the SM160 lesson), and chose `jps`: JDK-bundled, prints pid +
main class only, bounded by construction. It identified the BloopServer;
kill-by-pid worked.

BR — decades of JVM-adjacent teaching and development — had never met
`jps`, and said he was very glad to learn it ("today I learned").

## Finding

The transparency channel runs BOTH ways. BR reads every command in the feed
and the approval TUI primarily to SUPERVISE (the guard model: human control).
This specimen shows the same channel is a TEACHING surface: the human's own
capability grows as a side effect of watching the agent work. Supervision is
usually costed as overhead; here it paid a dividend.

Two notable properties:

- The learning was TACIT on the agent's side — no tutorial, no explanation
  requested; the human learned from seeing a well-chosen tool used in anger
  on a real problem (arguably the strongest form of tool pedagogy).
- It inverts the usual framing of the pair: the human normally teaches the
  agent (cues, corrections, memories); here the agent taught the human,
  without either party framing it as teaching.

## Ties

- SM160 (`tt box`): `jps` is now the decided basis for the JVM slice
  (`tt box jvm`) — same event, engineering half pinned in the PB.
- Earned-trust thread: a supervision channel that yields learning keeps the
  human ENGAGED in watching — a structural counterweight to the
  scrutiny-drops-as-trust-grows drift.
- Single datum; no trend claim. Candidate WR theme if it recurs: "the
  approval surface as a pedagogical surface."
