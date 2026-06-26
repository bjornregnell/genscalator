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
text grepr <dir> <ext> <regex>       # grep -r --include : recursive search → file:line:match
text cols  <file> <sep> <i...>       # cut/awk   : extract 1-based fields, tab-joined
```
Examples:
```
tt text count build.log '^! '
tt text freq  run.log  '\[fallback\] ([a-z][^,]*)'
tt text grepr src .scala 'TODO'
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

### newtool — generator (scaffold a new pure tool)
```
scala-cli run tools/newtool.scala -- <name>      # creates tools/<name>.scala from template.scala.txt
```

## Files
- `lib.scala` — shared PURE helpers (`readLatin1`/`readUtf8`, `histogram`, `edit1`). No deps.
- `text.scala` — the grep/awk replacement.
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
- More generic tools (log-analyze, run-and-verify driver, tsv stats, pdf scan), generalized from real
  case-study work.
- Guarded `cd`-and-run primitive (all guardrails: allowed-roots, reject `..`/symlinks, dry-run echo,
  scala-cli/scalex only).
- Capture-Checking Safe-mode PoC → pure tools safe by default.
