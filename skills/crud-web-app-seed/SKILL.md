---
name: crud-web-app-seed
description: Seed a complete working Scala CRUD web app (JDK-only server + Scala.js/Laminar client, sbt 1.x, Scala 3.9.0) into a directory of the user's choice, for a smooth newcomer up-and-running experience.
---

# crud-web-app-seed

Writes the complete, working "todo" CRUD web-app project in `template/` into a directory the user picks, so a newcomer
to genscalator gets a running Scala web app in a couple of commands. Direct common style throughout.

## When to use
When the user wants a from-scratch Scala full-stack web-app skeleton (a JDK server + a browser client sharing one
datamodel), or a starting point to adapt to their own domain.

## What it seeds (`template/`)
- `common/` — shared `Todo` datamodel + a tiny dependency-free JSON codec (compiles to JVM **and** JS).
- `server/` — a **JDK-only** HTTP server (in-memory CRUD JSON API + serves the client). No libraries.
- `client/` — a **Scala.js + Laminar** single-page UI.
- `build.sbt` + `project/` (sbt **1.12.13**, sbt-scalajs) · `PRD.md` (requirements in **reqT-lang**) · `README.md`.

## How to run this skill
1. Ask the user for a target directory (default `./todo-seed`).
2. Copy the `template/` tree there, preserving structure.
3. Print the run steps from the README: `sbt client/fastLinkJS`, then `sbt server/run`, then open
   <http://localhost:8080>.

## Stack (checked 2026-07-11; Scala re-pinned 2026-07-25, bumped to the LTS 2026-09-18)
Scala **3.9.0** (the LTS) · sbt **1.12.13** · Scala.js **1.22.0** · Laminar **17.2.1** · sbt-scalajs **1.22.0**
+ sbt-scalajs-crossproject **1.3.2**. NB sbt 2.x is released but sbt-scalajs has not migrated, so the seed pins sbt 1.x
(revisit when sbt-scalajs ships an sbt-2.x plugin). Not only for Scala devs — the genscalator `tt` tools serve other
stacks too.

## Status (2026-07-11; re-verified on the 3.9.0 LTS 2026-09-23, after fixing a defect the bump introduced)
**VERIFIED on 3.9.0.** Measured 2026-09-23 on a copy with no pre-existing build output:

| step | result |
| --- | --- |
| `sbt compile` (all modules, Scala 3.9.0) | **exit 0**, no warnings |
| `sbt client/fastLinkJS` | **exit 0**, output in `client/target/scala-3.9.0/todo-client-fastopt` |
| `sbt server/test` | **6 passed, 0 failed** |
| `GET /main.js` from the running server | **200**, the linked client |

**The defect this run found, now fixed.** The 2026-09-18 bump hardcoded the client-JS path to
`client/target/scala-3/...`, reasoning that sbt names that directory after the BINARY version, "plain `3`
for a final Scala 3 release", and is therefore stable on the LTS. Measured, sbt used the **full** version,
`scala-3.9.0`, so the linked client sat where the server did not look and `GET /main.js` answered 404 for a
client that had built perfectly well. The server now LOOKS for the file under any
`client/target/scala-*/todo-client-fastopt/`, taking the most recently written one, so the path cannot drift
on the next bump. `TODO_CLIENT_JS` still overrides it exactly.

**Pinned by a mutation-verified regression test.** `ServerCrudSuite` gained a test that starts the real
server and fetches `/main.js`, asserting that whenever a linked client exists on disk the server serves it.
It locates that client by WALKING `client/target` rather than by re-using the server's own lookup, so it
cannot agree with the code it checks and be wrong together. Its power was confirmed rather than assumed, two
ways: re-introducing the old hardcoded path turns it red ("a linked client exists at
`client/target/scala-9.9.9-FAKE/...` but the server did not serve it", 404 against 200), and renaming the
built directory to `scala-9.9.9-FAKE` leaves it green, which is the version-independence the fix is for.

**Not established: why sbt used the full version** rather than the binary version for a final release. Only
that it does. One unrecorded input worth naming before re-measuring: this box loads global sbt plugins from
`~/.sbt/1.0/plugins` (including `gpg.sbt`), which were in effect for these runs. The fix does not depend on
the answer, which is the point of searching for the file instead of naming its directory.

**sbt build VERIFIED ON RC4 (2026-07-25), superseded by the 3.9.0 measurement above.** `sbt compile` (all modules, Scala 3.9.0-RC4) and `sbt client/fastLinkJS` (produces `main.js`)
both pass with exit 0 via sbt 1.12.13 + sbt-scalajs 1.22.0 — re-run on the RC4 bump, compile clean with no warnings. Sources also compile-verified via scala-cli (`common`
round-trips its JSON on both platforms). The build test found and fixed one bug: the server's default client-JS path
(`todo-client-fastopt`, from `name := "todo-client"`). Remaining human step: `sbt server/run` and open the browser to
click through the CRUD. See `genscalator/research/reports/report049-crud-web-app-seed-feasibility.md`.
