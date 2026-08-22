# tools/ — Scala agent toolbox (cheat-sheet)

Typed, compiler-checked, reusable Scala scratch tools that replace the brittle bash/grep/awk/python
reflex. **Off-the-shelf: pick a tool, give args** — no re-deriving logic each time, no dynamic-shell
surprises (the compiler catches mistakes before they run). Project-agnostic. **Policy: track the latest LTS
Scala, bleeding edge — a release candidate counts**; re-check it per project. The version itself is stated
ONCE, in [`project.scala`](project.scala) — every tool carries `//> using file project.scala`
instead of naming a version, so a bump is one edit there rather than one per tool. This line is the single
place the *policy* is stated in prose; deliberately no version number is repeated here.

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
**The native fast path is the default** (since 2026-07-23): when the native-image dispatcher binary
exists and is fresh, `tt` runs it and a call answers in ~10 ms with no compile at all. When the binary
is stale (tools edited since it was built) or absent, `tt` falls back to `scala-cli` with a stderr
note — SLOWER, still correct; first such run compiles (~couple s), reruns are cached. Refresh the
binary with `scala-cli run deploy/buildnative.sc`; details in `docs/native.md`. Pure tools use only
the JDK; effectful drivers add `//> using dep com.lihaoyi::os-lib:0.11.8` (some also requests/ujson).

## Tests
The suite is **co-located** under [`test/`](test/): ~30 `.test.scala` files, roughly one per tool —
`test/cli.test.scala` (CLI-contract tests — each tool run as a subprocess, exit + stdout asserted) and
`test/lib.test.scala` (unit tests for `lib.scala`) are the founding two; most tools since have grown a sibling
(`dispatch.test.scala`, `ssg.test.scala`, `tsv.test.scala`, …). Run the whole toolbox + tests with
**`tt scala test <abs>/tools --prop tt.tools=<abs>/tools`** (the allowlist-clean driver form, correct from any
cwd) — or `scala-cli test tools` from the repo root. Test scope extends the toolbox's main scope, so a plain
`scala-cli compile tools` still builds only the tools (the `.test.scala` files are test-scope and excluded).

## Tools

