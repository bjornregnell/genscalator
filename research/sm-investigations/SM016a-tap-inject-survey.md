# SM016a — Tap/Inject Middle Layer: Transport + UX-Fidelity Survey

Scope: factual survey of the TRANSPORT and UX-FIDELITY sides of SM016 (a middle
layer between BR's keyboard and the Claude Code TUI that can TAP the stream and
INJECT TUI actions). Security design and the final recommendation are the lead
agent's; not covered here. Decided direction respected: tmux/pty is transport
only — injection *safety* lives in the pure-Scala capability harness.

Probed on this box (2026-07-08): **tmux 3.4**, **script (util-linux 2.39.3)**,
**socat** present; **expect, screen, ttyd, asciinema absent**.

## (a) Transport feasibility

### Option 1: tmux as substrate (run `claude` inside a tmux pane)

The strongest existing plumbing. tmux already IS a tap/inject broker: it owns the
pty that `claude` runs on, and re-renders to the outer terminal.

- **INJECT — `tmux send-keys -t <pane>`.** Injects arbitrary keystrokes into the
  pane's pty as if typed: literal text, named keys (`Enter`, `Escape`, `C-c`,
  `Up`), and with `-H` raw hex bytes. This is exactly enough to drive slash
  commands: `send-keys -t claude '/context' Enter`. Reliability: high; the bytes
  enter the same pty input queue as real typing, so the harness cannot
  distinguish them. Caveat: injection races with concurrent human typing
  (interleaving at byte granularity) — an injector should only fire when the
  human is idle, which the tap side can detect.
- **TAP output — `tmux pipe-pane -t <pane> -o 'cat >> log'`** streams the raw pty
  output bytes (escape sequences included) to a command, continuously. And
  **`capture-pane -p [-e]`** snapshots the *rendered* screen (with `-e`, including
  colour/attribute escapes) — this is the killer feature: tmux maintains a full
  terminal-state model, so the agent can read "what the screen looks like NOW"
  without parsing the raw stream itself. `capture-pane -S -N` also reads
  scrollback.
- **TAP input (keyboard side):** weaker. tmux has no first-class "pipe the user's
  keystrokes" hook (pipe-pane `-I` connects a command's *output* to pane input —
  it is another injector, not a keyboard tap). Input tapping would need either a
  pty shim under tmux or key-logging via tmux hooks, which are coarse.
  Uncertainty flag: I have not exhaustively checked tmux 3.4 hook coverage for
  per-key events; my recollection is none exist below key-binding granularity.
- **Control mode (`tmux -CC` / `-C`):** a machine-readable line protocol carrying
  pane output as escaped text events plus commands in-band — the "proper" API for
  a programmatic broker (iTerm2 uses it). More work than send-keys/pipe-pane but
  the most robust programmatic surface tmux offers.
- **Cost:** the human now lives inside tmux (status bar, prefix key, tmux's own
  keybindings). Mitigable: status off, prefix rebound to something unreachable;
  see (b).

### Option 2: pty broker / `tee` on a pseudo-terminal

Roll-your-own: allocate a pty pair, run `claude` on the slave side, sit between
the real terminal and the master, copying bytes both ways and teeing each
direction to the agent.

