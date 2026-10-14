# Handoff: open issues

Problems found while working on the codebase that were out of scope for the change at hand, and plans or audits too long for a line in [TODO.md](../../TODO.md). Each file describes one issue: what's wrong, the evidence, the impact, a suggested fix and its status. Pick one up, fix it, and delete its file in the same commit (or mark it **Done** with the commit if the write-up is worth keeping for a while).

Planned work that has its own document is not repeated here; link to it instead.

| # | Issue | Severity | Status |
|---|---|---|---|
| 01 | [Implementation plan](01-implementation-plan.md): from these foundations to an app on a phone driving an amp | — | Open |
| 02 | [Design brief](02-design-brief.md): a logo, an icon and the look of the whole app, for a design session | — | Landed; later screens wait on phases 1 and 2 |
| 03 | [Desktop app](03-desktop-app.md): the same screens on Windows and Linux through Compose Multiplatform | — | Open; step 1 in progress |

Severity: **Critical**, act now; **High**, security or data problem in production; **Medium**, wrong behavior or real risk; **Low**, friction or latent risk.

## Adding an issue

A problem found out of scope is said in the reply first. When the user wants it kept, copy this outline into `NN-short-name.md`, numbered after the highest on `main`, and add a row above:

```markdown
# NN. Title stating what is wrong

- **Severity:** Critical | High | Medium | Low
- **Status:** Open | In progress (branch) | Done (commit)
- **Found:** YYYY-MM-DD, on `main` at `<commit>`

## What is wrong
## Evidence
## Impact
## Suggested fix
```

Say what was confirmed by running it and what only by reading the code. Line numbers are as of the commit named.
