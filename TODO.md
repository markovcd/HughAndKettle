# TODO

Work asked for and not yet started. One line each; anything longer lives in
[docs/handoff/](docs/handoff/README.md) and is linked from its line. Take a line off in the commit
that lands it.

## Needs the user

- Run phase 0 of the [implementation plan](docs/handoff/01-implementation-plan.md#phase-0-confirm-the-link-on-hardware) on the hardware and paste the output; everything after it waits on this.
- While the WMI-1 is missing, run the [cable test](docs/cable.md#the-test) instead and paste what it asks for.
- Try phase 1 on a phone with the real box: it finds the box, connects, survives screen-off and recovers from a power cycle ([plan, phase 1](docs/handoff/01-implementation-plan.md#phase-1-the-link-on-the-phone)).
- Step through presets with the footswitch under `hk trim <host> grandmeister_deluxe_40 -40` and say whether the stored volume is heard before the trim lands; the printed milliseconds show how long it plays.
- Play a song through the amp with `hk song play` and say whether each switch lands on its beat and whether the preset's own delay time is heard before the song's.
- Send MIDI clock from the Syntakt into the amp with the delay on and say whether the delay time follows it; neither manual lists clock, so the guess is that it is ignored. Note what the undocumented controller 36 that tap tempo sends does.
- Make the release keys and commit the public half as `release/allowed_signers` ([plan, phase 6](docs/handoff/01-implementation-plan.md#phase-6-release)).
- Make the Windows signing certificate, commit its public half as `release/msix.cer` and set its two secrets ([release/README.md](release/README.md#the-windows-certificate)).

## Code

- Parked: write tones into the amp's presets and move presets between slots, as the GrandMeister app does. Storing over MIDI was not found ([wmi-1.md](docs/wmi-1.md#storing-a-preset)); sending the amp its own value block is the untried guess.
- Parked: keep a preset file whole, named, read-only if wanted and shared as one, rather than as loose tones.
- Parked: set the WMI-1's name, access point channel, password and home WiFi, as WMI Connect does, likely through the WiFly module's `$$$` command mode over TCP. A wrong setting locks the app out until a factory reset.
- Read the selector option names off the hardware or the manual: Black Spirit 200 cabinet and sagging names. `AmpModels` carries numbered placeholders until then.
- Decode the rest of the amp's value block: its flags byte and pairs 14 and 15 ([wmi-1.md](docs/wmi-1.md#the-value-block)).
- Time the delay at a few settings to confirm 51 ms to 1360 ms is spread evenly over 0..127; the amp's reports suggest it is, and `Scale` takes it as even.
- The [desktop app](docs/handoff/03-desktop-app.md), one item per step, in order.
- Phases 2 to 6 of the [implementation plan](docs/handoff/01-implementation-plan.md), one item per phase, in order.
