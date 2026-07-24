# tools/ — Scala agent toolbox (cheat-sheet)

Typed, compiler-checked, reusable Scala scratch tools that replace the brittle bash/grep/awk/python
reflex. **Off-the-shelf: pick a tool, give args** — no re-deriving logic each time, no dynamic-shell
surprises (the compiler catches mistakes before they run). Project-agnostic. Always uses the **latest
stable Scala** (now 3.8.4; re-check per project).

## Run
```
tt <tool> <args...>                              # from ANY repo (recommended)
scala-cli run tools/<tool>.scala -- <args>       # explicit, from this repo's root
```
**`tt`** ("typed tools") is the launcher (`tools/tt`, symlinked onto PATH at `~/.local/bin/tt`). The
symlink name is yours to choose — rename it if `tt` collides with something on your PATH (it's not a
standard command). It makes every tool ONE literal, statically-analyzable command — so it matches a precise allowlist entry and needs no
manual confirmation (no `/tmp`, no `..` traversal, no shell variable in the gated command). Allowlist
is **per-subcommand** for "start safe", e.g. `Bash(tt text *)`; add an entry as each tool is proven.
First run compiles (~couple s); reruns are cached. Pure tools use only the JDK (fast); effectful
drivers add `//> using dep com.lihaoyi::os-lib:0.11.8`.

## Tests
The suite is **co-located** under [`test/`](test/): `test/cli.test.scala` (CLI-contract tests — each tool run as a
subprocess, exit + stdout asserted) and `test/lib.test.scala` (unit tests for `lib.scala`). Run the whole toolbox +
tests with **`scala-cli test tools`** (from the repo root) — test scope extends the toolbox's main scope, so a plain
`scala-cli compile tools` still builds only the tools (the `.test.scala` files are test-scope and excluded).

## Tools

### text — typed grep/awk/cut/uniq replacement (PURE)
```
text count <file> <regex>            # grep -c   : count matches
text match <file> <regex>            # grep -n   : print matching lines, numbered
text context <file> <regex> [N]      # grep -C N : matching lines with N lines of context (default 2)
text freq  <file> <regex>            # sort|uniq -c|sort -rn : histogram of match (or capture group 1)
text grepr <dir> <ext[,ext2…]> <regex>        # grep -r --include : recursive search → file:line:match
text grepr <dir> <ext[,ext2…]> --any p1 p2…   # OR-match: a line matching ANY pattern (metachar-free alternation)
text cols  <file> <sep> <i...>       # cut/awk   : extract 1-based fields, tab-joined
```
`grepr` takes a comma-separated extension list (`.scala,.java`) and prints a friendly one-line error
(+ exit 2) on a missing/relative dir instead of a stack trace — pass an absolute dir.

**`grepr … --any p1 p2 …`** matches a line when ANY of the patterns match — an OR *without* the regex `|`. Put
`--any` **after** the extension list; every argument after it is treated as an OR-alternative. Two reasons for a
flag rather than a `p1|p2` alternation: (1) a `|` inside a quoted regex still false-trips the (not-quote-aware)
command/commit guard as if it were a shell pipe, raising a needless approval stall — `--any` sidesteps the
metacharacter entirely; (2) a *typed flag* beats an in-string keyword (`OR`) or a `;;` separator, because `;` is
itself a guard metachar (same stall) and a keyword bakes a mini-DSL into the pattern that also collides with
searching for the literal word. It fixes **alternation** only; the general cure (a quote-aware guard so any
metachar inside a quoted arg stops false-tripping) is a separate, hook-side hardening task.
Examples:
```
tt text count build.log '^! '
tt text freq  run.log  '\[fallback\] ([a-z][^,]*)'
tt text grepr src .scala,.java 'TODO'
tt text grepr src .scala,.md --any TODO FIXME XXX   # lines matching any of the three (no | needed)
```

