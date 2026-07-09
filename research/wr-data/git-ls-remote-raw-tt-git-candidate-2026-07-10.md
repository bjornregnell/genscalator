# Raw `git ls-remote` for an SSH-reachability check - a `tt git` tool-gap (2026-07-10)

**Event.** To answer BR's "can we reach github / coursegit over git-SSH?", CO4 ran raw `git ls-remote git@<host>:<repo> HEAD` (twice). It worked, but it is a RAW git command - the exact "reach for raw git/grep/awk" reflex the genscalator toolbox exists to replace with typed, allowlisted `tt` tools.

**BR's question: could `tt git` do that?** Yes, and it should - a candidate subcommand. `tt git` already exists (commit / pull / etc.) but lacks a REMOTE-reachability / auth check. A typed `tt git ls-remote <url>` (or `tt git check-remote <url>`) - audited + narrow: read-only ref listing, no side effects - would:
- make the SSH-access check a single allowlisted command instead of raw git;
- fit the raw-shell-tool -> typed-tt-wrapper pattern ([[commit-via-tt-git-not-raw-cd-git]], [[use-tt-grepr-not-raw-grep]]);
- be directly useful to the SM035 mirror workflow: verify each host's git-SSH access before a production `mirror.sc` run, one repo at a time.

**Ties:** SM035 (mirror.sc SSH gate), [[commit-via-tt-git-not-raw-cd-git]], [[use-tt-grepr-not-raw-grep]], the item-D typed-tool pipeline, [[never-allowlist-interpreters]] (a typed tool is allowlistable; raw git is not).
