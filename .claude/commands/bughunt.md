---
description: Hunt for bugs, confirm each one as a failing test, then fix it and file the test where it belongs.
---

# Bug hunt

Find bugs that are actually there. A finding is not a finding until something fails — a test that goes red, or a command whose output you can paste. Then fix it, and leave the test behind in the class that owns the behavior.

$ARGUMENTS narrows the hunt to a subsystem when it is given. With nothing, sweep.

## The shape of a run

1. **Start from green.** Build and run the whole suite first. A hunt that begins on a red suite is chasing somebody else's work.
2. **Probe in a scratch file.** One `BugHunt` test file per test project it needs. Everything lands there first, findings and misses alike.
3. **Confirm before fixing.** Every bug gets a red test or a pasteable command before a line of source changes. Reproduce it, then reach for the cause.
4. **Reachability, every time.** A function that throws on bad input is only a bug if a user, a file, a flag or a request can produce that input. Chase it to the entry point. A finding you cannot reach is a note, not a fix.
5. **Fix, then re-approve.** Run the suite. Accept a changed snapshot only after reading its diff and agreeing with it.
6. **File the tests.** Move each one to the class that owns the behavior, name it for the rule rather than the bug, and delete the scratch file. `A_chunk_that_lies_about_its_length_costs_nothing` outlives the hunt; `BugHunt.Probe3` does not.
7. **Changelog.** One terse bullet per fix a user would notice (the `changelog` skill). A fix to something no release shipped gets none.

## Instruments that pay

- **Totality over hostile values.** Feed every numeric entry point 0, ±1, ±ε, ±1e20, ±max, ±∞, NaN, denormals; every string entry point empty, huge, non-ASCII, control characters, unbalanced quotes. Assert nothing throws and nothing hangs.
- **Two implementations of one thing, compared.** An optimized path against the reference, a fast parser against the slow one, a client against the server's schema. Compare bit for bit on inputs no fixture reaches.
- **A length in a file is not an allocation.** Every hand-rolled reader takes a size off the wire. Assert that a twenty-byte file costs well under a megabyte of allocation.
- **Flags and fields with no ceiling.** Anything a user types a number into that the program multiplies: width × height overflowing an int, a count that allocates.
- **Round trips.** Save then load, print then parse, serialize then deserialize, do then undo: the result equals the start.
- **Random edits against invariants.** Apply random operations to the model and check its invariants after each (no dangling references, round trip intact, undo is the inverse of redo).
- **Paths from outside.** Archive entries, uploaded names and user-supplied paths are checked for traversal before they touch the file system.

## Reporting

Lead with the count and the list. Per bug: one sentence on what is wrong, the repro as a test name or a shell line, and where it is reachable from. Say plainly where you looked and found nothing — that is half of what makes the next hunt cheaper — and add those areas to a "where the repo is already strong" list in this file so the next hunt skips them unless the code moved.