### text — typed grep/awk/cut/uniq replacement (PURE)
```
text count <file> <regex>            # grep -c   : count matches
text match <file> <regex>            # grep -n   : print matching lines, numbered
text context <file> <regex> [N]      # grep -C N : matching lines with N lines of context (default 2)
text freq  <file> <regex>            # sort|uniq -c|sort -rn : histogram of match (or capture group 1)
text grepr <dir> <ext[,ext2…]> <regex>        # grep -r --include : recursive search → file:line:match
text grepr <dir> <ext[,ext2…]> <regex> --count  # just the total match count (grep -r | wc)
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
re-word. (First cut; deferred: fuzzy/semantic-line-break modes, post-edit-hook wiring, `:shortcode:`
emoji.) Examples:
```
tt md-fmt notes/plan.md --line-width 82        # print reflowed at 82 cols (the PB width)
tt md-fmt notes/plan.md --line-width 82 --write  # ... and rewrite in place
```

### sub — typed search-and-replace across files (EFFECTFUL; PREVIEW BY DEFAULT)
```
sub file <file> <regex> <replacement> [--write] [--literal]
sub tree <dir> <ext[,ext2,...]> <regex> <replacement> [--write] [--literal]
```
The typed replacement for `sed -i` / a `python3` one-liner — the shape whose ABSENCE fires the raw-interpreter
reflex (a Scala version bump touched 78 files with no typed tool for it). Deliberately **not** a verb on
`text`, which documents itself as pure; a tool that rewrites files is an effectful driver and lives on its own.
The safety property sed lacks: **nothing is written without `--write`** — the default run prints `path:line` with
the old line then the new one, so the destructive step is always a second, deliberate act on a diff you have
read. Patterns are Java regex matched **per line** (so `^`/`$` anchor to the line) and `$1` backrefs work in the
replacement; `--literal` turns off regex AND backrefs on both sides for text containing a literal `$` or `\`.
`tree` skips generated dirs (`.git .scala-build target node_modules .bloop .metals`), so a bulk rewrite can
never corrupt a build cache, and line endings plus a missing final newline are preserved byte-for-byte.
```
tt sub tree /abs/repo/tools .scala 'using scala 3\.8\.4' 'using scala 3.9.0-RC4'          # preview
tt sub tree /abs/repo/tools .scala 'using scala 3\.8\.4' 'using scala 3.9.0-RC4' --write  # apply
tt sub file build.txt 'v1.2 (old)' 'v1.3' --literal --write
```

### files — typed find / find|wc / grep -l replacement (PURE)
```
files <dir> <ext>                    # count + list files under dir ending <ext>     (find)
files <dir> <ext> <contentRegex>     # files whose content matches regex             (grep -l)
files <dir> <ext> [regex] --count    # just the number                               (find|wc)
files ... --exclude '<glob>'         # drop paths matching glob, relative to <dir>    (repeatable)
files ... --all                      # include EVERYTHING: hidden entries AND the curated skips
```
(Plus `text grepr ... --count` returns the recursive match count — no `| wc`.)
Hidden dot-entries are skipped by default, whole subtree and all — same pruning as `find` — and so are
directories named `target`, `out`, `build`, `node_modules`; unlike the dot-name skip the curated skips are
DISCLOSED on the count line together with `--exclude` suppressions, e.g. `12 files (2 excluded: target,
node_modules)` — a pruned subtree counts as one entry, and when nothing was excluded the plain count line
is printed. `--exclude` globs use java.nio glob syntax, matched against the path relative to `<dir>`;
a glob ending in `/**` prunes that whole subtree.
Examples:
```
tt files src .scala 'TODO'                  # source files containing TODO
tt files src .scala --count
tt files . .scala --exclude 'seeds/**'      # sources, minus the whole seeds subtree
```

### find — typed, safe file enumeration; the allowlistable read-half of `find` (PURE)
```
find <root>                          # list regular files under <root>
find <root> --name '<glob>'          # filter by filename glob                        (find -name)
find <root> --ext <e>                # filter by extension suffix
find <root> --type f|d               # regular files (f, the default) or directories (d)
find <root> --max-depth N            # descend at most N levels below <root>
find <root> --exclude '<glob>'       # drop paths matching glob, relative to <root>   (repeatable)
find <root> --all                    # include EVERYTHING: hidden entries AND the curated skips
find <root> ... --count              # just the count line, no paths
```
Exposes ONLY name/ext/type/depth/exclude — no `-exec`, no arbitrary predicates, no `-delete` — so it can be
blanket-allowed where raw `find` (a general file-executor) cannot. Hidden dot-entries (`.git`, `.scala-build`)
are skipped by default — whole subtree and all — so a repo scan stays clean and fast, and so are directories
named `target`, `out`, `build`, `node_modules`; unlike the dot-name skip the curated skips are DISCLOSED on
the matches line together with `--exclude` suppressions, e.g. `5 matches (2 excluded: target, node_modules)` —
a pruned subtree counts as one entry, and when nothing was excluded the plain matches line is printed. `--all`
includes everything: hidden entries AND the curated skips. `--exclude` globs use java.nio glob syntax, matched
against the path relative to `<root>`; a glob ending in `/**` prunes that whole subtree. Symlinks are not
followed. The guarded write-half (`--prune`,
confined + dry-run-by-default) is a separate, later step. Sibling of `files` (which adds a content-regex).
Examples:
```
tt find src --ext .scala                    # every .scala file under src
tt find docs --name 'SM*.md'                # docs named SM*.md
tt find . --type d --max-depth 1            # immediate sub-directories
tt find . --ext .json --exclude 'seeds/**'  # .json files, minus the whole seeds subtree
```

### json — read a JSON file: validate, inspect, pluck (PURE, read-only)
```
json check  <file>           # parse-or-fail (exit 0 = well-formed, 2 = not)
json pretty <file> [path]    # re-render indented, for READING
json get    <file> <path>    # print one scalar value, unquoted
json keys   <file> [path]    # an object's keys (one per line), or an array's length
```
The typed replacement for `jq` / `python3 -m json.tool`. Paths are dot-separated; a numeric segment
indexes an array (`permissions.allow.3`, `hooks.PreToolUse.0.matcher`); omit for the whole document.
**A VIEWER, NEVER A REWRITER:** `pretty` sorts keys and drops spacing, so never write its output back
over a source file (especially a human's settings file) — edit JSON as text.

### tsv — read and filter a tab-separated file (PURE reads; `drop` writes a NEW file only)
```
tsv cols  <file> [--no-header]                # column names + row count
tsv count <file> [filters]                    # how many data rows match
tsv rows  <file> [filters] [--limit N]        # print matching rows
tsv drop  <file> [filters] --out <new>        # write NON-matching rows to a NEW file
```
Filters are ANDed: `--col <name|index> --eq <v>` / `--matches <regex>` / `--same-as <name|idx>`
(the key==value shape); `--no-header` makes line 1 data with 0-based column indexes. **Never edits in
place:** `drop` requires `--out` and refuses an existing path, so a wrong predicate cannot damage the
input. Example: `tt tsv count cache.tsv --col 0 --same-as 1 --col 1 --matches '[åäöÅÄÖ]'`.

### zip — read-only zip inspection + guarded extract (JDK-only; EFFECTFUL only with `--write`)
```
zip list  <file.zip>         # entries: uncompressed, compressed, method, crc32, name
zip check <file.zip>         # decompress every entry so the JDK validates each CRC32
zip extract <file.zip> --dir D [--write] [--overwrite] [--exec GLOB] [--max-bytes N]
```
`check` vs a checksum: sha256 proves the bytes arrived as sent; `check` proves the archive is internally
sound — run both on a release. **`extract` is the most destructive verb in the toolbox** (it can write
executables), so it PREVIEWS by default like `tt sub`; `--write` applies. Every entry is adjudicated
BEFORE anything is written: path escapes, absolute paths (incl. Windows drive/UNC), control characters,
and a total-size zip-bomb cap (`--max-bytes`, 1 GiB default) are rejected; existing targets refused
unless `--overwrite`. `--exec GLOB` marks entries owner-executable — needed because `java.util.zip`
cannot restore permission bits, so the CALLER declares which entries are programs (deliberately stricter
than OS `unzip`).

### links — link + reference analysis across a repo (PURE, read-only)
```
links check <absdir> [--ext <list>]                  # dangling markdown/html links; exit 1 if any
links to <absdir> <path> [--ext <list>]              # which files reference <path>
links reach <absdir> --root <rel> ...                # files reachable from the roots, transitively
links reach <absdir> --root <rel> --unreachable      # the complement: what nothing points at
links reach <absdir> --root <rel> --leaf <rel>       # <rel> is kept, but what it cites is not
```
Answers the two questions every move or rename raises — *what is broken now?* and *what still points at
this?* — mechanically instead of with a pile of greps, and it is re-runnable after the move.

**The design point, and it is not obvious:** references come in three shapes, and only the first is a link.
Markdown `[text](target)`, html `href=`/`src=`, and **a bare or backticked repo-relative path in prose** —
which is how most shipped skills cite research files. So `check`, the pass/fail gate, uses the first two
only, where a dangling target is unambiguous; `to` and `reach`, which answer *may I move this?*, also count
the third, because there a **missed** reference is the expensive error and a false positive merely keeps a
file. For the same reason a `dir/prefix` citation (`research/topics/RT052`) counts every file in that dir
with that prefix. ⚠ That generosity hides a real trap when numbers are NOT unique: two files shared the
`052` prefix, so one ambiguous citation kept BOTH, and making it precise (2026-07-26) revealed the other
had no reference of its own. **A prefix citation can mask unreachability — prefer the full filename.** Two more rules worth knowing
before wiring `check` into a gate: a link to `x.html` is satisfied by a sibling `x.md` (the generated-page
rule, so a pre-render tree checks clean against its post-render links), and cited-DIRECTORY expansion
applies only at depth ≥ 3 components (a grouping dir like `research/` is not treated as citing everything
under it; an artifact dir like `research/experiments/indent-vs-braces/` is). Site-absolute targets
(`/genscalator/...`) are treated as external: they are URLs on the deployed
site, not repo paths, so validating them here would report a false break on every page.

**The mirror limit, and `--leaf`.** That same generosity misfires when the question is *may I DELETE
this?*. An append-only archive — a raw research log, a minion log — mentions files **historically**,
not because anything depends on them, so it silently pins whatever it ever named. Found 2026-07-26: a
frozen audit log mentioned a draft blog post, which mentioned another, and both were kept three hops
from anything alive. `--leaf <rel>` separates the two relations a plain walk conflates: a leaf is
**kept** when something points at it, but its **own** citations are not followed. Repeatable, opt-in,
and it cannot change a run that does not pass it.

**The check ignore file (issue-011).** A checked-in `.links.ignore` at the scanned root records links
that are dangling *by design* — the manual sources cite the `.html` pages only `tt ssg` generates, and
the serverless-spa-seed template cites the `main.js` its build step produces. One entry per line,
`from/file.md -> target.html  # reason`; the reason is MANDATORY (a malformed file exits 2, a config
error kept distinct from a link regression), so an exemption is documentation, not silence. Excused
links are printed with their reason and kept off the exit code; an unused entry is noted, never fatal.
With the six known cases recorded, `links check` exits 0 on the repo, and
`.github/workflows/links-check.yml` gates every push and PR on it.
```
tt links check /abs/repo                                              # is anything broken right now?
tt links to /abs/repo research/METHODOLOGY.md                         # who depends on this file?
tt links reach /abs/repo --root README.md --root skills --unreachable # safe-to-move candidates
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

### env — read environment variables without spilling them (PURE, read-only; the audited `printenv` replacement)
```
env list [regex]             # variable NAMES only, never values (case-insensitive filter)
env has <NAME>               # exit 0 if set AND non-blank, 1 otherwise; prints nothing
env get <NAME>               # one variable; value REDACTED if it looks like a credential
env get <NAME> --reveal      # print the real value, one variable, deliberately
```
There is deliberately **no verb that prints the whole environment** — that request IS the hazard this
tool removes: a bare `printenv` once put two live tokens into a transcript, which is durable, copied
and quoted. Read-only is not the same as safe. A value is withheld when the NAME looks
credential-bearing, the VALUE matches a known credential shape, or it is long and high-entropy;
redaction shows the first 4 chars + length. Example: `tt env list CLAUDE`.

### limit — declare a usage limit the harness feed does not carry
```
limit                                # list declarations (with time left)
limit set <label> <pct> [--resets-in <dur>]   # declare/update (dur: 3d20h, 5h, 90m)
limit rm <label>  |  limit clear     # remove
limit --file <f> ...                 # override the store (default ~/.claude/gs-limits.json; for tests)
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

### mode — record the declared modes of the joint state-of-mind (EFFECTFUL: small state files, per-session)
```
mode                    # list the active modes (this session's chips + the machine-scoped budget chips)
mode add <label>        # declare <label> (add a label to the recorded state; idempotent)
mode rm <label>         # clear <label> (from whichever store holds it)
mode clear              # clear this SESSION's modes (budget chips stay)
mode --file <f> ...     # single-file mode on <f>, NO session scoping (config-in-args, for tests)
mode --global-file <g> | --sessions-root <d> | --id <id> | --cwd <d>   # store/id/cwd overrides (for tests)
```
A "mode" is a label on the shared human<->agent state-of-mind; MANY can be active at once, and BOTH the human
and the agent may add/remove them (a joint, mutually-visible channel). **ALL modes are PER-SESSION (SM208)**:
chips are keyed on the harness session id (env `CLAUDE_CODE_SESSION_ID`, state in
`~/.claude/gs-sessions/<id>/`), so parallel sessions cannot flip each other's chips — including the
token-budget chips, because the shared FACT (account headroom) lives in `tt limit`'s machine store while a
budget CHIP is this session's spend policy. In a bare shell with no session id everything falls back to the
global `~/.claude/gs-modes` file; chips left there render in every session until removed. If a list finds
NO state under this session's key while recent (<48h) orphaned state for the SAME directory exists (the
harness re-minted the session id, e.g. a bg/fg round trip), ONE hint line goes to **stderr** — stdout stays
byte-identical — pointing at the recovery: `tt session adopt`. The statusline's **mode line** (`tt statusline --mode-line`) renders whatever is active,
each label reverse-video + bold in its own colour, padded one space each side. Labels are bare tokens
`[A-Za-z0-9._-]+`. Pairs with `session` (the session NAME) and `statusline` (rendering).

### session — name THIS session, so parallel sessions are tellable apart (EFFECTFUL: a small state file)
```
session                 # print the display name: YYMMDD-HHhMMm[-MyName]
session <name words>    # set the human name part (free text, spaces allowed; control chars rejected)
session list             # list sessions recorded for THIS directory, newest first (alias: ls; pure read)
session --clear         # remove the human name (the timestamp part remains)
session adopt           # re-attach state orphaned by a harness session-id re-mint
session adopt <id>      # pick among several candidates (a bare adopt lists them)
session --sessions-root <d> | --id <id> | --cwd <dir> | --now-ms <ms>    # overrides (for tests)
```
The timestamp part is ALWAYS present and FIRST: the age signal survives naming, duplicate human names cannot
collide, and the string is filesystem-safe by construction — though the display name is never a path
component; the store is keyed on the opaque harness session id. The statusline renders the name inverted
after a `gs session:` label on the mode line. Outside a harness session (no id) there is nothing to name:
the tool says so and exits 1.
The harness id is unique but NOT stable — a background/foreground round trip re-mints it, orphaning
name + chips under the old key while reads of the new key find silent emptiness. `adopt` is the explicit
recovery: with exactly ONE orphan recorded for the SAME working directory it copies that orphan under the
current key and reports what was adopted (name, chips, age); with SEVERAL candidates NOTHING is adopted —
one may be another LIVE session in this directory — they are listed newest first and you pick with
`adopt <id>` (exit 2 until you do). A lone word spelled adopt — or any of the read words
list/ls/show/status/current/get/name — in any capitalization is a verb, never a session name
(issue-037: `tt session list` used to silently rename the session); list/ls print the roster, the
other read words print the display name. Setting a name announces `session: renamed <old> -> <new>`
on stderr; stdout stays the display name, byte-stable. There is no auto-adopt. No orphan: says so and exits 2. Chips merge as a union with the orphan's first (chips declared after the re-mint survive);
the orphan's earlier `started` stamp wins (adoption claims continuity); the name yields to one already set
on the new key. When an empty-state read (`tt session` or `tt mode`) finds recent (<48h) orphaned state
for this directory, ONE hint line goes to stderr pointing at `tt session adopt`; stdout stays exactly as
before, so nothing that parses it can break.

### log — build/run-log analyzer (PURE)
```
log [summary|errors|warnings] <file>    # summary (default) = counts + lines + verdict
   [--error <regex>]...                 # add an error pattern   (repeatable)
   [--warn  <regex>]...                 # add a warning pattern  (repeatable)
   [--no-defaults]                      # use ONLY supplied patterns (skip curated markers)
   [--require-markers]                  # exit 1 when NO marker of any kind is recognised
   [--cap <n>]                          # max lines shown per bucket (default 50)
```
**Sane defaults, customizable.** Curated markers span the logs agents actually read — compiler/build
(`error:`, `error[E…]`, `[error]`), test runners / CI (`FAIL`, `##[error]`), runtime leveled logs
(`ERROR`/`FATAL`/`CRITICAL`, logfmt `level=error`, JSON `"level":"error"`), Python `Traceback`, Go
`panic:`, `npm ERR!`, and LaTeX (`^! `, Over/Underfull). All **targeted** so tally lines like "0 errors" /
"no warnings" don't false-positive. Two problem buckets: errors and warnings (test failures fold into
errors); the summary ALSO counts curated **success markers** (`=== success markers: N` — sbt `[success]`,
compiling/compiled, tests-passed lines, `BUILD SUCCESS`, targeted like the rest), so the zero-hit verdicts
can tell a genuinely clean run (`0 errors, 0 warnings, N success markers`) from a file with no log markers
at all (`… but no log markers recognised in N lines (is this a log?)`) or EMPTY input (0 bytes, called out
as not a clean run). `--require-markers` turns that into a gate: exit 1 when no marker of any kind (error,
warning, success) is found — empty input included — so an unattended run can't take a truncated, empty, or
non-log input as clean. A directory argument gets a pointer to `tt gitinfo` (the name invites a git
misreach; `tt log` analyzes build/run LOG FILES).
When the agent knows a log's own markers, it extends (or with `--no-defaults`, replaces) the set. Each
pattern compiles separately, so an inline `(?i)` in one can't leak into the others. Reads Latin-1 (some
logs, e.g. LaTeX, aren't valid UTF-8).
Examples:
```
tt log build.log                                  # curated defaults (the 90% case)
tt log errors run.log --cap 200                   # just errors, show more
tt log app.log --error 'MYAPP-FATAL'              # defaults + my app's marker
tt log weird.log --no-defaults --error 'BOOM:'    # only my pattern
tt log build.log --require-markers                # gate: exit 1 if nothing was recognised
```

### newtool — generator (scaffold a new pure tool)
```
newtool <name>                       # creates tools/<name>.scala from template.scala.txt
```
`<name>` must be an identifier (`[a-zA-Z][a-zA-Z0-9]*`); it becomes both the file and the CLI verb.
Refuses to overwrite an existing tool. ⚠ The `tools/` path is cwd-relative — run it FROM the
genscalator root. (Explicit form: `scala-cli run tools/newtool.scala -- <name>`.)

### verify — run-and-verify driver (EFFECTFUL)
```
verify [checks] -- <cmd> <args...>      # run <cmd> (NO shell), check exit/stdout/stderr, print PASS/FAIL
   --exit N        expected exit code (default 0)
   --out  <substr> / --out-re <regex>   stdout must contain / match
   --err  <substr> / --err-re <regex>   stderr must contain / match
   --tee           echo the child's output LIVE (still captured for checks; verdict prints last)
   --timeout N     kill the child and FAIL after N seconds (default: no limit)
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
retired: each verb is per-verb allowlistable (`Bash(tt scala test *)`) while bare `scala-cli` stays
off the allowlist. It does **not** make running code safe — tests and `@main` run real code (SECURITY-MODEL,
"When the tool's job is to run code"); it removes the *surplus* a broad allow grants. `--prop k=v` becomes
`--java-prop` (e.g. `--prop tt.tools=<abs-tools>` for the toolbox suite). Prints an audit line (argv, exit,
ms) and passes scala-cli's exit code through.
Examples:
```
tt scala test /abs/tools --prop tt.tools=/abs/tools
tt scala package-js /abs/my-spa -o /abs/my-spa/main.js
```

### sbt — run sbt in an explicit directory, no shell cd (EFFECTFUL)
```
sbt --dir <abs-dir> [sbt-args...]    # e.g. tt sbt --dir /home/me/proj --client compile
```
The dir-scoped sibling of `tt scala`: the working directory is a typed argument set via
`ProcessBuilder.directory()`, never a shell `cd`, so no compound `cd X && sbt …` shape is needed and
shell metacharacters in arguments are inert. `--dir` must come FIRST and be an ABSOLUTE path to an
existing sbt build (holds `build.sbt` or `project/`); everything after it passes through untouched.
Running sbt runs the project's own build code — keep this shown-gated, not allowlisted.

### guardcheck — flag guard-trip / banned-reflex patterns (PURE)
```
guardcheck cmd <shell-command>       # flag &&, ;, $(, backtick, |head, raw grep -r, …
guardcheck msg <commit-message>      # flag patterns that trip the commit guard (line-leading #, a #N turn index)
guardcheck hook                      # PreToolUse hook surface: stdin JSON in, verdict out
guardcheck posthook                  # PostToolUse twin (wiring: human-gated settings step)
```
A prosthetic for the confirmation-guard feedback the agent can't see. Besides the HIGH/MED syntax
tiers, `cmd` has a **NOTE tier** of tool-choice nudges: reaching for an interpreter (`python3`, `jq`,
`perl`, …), a bulk env read (`printenv`, bare `set`), or a raw command a `tt` verb covers gets a
[NOTE] pointing at the typed alternative. Exit 0 clean / 1 flagged / 2 usage.

### typo — keyboard-aware typo classifier (PURE)
```
typo adjacent <a> <b>                # are keys a,b adjacent on the Swedish QWERTY layout?
typo classify <typed> <intended>     # match / adjacency / transposition / deletion / insertion / substitution-far / complex
```
Feeds the human-fatigue / mutual-degradation gauge (BR's idea): the typo *kind* hints at tiredness.

### htmltext — strip a saved HTML page to readable text (PURE; writes a file with `out.file`)
```
htmltext <in.html> [out.file]        # drop head/script/style/svg/noscript, block tags → newlines, decode entities
htmltext <in.html> --cap <n>         # print at most n lines to stdout (default: uncapped)
```
Turns a Firefox "Save Page As" dump (e.g. journal guidelines) into plain text without the JS/CSS bloat.
When `--cap` truncates, a non-silent notice reports the true total (`=== truncated: showing N of M lines`);
write-to-file mode is always uncapped — `--cap` applies to stdout only.

### chrono — stopwatch for timing work spans (EFFECTFUL: state + log)
```
chrono start [label] | stop [--think <dur>] | now | fmt <ms> | think <dur> | report
```
Times a human-agent-human round (or any span); `stop --think 30s` also records the relayed think-time and prints
the `round = think + human` split; spans append to a `chrono-log.tsv` (default under `~/.genscalator/`, created
on first use; `-Dtt.chrono.log` overrides the path); `report` summarizes. (The agent can't
perceive its own think-time — this plus a human relay reconstruct a full round.)

### hangover — detect a just-ended agent blackout by the resume-gap (PURE read; the clock supplies `now`)
```
hangover <transcript.jsonl> [--now-ms N] [--threshold-sec N]   # threshold default 900 (15 min)
hangover hook [<json>]               # Claude Code SessionStart hook: stdin JSON -> a hangover line named by `source`
```
On resume, compares NOW to the last conversational record's timestamp and flags a gap that dwarfs execution
time: the "hangover" of a blackout the agent cannot perceive from inside (guard stall / long idle / compact /
box crash). Detects THAT you were out, not the cause; a `compact_boundary` among the recent records names it a
compact.

`hook` is the hangover-detector surface (BR's decision): wired as a **SessionStart** hook it fires on all four boundaries
and gets a `source` (`startup`/`resume`/`clear`/`compact`) that NAMES the seam a bare gap cannot tell apart.
Silent unless there is a hangover (its output is injected into context on every session start), fail-soft and
always exit 0 (a session start must never break on this). Wiring: `docs/hangover-hook.md` (human-gated).
Still uncovered: a mid-session stall or idle, which fires no SessionStart.

### parsereqt — parse reqT model text (PURE)
```
parsereqt parse <file>               # parse reqT model text into a structured form
parsereqt lint  <file>               # structural lint of a reqT model (SKIPS fenced code blocks)
```
(A bare `parsereqt <file>` without a verb is a usage error, exit 2.)

The lint flags bullets that silently fell through to a `Text` attribute — a mistyped concept (`Feautre:`),
an un-mapped term (`BadGoal:`), or a relation keyword written under a `has` block, where the relation is
LOST. It **skips fenced code blocks** (issue 010): a grammar illustration such as `ENT: id` is metasyntax,
not a mistake, and a check that reports the same 5 hits forever teaches its reader to ignore the number.
The skipped count is printed, never swallowed. The skip is lint-only — the vendored parser's handling of
fences is untouched, so fenced bullets still appear in the parsed model.

### svg — textual diagram spec → self-contained SVG (PURE; writes a file with `out`)
```
svg sequence <in.txt> [out.svg] [--light|--dark] [--transparent]   # spec → SVG (no out → stdout)
svg --sequence-diagram <in.txt> [out.svg]                          # aliases: `seq`, `-s`; flags also accept
                                                                   # --light-mode/--dark-mode/--transparent-bg
```
Input is a tiny PlantUML/mermaid-flavoured spec (`title:`, `actor <Id> [as label]` — `participant` is
accepted as a synonym —, `A -> B: call`,
`A --> B: reply`, `note over A,B: text`; `#`/`//` comments; self-message `A -> A` draws a loop). Output is a
**self-contained** SVG (inline `<style>`, no external refs) — inline it straight into an SSG page, an artifact, or a
report. **Theme:** default **auto** adapts to the viewer via `prefers-color-scheme`; **`--light`** / **`--dark`**
emit a *fixed, tailored* palette (predictable when the host page/PDF theme may differ from the OS setting — generate
the variant you need). **Background:** default is **opaque** and theme-coloured (transparent SVG backgrounds often
render badly in Markdown/GitHub); **`--transparent`** (aka `--no-bg`) drops it. Deliberately **not** reqT-lang: reqT
is conceptually a *bag* — element order isn't semantic (though reqT-lang preserves source order) and there's no
message concept, whereas a sequence's order *is* its meaning (see [`../research/037-svg-sequence-diagram-tool.md`](../research/topics/RT037-svg-sequence-diagram-tool.md)).
Example:
```
tt svg sequence blog/figures/seq-compact-dance.txt blog/figures/seq-compact-dance.svg
tt svg sequence flow.txt flow-dark.svg --dark
```

### ascii — same spec → good-looking monospace/box-drawing diagram (PURE)
```
ascii sequence <in.txt> [out.txt] [--pure]   # render a sequence-diagram spec to monospace art (no out → stdout)
ascii --sequence-diagram <in.txt> [out.txt]  # aliases for `sequence`: also `seq`, `-s`
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
gvdot sequence <in.txt> [out.pdf|.png|.svg|.ps]   # render via graphviz `dot` (no out → prints the generated DOT source)
gvdot --sequence-diagram <in.txt> [out.…]     # aliases: `seq`, `-s`; output format inferred from the out extension (default pdf)
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
web get <url> --trace                                    # HEAD-only redirect-chain trace (hop-capped, allowlist-stopping)
```
Replaces the dual-use `curl` reflex. It can **only fetch-and-print**: GET only (no POST/PUT/upload), **no
credential/cookie headers ever**, response **size-capped** (default 5 MB), optional **`--host` allowlist**.
So `Bash(tt web get *)` is safe to blanket-allow where a bare `curl *` allowlist would expose exfiltration
(`curl -d @secret`), RCE (`curl … | sh`), and credential leaks. Residual risk is only SSRF-*read* of internal
hosts — lock down with `--host`. Example: `tt web get https://codeberg.org/api/v1/repos/o/r/tags --status`.

### serv — local static-file preview server (EFFECTFUL: network, but LOOPBACK-only, GET/HEAD-only, read-only)
```
serv <dir> [--port N]      # serve <dir> at http://127.0.0.1:N/  (default N=8000; Ctrl-C to stop)
```
The audited replacement for `python3 -m http.server` when previewing a generated site (e.g. `tt ssg` output)
before deploy. Zero external deps (JDK `com.sun.net.httpserver`). **Always binds 127.0.0.1** — loopback only,
never `0.0.0.0`, so nothing is exposed off the box. GET/HEAD only; a directory serves its `index.html`; a
**path-traversal guard** keeps every served path under `<dir>` (`..`, encoded `..`, and leading-`/` cannot
escape → 403). Example: `tt serv site --port 8137` then open the printed URL. *(`--localhost` is accepted and
ignored; the bind is always loopback.)*

### ssg — hand-rolled markdown -> static HTML site generator (EFFECTFUL: writes into the out-dir, and `--status-update` rewrites source .md)
```
ssg <src> <out-dir> [--template <file>]                     # legacy form: <src> = a .md file or a dir
ssg --out <dir> <file.md>...                                # set mode: render exactly these files
ssg --status <s[,s]> --out <dir> <blog-dir>                 # render only posts whose status matches
ssg --status-update <from>:<to> [--date <d>] <dir|files>    # REWRITES the posts' status line in place
```
Renders the GitHub-flavored-markdown subset we use to self-contained HTML, consuming the SAME `MdParse.parse`
front-end that `md-fmt` reflows through (one parser, two renderers). Handles headings, paragraphs, blockquotes,
bold/italic (incl. `*italic*` inside `**bold**` and intraword-underscore safety), inline `code`, `[links](url)`,
`<autolinks>`, `![images]`, fenced code, GFM tables, bullet/ordered lists, **footnotes** (refs + a bottom
section), and **Scala syntax highlighting** in fenced `scala` blocks. Template resolution: `--template F`, else
`<srcdir>/_template.html`, else a minimal builtin; slots are `{{TITLE}}` (first h1), `{{TOC}}` and
`{{CONTENT}}`. ⚠ **The out-dir is MANAGED, not merely written to:** only figures actually referenced by the
rendered pages are copied, any UNREFERENCED file under `<out>/figures` is DELETED, and the set modes also
delete stale `.html` pages — so keep nothing hand-made in the out-dir. `--status-update` is the one verb that
touches SOURCES: it rewrites each post's status line (optionally stamping `--date`). Preview with `tt serv`.
Still deferred: nested lists (rendered flat) and reference links.
Example: `tt ssg blog/002-....md tmp/site` then `tt serv tmp/site` and open the URL.

### forge — Forgejo/Gitea forge client, default Codeberg (EFFECTFUL: network; create needs env token)
```
forge whoami   [--gh | --gl | --url BASE]                 # verify auth: print the token's login (never the token)
forge releases <owner>/<repo> [--url BASE] [--limit N]    # list releases  (READ, no auth → allowlistable)
forge tags     <owner>/<repo> [--url BASE] [--limit N]    # list tags      (READ, no auth → allowlistable)
forge issues <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]   # list issues (READ)
forge prs    <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]   # list PRs    (READ)
forge contributors <owner>/<repo> [--gh | --gl | --url BASE] [--limit N]   # list contributors (READ; --gh/--gl only)
forge issue  <owner>/<repo> <n> [--gh | --url BASE]        # show an issue + comments   (READ)
forge pr     <owner>/<repo> <n> [--gh | --url BASE]        # show a PR: merge state + body (READ)
forge pr-commits <owner>/<repo> <n> [--gh | --url BASE] [--limit N]   # list a PR's commits + credit-trailer check (READ)
forge pr-merge <owner>/<repo> <n> [--gh | --url BASE] [--method merge|squash|rebase]
               [--subject S] [--body-file F] [--yes]       # MERGE a PR (EFFECTFUL): previews by default, applies with --yes
forge protection <owner>/<repo> <branch> [--gh | --url BASE]   # show the protection rule (needs token)
forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
                     [--prerelease] [--draft] [--target COMMITISH] [--gh | --gl --url BASE]   # CREATE (effectful)
forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
                     [--prerelease] [--draft] [--gh | --url BASE] # PATCH a release (drafts found too); only provided fields
forge release-download <owner>/<repo> <tag> [--gh | --url BASE] [--pattern GLOB] [--dir D] [--verify]
                                                                  # download assets (finds DRAFTS too; --verify = sha256)
forge release-upload <owner>/<repo> <tag> <file> [--name N] [--clobber] [--gh | --url BASE]
                                                                  # attach ONE file (drafts too); refuses duplicate names
                                                                  # unless --clobber REPLACES the bytes under that name
forge release-delete <owner>/<repo> <tag> [--gh | --url BASE] [--yes] [--allow-published]
                                                                  # DESTRUCTIVE: previews by default, applies with --yes;
                                                                  # a PUBLISHED release also needs --allow-published
forge asset-rm <owner>/<repo> <tag> <asset> [--gh | --url BASE] [--yes] [--allow-published]
                                                                  # DESTRUCTIVE: remove ONE asset; same preview contract
                                                                  # as release-delete; the release itself is untouched
forge file <owner>/<repo> <path> [--ref R] [--out F] [--max-bytes N] [--gh | --gl | --url BASE]
                                                                  # READ ONE repo file — the remote sibling of
                                                                  # `tt git show`; CARRIES A TOKEN, so it reaches a
                                                                  # private repo without a clone (5 MB cap by default)
```
Replaces hand-curling the REST API (a `curl` with a token on the command line). **READ verbs need no auth**
(public repos) → safe to allowlist (`Bash(tt forge releases *)`, `Bash(tt forge tags *)`). The **effectful
verbs** — `release-create`, `release-edit`, `release-upload`, `release-download` (writes files) and `release-delete` (the one
DESTRUCTIVE verb: preview-by-default, `--yes` to apply, `--allow-published` additionally required for a
published release; the git tag is never deleted) — read their token **only** from fixed human-set env vars
(**`GENSCALATOR_CODEBERG_TOKEN`**, then `CODEBERG_TOKEN`, then `FORGE_TOKEN`; GitHub: `GENSCALATOR_GITHUB_TOKEN`/
`GITHUB_TOKEN`/`GH_TOKEN`; GitLab: `GENSCALATOR_GITLAB_TOKEN`/`GITLAB_TOKEN`) — never a flag — so the agent
can't self-authorize (same trust-boundary rule as `verify`'s `TT_VERIFY_ALLOW`). Each prints an `[audit]` line
and none is blanket-allowlistable. **Dialects:** `--gh` targets the GitHub REST API rooted at the fixed
`api.github.com` — never derived from `--url`, so a token cannot be redirected; `--gl` targets GitLab
(`--url BASE` for self-managed, trusted-host-guarded via `TT_FORGE_GITLAB_HOSTS`/`TT_FORGE_HOSTS`).
⚠ For the WRITE verbs the dialect is set ONLY by `--gh`/`--gl` — a github.com `--url` switches path shapes for
READ verbs but is REFUSED by `release-create` (the trusted-host check), so use the flag, not the URL. Reads
work anonymously on GitHub (60/h rate limit); `protection` requires the token (admin read).
**`contributors`** reads the repo's contributor list — `--gh` prints `login⇥contributions⇥type` (type = `User`/`Bot`,
the field that answers "why is a bot on the list"), `--gl` prints `name⇥email⇥commits`; the Gitea/Forgejo REST API
has no contributors endpoint (Codeberg 404s), so the default dialect says so plainly rather than erroring cryptically.
**`pr-commits`** lists a PR's commits (short sha⇥date⇥author⇥headline) and surfaces `Co-Authored-By:`/"Generated
with" lines with a one-line verdict — the CONTRIBUTING.md no-assistant-credit pre-merge check in one allowlisted
call. **`pr-merge`** merges a PR: previews by default (release-delete's shape) and applies only with `--yes`; the
default subject `Merge PR #<n>: <title>` satisfies CONTRIBUTING.md's name-the-PR rule, the body comes from
`--body-file` only, an unmergeable/draft/closed PR is refused by name, and the source branch is never deleted.
`whoami` now takes `--gh`/`--gl` too, so the verb whose job is "check my auth" can check the token the other verbs use.
Example:
```
tt forge releases bjornregnell/genscalator --limit 5
tt forge prs lunduniversity/introprog --gh                 # open PRs on a GitHub repo
tt forge issue lunduniversity/introprog 951 --gh           # one issue with its comment thread
tt forge contributors lunduniversity/introprog --gh        # who GitHub credits (login/contributions/type)
tt forge release-create bjornregnell/genscalator v0.8.0 --name "v0.8.0: …" --body-file NOTES.md --prerelease
```

### git — safe git helper: commit-from-file, ff-pull, fetch, read-only show (EFFECTFUL, non-destructive)
```
git commit --repo <dir> --message-file <path> [--add <pathspec>]... [--push] [--remote <name>]... [--tags]
git push   --repo <dir> [--remote <name>]... [--tags]    # push committed work, no new commit
git pull   --repo <dir>                                  # fast-forward ONLY: FFs or fails loudly
git fetch  --repo <dir> [--remote <name>]...             # remote-tracking refs only, never the working tree;
                                                         # names the remote it fetched, reports upstream standing
                                                         # instead of a bare "up to date", lists unfetched remotes
git show   --repo <dir> --ref <ref> --path <relpath> [--out <file>]   # READ-ONLY: file content at a ref
git log    --repo <dir> [--grep P] [--co-author P] [--author P] [--committer P] [--since D] [--limit N] [--path <relpath>]...  # READ-ONLY search
git diff   --repo <dir> [--ref <ref>] [--ref2 <ref>] [--staged] [--stat] [--path <relpath>]... [--limit N]  # READ-ONLY, capped
git rm     --repo <dir> --path <relpath>...              # TRACKED files only, staged not committed
```
**`diff`** is READ-ONLY and CAPPED (`--limit`, default 200) with truncation always announced, so it never
wants a `| head` — the pipe is what trips the guard, and a tool that needs one cannot be allowlisted. It
closes the most-cited gap in this toolbox: with no typed diff, "what did that commit change" forced either
raw `git -C` or a `tt git show --out` plus an external `diff`, both outside what the guard can inspect.
Modes: no ref = all uncommitted work (`diff HEAD`); `--staged` = index only; `--ref A` = what commit A
itself changed (via `show`, so a root commit works where `A~1 A` would fail); `--ref A --ref2 B` = between
two refs. `--path` (repeatable) narrows, `--stat` gives the summary alone. `--staged` with `--ref` is
rejected rather than silently ignored.
Exposes only verbs that cannot lose data — no reset/rebase/merge/clean/`--force`. **`rm` is the one
destructive verb, and it is safe by CONSTRUCTION rather than by care**: it removes only files git already
has a committed copy of, so every removal is recoverable with a checkout. It refuses untracked files
(nothing to restore them from), directories, globs, absolute paths and `..`; it validates every path
before removing any, so a bad third path cannot leave the first two gone; and it stages rather than
commits, composing with `tt git commit --add`. ⚠ **It closes a real gap — retiring a generated file whose
owner moved to another repo had no typed shape at all, and a missing verb is exactly what makes an agent
reach for raw `rm`, which the guard cannot inspect — but it does mean `Bash(tt git *)` is no longer
purely non-destructive. Pair a blanket `tt` allow with an `ask` rule for `Bash(tt git rm *)`.**
`commit` reads its message from a FILE, so prose with shell metacharacters
(backticks, `$`, `!`, braces, bare `*`) never touches the command line (kills the recurring commit-message
allowlist tripwire); `--add` stages only the listed paths (never an implicit add-all). **`show`** extracts a
file's content at any commit-ish (HEAD, branch, tag, SHA) **byte-exact** to stdout or, with `--out`, to a file
— the allowlist-clean replacement for redirecting raw `git show ref:path` output (the redirect plus git's
general surface blocked allowlisting, e.g. when a PR-review sub-agent needs a file at the base ref). On a bad
ref or path it exits non-zero with git's error — never a partial/empty success. **`log`** is a READ-ONLY
commit-log search: it caps (`--limit`, default 50) and tab-formats the output (`<short-sha>⇥<author-email>⇥<subject>`
plus a `=== N commit(s)` line that flags when the cap was hit), so it needs no `| head` and `Bash(tt git log *)`
stays allowlist-safe — `--co-author P` greps the `Co-Authored-By:` trailer forges attribute contributors from,
and `--path <relpath>` (repeatable, issue 038) keeps only commits that TOUCHED the given repo-root-relative
path(s), passed to git after a `--` separator so a path can never be mistaken for a ref.
**`--remote <name>`** (repeatable, with `--push`) sends the unit to a MIRROR SET in one call instead of
one raw `git push <remote>` per extra remote — genscalator pushes github + gitlab + coursegit every unit, and
that gap was forcing the raw-git reflex this tool exists to retire. It fails on the first remote that
rejects, so a half-pushed set is reported rather than swallowed, and **`push`** is the same thing standalone,
for syncing a mirror without making a commit. A branch with no upstream in a single-remote repo is refused by
git itself (`push.default simple`) — set it once with `git push -u`; the tool never sets one behind your back.
**`--tags`** (with `--push`) sends tags too, as a second push per remote, closing the loop with
`tt forge release-create`: that verb needs a tag that already exists on the remote, so the toolbox could
create a release against a tag it had no way to push. It is **`--tags`, not `--follow-tags`** — the tempting
choice sends only ANNOTATED tags, and this project's own are mixed (`v0.8.0`/`v0.9.0`/`v0.9.1` lightweight,
`v0.9.2` annotated), so it would publish some releases and silently skip others depending on how each was
tagged. Still never `--force`: without it git REFUSES to move an existing remote tag, so a tag push can only
ADD refs, which is what keeps it inside the safe subset. Off unless asked — an ordinary push never publishes
a tag as a side effect. Creating tags (`git tag -a`) stays out of scope: this sends tags you already made.
Examples:
```
tt git commit --repo /abs/repo --message-file tmp/msg.txt --add src/app.scala --push
tt git commit --repo /abs/repo --message-file tmp/msg.txt --add tools --push \
  --remote origin --remote gitlab --remote coursegit          # one unit, three mirrors
tt git push --repo /abs/repo --remote gitlab --remote coursegit   # sync mirrors, no new commit
tt git push --repo /abs/repo --remote origin --remote codeberg --tags   # branch + tags, both mirrors
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

update --native [--home <dir>] [--repo <owner/repo>] [--tag <tag>] [--write]
   --home <dir>        the INSTALLED tree to replace     (default: GENSCALATOR_HOME, then ~/.genscalator)
   --repo <owner/repo> where releases are published      (default: bjornregnell/genscalator)
   --tag <tag>         install a specific tag            (default: the latest published release)
   --write             APPLY. Without it this previews and touches nothing.
```
genscalator's **own update-awareness**, because the platform gives none: a third-party Claude Code marketplace does
**not** auto-update, there is no per-plugin update command, and plugin authors get no update-check API. But
genscalator *is* a git checkout, so git is the mechanism — `update` **fetches remote-tracking refs (read-only, never
the working tree)**, compares your installed version against the remote, and, if you are behind, prints the incoming
commits plus the steps **you** run (`/plugin marketplace update bjornregnell` (the MARKETPLACE name from marketplace.json, not the plugin name) then `/reload-plugins` — the harness
commands a tool cannot drive). **In this git-checkout mode it changes nothing itself; the human is the actuator.**
Exits 0 in all normal cases and degrades gracefully when offline, when the branch has no upstream, or when
genscalator is not a git checkout. `--throttle` is what **`gs warm`** calls (`tt update --brief --throttle 24`), so
warm gains update-awareness without ever hanging or nagging.

**`--native` is the other mode, and it DOES act** — it replaces an *installed binary* tree with the latest published
release. (The sentence above used to read "it changes nothing itself" without qualification; `--native` made that
claim false, which is the SM248 class — a doc asserting something about code that nobody re-checked.) It downloads
the asset for **this** platform, verifies its published `sha256`, validates every CRC32 in the archive, unpacks to a
staging dir *beside* the install, and only then swaps. **PREVIEWS by default like `tt sub` and `tt zip extract`;
`--write` applies.**

The swap is **two renames, never a write-through**: overwriting a running executable can corrupt the live process on
POSIX and is refused outright on Windows, while *renaming* one is permitted on both — so the install is moved aside
and the staged tree moved in. One code path, no platform branch (D7b, verified on Windows CI). If the second rename
fails the first is undone, because that is the one failure that would otherwise leave you with no toolbox at all.

It **refuses rather than guesses** in five places, each of which would otherwise brick an install: a platform with
no published binary (Intel macOS and Windows-on-ARM — build from source, the documented route); a `--home` that is a
**git checkout** rather than a binary install (deliberately *not* the repo self-locate the other verbs use, which can
resolve to a contributor's clone); an archive whose payload had no published `.sha256` to check it against; a
release carrying no downloadable assets at all; and an asset that does not unpack to exactly one `.zip` payload.
Examples:
```
tt update                       # full report: installed version, ahead/behind, and what to do
tt update --brief               # speak only if a newer release is available
tt update --brief --throttle 24 # gs warm's call: check at most once a day, silent unless behind
tt update --native              # PREVIEW: what would be installed, and the swap that would happen
tt update --native --write      # apply it
```

### statusline — format the Claude Code statusLine stdin JSON into ONE compact line (read-mostly: reads stdin + state files, prints)
Reads the JSON Claude Code pipes to the configured `statusLine` command each turn and prints one compact,
colour-coded line — model, context-fill (the rot gauge), usage limits, cost — with optional `--mode-line`
(line 2: `gs session:` + this session's name inverted, `gs mode:` + the declared chips — session-scoped
per SM208, keyed on the stdin JSON's `session_id`; `--sessions-root` overrides the store for tests) and
`--box-line` (line 3, measured box health). It also reads the transcript
and the `tt mode`/`tt limit` state files each render, and has ONE opt-in write: iff the marker file
`~/.claude/gs-statusline-dump-on` exists, the raw stdin JSON is teed to `~/.claude/gs-statusline-last.json`
(the recall-free way to confirm fields against a real invocation). Thresholds and segments are tunable —
`--warn`, `--ctx-warn`, `--dumb-zone`, `--auto-compact`, `--tok-warn`, `--tok-danger`, `--tired-chars`,
`--no-tok`, `--rot-only` — see `tt statusline --help`. Full legend: `docs/statusline-manual.md`.

### box — safe host + local box ops: health, and host-pinned remote ops for a known compute box (EFFECTFUL)
```
box health        [--top N] [--wide]             # local top-N by RSS with CPU%, mem, load (default N=10)
box kill <t>      [--yes]                        # t = bloop | sbt | scala-cli; SIGKILL matched dev servers;
                                                 # DRY-RUN without --yes
box models        [--host H]                     # ollama inventory (name/size/modified)
box df | gpu | freegb [--host H]                 # disk usage / nvidia-smi / free-GB integer on the box
box pull <model>  [--host H] [--min-free-gb N]   # ollama pull; REFUSED below the free-disk floor (default 50)
```
Replaces the dual-use `ssh *` / `ps` / `pkill` reflexes with a FIXED verb enum, no shell passthrough, a
pinned default host (`bjornyx.local`; host and model names strictly validated, so caller input cannot inject
remote shell). Local ops read `/proc` directly (no `ps`) and `kill` matches only a closed enum of dev servers
by PID via `ProcessHandle` (no pkill patterns). ssh runs BatchMode (never hangs on a password prompt); quick
ops time out after 60 s, a pull after 1 h. ⚠ **With `kill` aboard, `Bash(tt box *)` is NOT blanket-safe** —
kill stays human-gated; allowlist the granular read-only verbs instead (`Bash(tt box health *)`, …).

### gitinfo — typed, READ-ONLY git status/overview (PURE, read-only)
Branch, clean/dirty count, ahead/behind vs upstream, and the recent log in ONE call; `--remote <name>` also
checks whether local HEAD is in sync with that remote's HEAD (via `ls-remote`). Retires raw
`git -C … status/log/ls-remote`; only read-only git subcommands, never add/commit/checkout/fetch.

`--files` adds the paths behind the count, one per line, each labelled `staged` / `unstaged` / `both` /
`untracked` / `conflict` with its raw porcelain code. Reach for it instead of a bare `git status --short`:
`tt git commit --add` needs exact paths, and guessing them is how a human's in-progress file ends up
committed under an agent's message (the 2026-07-27 near-miss that motivated issue 004). A rename reports
its DESTINATION path — the one you would `--add`.

### prd — read + navigate the genscalator PRD.md (PURE, read-only)
See what the PRD says without re-emitting it token-by-token: `tt prd show` (whole file), `tt prd summarize`
(a FUTURE-roadmap gist), `tt prd find <what>` (locate a term by its nearest heading); `--prd <file>`
overrides the PRD path. Complements `tt parsereqt` (which parses + lints the reqT-lang).

### harden — Layer-1 deterministic secret scanner (PURE, read-only)
Surfaces CANDIDATE secrets for semantic (Layer-2) triage. `tt harden repo <dir>` scans git-TRACKED text files
(respects `.gitignore`); `tt harden egress <dir>` scans ALL files under a dir destined to LEAVE (a ZIP-staging
or deploy bundle) — the higher-value half, since a secret safe at rest can leak on egress. `--entropy <bits>`
tunes the high-entropy detector (default 3.6). Findings are printed REDACTED. Exit: 0 clean, 1 candidates
found, 2 usage/error.

### skillcheck — verify the genscalator skill set is active; catch the silent skill outage (PURE, read-only)
The agent CANNOT feel a missing skill (no phenomenology of absence), so this prints the EXPECTED set (derived
from the `skills/*/SKILL.md` dirs, so it never drifts) to diff against the live `/skills` list; feed the active
names via `--active` for a machine-checked, exit-coded diff.

### skillgrants — print what a skill GRANTS: its allowed-tools frontmatter, for informed consent (PURE, read-only)
When the harness loads a skill it silently widens the auto-approved tool set by that skill's `allowed-tools`,
but never shows the human WHICH tools at grant time. This is that read: name a skill (or list all) and see
exactly which tools it opens. Both `skillcheck` and `skillgrants` take `--skills <dir>` to override the
skills dir.

### memory — keep the committed memory/ snapshot in step with the live Claude Code memory store (EFFECTFUL: `sync` copies into the repo)
```
memory where [--repo <dir>]          # print the DERIVED live-store path and stop
memory check [--repo <dir>]          # read-only drift report; exit 1 if drifted
memory sync  [--repo <dir>]          # copy live -> <repo>/memory (additive)
memory ...   --force                 # proceed past the collapse guard
```
The live store is OUTSIDE the repo, in a `~/.claude/projects/` directory named after the project path;
this tool DERIVES that path from `--repo` (default: cwd) instead of hardcoding it, because a hardcoded
path goes stale silently when the project moves — it copies nothing and still exits 0. Four guards each
FAIL LOUDLY rather than doing nothing quietly: `<repo>/memory` must exist; the derived live dir must
exist; the live dir must hold `MEMORY.md`; and the live file count must not have collapsed against the
snapshot (the 148-vs-14 scream) — `--force` overrides deliberately.

### bloop — targeted BloopServer control: status + restart + clean (EFFECTFUL)
```
bloop status | restart               # what is bloop doing / targeted kill + lazy respawn
bloop clean --dir <abs> [--yes]      # recursively DELETE .scala-build dirs under <abs>; DRY-RUN without --yes
```
Bloop is a disposable compile daemon that respawns lazily, so "restart" is a targeted kill + lazy respawn. It
uses `kill -9` deliberately: when bloop is wedged (the empirical villain) polite protocols hang and a
signal is the reliable cure. Its RSS also surfaces on the statusline box line so regrowth is visible early.
⚠ `clean` is the destructive verb of the trio (build caches only, but recursive) — dry-run by default.

### wr — Workflow-Research utilities for the WR corpus itself (PURE, read-only)
`tt wr stamp <project-dir> <regex> [--user|--human] [--limit N]` retrofits the REAL date-time of an utterance
or event from the session `.jsonl` transcripts — the grounded-timestamp tool behind the WR-data discipline.

### issue — typed verb for the in-repo issue workflow (EFFECTFUL: `close --yes` rewrites + moves ONE file)
```
issue next  [--repo <dir>]                    # the next free NNN across open/ AND closed/ (never reused)
issue list  [--state open|closed|all]         # one line per issue: number, state, labels, summary
issue close <NNN> (--fixed-by <ref> | --as <text>) [--date YYYY-MM-DD] [--yes]
```
Executes `reqts/issues/README.md`'s rules (issue-032): closing rewrites the `> status:` preamble AND
moves the file open/ → closed/ as ONE operation, so directory and preamble cannot disagree — PREVIEW by
default, `--yes` to apply (the `release-delete` pattern). The date comes from the system clock (the
`tt chrono` source) or an explicit `--date`, never guessed. Refuses (exit 1) an unknown number, an
already-closed issue, a number claiming several files, or a preamble already declaring a closed state.
`list` flags a preamble that disagrees with its directory with a ⚠. Local clone only; staging and
committing stay with the caller (stage BOTH paths so git records the rename).

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
One line per file; a tool's real reference is its `###` section above and its `--help`.
**Verbs** (each has a `@main` and a `###` section above):
- `text.scala`, `files.scala`, `find.scala`, `sub.scala`, `md-fmt.scala` — the grep/awk/find/sed family.
- `json.scala`, `tsv.scala`, `zip.scala` — data readers (zip: + the guarded extract).
- `links.scala`, `which.scala`, `env.scala`, `limit.scala`, `doc.scala`, `mode.scala`, `session.scala`, `log.scala` — repo/host/state reads (mode/session write their small state files).
- `verify.scala`, `scala.scala`, `sbt.scala` — the run drivers (effectful; os-lib).
- `guardcheck.scala`, `typo.scala`, `htmltext.scala`, `chrono.scala`, `hangover.scala`, `parsereqt.scala` — small analyzers.
- `svg.scala`, `ascii.scala`, `gvdot.scala` — the sequence-diagram trio (shared spec in `seqspec.scala`).
- `web.scala` — safe read-only HTTP GET (effectful: network; requests).
- `serv.scala` — loopback-only static preview server.
- `ssg.scala` — the markdown → HTML site generator (writes into its out-dir; see its ⚠ managed-out-dir note).
- `forge.scala` — forge client (Gitea/Codeberg + GitHub `--gh` + GitLab `--gl`): reads + env-token release verbs incl. the destructive `release-delete` (effectful; requests+ujson+os-lib).
- `git.scala` — safe git helper: commit-from-file, ff-only pull, fetch, read-only show/log (effectful; os-lib).
- `gitinfo.scala` — read-only git overview.
- `issue.scala` — the in-repo issue workflow (next/list read-only; close is the guarded rewrite+move, preview-by-default).
- `update.scala` — update-awareness (git-checkout mode: read-only apart from git fetch) AND `--native`, which REPLACES an installed binary tree (preview-by-default; effectful; os-lib+requests+ujson).
- `statusline.scala`, `box.scala`, `bloop.scala` — harness/host instruments (box kill + bloop clean are the guarded destructive verbs).
- `prd.scala`, `harden.scala`, `skillcheck.scala`, `skillgrants.scala`, `memory.scala`, `wr.scala` — genscalator-upkeep reads (memory `sync` writes the repo snapshot).
- `newtool.scala` — the generator.
**Mainless helpers** (no `@main`, not `tt` verbs):
- `lib.scala` — shared PURE helpers (`readLatin1`/`readUtf8`, `histogram`, `edit1`, path/platform/glob/json-string utilities). No deps.
- `seqspec.scala` — shared sequence-diagram spec model + parser; reused by `svg`, `ascii` + `gvdot`.
- `boxstats.scala` — shared /proc gatherers (statusline + bloop + box).
- `limitstore.scala`, `sessionstore.scala`, `minijson.scala`, `mdparse.scala` — shared stores/parsers (limit+statusline; mode+session+statusline; json; md-fmt+ssg).
- `secrets.scala` — the one definition of "what is a secret" (redaction + detection; harden + env).
- `releaselib.scala`, `ziplib.scala` — release download/verify + zip machinery shared by `forge` and `update` (the toolbox's most security-sensitive shared code — worth an auditor's read).
- `versionlib.scala` — the `tt --version` line (display normalisation over the four VERSION.txt carrier shapes + carrier-kind discrimination; issue 028). Handlers live in `tt` (bash) and `dispatch.scala`, not in a verb file.
- `dispatch.scala` — the single native dispatcher (its `@main` IS `tt`, not a verb).
- `template.scala.txt` — starter template (version + lib includes, dispatch skeleton).
- `project.scala` — the single source of the Scala version (no code, no `@main`); every tool includes it.