- **TAP:** perfect and symmetric — every byte in BOTH directions passes through
  the broker, so keyboard input tapping (tmux's weak spot) is trivial here.
- **INJECT:** write bytes to the pty master; indistinguishable from typing, same
  race caveat as tmux.
- **What exists:** `script -f` (util-linux, present) is precisely a pty +
  bidirectional tee (`script -f out.log` logs output; `--log-in`/`-I` logs input
  too) but offers no injection hook. `socat` (present) can splice
  `EXEC:claude,pty,raw,echo=0` to the current tty plus a tap. A purpose-built
  broker is ~150 lines against `posix_openpt`/`forkpty` — in this project's
  idiom, a small Scala program using JNA/JNI or wrapping `socat`/`script` is the
  natural shape. It must also handle: raw-mode setup on the outer tty, SIGWINCH
  forwarding + `TIOCSWINSZ` on the pty, and job-control signals.
- **Reliability:** byte-copying is trivially reliable; the risk lives in terminal
  *mode* bookkeeping (raw mode, resize, cleanup on exit), which is exactly the
  well-trodden part of every pty tool. Unlike tmux, there is no rendered-screen
  model — the agent gets the raw stream only, unless it embeds a terminal-state
  library (see drift warning in (b)).

### Option 3: `expect` / scripted pty wrapper

`expect` (Tcl) is the classic pty automation tool: spawns a program on a pty,
pattern-matches its output, sends input, and has `interact` mode which is exactly
tap+inject-while-human-drives (user keystrokes pass through, patterns can trigger
actions in both streams).

- **Fit:** conceptually the closest single tool to SM016's shape.
- **Against:** NOT installed on this box; Tcl is outside the project's Scala
  toolchain and the never-allowlist-interpreters policy; and expect's whole value
  is *pattern-matching the stream*, which is precisely the TUI-drift trap (b)
  warns against. Its `interact` passthrough is good, but that part is what
  Option 2 rebuilds in-project. Verdict: instructive prior art, poor substrate.

## (b) UX fidelity + robustness to TUI drift

BR's bar: the human-side experience must be near-native — full keyboard control,
all colours/styling, no mouse required. That means faithful escape-sequence
passthrough in BOTH directions.

**How hard is faithful passthrough?** For a *byte-transparent* broker (Option 2):
easy — bytes it does not touch cannot be corrupted. The genuinely hard parts are
the handful of places a broker must actively participate rather than copy:

- **Resize / SIGWINCH:** must catch SIGWINCH on the outer tty and re-issue the
  new size to the inner pty (`TIOCSWINSZ`), or the TUI reflows wrongly. Small,
  fiddly, well-understood.
- **Raw mode + cleanup:** outer tty must be in raw mode while the TUI runs and
  restored on ANY exit path (crash included), else the user's shell is wrecked.
- **Alt-screen, bracketed paste, mouse-tracking, cursor addressing:** all are
  just escape sequences flowing *through* a transparent broker — zero work if
  you don't parse. They only become hard if the middle layer keeps its own
  screen model (then it must implement every mode the harness uses, and chase
  every new one).
- **Terminal identity round-trips:** the inner app queries the terminal (DA1,
  cursor-position reports, XTGETTCAP, kitty-keyboard-protocol probes) and the
  *terminal* answers on the input stream. A transparent broker passes these
  fine; anything that filters/parses input must whitelist them or the TUI
  mis-detects its terminal. This is a real, current failure mode: modern TUIs
  (Claude Code included) do such probing at startup.

**tmux is NOT byte-transparent** — it parses everything into its own screen model
and re-emits for the outer terminal. Fidelity is very good but capped by tmux's
emulation: correct `TERM`/terminfo inside, `terminal-features`/`terminal-overrides`
for truecolor, and historically imperfect coverage of newest protocols (extended
keyboard protocols, synchronized output, new underline styles arrive in tmux with
lag). tmux 3.4 supports passthrough escapes (`allow-passthrough`) for some cases.
So: tmux costs some fidelity ceiling + a resident tmux UX (prefix key eats one
chord; mitigable by rebinding); the raw pty broker costs implementation effort
but has a near-perfect fidelity ceiling.

**Robustness to TUI drift — the load-bearing design argument.** The harness TUI
changes at every release (layout, colours, widgets, status lines, new escape
modes). Any middle layer that PARSES the TUI — scraping regions, pattern-matching
prompts (the expect idiom), reimplementing rendering — hard-couples to the
current TUI and breaks silently on every harness update, in the worst way
(plausible-but-wrong reads). Therefore the design should be a **thin transparent
passthrough**: copy bytes untouched in both directions, and tap/inject only at
the EDGES —

- **tap** = mirror the two raw byte streams to logs/agent (and optionally a
  *rendered* snapshot via tmux `capture-pane -e` or a maintained terminal-state
  library, clearly labelled best-effort, never load-bearing);
- **inject** = write keystroke bytes for actions a human could type (slash
  commands, Enter, Escape) — the harness's *keyboard command surface*
  (`/context`, `/compact`) is its de-facto public API and is far more stable
  than its pixels/escape output.

That inversion — depend on the stable input vocabulary, never on the volatile
output rendering — is what makes the layer survivable across harness updates.
Semantic interpretation of the tapped stream, if wanted at all, belongs in a
separable, allowed-to-lag component on top, not in the transport.

## Summary table

| Mechanism | Tap output | Tap keyboard | Inject | Fidelity ceiling | Drift risk | On box |
|---|---|---|---|---|---|---|
| tmux pane | pipe-pane (raw) + capture-pane -e (rendered) | weak/none | send-keys (excellent) | good, capped by tmux emulation | low if send-keys/capture only | yes (3.4) |
| pty broker (Scala/socat/script-like) | perfect (raw) | perfect (raw) | write to master | near-perfect (transparent) | lowest (no parsing) | socat+script yes; broker = to build |
| expect | pattern-match (raw) | via interact | send | good | HIGH (pattern-matching is the trap) | not installed |

Open uncertainties, flagged: exact tmux 3.4 coverage of kitty-keyboard/
synchronized-output passthrough not verified here; no per-key input hook in tmux
verified only from documentation recall, not exhaustive testing; Claude Code's
startup terminal-probing specifics not empirically traced (would need a logged
run, e.g. under `script -I`, itself a candidate first experiment).