### md-fmt — markdown-aware line reflow to a target width (PURE by default; `--write` is the one guarded effect)
```
md-fmt <file>                        # reflow to stdout at the default width (80)
md-fmt <file> --line-width N         # target N columns
md-fmt <file> --write                # rewrite the file in place (content-guarded)
```
Reflows prose / list-item / blockquote blocks while PRESERVING structure: headings, ``` fences, |tables|,
`---` rules, blank lines, blockquote `>` prefixes, list markers + the author's continuation indent. Never
breaks inside `` `inline code` `` or `[links](url)`. Idempotent. A content-preservation guard REFUSES any
result (and any `--write`) that would change the text beyond whitespace + `>` — so it can only re-flow, never
re-word. (SM012 first cut; deferred: fuzzy/semantic-line-break modes, post-edit-hook wiring, `:shortcode:`
emoji.) Examples:
```
tt md-fmt notes/plan.md --line-width 82        # print reflowed at 82 cols (the PB width)
tt md-fmt notes/plan.md --line-width 82 --write  # ... and rewrite in place
```

### files — typed find / find|wc / grep -l replacement (PURE)
```
files <dir> <ext>                    # count + list files under dir ending <ext>     (find)
files <dir> <ext> <contentRegex>     # files whose content matches regex             (grep -l)
files <dir> <ext> [regex] --count    # just the number                               (find|wc)
```
(Plus `text grepr ... --count` returns the recursive match count — no `| wc`.)
Examples:
```
tt files src .scala 'TODO'                  # source files containing TODO
tt files src .scala --count
```

### find — typed, safe file enumeration; the allowlistable read-half of `find` (PURE)
```
find <root>                          # list regular files under <root>
find <root> --name '<glob>'          # filter by filename glob                        (find -name)
find <root> --ext <e>                # filter by extension suffix
find <root> --type f|d               # regular files (f, the default) or directories (d)
find <root> --max-depth N            # descend at most N levels below <root>
find <root> --all                    # include hidden entries (default: skip dot-names)
find <root> ... --count              # just the count line, no paths
```
Exposes ONLY name/ext/type/depth — no `-exec`, no arbitrary predicates, no `-delete` — so it can be blanket-allowed
where raw `find` (a general file-executor) cannot. Hidden dot-entries (`.git`, `.scala-build`) are skipped by
default — whole subtree and all — so a repo scan stays clean and fast; `--all` includes them. Symlinks are not
followed. The guarded write-half (`--prune`,
confined + dry-run-by-default) is a separate, later step (SM031). Sibling of `files` (which adds a content-regex).
Examples:
```
tt find src --ext .scala                    # every .scala file under src
tt find docs --name 'SM*.md'                # docs named SM*.md
tt find . --type d --max-depth 1            # immediate sub-directories
```

### which — what is this command? (PURE, read-only)
```
which <name> [<name> ...]            # PATH hits in order, symlink chain, kind, size/mode/mtime
```
The guard-clean composite of the whole bash reflex family `command -v` / `which` / `which -a` / `type` /
`file` / `readlink -f` / `ls -l`: for each name, every `$PATH` hit in order (the first is what a shell
runs; later ones are flagged shadowed), the symlink chain hop by hop, and the FINAL target's kind from
magic bytes (ELF / script with its shebang line / jar / text) plus size, mode and mtime. Knows the bash
builtins, so `tt which cd` answers honestly (aliases/functions live in the interactive shell and are
invisible to any subprocess — stated, not guessed). A name containing `/` is inspected as a path. It
never EXECUTES the target (no `--version` probing) — the line that keeps it allowlistable. Exit 0 when
every name resolved, 2 otherwise (scriptable existence check).
Examples:
```
tt which tt                                 # script or binary? symlinked from where? shadowed?
tt which cd echo                            # builtin honesty (echo is BOTH builtin and file)
tt which scala-cli sbt java                 # batch-check a toolchain
```

### limit — declare a usage limit the harness feed does not carry
```
limit                                # list declarations (with time left)
limit set <label> <pct> [--resets-in <dur>]   # declare/update (dur: 3d20h, 5h, 90m)
limit rm <label>  |  limit clear     # remove
```
Born from the f5 gap (2026-07-24): Claude Code's statusline JSON has NO per-model weekly window, while
`/usage` shows e.g. Fable at 84%. The HUMAN reads the number there and declares it here; `tt statusline`
renders it inside the lim block as `f5·~84%·3d` — the **`~` marks the % human-declared**, the countdown
stays live (computed from the declared anchor), and the cluster **auto-drops once the reset passes**, so
a stale declaration cannot outlive its window. Updating with a newer % (`limit set f5 91`) keeps the
anchor. Store: `~/.claude/gs-limits.json` — GLOBAL, deliberately not per-session (account limits are
account-global). Shared store logic lives in mainless `limitstore.scala` (the minijson pattern).
Examples:
```
tt limit set f5 84 --resets-in 3d20h        # from the /usage paste
tt limit set f5 91                          # newer banner, same window
tt limit rm f5
```

### doc — print a genscalator doc verbatim (PURE, read-only)
```
doc <name>                # print docs/<name>(.txt|.md) to stdout, verbatim
doc                       # list the available docs
doc --docs <dir> <name>   # override the docs dir (config-in-args, e.g. tests)
```
Cats a doc under `docs/` at NATIVE speed, so a rendered command (e.g. `gs help`) becomes an instant `tt doc
gs-help` instead of the agent re-emitting the file token-by-token. Path-safe: `<name>` is a bare filename
resolved only under the docs dir (no `/`, no `..`); it tries `<name>`, then `<name>.txt`, then `<name>.md`. The
docs dir is `<tools>/../docs`, located via `-Dtt.tools` (the `tt` launcher passes it) or a cwd walk-up.
Examples:
```
tt doc gs-help                              # the gs command help
tt doc statusline-manual                    # the statusline manual
tt doc                                      # list docs
```

### mode — record the declared modes of the joint state-of-mind (EFFECTFUL: a small state file)
```
mode                    # list the active modes (one per line)
mode add <label>        # declare <label> (add a label to the recorded state; idempotent)
mode rm <label>         # clear <label>
mode clear              # clear all
mode --file <f> ...     # override the state file (default ~/.claude/gs-modes; config-in-args, for tests)
```
A "mode" is a label on the shared human<->agent state-of-mind; MANY can be active at once, and BOTH the human
and the agent may add/remove them (a joint, mutually-visible channel). Declaring = adding a label to the
recorded state. The statusline's **mode line** (`tt statusline --mode-line`) renders whatever is active here,
each label reverse-video + bold in its own colour. Labels are bare tokens `[A-Za-z0-9._-]+`. Pairs with
`statusline` (which reads this state and renders line 2).

### log — build/run-log analyzer (PURE)
```
log [summary|errors|warnings] <file>    # summary (default) = counts + lines + verdict
   [--error <regex>]...                 # add an error pattern   (repeatable)
   [--warn  <regex>]...                 # add a warning pattern  (repeatable)
   [--no-defaults]                      # use ONLY supplied patterns (skip curated markers)
   [--cap <n>]                          # max lines shown per bucket (default 50)
