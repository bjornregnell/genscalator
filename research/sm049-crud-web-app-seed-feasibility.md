# SM049 — `crud-web-app-seed` skill: feasibility first pass (agent solo, 2026-07-11)

**Status:** solo exploration (BR solo-ack'd; opinion-discussion reserved for his return). This is a findings +
options report, not a decision. Goal recap: a genscalator-plugin **skill** that seeds, in a dir of the user's choice,
a **working Scala web app** in **direct common style** — **JDK-only server side**, **ScalaJS + Laminar client side**,
a **multi-project common-core datamodel** build. BR's over-arching goal: a **smooth up-and-running experience for a
newcomer**; and NB we are not long-term targeting only Scala devs (the `tt` tools serve Python apps etc. too).

## The key feasibility question BR flagged: does sbt 2.x work for the ScalaJS + Laminar part?
**Answer (empirical, 2026-07-11): the client language stack works; the sbt 2.x BUILD does not yet.**
- **Client stack GREEN.** A minimal Laminar app compiled cleanly via scala-cli: **Scala 3.7.3 + Scala.js 1.22.0 +
  Laminar 17.2.0** (latest Laminar is 17.2.1). So ScalaJS + Laminar themselves are fully current and feasible.
- **sbt is at 2.0.1** (latest release, published 2026-06-29) — sbt 2.x is out and stable, not a blocker by immaturity.
- **BUT `sbt-scalajs` has NOT migrated to sbt 2.x.** Maven central shows `org.scala-js:sbt-scalajs_2.12_1.0`
  (the **sbt 1.x** plugin cross-version) up to **1.22.0**, and the **sbt 2.x** coordinate
  `org.scala-js:sbt-scalajs_3_2.0` **404s**. So an sbt **2.x** multi-project build with the Scala.js plugin is **not
  possible today**.
- **Echt caveat:** the sbt-2.x plugin coordinate `_3_2.0` is inferred from sbt 2.x's Scala-3 build-def model; the 404
  is a strong signal but worth a one-line double-check against the sbt 2.x plugin-publishing docs before we treat it
  as final. The direction (sbt-scalajs lags sbt 2.x) is consistent with sbt 2.0 being only weeks old.

## Implication + the two viable paths (for BR to choose on return)
BR's *exact* idea (latest **sbt 2.x** + ScalaJS + Laminar) is blocked on the plugin. Two things work **today**:
- **Path A — sbt 1.x seed.** Use sbt **1.11.7** (latest 1.x) with `sbt-scalajs` 1.22.0 + `sbt-scalajs-crossproject`.
  A conventional, battle-tested multi-project `commonJS/commonJVM` cross-build. *Pro:* exactly the multi-project sbt
  shape BR described, works now. *Con:* not "latest sbt 2.x"; sbt itself is heavier for a newcomer than scala-cli;
  we'd migrate to sbt 2.x once sbt-scalajs ships it.
- **Path B — scala-cli-native seed (no sbt).** scala-cli natively builds the JVM server AND the ScalaJS+Laminar client
  (proven above) with a shared `common/` datamodel directory; `scala-cli --js` links the client, plain scala-cli runs
  the JDK server. *Pro:* zero build-tool install, smoothest newcomer up-and-running, and **toolbox-aligned** (scala-cli
  is genscalator's own runtime); trivially extends to non-Scala later. *Con:* not an "sbt build" per BR's literal ask;
  multi-project is by-convention (dirs) not sbt subprojects.
- (Path C — wait for sbt-scalajs on sbt 2.x — not viable for a "now" seed.)

**Agent lean (reserved per BR's ask, noted for later):** for the *smooth-newcomer* goal, Path B (scala-cli-native) is
the strongest fit and most on-brand; Path A best honors the literal "sbt multi-project" request. Worth deciding
together — hence not built yet.

## Next steps (after BR picks a path)
1. Sketch the multi-project template: `common/` (shared datamodel, pure JDK-safe Scala), `server/` (JDK-only HTTP,
   e.g. the JDK `HttpServer` like `tt serv`), `client/` (ScalaJS + Laminar CRUD UI), all in **direct common style**.
2. A trivial working CRUD (in-memory) end-to-end: server JSON API + Laminar client that lists/creates/updates/deletes.
3. Wrap as the `crud-web-app-seed` skill (writes the template into a chosen dir + a README with run steps).
4. Verify a newcomer can go from `skill → running app` in a couple of commands.

## Method note (AFK-safe)
All checks were solo-safe: scala-cli compile (allowlisted, resolves deps without a harness ack) + `tt web get`
(allowlisted `Bash(tt *)`, so no domain-ack stall) for the maven-central metadata. No WebFetch (which would have
domain-gated and risked stalling BR). Trial sources in the session scratchpad. Ties: SM048 (newcomer experience), the
scala-style skill (common style), [[dependency-preference-cascade]], `tt serv` (the JDK-server precedent).
