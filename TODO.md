# TODO

Work asked for and not yet started. One line each; anything longer lives in
[docs/handoff/](docs/handoff/README.md) and is linked from its line. Take a line off in the commit
that lands it.

## Needs the user

- Run phase 0 of the [implementation plan](docs/handoff/01-implementation-plan.md#phase-0-confirm-the-link-on-hardware) on the hardware and paste the output; everything after it waits on this.
- While the WMI-1 is missing, run the [cable test](docs/cable.md#the-test) instead and paste what it asks for.
- Make the release keys and commit the public half as `release/allowed_signers` ([plan, phase 6](docs/handoff/01-implementation-plan.md#phase-6-release)).
- Make the Windows signing certificate, commit its public half as `release/msix.cer` and set its two secrets ([release/README.md](release/README.md#the-windows-certificate)).

## Code

- Parked: write tones into the amp's presets and move presets between slots, as the GrandMeister app does. STORE pressed by hand then a program change from the app stores the panel ([wmi-1.md](docs/wmi-1.md#storing-a-preset)), so a tone can be written by sending it, asking for STORE, and sending the preset's program change; arming a store over MIDI alone was not found.
- Parked: keep a preset file whole, named, read-only if wanted and shared as one, rather than as loose tones.
- Parked: set the WMI-1's name, access point channel, password and home WiFi, as WMI Connect does, likely through the WiFly module's `$$$` command mode over TCP. A wrong setting locks the app out until a factory reset.
- Read the selector option names off the hardware or the manual: Black Spirit 200 cabinet and sagging names. `AmpModels` carries numbered placeholders until then.
- Decode the rest of the amp's value block: pairs 14 and 15, and the switches byte's upper bits ([wmi-1.md](docs/wmi-1.md#the-value-block)).
- Read the amp's system exclusive over a cable on macOS, where Java's MIDI input garbles it ([cable.md](docs/cable.md#what-a-mac-found)); until then `hk cable listen` there prints junk.
- Send the delay time in the amp's 256 steps: controller 4 with its low byte on 36 at 0 or 64, as tap tempo does ([wmi-1.md](docs/wmi-1.md#what-the-amp-reports)).
- The [desktop app](docs/handoff/03-desktop-app.md), one item per step, in order.
- Phases 2 to 6 of the [implementation plan](docs/handoff/01-implementation-plan.md), one item per phase, in order.
