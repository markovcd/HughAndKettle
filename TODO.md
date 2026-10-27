# TODO

Work asked for and not yet started. One line each; anything longer lives in
[docs/handoff/](docs/handoff/README.md) and is linked from its line. Take a line off in the commit
that lands it.

## Needs the user

- Make the release keys and commit the public half as `release/allowed_signers` ([plan, phase 6](docs/handoff/01-implementation-plan.md#phase-6-release)).
- Make the Windows signing certificate, commit its public half as `release/msix.cer` and set its two secrets ([release/README.md](release/README.md#the-windows-certificate)).

## Code

- Parked: move presets between slots, as the GrandMeister app does; each move is a write needing STORE on the amp ([ADR 0014](docs/adr/0014-a-tone-is-written-into-a-preset-by-answering-the-amps-store-button.md)).
- Parked: keep a preset file whole, named, read-only if wanted and shared as one, rather than as loose tones.
- Parked: set the WMI-1's name, access point channel, password and home WiFi, as WMI Connect does, likely through the WiFly module's `$$$` command mode over TCP. A wrong setting locks the app out until a factory reset.
- Read the selector option names off the hardware or the manual: Black Spirit 200 cabinet and sagging names. `AmpModels` carries numbered placeholders until then.
- Read the amp's system exclusive over a cable on macOS, where Java's MIDI input garbles it ([cable.md](docs/cable.md#what-a-mac-found)); until then `hk cable listen` there prints junk.
- The loudness match's summary writes the matched volumes into the presets, one STORE each, through the write flow, as Arek's canvas board DF draws it.
- `hk trim`, `hk lock`, `hk song play` and every other `hk` recall answer add the loudness match's corrections kept in `~/.hk/loudness`, as the app does.
- Send the delay time in the amp's 256 steps: controller 4 with its low byte on 36 at 0 or 64, as tap tempo does ([wmi-1.md](docs/wmi-1.md#what-the-amp-reports)).
- `DesktopClickSoundTest`'s first click took 188 ms to start, against 20, in a full build on the Mac, and passed four times alone: find whether the sound output wakes slowly or the build starves it, then fix the click or the test's reason.
- The [desktop app](docs/handoff/03-desktop-app.md), one item per step, in order.
- Phases 2 to 6 of the [implementation plan](docs/handoff/01-implementation-plan.md), one item per phase, in order.
