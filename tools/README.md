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
text grepr <dir> <ext[,ext2…]> <regex>  # grep -r --include : recursive search → file:line:match
text cols  <file> <sep> <i...>       # cut/awk   : extract 1-based fields, tab-joined
```
`grepr` takes a comma-separated extension list (`.scala,.java`) and prints a friendly one-line error
(+ exit 2) on a missing/relative dir instead of a stack trace — pass an absolute dir.
Examples:
```
tt text count build.log '^! '
tt text freq  run.log  '\[fallback\] ([a-z][^,]*)'
tt text grepr src .scala,.java 'TODO'
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
models an unordered *set*, a sequence is *ordered in time* — see [`../research/037-svg-sequence-diagram-tool.md`](../research/037-svg-sequence-diagram-tool.md).
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

### web — safe read-only HTTP (EFFECTFUL: network, but GET-only)
```
web get <url> [--host H]... [--max-bytes N] [--status]   # fetch and print; GET only, no credential headers
```
Replaces the dual-use `curl` reflex. It can **only fetch-and-print**: GET only (no POST/PUT/upload), **no
credential/cookie headers ever**, response **size-capped** (default 5 MB), optional **`--host` allowlist**.
So `Bash(tt web get *)` is safe to blanket-allow where a bare `curl *` allowlist would expose exfiltration
(`curl -d @secret`), RCE (`curl … | sh`), and credential leaks. Residual risk is only SSRF-*read* of internal
hosts — lock down with `--host`. Example: `tt web get https://codeberg.org/api/v1/repos/o/r/tags --status`.

### forge — Forgejo/Gitea forge client, default Codeberg (EFFECTFUL: network; create needs env token)
```
forge whoami   [--url BASE]                               # verify auth: print the token's login (never the token)
forge releases <owner>/<repo> [--url BASE] [--limit N]    # list releases  (READ, no auth → allowlistable)
forge tags     <owner>/<repo> [--url BASE] [--limit N]    # list tags      (READ, no auth → allowlistable)
forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
                     [--prerelease] [--draft] [--target COMMITISH] [--url BASE]   # CREATE (effectful)
```
Replaces hand-curling the REST API (a `curl` with a token on the command line). **READ verbs need no auth**
(public repos) → safe to allowlist (`Bash(tt forge releases *)`, `Bash(tt forge tags *)`). The one **effectful**
verb (`release-create`) reads its token **only** from a fixed set of human-set env vars
(**`GENSCALATOR_CODEBERG_TOKEN`**, then `CODEBERG_TOKEN`, then `FORGE_TOKEN`) — never a flag — so the agent can't self-authorize (same trust-boundary rule as `verify`'s
`TT_VERIFY_ALLOW`). It prints an `[audit]` line and is deliberately **not** blanket-allowlistable (creating a
release stays a visible, confirmed op). Example:
```
tt forge releases bjornregnell/genscalator --limit 5
tt forge release-create bjornregnell/genscalator v0.8.0 --name "v0.8.0: …" --body-file NOTES.md --prerelease
```

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
- `files.scala` — the find / grep -l replacement.
- `guardcheck.scala` — guard-trip / banned-reflex flagger.
- `typo.scala` — keyboard-aware typo classifier.
- `htmltext.scala` — HTML→text stripper.
- `chrono.scala` — stopwatch (effectful: state + log).
- `parsereqt.scala` — reqT model parser.
- `seqspec.scala` — shared sequence-diagram spec model + parser (no `@main`, like `lib.scala`); reused by `svg` + `ascii`.
- `svg.scala` — sequence-diagram spec → self-contained, theme-aware SVG (pure; writes a file with `out`).
- `ascii.scala` — sequence-diagram spec → good-looking monospace/box-drawing art (pure; `--pure` for 7-bit ASCII).
- `web.scala` — safe read-only HTTP GET (effectful: network; requests).
- `forge.scala` — Forgejo/Gitea forge client, releases/tags + env-token create (effectful; requests+ujson+os-lib).
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
