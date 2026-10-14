# Hugh & Kettle

An Android app that drives a Hughes & Kettner amp (Black Spirit 200, GrandMeister Deluxe 40,
GrandMeister 36, more to come) over the WMI-1 WiFi MIDI interface, the way the iPad apps do, plus
`hk`, a command line that can do everything the app can.

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
| The desktop app in a window (in progress: the theme and a knob so far) | `./gradlew :desktop:run` |
| Try a release with throwaway keys, publishing nothing | `./release.sh` |

Test reports with a duration per test land in `<module>/build/test-results/test/*.xml`.

## `hk`

```sh
hk discover [--seconds N] [--port N] [--first] [--raw]   # hear WMI-1 status broadcasts; one JSON line per box
hk send <host[:port]> cc <controller> <value> [--channel 1..16] [--keep-open N] [--verbose]
hk send <host[:port]> pc <program> [--channel 1..16] [--keep-open N] [--verbose]
hk send <host[:port]> raw <hex bytes> [--keep-open N] [--verbose]
hk listen <host[:port]> [--seconds N] [--reconnect] [--verbose]  # print every message the amp sends
hk cable list | send <name> cc|pc|raw ... [--channel 1..16] | listen <name> [--seconds N]  # a USB MIDI cable, no WMI-1
hk set <host[:port]> <amp-id> <control-id> <0..127 | on | off | option>
hk set <host[:port]> <amp-id> preset <1..128>
hk state <host[:port]> <amp-id> [--seconds N]                # what the amp reported, as one snapshot
hk lock <host[:port]> <amp-id> [<0..127 | 520ms>] [--seconds N]  # hold the delay time across preset recalls
hk tone list | save <amp-id> <name> [<control-id>=<value> ...] [--replace] | recall <host[:port]> <name>
hk tone rename <name> <new-name> | move <name> <position> | delete <name>
hk tone export <name> <file> | import <file> [--replace]     # every tone command takes --dir D
hk tone read <host[:port]> <amp-id> [<preset>[-<preset>]] [--replace] [--names D]  # the amp's presets into tones
hk preset list <amp-id> | name <amp-id> <preset> <name> | unname <amp-id> <preset>  # takes --dir D
hk fake-wmi [--port N] [--name ID] [--broadcast-to ADDRESS | --no-broadcast] [--seconds N]
hk latency [--count N]                                      # send-to-parsed time against a local fake WMI-1
```

Stdout carries one JSON object per line and nothing else; stderr carries usage, errors and, with
`--verbose`, every byte the WMI-1 sends. `--raw` adds a line with each packet's hex ahead of the box
it parsed to; `--keep-open N` holds the link N seconds after sending; `listen` runs until stdin closes
or `--seconds` pass, and with `--reconnect` keeps the link up the way the app does, retrying with
backoff when it drops and printing each state of the link as a JSON line of its own. `set` takes a
control by its id (`gain 100`, `channel lead`, `boost on`, `delay_time 520ms`) and prints a preset's bank and slot on an
amp that has them; `state` listens for `--seconds` (2 by default) and asks the amp nothing. `tone`
keeps tones in `--dir` (`~/.hk/tones` by default) in the same files the app keeps: `save` takes values
as `set` does (`gain=100 channel=lead`), `recall` sends each value the tone holds, and `import` skips
and reports a control the amp does not have. `import` also takes a preset file of H&K's GrandMeister
Deluxe 40 Remote app (`.gm40memory`, such as the artist files on hughes-and-kettner.com/presets):
each named preset becomes a tone, numbered apart from a tone of the same name unless `--replace` is
given, printed one per line and then as a `{"presets":N}` line. `tone read` recalls each preset and saves the knobs the
amp reports for it as a tone named after the preset; it reads every preset unless given one or a
range, and a preset that sends nothing within 2 s is skipped and said on stderr. `preset` names the
amp's presets in `--dir` (`~/.hk/preset-names` by default), the way the app does; `tone read` finds
them there or in `--names`. Amp ids: `black_spirit_200`, `grandmeister_deluxe_40`,
`grandmeister_36`.
Exit codes: 0 done, 2 bad arguments, 3 no link, 4 nothing found, 5 a file that could not be read or
written, or is not a tone.

Without an amp, run a fake one and point the app or `hk send` at it:

```sh
cli/build/install/hk/bin/hk fake-wmi --port 2000 --broadcast-to 127.0.0.1
# in another shell
cli/build/install/hk/bin/hk send 127.0.0.1 cc 20 100
```

The fake prints `{"event":"midi","hex":"B0 14 64","type":"control_change",...}` and runs until stdin
closes, or for `--seconds N` when nothing holds stdin open (a background job). An emulator reaches
the host machine's fake at `10.0.2.2`.

## Layout

| Module | Holds |
|---|---|
| `midi` | MIDI 1.0 messages, a stream parser that survives banners and split chunks, hex helpers |
| `amp` | Amp models as tables of controls, how a control becomes a message, the amp's state, tones and their files, preset names, and reading presets into tones |
| `wmi` | Discovery of a WMI-1, the TCP link to it, the fake WMI-1 |
| `cli` | `hk` |
| `ui` | The screens the Android and desktop apps share: the theme, the tabs, cards and rows, the panel's state, the link holder, the tone shelf, and their words as Compose resources |
| `app` | The Android shell: the activity, the phone's layout (`AmpPanel`), the service that holds the link, the settings stores, WiFi, the USB cable and the tone file picker |
| `desktop` | The desktop app, a window over `ui`; see [the plan](docs/handoff/03-desktop-app.md) |
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

## License

MIT, in [LICENSE](LICENSE). Hugh & Kettle is not made or endorsed by Hughes & Kettner.
