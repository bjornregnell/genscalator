# Todo web-app seed

A minimal but complete CRUD web app in **direct common style**: a shared datamodel, a **JDK-only** HTTP server, and a
**Scala.js + Laminar** browser client. Built with **sbt 1.x** and **Scala 3.9.0-RC1** (the coming LTS).

## Stack
- Scala **3.9.0-RC1** · sbt **1.12.13**
- Server: only the JDK (`com.sun.net.httpserver`) — no libraries
- Client: Scala.js **1.22.0** + Laminar **17.2.1**
- Shared `common` cross-project: the `Todo` model + a tiny hand-rolled JSON codec (so the server needs no JSON lib)

## Layout
- `common/` — shared `Todo` + JSON codec (compiles to both JVM and JS)
- `server/` — JDK HttpServer, in-memory CRUD, serves the client
- `client/` — Laminar single-page UI

## Run (two steps)
1. Build the client to JavaScript: `sbt client/fastLinkJS`
2. Start the server (serves the API + the built client): `sbt server/run`

Then open <http://localhost:8080>. Re-run `sbt client/fastLinkJS` after client changes. If the built JS is not found,
the server prints the path it looked for — set `TODO_CLIENT_JS` to the actual `.../client-fastopt/main.js` path.

## The API
| method | path | does |
|---|---|---|
| GET | `/api/todos` | list |
| POST | `/api/todos` | create, body `{"title":"..."}` |
| PUT | `/api/todos/:id` | toggle done |
| DELETE | `/api/todos/:id` | delete |

See **`PRD.md`** for the requirements in reqT-lang.

## Not only for Scala devs
This seeds a Scala app, but genscalator's `tt` tools serve other stacks too — the seed is a starting point, adapt the
domain (`Todo`) to your own.
