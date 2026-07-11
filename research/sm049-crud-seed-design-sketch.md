# SM049 — crud-web-app-seed: design sketch (info-prep for BR; options, not decisions)

Companion to `sm049-crud-web-app-seed-feasibility.md` (which has the version sweep + BR's HD to use sbt 1.x). This is
the "prepare info while solo" deliverable: the proposed shape + the open design questions to settle **with BR** (his
opinion-discussion is reserved). Nothing here is built; no fragile code authored unsupervised.

## Decided stack (recap)
sbt **1.12.13** · Scala **3.9.0-RC1** (coming LTS; RC2 also out) · Scala.js **1.22.0** · Laminar **17.2.1** ·
scalajs-dom **2.8.1** · `sbt-scalajs` **1.22.0** + `sbt-scalajs-crossproject` (`org.portable-scala`) · **server = JDK
only**. Client stack compile-verified on 3.9.0-RC1.

## Proposed project layout (sbt 1.x multi-project, portable cross-project)
```
crud-seed/
  build.sbt                 # 3 projects: common (crossProject JVM+JS), server (JVM), client (JS)
  project/build.properties  # sbt.version=1.12.13
  project/plugins.sbt       # sbt-scalajs + sbt-scalajs-crossproject
  common/  (crossProject)   # shared datamodel + wire codec — compiles to BOTH JVM and JS
    src/main/scala/crudseed/Todo.scala
  server/  (JVM, JDK-only)  # com.sun.net.httpserver.HttpServer, in-memory CRUD, serves the client + a JSON API
    src/main/scala/crudseed/server/Main.scala
  client/  (Scala.js+Laminar)
    src/main/scala/crudseed/client/Main.scala
    index.html
  README.md                 # 2-command run steps for a newcomer
```
Direct common style throughout (short bodies braceless, new control syntax, braces only for long/blank-line scopes).

## The one real design question: JSON with a JDK-only server
The server may use **only the JDK** (no ujson/circe), so the shared `common` codec must be dependency-free and
compile on both JVM and JS. Options (for BR to pick):
- **(a) Hand-rolled tiny JSON in `common`** — a ~40-line encode + a small parser for the flat `Todo` shape, pure
  Scala, zero deps. *Pro:* honors JDK-only literally, portable, no client/server dep skew. *Con:* hand-rolled parsing
  is the fiddly bit; keep the model flat to keep it trivial. (This is the part I deliberately did NOT hack together
  solo — it wants your eye on the taste/robustness tradeoff.)
- **(b) Dead-simple wire format** (not JSON) — e.g. one Todo per line, tab-separated `id\ttitle\tdone`. *Pro:*
  trivially portable + parseable, very newcomer-legible. *Con:* not "a real JSON API"; less transferable to non-Scala
  clients later (a stated long-term goal).
- **(c) Relax "JDK-only" to allow ONE tiny portable JSON lib** in `common` (e.g. a minimal cross-published codec).
  *Pro:* idiomatic JSON, robust. *Con:* breaks the literal JDK-only constraint (your call whether that constraint is
  about the server *framework* or truly zero-deps).
- **Agent lean (reserved):** (a) for a small flat model; (b) if we want maximum newcomer legibility. Worth 2 minutes
  of your steer since it colours the whole seed.

## CRUD scope (minimal-but-complete)
An in-memory `Todo` list: `GET /api/todos` (list), `POST /api/todos` (create), `PUT /api/todos/:id` (toggle done),
`DELETE /api/todos/:id`. Laminar client renders the list + an add box + per-row toggle/delete, re-fetching via the
browser `fetch` API (scalajs-dom). Server also serves `index.html` + the linked `client.js` (the Scala.js output).

## Newcomer run experience (the over-arching goal)
Target: `sbt server/run` starts the API + serves the built client at `http://localhost:8080`, and the README shows
exactly the 2–3 commands (`sbt client/fastLinkJS` then `sbt server/run`, or a single aliased task). The skill writes
this whole tree into a dir of the user's choice + prints the run steps.

## Verification plan (AFK-safe vs BR-present)
- **Solo-verifiable now** (scala-cli, no sbt): the `common` model + codec, the Laminar client sources, and the server
  handler logic all compile via scala-cli per-platform (client already proven).
- **Needs BR-present / a solo-safe sbt path:** the full `sbt` multi-project build + the `fastLinkJS` → server-serves
  wiring — because bare `sbt` isn't allowlisted (`sbt --client *` is, but a fresh project may need a plain-`sbt`
  bootstrap → AFK-stall risk). So I'll author sources + scala-cli-verify them; the sbt build gets verified when you're
  around or once a safe sbt invocation is confirmed.

## Open questions for BR (when back)
1. **JSON approach** — (a) hand-rolled / (b) simple wire format / (c) allow one tiny portable codec?
2. **Scala 3.9.0-RC1 vs RC2** (RC2 is out).
3. **Scope** — Todo CRUD as the canonical seed, or a different domain?
4. Skill mechanics — does the skill write into `./crud-seed/` by default + prompt for the dir, and does it run the
   first build for the user or just print the steps?

Ties: SM048 (v0.9.0 / newcomer onboarding), the scala-style skill (common style), `tt serv` (the JDK-HttpServer
precedent to reuse on the server side), [[dependency-preference-cascade]].
