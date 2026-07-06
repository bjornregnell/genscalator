# WR data — OPEN question: is LaTeX easier than PDF for an agent to read? (2026-07-06, UNANSWERED)

**Question (BR):** is LaTeX source easier than a PDF for an agent to read?

**Context.** Fetching a paper: a **PDF is binary** — `tt web get` prints text (no file-output), so it cannot read a PDF; that needs `curl -o` + Read (the logged **tt-web-file-output gap**), and `curl -o` is not blanket-allowlisted (stalls in AFK). arxiv offers a **LaTeX e-print source** + an auto-generated **HTML** version — both TEXT, fetchable via `tt web get` / `WebFetch`, guard-free. Hypothesis worth testing: LaTeX/HTML source is easier + cleaner for an agent to read than a PDF (no binary extraction, no layout/column mangling, math stays as `\latex` not as positioned glyphs).

**Status: OPEN — do NOT answer now (BR).** Logged for later; candidate WR-data investigation. Relates to the `tt-web-file-output` gap and the surf-for-papers path (prefer arxiv source/HTML over PDF). [[genscalator-toolbox-single-dispatcher]]
