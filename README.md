# Hugh & Kettle

An Android app that drives a Hughes & Kettner amp (Black Spirit 200, GrandMeister Deluxe 40, more to
come) over the WMI-1 WiFi MIDI interface, the way the iPad apps do, plus `hk`, a command line that
can do everything the app can.

**Status: foundations.** The toolchain, the gate, the MIDI codec, the amp tables, the link code and
the fake WMI-1 exist and are tested; the link has not yet been tried against a real WMI-1. Phase 0 of
the [implementation plan](docs/handoff/01-implementation-plan.md) is that test, and
[docs/wmi-1.md](docs/wmi-1.md) says what is known and what is inferred.

## Build and run

Needs JDK 21, and an Android SDK with `ANDROID_HOME` pointing at it (or `local.properties` with
`sdk.dir=`). Docker for the gate.

| What | Command |
|---|---|
| Everything on this machine: compile, ktlint, lint, tests | `./gradlew build` |
| One test class | `./gradlew :midi:test --tests '*MidiStreamParserTest*'` |
| Format Kotlin | `./gradlew ktlintFormat` |
| **The gate** (the only definition of passing) | `./gate.sh` |
| The `hk` command line, installed under `cli/build/install/hk` | `./gradlew :cli:installDist` |
| The app on a connected phone or emulator | `./gradlew :app:installDebug` |
| Try a release with throwaway keys, publishing nothing | `./release.sh` |

Test reports with a duration per test land in `<module>/build/test-results/test/*.xml`.

## `hk`

```sh
hk discover [--seconds N] [--port N] [--first]        # hear WMI-1 status broadcasts; one JSON line per box
hk send <host[:port]> cc <controller> <value> [--channel 1..16]
hk send <host[:port]> pc <program> [--channel 1..16]
hk send <host[:port]> raw <hex bytes>
hk fake-wmi [--port N] [--name ID] [--broadcast-to ADDRESS | --no-broadcast]
```

Stdout carries one JSON object per line and nothing else; stderr carries usage and errors. Exit codes:
0 done, 2 bad arguments, 3 no link, 4 nothing found.

Without an amp, run a fake one and point the app or `hk send` at it:

```sh
cli/build/install/hk/bin/hk fake-wmi --port 2000 --broadcast-to 127.0.0.1
# in another shell
cli/build/install/hk/bin/hk send 127.0.0.1 cc 20 100
```

The fake prints `{"event":"midi","hex":"B0 14 64","type":"control_change",...}`. An emulator reaches
the host machine's fake at `10.0.2.2`.

## Layout

| Module | Holds |
|---|---|
| `midi` | MIDI 1.0 messages, a stream parser that survives banners and split chunks, hex helpers |
| `amp` | Amp models as tables of controls, and how a control becomes a message |
| `wmi` | Discovery of a WMI-1, the TCP link to it, the fake WMI-1 |
| `cli` | `hk` |
| `app` | The Compose screen: a link row and one row per control |
| `build-logic` | The Gradle convention plugins; `gradle/libs.versions.toml` has every version |

Docs: [AGENTS.md](AGENTS.md) is where an agent starts; `docs/agents/rules/` are the standing rules;
`docs/adr/` the decisions; [docs/glossary.md](docs/glossary.md) the words;
[docs/wmi-1.md](docs/wmi-1.md) the box; [docs/handoff/](docs/handoff/README.md) the plan and open issues;
[TODO.md](TODO.md) what is asked for and not started; [CHANGELOG.md](CHANGELOG.md) what shipped.

## Releases

`release.sh` is the release: it refuses on a dirty tree, an existing tag or an empty changelog, runs
the gate on the exact commit, signs the APK and a `SHA256SUMS` file, and with `--publish` tags and
creates the GitHub release. The `Release` workflow runs it with the real keys. Verify a download with
the two lines in [release/README.md](release/README.md).
