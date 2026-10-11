# 3. An amp's MIDI chart is a table of controls, one per amp model, with ids that are persisted

Date: 2026-10-11

## Context

Each Hughes & Kettner MIDI amp publishes a chart of controller numbers: gain on 20, channel switching
on 31 in four sectors, and so on. The Black Spirit 200 and the GrandMeister Deluxe 40 share almost
every number and differ in a few (cabinet and sagging on one, power soak on the other). More amps will
follow. Preset files written by the app will name controls.

## Decision

A control is data: an id, a name, a controller number and a kind (knob, switch, or selector with its
options). An amp model is a list of controls. `AmpModels` holds the tables, built from a shared list
plus each amp's own rows. The kind, not a subclass, decides how a value is read and written;
`Sectors` is the one place that cuts 0..127 into sectors.

A control's `id` (`gain`, `channel`, `power_soak`) is a persisted identifier: once written into a
preset file or shipped, it is never renamed. Its `name` is free to change.

## Consequences

- Adding an amp is adding rows, with the manual page as the source, plus a test that the table's
  numbers match the manual's. No code path changes.
- The UI renders from the table: a knob is a slider, a switch a switch, a selector a row of options.
- Option names the manuals do not give are numbered placeholders until read off the hardware;
  TODO.md tracks them. A placeholder is honest; a guessed order is not.
- The tests assert the chart against the manual for a handful of rows and assert structural rules
  (distinct ids, distinct controllers) for all of them, so a new row is covered without being listed.