```
**Sane defaults, customizable.** Curated markers span the logs agents actually read — compiler/build
(`error:`, `error[E…]`, `[error]`), test runners / CI (`FAIL`, `##[error]`), runtime leveled logs
(`ERROR`/`FATAL`/`CRITICAL`, logfmt `level=error`, JSON `"level":"error"`), Python `Traceback`, Go
`panic:`, `npm ERR!`, and LaTeX (`^! `, Over/Underfull). All **targeted** so tally lines like "0 errors" /
"no warnings" don't false-positive. Two buckets only: errors and warnings (test failures fold into errors).
When the agent knows a log's own markers, it extends (or with `--no-defaults`, replaces) the set. Each
pattern compiles separately, so an inline `(?i)` in one can't leak into the others. Reads Latin-1 (some
logs, e.g. LaTeX, aren't valid UTF-8).
Examples:
```
tt log build.log                                  # curated defaults (the 90% case)
tt log errors run.log --cap 200                   # just errors, show more
tt log app.log --error 'MYAPP-FATAL'              # defaults + my app's marker
tt log weird.log --no-defaults --error 'BOOM:'    # only my pattern
```

### newtool — generator (scaffold a new pure tool)
```
scala-cli run tools/newtool.scala -- <name>      # creates tools/<name>.scala from template.scala.txt
```

