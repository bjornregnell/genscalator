# Todo web-app seed — a genscalator starter you can learn from

Welcome! You are looking at a **complete, working Scala web app** that the **genscalator** plugin just seeded for you.
It is deliberately small and readable so you can run it, poke at it, and learn how a full-stack Scala app fits
together. This README explains the big idea, what genscalator gives you, and how to run and learn from this app.

## What is genscalator?

genscalator is a **Claude Code plugin** for building software *with* an AI agent while keeping **you, the human, in
control**. Its philosophy in one line: **prefer small, typed, auditable tools and clear conventions over open-ended
prompting**, so the agent's actions are legible and safe and the results are yours.

Two things it gives you:
- **`tt` tools** — a toolbox of small, single-purpose command-line tools (each does one thing, safely), so the agent
  reaches for a typed tool instead of ad-hoc shell commands.
- **skills** — focused playbooks the agent can follow (like the one that just built this app).

Everything is plain Scala you can read, and nothing is a black box.

## Using the plugin in a fresh Claude session

1. Open a new Claude Code session in a folder where you want to work.
2. Make sure the genscalator plugin is enabled (it adds the `tt` tools and the skills below).
3. Just ask, in plain language. For example: *"use the crud-web-app-seed skill to create a todo web app here"* —
   which is exactly what produced this project.

Skills the agent can use (ask for them by name or describe what you want):
- **crud-web-app-seed** — seed a working web app like this one.
- **scala-style** — the common Scala style (this app is written in it).
- **reqt-lang** — write requirements in a tiny markdown language (see `PRD.md`).
- **tt-toolbox** / **contribute-tool** — understand and extend the `tt` tools.
- **scala-code-review**, **research-methods**, **in-session-experiment**, **blog-assistant** — review, method, and
  writing helpers.

A few `tt` tools you will meet: **`tt serv`** (a tiny local web preview server), **`tt ssg`** (markdown to HTML),
**`tt parsereqt`** (parse/lint the reqT-lang `PRD.md`), **`tt text` / `tt files`** (typed search), **`tt git`**
(safe commits). Run `tt` with no arguments to see them all.

## This app at a glance

A tiny **todo** CRUD app in **direct common style**, three parts:
- **`common/`** — the shared `Todo` model **and** a tiny hand-rolled JSON codec. It compiles to **both** the server
  (JVM) and the browser (Scala.js), so the two sides can never disagree about the data.
- **`server/`** — an HTTP server using **only the JDK** (`com.sun.net.httpserver`), no libraries. In-memory CRUD API.
- **`client/`** — a **Scala.js + Laminar** single-page UI that talks to the server.

Stack: Scala **3.9.0-RC1** (the coming LTS) · sbt **1.12.13** · Scala.js **1.22.0** · Laminar **17.2.1**.

## Run it (two steps)

1. Build the browser client to JavaScript:
   ```
   sbt client/fastLinkJS
   ```
2. Start the server (it serves the API **and** the built client):
   ```
   sbt server/run
   ```
Then open <http://localhost:8080> and add a few todos.

Re-run `sbt client/fastLinkJS` after you change the client. If the server says it cannot find the built JavaScript,
it prints the path it looked for; set `TODO_CLIENT_JS` to the real `.../todo-client-fastopt/main.js` path.

*Tip (preview just the UI):* `tt serv` is genscalator's static preview server. It serves files, so it is handy for
looking at built HTML/JS, but the todo CRUD needs the JDK server above for its `/api/todos` calls to work.

## The API

| method | path | does |
|---|---|---|
| GET | `/api/todos` | list |
| POST | `/api/todos` | create, body `{"title":"..."}` |
| PUT | `/api/todos/:id` | toggle done |
| DELETE | `/api/todos/:id` | delete |

## Learn from it (a suggested reading order)

1. **`PRD.md`** — the requirements in **reqT-lang**. Try `tt parsereqt parse PRD.md` and `tt parsereqt lint PRD.md`
   to see the model the little language builds.
2. **`common/.../Todo.scala`** — the shared model and the hand-rolled JSON. Small enough to read in one sitting; it is
   why the server needs no JSON library.
3. **`server/.../Main.scala`** — a whole HTTP server in one file, JDK only. See how the four CRUD routes map to the
   in-memory store.
4. **`client/.../Main.scala`** — the Laminar UI. Notice how the view reacts to a `Var` and how it reuses the *same*
   `common` codec as the server.

Then try a change end to end: add a `priority: Int` field to `Todo`, follow the compiler through `common`, `server`,
and `client`, and watch the shared model keep both sides honest.

## Not only for Scala

This seed is a Scala app, but genscalator's `tt` tools are useful beyond Scala. Treat this as a starting point: keep
the shape, swap the domain (`Todo`) for your own.
