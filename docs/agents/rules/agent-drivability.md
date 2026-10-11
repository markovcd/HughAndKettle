# Agent drivability

## Drivability comes first

When a feature, a command or a fix can be shaped more than one way, pick the one an agent can drive: run it, check it and debug it with no eyes on a window. It outranks convenience of implementation and polish of the UI.

In practice:

- **A command before a window.** Whatever the product does can be asked of a CLI, an endpoint or a test harness. A question an agent keeps answering with a throwaway test gets a command or a flag instead.
- **Answers a script can read.** Exit codes mean one thing each, a report has `--json`, stdout carries only the answer and logs go to stderr.
- **Commands are functions.** A command takes its input and two writers (out, err) and returns an exit code, so it is testable in-process without spawning anything.
- **The UI headless.** What a gesture or a key does lives in a service; the control only hands the event on. A feature whose logic can only be reached through a UI event handler is not finished.
- **Failures say where.** A failure names the file, line, item or frame, so the next step is a fix rather than a bisect.
- **One command is the gate.** Restore, build and every test in a clean, reproducible environment (a container, a CI job run locally), with deterministic pass/fail. A local run that skips what the machine lacks is not the gate.
- **Safe to run.** An agent can build, run and test without production credentials or production data: a local database with a fake seed, secrets out of the repo.
- **Enforced beats written.** A rule a test or an analyzer checks reaches every agent; a rule in a document reaches the ones that read it. Where a rule can become a check (layering, a banned API, a naming pattern), make it one.

**Why:** most work in an agent-built codebase is built, tested and debugged by an agent. Whatever an agent cannot reach is work nobody can check.

**How to apply:** before settling a design, ask how an agent would confirm it works. If the answer is a screenshot, a person, or a throwaway program, the design is not done.

## A hack is a feature request

Reaching for a hack to get something done is the signal that the project is missing a feature. Name the friction and propose the feature that would remove it.

Hacks look like:

- a throwaway test or scratch program written only to read a value out of the system;
- reflection into private state;
- screen coordinates, synthetic keystrokes or a window capture where a command could answer;
- parsing human-readable output because there is no `--json`;
- sleeping and polling for a state nothing reports;
- hand-editing a data or settings file to reach a state the tools cannot;
- running the same sequence of commands again because no single command does it.

**Why:** a hack solves the task once and leaves the friction for the next session, which pays for it again.

**How to apply:** finish the task with the hack if it is the only way, then end the reply with the proposal: what hurt, in a line, and the command, flag or service that would have made it unnecessary. Do not build the feature unasked. When the user agrees, add it to `TODO.md`. A proposal that repeats an item already in `TODO.md`, or one an ADR declined, says so instead.