### verify — run-and-verify driver (EFFECTFUL)
```
verify [checks] -- <cmd> <args...>      # run <cmd> (NO shell), check exit/stdout/stderr, print PASS/FAIL
   --exit N        expected exit code (default 0)
   --out  <substr> / --out-re <regex>   stdout must contain / match
   --err  <substr> / --err-re <regex>   stderr must contain / match
```
The toolbox's first **effectful driver** (os-lib; not a pure tool). Replaces the `cd && … > log 2>&1;
echo $?` bundle with **one allowlistable call** — so `Bash(tt verify *)` is safe to blanket-allow.
Safe-by-design: runs the command **directly as argv (no shell)**, so `;`/`|`/`&&`/`$()`/globs are inert,
and only executables on the allowlist run — **`scala-cli`, `tt`, `scalex`**, plus any in the *human-set*
`TT_VERIFY_ALLOW` (comma-separated). The agent can't widen that via a flag (a flag would be agent-authored,
not human approval). Prints an audit line (argv, exit, ms) — the seed of the `--audit` roadmap flag.
Examples:
```
tt verify --exit 0 --out 8 -- scala-cli run tools/text.scala -- grepr /abs/tools .scala,.md grepr --count
tt verify -- tt files /abs/src .scala --count
```

### scala — typed driver over scala-cli for a project DIRECTORY (EFFECTFUL)
```
scala test <dir> [--prop k=v]...                # run the suite (exit 0 = green)
scala compile <dir> [--prop k=v]...             # compile only
scala run <dir> [--prop k=v]...                 # run the project @main (tightest verb)
scala package-js <dir> -o <out> [--prop k=v]... # link Scala.js to <out>
```
An **effectful driver** (os-lib) that runs `scala-cli` on a validated **directory** — never `-e` inline
eval, never an arbitrary script path — with the argv built here (no shell, no arbitrary-flag passthrough)
and `--server=false` baked (the no-bloop path). This is what lets the blanket `Bash(scala-cli *)` allow be
retired (SM205): each verb is per-verb allowlistable (`Bash(tt scala test *)`) while bare `scala-cli` stays
off the allowlist. It does **not** make running code safe — tests and `@main` run real code (SECURITY-MODEL,
"When the tool's job is to run code"); it removes the *surplus* a broad allow grants. `--prop k=v` becomes
`--java-prop` (e.g. `--prop tt.tools=<abs-tools>` for the toolbox suite). Prints an audit line (argv, exit,
ms) and passes scala-cli's exit code through.
Examples:
```
tt scala test /abs/tools --prop tt.tools=/abs/tools
tt scala package-js /abs/my-spa -o /abs/my-spa/main.js
```

### guardcheck — flag guard-trip / banned-reflex patterns (PURE)
```
guardcheck cmd <shell-command>       # flag &&, ;, $(, backtick, |head, raw grep -r, line-leading #, …
guardcheck msg <commit-message>      # flag patterns that trip the commit guard (e.g. a #N turn index)
```
A prosthetic for the confirmation-guard feedback the agent can't see. Exit 0 clean / 1 flagged / 2 usage.

### typo — keyboard-aware typo classifier (PURE)
```
typo adjacent <a> <b>                # are keys a,b adjacent on the Swedish QWERTY layout?
typo classify <typed> <intended>     # adjacency / transposition / deletion / insertion / substitution-far / complex
```
Feeds the human-fatigue / mutual-degradation gauge (BR's idea): the typo *kind* hints at tiredness.

### htmltext — strip a saved HTML page to readable text (PURE)
```
htmltext <in.html> [out.file]        # drop head/script/style/svg, block tags → newlines, decode entities
```
Turns a Firefox "Save Page As" dump (e.g. journal guidelines) into plain text without the JS/CSS bloat.

### chrono — stopwatch for timing work spans (EFFECTFUL: state + log)
```
chrono start [label] | stop [--think <dur>] | now | fmt <ms> | think <dur> | report
```
Times a human-agent-human round (or any span); `stop --think 30s` also records the relayed think-time and prints
the `round = think + human` split; spans append to `chrono-log.tsv`; `report` summarizes. (The agent can't
perceive its own think-time — this plus a human relay reconstruct a full round.)

### hangover — detect a just-ended agent blackout by the resume-gap (PURE read; the clock supplies `now`)
```
hangover <transcript.jsonl> [--now-ms N] [--threshold-sec N]
hangover hook [<json>]               # Claude Code SessionStart hook: stdin JSON -> a hangover line named by `source`
```
On resume, compares NOW to the last conversational record's timestamp and flags a gap that dwarfs execution
time: the "hangover" of a blackout the agent cannot perceive from inside (guard stall / long idle / compact /
box crash). Detects THAT you were out, not the cause; a `compact_boundary` among the recent records names it a
compact.

`hook` is the SM121 surface (BR's decision): wired as a **SessionStart** hook it fires on all four boundaries
and gets a `source` (`startup`/`resume`/`clear`/`compact`) that NAMES the seam a bare gap cannot tell apart.
Silent unless there is a hangover (its output is injected into context on every session start), fail-soft and
always exit 0 (a session start must never break on this). Wiring: `docs/hangover-hook.md` (human-gated).
Still uncovered: a mid-session stall or idle, which fires no SessionStart.

### parsereqt — parse reqT model text (PURE)
```
parsereqt <file>                     # parse reqT model text into a structured form
```

### svg — textual diagram spec → self-contained SVG (PURE; writes a file with `out`)
```
svg sequence <in.txt> [out.svg] [--light|--dark] [--transparent]   # spec → SVG (no out → stdout)
svg --sequence-diagram <in.txt> [out.svg]                          # alias for `sequence`
```
Input is a tiny PlantUML/mermaid-flavoured spec (`title:`, `actor <Id> [as label]`, `A -> B: call`,
`A --> B: reply`, `note over A,B: text`; `#`/`//` comments; self-message `A -> A` draws a loop). Output is a
**self-contained** SVG (inline `<style>`, no external refs) — inline it straight into an SSG page, an artifact, or a
report. **Theme:** default **auto** adapts to the viewer via `prefers-color-scheme`; **`--light`** / **`--dark`**
emit a *fixed, tailored* palette (predictable when the host page/PDF theme may differ from the OS setting — generate
the variant you need). **Background:** default is **opaque** and theme-coloured (transparent SVG backgrounds often
render badly in Markdown/GitHub); **`--transparent`** (aka `--no-bg`) drops it. Deliberately **not** reqT-lang: reqT
is conceptually a *bag* — element order isn't semantic (though reqT-lang preserves source order) and there's no
message concept, whereas a sequence's order *is* its meaning (see [`../research/037-svg-sequence-diagram-tool.md`](../research/037-svg-sequence-diagram-tool.md)).
Example:
```
tt svg sequence blog/figures/seq-compact-dance.txt blog/figures/seq-compact-dance.svg
tt svg sequence flow.txt flow-dark.svg --dark
```

