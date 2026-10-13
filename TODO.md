# TODO

Work asked for and not yet started. One line each; anything longer lives in
[docs/handoff/](docs/handoff/README.md) and is linked from its line. Take a line off in the commit
that lands it.

## Needs the user

- Run phase 0 of the [implementation plan](docs/handoff/01-implementation-plan.md#phase-0-confirm-the-link-on-hardware) on the hardware and paste the output; everything after it waits on this.
- While the WMI-1 is missing, run the [cable test](docs/cable.md#the-test) instead and paste what it asks for.
- Try phase 1 on a phone with the real box: it finds the box, connects, survives screen-off and recovers from a power cycle ([plan, phase 1](docs/handoff/01-implementation-plan.md#phase-1-the-link-on-the-phone)).
- Pick a license for the repository.
- Make the release keys and commit the public half as `release/allowed_signers` ([plan, phase 6](docs/handoff/01-implementation-plan.md#phase-6-release)).

## Code

- Read the selector option names off the hardware or the manual: Black Spirit 200 cabinet and sagging names, GrandMeister 36 power soak steps. `AmpModels` carries numbered placeholders until then.
- Decode the block of current values the amp sends after a channel switch or preset recall ([wmi-1.md](docs/wmi-1.md#what-the-amp-reports)), so presets can be read back.
- Decide whether the panel's Gain and Volume should send on 56 and 57, the controllers the amp reports them on ([wmi-1.md](docs/wmi-1.md#what-the-amp-reports)).
- Time the delay at a few settings to confirm 51 ms to 1360 ms is spread evenly over 0..127; the amp's reports suggest it is, and `Scale` takes it as even.
- Phases 2 to 6 of the [implementation plan](docs/handoff/01-implementation-plan.md), one item per phase, in order.
