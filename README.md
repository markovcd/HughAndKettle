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
hk discover [--seconds N] [--port N] [--first] [--raw]   # hear WMI-1 status broadcasts; one JSON line per box
hk send <host[:port]> cc <controller> <value> [--channel 1..16] [--keep-open N] [--verbose]
hk send <host[:port]> pc <program> [--channel 1..16] [--keep-open N] [--verbose]
hk send <host[:port]> raw <hex bytes> [--keep-open N] [--verbose]
hk listen <host[:port]> [--seconds N] [--reconnect] [--verbose]  # print every message the amp sends
hk set <host[:port]> <amp-id> <control-id> <0..127 | on | off | option>
hk set <host[:port]> <amp-id> preset <1..128>
hk state <host[:port]> <amp-id> [--seconds N]                # what the amp reported, as one snapshot
hk tone list | save <amp-id> <name> [<control-id>=<value> ...] [--replace] | recall <host[:port]> <name>
hk tone rename <name> <new-name> | move <name> <position> | delete <name>
hk tone export <name> <file> | import <file> [--replace]     # every tone command takes --dir D
hk fake-wmi [--port N] [--name ID] [--broadcast-to ADDRESS | --no-broadcast]
```

Stdout carries one JSON object per line and nothing else; stderr carries usage, errors and, with
`--verbose`, every byte the WMI-1 sends. `--raw` adds a line with each packet's hex ahead of the box
it parsed to; `--keep-open N` holds the link N seconds after sending; `listen` runs until stdin closes
or `--seconds` pass, and with `--reconnect` keeps the link up the way the app does, retrying with
backoff when it drops and printing each state of the link as a JSON line of its own. `set` takes a
control by its id (`gain 100`, `channel lead`, `boost on`) and prints a preset's bank and slot on an
amp that has them; `state` listens for `--seconds` (2 by default) and asks the amp nothing. `tone`
keeps tones in `--dir` (`~/.hk/tones` by default) in the same files the app keeps: `save` takes values
as `set` does (`gain=100 channel=lead`), `recall` sends each value the tone holds, and `import` skips
and reports a control the amp does not have. Amp ids: `black_spirit_200`, `grandmeister_deluxe_40`.
Exit codes: 0 done, 2 bad arguments, 3 no link, 4 nothing found, 5 a file that could not be read or
written, or is not a tone.

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
| `app` | The Compose screen (a link card with the WMI-1s heard, then one row per control) and the service that holds the link |
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