### ascii — same spec → good-looking monospace/box-drawing diagram (PURE)
```
ascii sequence <in.txt> [out.txt] [--pure]   # render a sequence-diagram spec to monospace art (no out → stdout)
ascii --sequence-diagram <in.txt> [out.txt]  # alias for `sequence`
```
The **plaintext sibling of `svg`** — reads the *same* spec (grammar shared via `seqspec.scala`) and renders a
diagram for terminals, PR/commit comments, and plaintext reports. Default uses **Unicode box-drawing** glyphs
(`│ ─ ┌ ┐ └ ┘ ┬ ┴ ┼ ▶ ◀`) for looks; **`--pure`** falls back to strict **7-bit ASCII** (`| - + > <`). A dashed
reply (`A --> B`) renders as a gapped line; a self-message (`A -> A`) draws a small loop. Example:
```
tt ascii sequence flow.txt          # print to the terminal
tt ascii sequence flow.txt flow.txt.art --pure
```

### gvdot — same spec → image via graphviz `dot` (EFFECTFUL: spawns `dot`, writes a file)
```
gvdot sequence <in.txt> [out.pdf|.png|.svg]   # render via graphviz `dot` (no out → prints the generated DOT source)
gvdot --sequence-diagram <in.txt> [out.…]     # alias; output format inferred from the out extension (default pdf)
```
The **graphviz sibling** — reads the *same* spec (shared via `seqspec.scala`) and renders it by generating **DOT**
and shelling to **`dot`** (auto-layout: `pdf`/`png`/`svg`). **Needs graphviz** on PATH for the render path; if
missing it errors with `sudo apt install graphviz`. With **no out** it just prints the DOT source (needs no `dot` —
inspectable/testable). **Safety:** `dot` is run as **argv with no shell**, DOT fed on **stdin** (spec text can't
inject). Graphviz docs: https://graphviz.org/ · `dot -h` · `man dot`. Example:
```
tt gvdot sequence flow.txt flow.pdf         # PDF via graphviz
tt gvdot sequence flow.txt                  # just the DOT source
```

### web — safe read-only HTTP (EFFECTFUL: network, but GET-only)
```
web get <url> [--host H]... [--max-bytes N] [--status]   # fetch and print; GET only, no credential headers
```
Replaces the dual-use `curl` reflex. It can **only fetch-and-print**: GET only (no POST/PUT/upload), **no
credential/cookie headers ever**, response **size-capped** (default 5 MB), optional **`--host` allowlist**.
So `Bash(tt web get *)` is safe to blanket-allow where a bare `curl *` allowlist would expose exfiltration
(`curl -d @secret`), RCE (`curl … | sh`), and credential leaks. Residual risk is only SSRF-*read* of internal
hosts — lock down with `--host`. Example: `tt web get https://codeberg.org/api/v1/repos/o/r/tags --status`.

### serv — local static-file preview server (EFFECTFUL: network, but LOOPBACK-only, GET-only, read-only)
```
serv <dir> [--port N]      # serve <dir> at http://127.0.0.1:N/  (default N=8000; Ctrl-C to stop)
```
The audited replacement for `python3 -m http.server` when previewing a generated site (e.g. `tt ssg` output)
before deploy. Zero external deps (JDK `com.sun.net.httpserver`). **Always binds 127.0.0.1** — loopback only,
never `0.0.0.0`, so nothing is exposed off the box. GET/HEAD only; a directory serves its `index.html`; a
**path-traversal guard** keeps every served path under `<dir>` (`..`, encoded `..`, and leading-`/` cannot
escape → 403). Example: `tt serv site --port 8137` then open the printed URL. *(`--localhost` is accepted and
ignored; the bind is always loopback.)*

### ssg — hand-rolled markdown -> static HTML site generator (EFFECTFUL: writes .html files)
```
ssg <src> <out-dir> [--template <file>]     # <src> = a .md file or a dir of .md files
```
Renders the GitHub-flavored-markdown subset we use to self-contained HTML, consuming the SAME `MdParse.parse`
front-end that `md-fmt` reflows through (one parser, two renderers). Handles headings, paragraphs, blockquotes,
bold/italic (incl. `*italic*` inside `**bold**` and intraword-underscore safety), inline `code`, `[links](url)`,
`<autolinks>`, `![images]`, fenced code (a `language-*` class), GFM tables, and bullet/ordered lists. Template
resolution: `--template F`, else `<srcdir>/_template.html`, else a minimal builtin; slots are `{{TITLE}}` (first
h1) and `{{CONTENT}}`. A sibling `figures/` dir is copied so relative images resolve. Preview with `tt serv`.
Deferred (SM019 refinement): nested lists (rendered flat), footnotes, reference links, syntax highlighting.
Example: `tt ssg blog/002-....md tmp/site` then `tt serv tmp/site` and open the URL.

