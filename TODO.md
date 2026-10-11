# TODO

Work asked for and not yet started. One line each; anything longer lives in
[docs/handoff/](docs/handoff/README.md) and is linked from its line. Take a line off in the commit
that lands it.

## Needs the user

- Say which amp sits behind the WMI-1 (Black Spirit 200, GrandMeister Deluxe 40, GrandMeister 36, other); that model's table is the first to be verified.
- Run phase 0 of the [implementation plan](docs/handoff/01-implementation-plan.md#phase-0-confirm-the-link-on-hardware) on the hardware and paste the output; everything after it waits on this.
- Pick a license for the repository.
- Make the release keys and commit the public half as `release/allowed_signers` ([plan, phase 6](docs/handoff/01-implementation-plan.md#phase-6-release)).

## Code

- Read the selector option names and their order off the hardware or the manual: modulation type (Chorus, Flanger, Phaser, Tremolo order), Black Spirit 200 cabinet and sagging names, GrandMeister Deluxe 40 power soak order. `AmpModels` carries numbered placeholders until then.
- Add the GrandMeister 36 MIDI chart to `AmpModels` from its manual, and TubeMeister Deluxe if its chart exists.
- Phases 1 to 6 of the [implementation plan](docs/handoff/01-implementation-plan.md), one item per phase, in order.
