---
name: tests
description: Use when running the test suite, diagnosing a slow or newly-slow test, or shipping a feature - ranking a run by duration and treating an unexplained outlier as a defect, running narrow tests while iterating versus the full suite once per commit, and writing the scenario a feature ships with as a requirement.
---

# Tests

## Read the durations, not just the result

A run that passes still has something to say. After running the suite, rank the tests by how long each took and look at the top of the list. Most runners write a per-test time to a report (JUnit XML, `--durations`, `-xml`, `--reporter=json`); a per-assembly total is not the same thing and will not show a passing test that costs thirty seconds.

Know the shape to compare against: the test count, the median, the slowest. In a healthy unit suite the median is well under a millisecond and anything past a second is an outlier worth explaining.

## A slow test is usually telling you something is wrong

Treat an unexplained outlier as a defect, not a cost. Almost always it is one of these:

- **It waits on a wall clock.** A spin-wait with a deadline takes as long as whatever it waits for, and its full deadline when that never arrives. The duration is often the bug showing itself before the failure does.
- **It waits out a deadline to prove a negative.** Nothing arriving is only provable by waiting, so cap that wait hard and say in a comment why the cap is enough. Better: make the deadline an argument the test can shorten.
- **It leaves something running.** Work a test starts and does not stop (a window, a timer, a thread, a server) is charged to every test after it, so the cost shows up spread across the run rather than on the test that caused it.
- **It redoes shared work.** Something built once per test that could be built once per class.

Some tests are honestly slow (rendering, real I/O). Those keep their time and say why in a comment. Everything else gets made fast.

## Fix it when you find it

Not a note for later. An outlier found while running the suite for another reason is fixed in a commit of its own, before or after the work in hand but not inside it.

## Run what the change touches, and the suites once per commit

While iterating, build and run only the test classes or files a change touches. Run the full suites once, just before each commit, and not again after an edit that only touches comments, docs or a test's own file. Proving a new test fails without the fix is one run of that test, never of the suite.

**Why:** a session that ran a two-minute suite after every edit spent most of its budget on runs that could only pass. The per-commit run still catches what the narrow runs miss.

Know the runner's filter syntax before relying on it. Some platforms silently ignore a filter flag and run everything, or run nothing and report green; check the count of tests that ran.

## A feature ships with a scenario

If the repo keeps behavior specs (Gherkin, acceptance tests, an e2e suite), every new user-facing feature gets at least one scenario in the same commit. Unit tests still cover the edges; the scenario states the requirement.

A feature is something a user does or relies on. A change to how the system runs what users already had (speed, scheduling, internals) is covered by unit tests alone.

The scenario reads as a requirement in the user's words, not a script of mechanics:

```gherkin
# Yes: the requirement
Scenario: Changing a tone's frequency while it plays does not click
  Given a 10 Hz sine is playing
  When its frequency is turned to 12 Hz
  Then the sound never clicks

# No: the mechanics
Scenario: Phase is adopted across a recompile
  Given node "tone" output 0 is wired to node "out" input 2
  When 125 samples are evaluated
  Then no two neighboring samples differ by more than 0.08
```

**Why:** a scenario is the one test a reader checks against what the product is meant to do. Written as wiring, it only restates the code, and nobody can tell from it whether the behavior is right.

Name the feature file and scenarios after what the user gets. Keep the wiring, indexes and magic numbers in the step definitions unless the number is the requirement. A new area gets its own steps class rather than a phrase bolted onto one about something else.