### forge — Forgejo/Gitea forge client, default Codeberg (EFFECTFUL: network; create needs env token)
```
forge whoami   [--url BASE]                               # verify auth: print the token's login (never the token)
forge releases <owner>/<repo> [--url BASE] [--limit N]    # list releases  (READ, no auth → allowlistable)
forge tags     <owner>/<repo> [--url BASE] [--limit N]    # list tags      (READ, no auth → allowlistable)
forge issues <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]   # list issues (READ)
forge prs    <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]   # list PRs    (READ)
forge contributors <owner>/<repo> [--gh | --gl | --url BASE] [--limit N]   # list contributors (READ; --gh/--gl only)
forge issue  <owner>/<repo> <n> [--gh | --url BASE]        # show an issue + comments   (READ)
forge pr     <owner>/<repo> <n> [--gh | --url BASE]        # show a PR: merge state + body (READ)
forge protection <owner>/<repo> <branch> [--gh | --url BASE]   # show the protection rule (needs token)
forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
                     [--prerelease] [--draft] [--target COMMITISH] [--url BASE]   # CREATE (effectful)
```
Replaces hand-curling the REST API (a `curl` with a token on the command line). **READ verbs need no auth**
(public repos) → safe to allowlist (`Bash(tt forge releases *)`, `Bash(tt forge tags *)`). The one **effectful**
verb (`release-create`) reads its token **only** from a fixed set of human-set env vars
(**`GENSCALATOR_CODEBERG_TOKEN`**, then `CODEBERG_TOKEN`, then `FORGE_TOKEN`) — never a flag — so the agent can't self-authorize (same trust-boundary rule as `verify`'s
`TT_VERIFY_ALLOW`). It prints an `[audit]` line and is deliberately **not** blanket-allowlistable (creating a
release stays a visible, confirmed op). **GitHub dialect:** `--gh` (or a github.com `--url`) switches the path
shapes to the GitHub REST API, rooted at the fixed `api.github.com` — never derived from `--url`, so a token
cannot be redirected. The GitHub token comes only from fixed env names (`GENSCALATOR_GITHUB_TOKEN`,
`GITHUB_TOKEN`, `GH_TOKEN`); reads work anonymously (60/h rate limit), `protection` requires it (admin read).
**`contributors`** reads the repo's contributor list — `--gh` prints `login⇥contributions⇥type` (type = `User`/`Bot`,
the field that answers "why is a bot on the list"), `--gl` prints `name⇥email⇥commits`; the Gitea/Forgejo REST API
has no contributors endpoint (Codeberg 404s), so the default dialect says so plainly rather than erroring cryptically
(SM217). Example:
```
tt forge releases bjornregnell/genscalator --limit 5
tt forge prs lunduniversity/introprog --gh                 # open PRs on a GitHub repo
tt forge issue lunduniversity/introprog 951 --gh           # one issue with its comment thread
tt forge contributors lunduniversity/introprog --gh        # who GitHub credits (login/contributions/type)
tt forge release-create bjornregnell/genscalator v0.8.0 --name "v0.8.0: …" --body-file NOTES.md --prerelease
```

### git — safe git helper: commit-from-file, ff-pull, fetch, read-only show (EFFECTFUL, non-destructive)
```
git commit --repo <dir> --message-file <path> [--add <pathspec>]... [--push]
git pull   --repo <dir>                                  # fast-forward ONLY: FFs or fails loudly
git fetch  --repo <dir>                                  # remote-tracking refs only, never the working tree
git show   --repo <dir> --ref <ref> --path <relpath> [--out <file>]   # READ-ONLY: file content at a ref
git log    --repo <dir> [--grep P] [--co-author P] [--author P] [--committer P] [--since D] [--limit N]  # READ-ONLY search
```
Exposes ONLY the safe, non-destructive verbs — no reset/rebase/merge/rm/clean/`--force` — so `Bash(tt git *)`
cannot become a data-loss vector. `commit` reads its message from a FILE, so prose with shell metacharacters
(backticks, `$`, `!`, braces, bare `*`) never touches the command line (kills the recurring commit-message
allowlist tripwire); `--add` stages only the listed paths (never an implicit add-all). **`show`** extracts a
file's content at any commit-ish (HEAD, branch, tag, SHA) **byte-exact** to stdout or, with `--out`, to a file
— the allowlist-clean replacement for redirecting raw `git show ref:path` output (the redirect plus git's
general surface blocked allowlisting, e.g. when a PR-review sub-agent needs a file at the base ref). On a bad
ref or path it exits non-zero with git's error — never a partial/empty success. **`log`** is a READ-ONLY
commit-log search: it caps (`--limit`, default 50) and tab-formats the output (`<short-sha>⇥<author-email>⇥<subject>`
plus a `=== N commit(s)` line that flags when the cap was hit), so it needs no `| head` and `Bash(tt git log *)`
stays allowlist-safe — `--co-author P` greps the `Co-Authored-By:` trailer forges attribute contributors from
(SM217). Examples:
```
tt git commit --repo /abs/repo --message-file tmp/msg.txt --add src/app.scala --push
tt git show --repo /abs/repo --ref main --path src/app.scala
tt git show --repo /abs/repo --ref v1.2 --path README.md --out tmp/old-readme.md
tt git log  --repo /abs/repo --co-author Claude --limit 20    # commits with a Claude co-author trailer
```

