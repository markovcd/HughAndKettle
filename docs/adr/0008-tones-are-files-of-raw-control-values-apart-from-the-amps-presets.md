# 8. A tone is a file of raw control values, kept apart from the amp's presets

Date: 2026-10-12

## Context

Phase 3 of the plan keeps sounds on the phone so they can be recalled, moved to another phone and
backed up. The amp has 128 presets of its own, recalled by a program change, and nothing yet says
whether the app can read one back. A sound kept by the app is a different thing from a preset: it
lives on the phone, holds only what the app knew, and is recalled by sending each control.

The selector option names are partly placeholders (TODO.md), and the amp's MIDI chart is data whose
control ids are persisted (ADR 0003).

## Decision

The app's sound is a **tone**, so that "preset" keeps meaning the amp's. A tone is a name, an amp
model id and a map of control id to the raw 0..127 the control is sent. It is written as JSON with
`"version": 1`; `ToneFile` in `amp` is the one reader and writer, and `hk` and the app share it.

- Raw values, not option names, so a selector option renamed when the hardware is read still reads back.
- A file from a newer version is refused whole; a control the amp does not have, or a value outside
  0..127, is skipped and reported; the rest of the tone loads.
- Only known values are kept. Recalling a tone sends those, in panel order, and leaves the others
  where the amp has them.
- `ToneStore` keeps a folder: one `<n>.json` per tone and an `order` file listing them under its own
  version line. A file is never renamed for its tone's name, so a rename rewrites one file and a
  reorder rewrites only `order`. Every write goes to a temporary file moved into place.
- `kotlinx-serialization-json` is read through its `JsonElement` tree, so the build takes no
  compiler plugin.

## Consequences

- An exported tone file is the same file the folder holds, so export and import are a copy and a
  checked read.
- A tone file dropped into the folder by hand is listed, after the ordered ones; a damaged one is
  reported and left in place.
- Writing a tone into one of the amp's presets waits on the amp's store gesture over MIDI, which
  nobody has seen yet.
- A later version of the format raises `version` only when an older reader would misread it, and
  carries its upgrade step and a fixture of every older version.
