# Hugh & Kettle

An Android app that drives a Hughes & Kettner amp over its WMI-1 WiFi MIDI interface, the way
the iPad apps do, with an `hk` command line that can do everything the app can. See
[README.md](README.md) for the build and run commands, `docs/adr/` for the decisions behind the
design, [docs/wmi-1.md](docs/wmi-1.md) for what is known about the box the app talks to,
[docs/glossary.md](docs/glossary.md) for the one word each thing is called by, and
[docs/handoff/01-implementation-plan.md](docs/handoff/01-implementation-plan.md) for the plan.

- **The gate:** `./gate.sh`. It builds a container carrying the JDK and the Android SDK and runs
  `./gradlew build` (restore against the lock files, compile, ktlint, Android Lint, every test) inside
  it. It is the truth; a local run can be green about code it skipped.
- **Fast loop:** `./gradlew :midi:test --tests '*MidiStreamParserTest*'` runs one test class of one
  module; `./gradlew build` runs everything on the machine, which is enough while iterating.
- **TODO.md** lists work asked for and not yet started. Add to it when the user says "add to the todo", and take an item off in the commit that lands it. A plan, an audit or an out-of-scope problem too long for a line is written up in [docs/handoff/](docs/handoff/README.md) and linked from its item.

Standing rules are in `docs/agents/rules/`. They apply to nearly every task, so read all of them before starting; Claude Code imports them through the paths below:

- @docs/agents/rules/git-workflow.md — declarative commit subjects, squash unpushed churn, never sweep up another session's uncommitted work.
- @docs/agents/rules/code-style.md — new code is indistinguishable from the file it lands in; failures are values; one place knows a thing.
- @docs/agents/rules/testing.md — a behavior change lands with a test that failed before it; test names state the rule.
- @docs/agents/rules/honest-tests.md — a failing test is fixed by fixing the code, never by weakening, skipping or special-casing the test.
- @docs/agents/rules/security.md — secrets stay out of everything; outside input is checked where it enters; a check is never loosened to make something work.
- @docs/agents/rules/ci-cd.md — land small and often, a red mainline is fixed before anything else, and the mainline is always releasable.
- @docs/agents/rules/pipeline.md — CI runs the gate itself; workflows pinned, least-permission and timed out; a release is one script, refuses before moving anything, and ships signed.
- @docs/agents/rules/saved-data.md — what was written stays readable; a format change carries its migration and a test that loads the old shape.
- @docs/agents/rules/one-type-per-file.md — one top-level type per file, named for it; split a file that holds several whenever a change touches it.
- @docs/agents/rules/prose-style.md — succinct comments that never narrate history.
- @docs/agents/rules/terminology.md — when the user says a word the glossary rules out, correct it in one line.
- @docs/agents/rules/agent-drivability.md — drivability by an agent comes first; a hack needed to get something done is a feature to propose.
- @docs/agents/rules/propose-refactors.md — code that has gone untidy or ballooned is a refactor to propose, after the task and outside its commit.
- @docs/agents/rules/working-habits.md — stop when the work is done, report failure plainly, verify at the cheapest level that proves it.

Task-specific know-how is in `.claude/skills/`, loaded when a skill's description matches the task:

- `tests`: rank a run by duration, treat an unexplained slow test as a defect, run narrow while iterating and everything once per commit, write feature scenarios as requirements.
- `dependencies`: check packages and the toolchain before anything big; take cheap upgrades, hand off expensive ones.
- `adrs`: check `docs/adr/` before proposing a refactor; rewrite a day-old ADR in place; number a new ADR at commit time.
- `changelog`: what CHANGELOG.md may contain.
- `docs-in-step`: a change to anything the docs or site describe updates them in the same commit.
- `performance`: how to measure without fooling yourself.
- `running-the-app`: before launching the real app, wait for any other instance; drive only the process you started.

Commands in `.claude/commands/` are run by name: `/bughunt` hunts for bugs, confirms each as a failing test, fixes it, and files the test where it belongs.
