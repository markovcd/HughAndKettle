# Prose style

## Comments never narrate history

Keep comments short. Explain a thing only where it is not obvious, and then in a sentence or two. Never describe how the code used to be or what a change did: no "now", "no longer", "three rather than six", "unchanged by the refactor", "fixed the bug where".

**Why:** long prose buries the rule it is stating, and history already lives in git and `docs/adr/`. A comment about the past is wrong the moment the next change lands.

**How to apply:** write the comment as if the current shape were the only shape it ever had, and cut it to the shortest form that still carries the non-obvious part. A doc comment explains why a thing is the way it is; what it does is in the code. Prefer one summary line; add more only when a real surprise needs justifying. A comment that is growing into an argument is an ADR. The same rule holds for commit messages.

## One spelling

Write American spellings: "color", "behavior", "analyzer", in code, comments, docs and commit messages. The identifiers set the spelling, and prose that spells differently disagrees with the code it describes.

## No filler

Cut clauses starting "which", "so that", "because" where the sentence stands without them. No "simply", "just", "basically", "note that". A bullet list is for parallel things; a single thought is a sentence.
