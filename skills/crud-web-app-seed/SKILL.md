---
name: crud-web-app-seed
description: Seed a complete working Scala CRUD web app (JDK-only server + Scala.js/Laminar client, sbt 1.x, Scala 3.9.0-RC1) into a directory of the user's choice, for a smooth newcomer up-and-running experience.
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

## Stack (checked 2026-07-11)
Scala **3.9.0-RC1** (coming LTS) · sbt **1.12.13** · Scala.js **1.22.0** · Laminar **17.2.1** · sbt-scalajs **1.22.0**
+ sbt-scalajs-crossproject **1.3.2**. NB sbt 2.x is released but sbt-scalajs has not migrated, so the seed pins sbt 1.x
(revisit when sbt-scalajs ships an sbt-2.x plugin). Not only for Scala devs — the genscalator `tt` tools serve other
stacks too.

## Status (2026-07-11)
**sbt build VERIFIED.** `sbt compile` (all modules, Scala 3.9.0-RC1) and `sbt client/fastLinkJS` (produces `main.js`)
both pass with exit 0 via sbt 1.12.13 + sbt-scalajs 1.22.0. Sources also compile-verified via scala-cli (`common`
round-trips its JSON on both platforms). The build test found and fixed one bug: the server's default client-JS path
(`todo-client-fastopt`, from `name := "todo-client"`). Remaining human step: `sbt server/run` and open the browser to
click through the CRUD. See `genscalator/research/sm049-crud-web-app-seed-feasibility.md`.