## Conventions
- **Pure tools** (read → compute → print): keep them pure; later default to **Capture-Checking Safe
  mode** so the compiler errors on accidental side effects (PoC pending — see `../reqts/ROADMAP.md`).
- **Effectful drivers** (run sbt/pdflatex, write files): separate files; os-lib `os.proc`; not Safe mode.
- Live **in-project** (this repo, or `<project>/.../scratch/` for one-offs) so paths stay inside the
  trusted tree (avoids the `/tmp` path-resolution-bypass approval). Drivers should root-find (walk up).
- Clean `===` section output; return a clear verdict (e.g. error count) so no bash post-processing is needed.

## Roadmap
(Version planning lives in `../reqts/ROADMAP.md`; this list is toolbox-local ideas only.)
- More generic tools (pdf scan), generalized from real case-study work. (`log` shipped v0.6.0; `verify`
  v0.7.0; `tsv`, `json`, `env`, `zip`, `memory`, `sbt` have shipped since this list was first written.)
- Extend the guarded-run primitive (`verify` already does allowed-executables + no-shell): add allowed-roots
  / `cwd`, reject `..`/symlinks, a `--dry-run` echo, and a general `--audit` flag (verify's audit line —
  argv, exit, ms — is its seed).
- Capture-Checking Safe-mode PoC → pure tools safe by default.
