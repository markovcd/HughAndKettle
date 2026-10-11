---
name: performance
description: Use when something is slow, or before proposing any work to make code faster - how to measure without fooling yourself, and keeping a record of what was measured, what was ruled out, and the open leads.
---

# Performance

## Measure first

Do optimization work only when a number says so, and only for a case a real user hits. A lead without a measurement is a guess.

## Measuring without fooling yourself

- **Say which configuration a number came from.** Debug or release, which backend, which flags, which machine, which commit. A table of numbers read without that was once misread for a week: the "slow case" was a code path the optimizer had silently given up on.
- **Time both arms in one process**, old and new, over the same input. Across processes the machine's clocks and caches move numbers by more than most changes do. For a rewrite, copy the old version into a scratch file under another name and run both side by side.
- **Split before you guess.** Put a stopwatch around each stage of one iteration. The expensive stage is regularly not the one the code's own comments say it is.
- **Ask the runtime what it did.** JIT and compiler diagnostics (tiering, inlining, "method too large to optimize"), a profiler, allocation counters. A method past the optimizer's size limit can be ten times slower for no visible reason.
- **Measure what users run.** A benchmark project that cannot load real inputs (plugins, real data, real configs) measures something else.
- **Check the output did not change.** Anything that claims to be a faster route to the same result has to produce the same result: compare bytes, not eyeballs, and keep the comparison as a test.
- **Warm up.** A first timing on freshly loaded code measures the loader and the JIT.

## Keep the record

Keep a `docs/performance.md` (or this skill's own body, extended) with three parts:

1. **Where the time goes**, as a table with the configuration and machine named.
2. **Open leads**, each with the likely fix, the expected gain, and what has to stay true (the output byte for byte).
3. **Ruled out, with numbers.** What was tried or measured and why it is not worth the code: "per-buffer stage: 7.5% of ops, not worth it", "chunk size 128/256/512 measured the same; 1024 fell off". This is the part that stops the next session repeating the experiment.
