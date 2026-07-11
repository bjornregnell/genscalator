# PRD — Todo web-app seed (reqT-lang)

Written in **reqT-lang** (a markdown-subset for requirements: **ENT** entities, **REL** relations, **ATTR**
attributes) — the same little language genscalator uses for its own PRD. It doubles as a worked example of reqT-lang
for a small app. Parse + lint it with `tt parsereqt parse PRD.md` and `tt parsereqt lint PRD.md`.

## Stakeholders
* Stakeholder: Newcomer has
  * Gist: a developer trying genscalator who wants a working Scala web app running in minutes.
* Stakeholder: EndUser has
  * Gist: uses the running todo app in a browser.

## Goals
* Goal: smoothOnboarding has
  * Gist: from seed to a running app in a couple of commands.
  * Prio: 1
* Goal: sharedModel has
  * Gist: one datamodel and one wire format, shared by server and client, so they cannot drift.
* Goal: minimalServerDeps has
  * Gist: the server uses ONLY the JDK, no server framework or JSON library.

## Features
* Feature: todoModel has
  * Gist: a `Todo(id, title, done)` case class in `common`, compiled to both JVM and JS.
  * Spec: immutable; identity by `id`.
* Feature: jsonCodec has
  * Gist: a tiny hand-rolled JSON encode + parse in `common`, dependency-free, so the server needs only the JDK.
* Feature: crudApi has
  * Gist: an HTTP JSON API over the JDK `HttpServer` for the four CRUD operations.
  * Spec: `GET /api/todos`, `POST /api/todos`, `PUT /api/todos/:id`, `DELETE /api/todos/:id`.
* Feature: laminarUi has
  * Gist: a Scala.js + Laminar single-page UI that lists todos and adds, toggles, and deletes them.

## Functions
* Function: listTodos has
  * Gist: return all todos as JSON.
* Function: createTodo has
  * Gist: add a todo from a posted title.
* Function: toggleTodo has
  * Gist: flip a todo's done flag.
* Function: deleteTodo has
  * Gist: remove a todo by id.

## Components
* Component: common has
  * Gist: the shared cross-project (JVM + JS).
* Component: server has
  * Gist: the JDK-only JVM backend.
* Component: client has
  * Gist: the Scala.js + Laminar frontend.

## Relations
* Feature: crudApi requires Feature: todoModel
* Feature: crudApi requires Feature: jsonCodec
* Feature: laminarUi requires Feature: todoModel
* Feature: laminarUi requires Feature: crudApi
* Feature: jsonCodec helps Goal: minimalServerDeps
* Feature: todoModel helps Goal: sharedModel
* Feature: laminarUi helps Goal: smoothOnboarding
* Feature: crudApi has Function: listTodos
* Feature: crudApi has Function: createTodo
* Feature: crudApi has Function: toggleTodo
* Feature: crudApi has Function: deleteTodo
* Component: common has Feature: todoModel
* Component: common has Feature: jsonCodec
* Component: server has Feature: crudApi
* Component: client has Feature: laminarUi
