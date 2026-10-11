# 5. `hk` is the first client of every feature, and the fake WMI-1 is how the app is driven without an amp

Date: 2026-10-11

## Context

Most of this codebase will be exercised by agents, who can run a command and read its output but
cannot see a phone. The hardware (an amp and a WMI-1) is on the user's desk and nowhere else.

## Decision

- Every capability lands in a JVM module first and is reachable from the `hk` command line before it
  reaches the app: `discover`, `send`, `fake-wmi` today; presets, state readback and module
  configuration as they arrive. A command is a function of its arguments and two writers returning an
  exit code, so it is tested in-process. Stdout carries one JSON object per line; stderr carries
  everything else; exit codes mean one thing each (`ExitCodes`).
- `FakeWmi` is a real TCP server and UDP broadcaster that speaks the module's bytes. Tests, the CLI
  (`hk fake-wmi`) and the app on an emulator or a phone all point at it. A later fake amp behind it
  answers like the real one once phase 0 says what the real one does.
- A question an agent keeps answering with a throwaway program becomes a command or a flag.

## Consequences

- A feature is finished when `hk` can do it and a test proves it, not when a screen shows it.
- The app's screens stay thin by construction, because the logic they would hold already exists one
  layer down with a command on it.
- The fake is a second implementation at the real boundary (the network), which is why `WmiLink` has
  no interface: it talks to a socket, and the fake sits at the other end of one.
