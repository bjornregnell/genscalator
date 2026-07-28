# Platform update digest — 2026-07-06 (step-4b box update)

Scrubbed from the raw terminal log for public-repo hygiene. The **full raw log is retained privately** in the closed synch repo (muntabot-synch-introprog); username, full software/snap/flatpak inventory, the sudo prompt line, and expired download URLs were removed here. Study-relevant toolchain + platform state only.

- **Scala toolchain (SDKMAN):** java 25.0.2-tem; scala 3.8.4 (unchanged); **scala-cli 1.14.0 → 1.15.0**; **sbt 2.0.0 → 2.0.1**. → the coding-fidelity arm (§047) will compile/test with **scala-cli 1.15.0**.
- **OS:** Ubuntu 24.04 "noble"; kernel 6.8.0 line; 14 apt security upgrades (python3.12.3-1ubuntu0.15, ruby3.2, socat, tar, gzip, iproute2, and related).
- **ollama:** snap `v0.24.0` present on the local box (note: the study's dumb-model fleet runs on **bjornyx**, a separate machine, not this box).

**Relevance to 047 (step-4b version confound):** the update changed the local Scala toolchain (scala-cli + sbt minor bumps; scala and java unchanged). **No Claude Code CLI or model version appears in this log** (Claude Code is not managed by apt/snap/sdk here) — the study's Claude version baseline (CLI 2.1.201 / claude-opus-4-8[1m]) is tracked separately in `047-fresh-restart-fidelity.md`.