### update — check whether genscalator is BEHIND its marketplace remote, and SUGGEST the manual update steps (EFFECTFUL: git fetch; read-only)
```
update [--repo <dir>] [--brief] [--throttle <hours>]
   --repo <dir>        the genscalator repo to check     (default: self-locate via the tools dir)
   --brief             print ONLY an actionable "newer release available" notice; silent otherwise
   --throttle <hours>  actually fetch at most once per <hours> window (stamp-file gated); implies --brief
```
genscalator's **own update-awareness**, because the platform gives none: a third-party Claude Code marketplace does
**not** auto-update, there is no per-plugin update command, and plugin authors get no update-check API. But
genscalator *is* a git checkout, so git is the mechanism — `update` **fetches remote-tracking refs (read-only, never
the working tree)**, compares your installed version against the remote, and, if you are behind, prints the incoming
commits plus the steps **you** run (`/plugin marketplace update bjornregnell` (the MARKETPLACE name from marketplace.json, not the plugin name) then `/reload-plugins` — the harness
commands a tool cannot drive). It changes nothing itself; the human is the actuator. Exits 0 in all normal cases and
degrades gracefully when offline, when the branch has no upstream, or when genscalator is not a git checkout.
`--throttle` is what **`gs warm`** calls (`tt update --brief --throttle 24`), so warm gains update-awareness without
ever hanging or nagging. Examples:
```
tt update                       # full report: installed version, ahead/behind, and what to do
tt update --brief               # speak only if a newer release is available
tt update --brief --throttle 24 # gs warm's call: check at most once a day, silent unless behind
```

### statusline — format the Claude Code statusLine stdin JSON into ONE compact line (PURE: reads stdin, prints; SM039)
Reads the JSON Claude Code pipes to the configured `statusLine` command each turn and prints one compact,
colour-coded line — model, context-fill (the rot gauge), usage limits, cost — with optional `--mode-line`
(line 2, the declared modes) and `--box-line` (line 3, measured box health). Full legend: `docs/statusline-manual.md`.

### box — safe host + local box ops: health, and host-pinned remote ops for a known compute box (EFFECTFUL; SM181)
Replaces the dual-use `ssh *` / `ps` / `pkill` reflexes with a narrow, allowlistable tool: a FIXED verb enum,
no shell passthrough, a pinned default host. LOCAL health/kill shapes for this machine plus host-pinned REMOTE
ops for the known compute box.

### gitinfo — typed, READ-ONLY git status/overview (PURE, read-only)
Branch, clean/dirty count, ahead/behind vs upstream, and the recent log in ONE call; `--remote <name>` also
checks whether local HEAD is in sync with that remote's HEAD (via `ls-remote`). Retires raw
`git -C … status/log/ls-remote`; only read-only git subcommands, never add/commit/checkout/fetch.

### prd — read + navigate the genscalator PRD.md (PURE, read-only)
See what the PRD says without re-emitting it token-by-token: `tt prd show` (whole file), `tt prd summarize`
(a FUTURE-roadmap gist), `tt prd find <what>` (locate a term by its nearest heading). Complements
`tt parsereqt` (which parses + lints the reqT-lang).

### harden — Layer-1 deterministic secret scanner (PURE, read-only; SM042)
Surfaces CANDIDATE secrets for semantic (Layer-2) triage. `tt harden repo <dir>` scans git-TRACKED text files
(respects `.gitignore`); `tt harden egress <dir>` scans ALL files under a dir destined to LEAVE (a ZIP-staging
or deploy bundle) — the higher-value half, since a secret safe at rest can leak on egress.

### skillcheck — verify the genscalator skill set is active; catch the silent skill outage (PURE, read-only; SM070)
The agent CANNOT feel a missing skill (no phenomenology of absence), so this prints the EXPECTED set (derived
from the `skills/*/SKILL.md` dirs, so it never drifts) to diff against the live `/skills` list; feed the active
names via `--active` for a machine-checked, exit-coded diff.

### skillgrants — print what a skill GRANTS: its allowed-tools frontmatter, for informed consent (PURE, read-only; SM100)
When the harness loads a skill it silently widens the auto-approved tool set by that skill's `allowed-tools`,
but never shows the human WHICH tools at grant time. This is that read: name a skill (or list all) and see
exactly which tools it opens.

### bloop — targeted BloopServer control: status + restart (EFFECTFUL; SM146c)
Bloop is a disposable compile daemon that respawns lazily, so "restart" is a targeted kill + lazy respawn. It
uses `kill -9` deliberately: when bloop is wedged (the empirical villain, SM150) polite protocols hang and a
signal is the reliable cure. Its RSS also surfaces on the statusline box line so regrowth is visible early.

