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

## Tools

### text — typed grep/awk/cut/uniq replacement (PURE)
```
text count <file> <regex>            # grep -c   : count matches
text match <file> <regex>            # grep -n   : print matching lines, numbered
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
