---
name: changelog
description: Use when adding to or editing CHANGELOG.md - what counts as user-facing, and why a feature gets exactly one terse bullet.
---

# CHANGELOG.md

## User-facing only

Entries cover only changes a user of the product would notice. Leave out comment trimming, README changes, ADRs, tests, refactors and internal references on bullets. When filling out a release section, skip commits that only touch those, and do not write an "Internals" section made of them. A fix to something no release ever shipped gets no bullet.

## One terse bullet per feature

A feature gets **one** bullet, naming what it is and nothing more. No second bullet for its settings, its fallback, its command-line flag or its caveats, and no sentence explaining how or why it works; that belongs in the ADR and the code. The reader wants to see what changed, not learn the feature.

Draft the section, then collapse it: if two bullets are about the same feature, they are one bullet. Cut clauses starting "which", "so that", "because". Keep numbers only where the number *is* the news. A batch of small related changes is one feature, so one bullet.

New entries go under `## Unreleased`, in `### Features` or `### Fixes`.
