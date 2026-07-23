---
name: serverless-spa-seed
description: Seed a minimal client-only Scala.js + Laminar single-page app (no server, state in localStorage, built with scala-cli --js, no sbt) into a directory of the user's choice — the simplest genscalator web seed and a gentle Scala.js on-ramp.
---

# serverless-spa-seed

Writes a complete, working client-only "todo" SPA from `template/` into a directory the user picks.
Simpler sibling of `crud-web-app-seed`: no server, no sbt, no cross-project — just a Scala.js +
Laminar browser app whose state lives in `localStorage`. Direct common style.

## When to use
When the user wants the SMALLEST from-scratch Scala web app — a browser-only SPA with no backend —
or a gentle first taste of Scala.js + Laminar. For a full stack (a JDK server + a shared
datamodel), use `crud-web-app-seed` instead.

## What it seeds (`template/`)
- `project.scala` — scala-cli build config: `//> using platform js`, Scala, Laminar, scalajs-dom, `jsModuleKind none`.
- `Todo.scala` — the datamodel + a dependency-free JSON codec (shared verbatim with the crud seed; here it (de)serializes the list to `localStorage`).
- `Main.scala` — the Scala.js + Laminar UI: a reactive `Var[List[Todo]]` loaded on start and saved to `localStorage` on every change.
- `index.html` — a static page: `<div id="app">` + `<script src="main.js">`.
- `README.md` — the run steps.

## How to run this skill
1. Ask the user for a target directory (default `./serverless-spa-seed`).
2. Copy the `template/` tree there, preserving structure.
3. Print the run steps from the README: `scala-cli package . --js -o main.js -f`, then serve over
   http (`tt serv`) and open the app in a browser.

## Stack (checked 2026-07-23)
Scala **3.9.0-RC1** · Scala.js **1.22.0** · Laminar **17.2.1** · scalajs-dom **2.8.1**, built with
**scala-cli** (no sbt). Matches the crud seed's Scala/Scala.js/Laminar set, minus the server and sbt.

## Status (2026-07-23)
**Link VERIFIED.** `scala-cli package template --js` compiles + links to `main.js` (exit 0, Scala
3.9.0-RC1 / Scala.js 1.22.0). The linked script auto-runs `@main` on load (confirmed: it reaches
`TodoApp.load` → `window.localStorage`). Browser click-through (add / tick / delete, reload
persists) is the human step, as with the crud seed.
