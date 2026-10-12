# TODO

Work asked for and not yet started. One line each; anything longer lives in
[docs/handoff/](docs/handoff/README.md) and is linked from its line. Take a line off in the commit
that lands it.

## Needs the user

- Run phase 0 of the [implementation plan](docs/handoff/01-implementation-plan.md#phase-0-confirm-the-link-on-hardware) on the hardware and paste the output; everything after it waits on this.
- Try phase 1 on a phone with the real box: it finds the box, connects, survives screen-off and recovers from a power cycle ([plan, phase 1](docs/handoff/01-implementation-plan.md#phase-1-the-link-on-the-phone)).
- Pick a license for the repository.
- Make the release keys and commit the public half as `release/allowed_signers` ([plan, phase 6](docs/handoff/01-implementation-plan.md#phase-6-release)).

## Code

- Read the selector option names and their order off the hardware or the manual: modulation type (Chorus, Flanger, Phaser, Tremolo order), Black Spirit 200 cabinet and sagging names, GrandMeister Deluxe 40 power soak order. `AmpModels` carries numbered placeholders until then.
- Check on the hardware that delay time's 51 ms to 1360 ms is spread evenly over 0..127; `Scale` takes it as even.
- Add the GrandMeister 36 MIDI chart to `AmpModels` from its manual, and TubeMeister Deluxe if its chart exists.
- Phases 2 to 6 of the [implementation plan](docs/handoff/01-implementation-plan.md), one item per phase, in order.
