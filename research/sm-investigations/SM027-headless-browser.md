# SM027 - `tt` headless-browser tool: SEE (screenshot) + STEER (drive/GUI-test)

**Status:** INVESTIGATION - report before building (BR pin 2026-07-08 + extended
scope 2026-07-10). No code yet. **Recommendation: build Tier 1 (SEE) now; Tier 2
(STEER) is a separate, feasible, pure-JDK follow-up.**

## The problem: the agent is blind to rendered output
`tt web get` returns raw HTML - not what a human sees. The agent cannot tell
whether the CSS loaded, a theme toggle works, figures render, or a mobile layout
breaks. Two concrete misses already: blog index bold-in-link + nav-path bugs were
caught by BR's eyes, not the agent's; and **right now the agent is blind to whether
the SM037 soaré pages render correctly** (served at 127.0.0.1:8092, but BR must
eyeball them - exactly this gap). A screenshot the agent can Read back closes the
loop = visual verification / regression.

## Tier 1 - SEE (cheap, build first)
A fixed, audited subprocess:
```
chromium --headless --screenshot=<out.png> --window-size=<W>,<H> <url>
```
→ the agent Reads `out.png` back (the Read tool renders PNGs). Immediately useful
for the whole HTML thread (soaré, blog, any `tt serv`-served page).

**Dependency posture (dependency-cascade):** chromium is NOT a JDK library and NOT
an interpreter - it is an **external system binary invoked as a fixed-shape
subprocess**, exactly the pattern the toolbox already uses for `dot` (tt gvdot),
`pdflatex`/`pdftotext` (tt pdf), and `lftp` (deployblog). So it fits the existing
"audited external binary" tier, not a new dependency class. It does NOT violate
[[never-allowlist-interpreters]]: the allowlistable unit is `tt shot <url>`, whose
internal chromium call is a subprocess the guard never sees (same as deployblog's
internal lftp), and the tool emits a *fixed* argv - no `--user-data-dir` tricks, no
`-exec`-style blank surface.

**Binary detection:** probe `chromium` / `chromium-browser` / `google-chrome` /
`google-chrome-stable` in order; clear error + install hint if none. (BR to confirm
which is on blixten when building - a one-line `command -v` check, human-run.)

**Safety scope:** loopback + explicit URLs only by default (screenshot our own
served pages); an explicit `--allow-remote` opt-in for real URLs, off by default.
`--window-size` presets for desktop/mobile/rotation regression (the responsive
check the template needs). Output confined to a declared out path.

## Tier 2 - STEER (bigger, but pure-JDK feasible)
Interactive drive: navigate, click, type, wait, assert, screenshot - GUI-test flows
against a live loopback page.

**Key finding: no external dep needed.** CDP (Chrome DevTools Protocol) is
JSON-over-WebSocket, and the **JDK ships a WebSocket client** -
`java.net.http.HttpClient.newWebSocketBuilder()` / `java.net.http.WebSocket` (since
Java 11; we run JDK 21/25). So a thin CDP client is buildable with **zero
dependencies**:
1. Launch `chromium --headless --remote-debugging-port=<N> <url>` (subprocess).
2. `GET http://127.0.0.1:<N>/json` (JDK `HttpClient`) → parse the target list →
   extract `webSocketDebuggerUrl`.
3. Connect that ws via `java.net.http.WebSocket`; send/receive CDP JSON.
4. Implement ONLY the slice we need: `Page.navigate`, `Page.captureScreenshot`,
   `Input.dispatchMouseEvent` (clicks), `Input.insertText` (typing),
   `Runtime.evaluate` (DOM asserts), and a `Page.loadEventFired` wait.
The only hand-rolled piece is a **minimal JSON codec** for CDP messages (small; or
reuse whatever JSON the `tt forge`/`tt web` path already uses). A GUI regression =
script a flow + assert + screenshot.

## The trap (why this is a `tt` tool, not `npm i`)
The obvious "click all the things" libraries - **Playwright / Puppeteer / Selenium**
- are node / heavy-driver runtimes: they pull a large dependency and (the node
ones) drag in an interpreter, violating [[never-allowlist-interpreters]] and the
dependency-cascade. The clean path is **our own thin CDP-over-WebSocket driver in
Scala** talking to a headless-chromium subprocess: no interpreter, JDK-only,
audited, allowlistable. That is the entire reason this earns a place in the toolbox
instead of a framework install.

## Naming (BR's call - options)
BR floated `tt surf` / `tt shot` / `tt web shot` / `tt headless`. Recommendation:
**`tt shot <url>`** for Tier 1 (SEE - short, matches the verb), and Tier 2 as a
sibling verb (`tt drive` / `tt web drive`, STEER) so the cheap common case stays a
one-word command. A unified `tt browser <shot|drive>` is the alternative if we
prefer one namespace. Defer the final name to BR.

## Recommendation
1. **Build Tier 1 (`tt shot`) now** - small, high daily value (unblocks the agent's
   blindness on soaré/blog/any served UI), pure fixed-subprocess, testable
   (argv-construction + out-path confinement; the pixel check is manual/visual).
2. **Tier 2 (`tt drive`) as a separate PR + decision** - pure-JDK (WebSocket +
   tiny JSON codec), scoped to loopback, implement the ~6 CDP methods we actually
   need. Report first, then BR greenlights the CDP slice.
Composes with: SM019/SM037 (render verification), SM022 (screenshot the live
dashboard), SM026 (`tt table --gen html` → screenshot to verify). Ties the
"agent stops being blind" thread to a concrete, dependency-clean build.
