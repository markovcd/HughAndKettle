---
name: adrs
description: Use before proposing a refactor, or when writing or editing an architecture decision record under docs/adr/ - checking which refactors the ADRs already declined, telling real drift from a pattern that was never extended, when to rewrite a day-old ADR in place versus add a dated amendment, and how to number a new one at commit time.
---

# ADRs

An ADR is Michael Nygard's format: context, decision, consequences. One file per decision under `docs/adr/`, numbered, titled with a sentence stating the decision ("A plugin is compiled against a contract with a version of its own"), and listed in `docs/adr/README.md`.

## When to write one

When a change is a decision rather than a fix: something a later reader would otherwise undo, or a refactor someone will propose again. A code comment growing into an argument is an ADR. A fix, a rename or an upgrade is not.

## Check them before proposing a refactor

ADRs are not background reading: they pre-emptively decline the refactors a first scan of the code would surface. Before proposing a change of shape, grep `docs/adr/` for the thing you want to change, and read the header comment of the file itself; "these are separate on purpose" is often written there. Proposing a declined refactor reads as not having done the reading.

The refactors worth proposing are:

- where the code has drifted from a decision (an ADR says a class is 400 lines; it is 2700);
- where a pattern the repo established was never carried through. Grep before claiming it: a pattern that looks unfinished may already have been extended elsewhere.

Where an ADR and the code disagree, that is a finding worth raising. Where a guide and an ADR disagree, the ADR is right and the guide is stale.

## Rewrite a day-old ADR in place

An ADR written in the last day or so is the same decision still being settled. Edit its text and let the new wording stand as if it had always read that way: no amendment section, no note of what it used to say, no second ADR superseding it.

Check the date with `git log -1 --format=%ci -- docs/adr/<file>`. Within 24 hours, overwrite the affected paragraphs, including the title and the index row if the decision itself moved. Older than that, add a dated `## Amendment (YYYY-MM-DD)` section, or a new ADR that supersedes it if the decision reversed.

## Number a new ADR at commit time

Several sessions may write ADRs at once, and two can collide on a number. Before committing a new ADR, read the highest number on `main` (`git ls-tree --name-only main docs/adr/`), then fix the file name, its heading, the index row and any links to it.
