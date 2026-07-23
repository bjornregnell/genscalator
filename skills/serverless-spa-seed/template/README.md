# serverless-spa — a client-only Scala.js + Laminar todo

A single-page app with NO server. The whole todo list lives in your browser's `localStorage`,
so it survives a reload. Built with `scala-cli` — no sbt.

## Run

1. Link the Scala.js to `main.js` (the first run fetches Scala + Laminar):

       scala-cli package . --js -o main.js -f

2. Serve the folder over http and open it in a browser. `localStorage` is unreliable on
   `file://`, so serve rather than double-clicking `index.html`:

       tt serv          # genscalator's static server; open the URL it prints

   (any static server works, e.g. `python3 -m http.server`).

3. Add, tick, and delete todos. Reload the page — they are still there.

## What's here

- `project.scala` — the scala-cli build config (`platform js`, Scala, Laminar, scalajs-dom).
- `Todo.scala` — the datamodel + a dependency-free JSON codec (the same one the crud seed
  shares between server and client; here it serializes the list into `localStorage`).
- `Main.scala` — the Laminar UI: a reactive `Var[List[Todo]]`, loaded from and saved to
  `localStorage` on every change.
- `index.html` — a static page with `<div id="app">` and `<script src="main.js">`.

## Next steps

- Change the `Todo` model and the UI to your own domain.
- For a version with a real backend (a JDK server + a shared datamodel), see the
  `crud-web-app-seed` sibling.