### wr — Workflow-Research utilities for the WR corpus itself (PURE, read-only)
`tt wr stamp <project-dir> <regex> [--user|--human] [--limit N]` retrofits the REAL date-time of an utterance
or event from the session `.jsonl` transcripts — the grounded-timestamp tool behind the WR-data discipline.

## Companion: scalex
The `tt` tools are **textual** — grep/awk/cut over any file. For **Scala code structure** the companion
is **[scalex](https://github.com/nguyenyou/scalex)**: "grep, but it understands Scala's AST." It parses
with Scalameta and caches per git OID — no build server (~2–5 s cold index, **<400 ms** warm).

**Separately installed, not bundled.** scalex is its own upstream project (a GraalVM-native CLI shipping
its own Claude Code plugin); genscalator recommends and integrates it. Install (adopter):
```
/plugin marketplace add nguyenyou/scalex
/plugin install scalex@scalex-marketplace
```
Core commands:
```
scalex explain <Sym>           # definition + scaladoc + members + impls
scalex def <Sym>               # where defined
scalex refs <Sym> --count      # categorized usage / impact
scalex hierarchy <Sym>         # super/sub types
scalex imports <Sym>           # resolve imports (incl. wildcard `import pkg.*`)
scalex body <method> --in <Type>
scalex batch ...               # several queries, one index load
```
Filters: `--kind / --path / --no-tests / --exact / --max-output` (~30 commands total).

**When to reach for it:** any Scala *structure* question (where defined, who uses, what extends, show the
body, resolve an import) — symbol-aware and structured, so fewer follow-up calls than grep. Use `tt`/grep
for plain text and logs; use Metals MCP when you need true compiler semantics (inferred types,
diagnostics, refactors). Full guide: [`../docs/tool-selection.md`](../docs/tool-selection.md).

## Files
- `lib.scala` — shared PURE helpers (`readLatin1`/`readUtf8`, `histogram`, `edit1`). No deps.
- `text.scala` — the grep/awk replacement.
- `log.scala` — the build/run-log analyzer.
- `verify.scala` — the run-and-verify driver (effectful; os-lib).
- `scala.scala` — typed driver over scala-cli for a project dir (effectful; os-lib).
- `files.scala` — the find / grep -l replacement.
- `guardcheck.scala` — guard-trip / banned-reflex flagger.
- `typo.scala` — keyboard-aware typo classifier.
- `htmltext.scala` — HTML→text stripper.
- `chrono.scala` — stopwatch (effectful: state + log).
- `parsereqt.scala` — reqT model parser.
- `seqspec.scala` — shared sequence-diagram spec model + parser (no `@main`, like `lib.scala`); reused by `svg`, `ascii` + `gvdot`.
- `svg.scala` — sequence-diagram spec → self-contained, theme-aware SVG (pure; writes a file with `out`).
- `ascii.scala` — sequence-diagram spec → good-looking monospace/box-drawing art (pure; `--pure` for 7-bit ASCII).
- `gvdot.scala` — sequence-diagram spec → image via graphviz `dot` (effectful; needs graphviz; argv-no-shell, DOT on stdin).
- `web.scala` — safe read-only HTTP GET (effectful: network; requests).
- `forge.scala` — forge client (Gitea/Codeberg + GitHub via `--gh`): releases/tags/issues/PRs/protection reads + env-token create (effectful; requests+ujson+os-lib).
- `git.scala` — safe git helper: commit-from-file, ff-only pull, fetch, read-only show (effectful; os-lib).
- `update.scala` — update-awareness: is genscalator behind its marketplace remote? (effectful: git fetch; read-only; os-lib).
- `newtool.scala` — the generator.
- `template.scala.txt` — starter template (latest Scala header, lib include, dispatch skeleton).

## Conventions
- **Pure tools** (read → compute → print): keep them pure; later default to **Capture-Checking Safe
  mode** so the compiler errors on accidental side effects (PoC pending — see ../README.md roadmap).
- **Effectful drivers** (run sbt/pdflatex, write files): separate files; os-lib `os.proc`; not Safe mode.
- Live **in-project** (this repo, or `<project>/.../scratch/` for one-offs) so paths stay inside the
  trusted tree (avoids the `/tmp` path-resolution-bypass approval). Drivers should root-find (walk up).
- Clean `===` section output; return a clear verdict (e.g. error count) so no bash post-processing is needed.

## Roadmap
- More generic tools (tsv stats, pdf scan), generalized from real case-study work. (`log` analyzer shipped
  in v0.6.0; `verify` run-and-verify driver in v0.7.0.)
- Extend the guarded-run primitive (`verify` already does allowed-executables + no-shell): add allowed-roots
  / `cwd`, reject `..`/symlinks, a `--dry-run` echo. `verify` also prototypes the `--audit` flag below.
- Capture-Checking Safe-mode PoC → pure tools safe by default.
