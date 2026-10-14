# 11. The GrandMeister app's preset files import as tones

Date: 2026-10-14

## Context

H&K's GrandMeister Deluxe 40 Remote app for iPad saves the amp's 128 presets as a `.gm40memory`
file, and hughes-and-kettner.com/presets offers artists' preset files in that format for free. Writing
into the amp's presets over MIDI is still not found (ADR 0010), but each preset in the file holds
everything a tone does and more: the channel, every knob, boost, noise gate, FX loop and power soak.

The file is an `NSKeyedArchiver` binary property list. Its presets are `GM40Preset` objects with a
name, an author, knobs as 0..100 reals, the delay time in milliseconds, the modulation type as a
number, `speakerOff` and `speakerPower` for the power soak, and no switches for the effects.

## Decision

`hk tone import` and the app's Import read a preset file as well as a tone file, telling them apart
by the property list's `bplist00` header. `ToneImport` is the one place that does; `Gm40Memory` reads
the file through a first-party `BinaryPlist` reader, with no dependency.

- Each named preset becomes a GrandMeister Deluxe 40 tone of the same name. An unnamed preset is one
  of the app's empty slots and is left out.
- A knob's 0..100 is scaled to 0..127; the delay time goes through the knob's scale; the channel and
  the modulation type are option indexes in the order the amp's chart has them.
- An effect is switched on when its level is above 0. The app keeps no effect switches, and the amp
  stores a switched-off effect's level as 0.
- `speakerOff` is the power soak's Off; otherwise `speakerPower` counts its four steps from 1 W, the
  amp's order with Off taken out. The order is inferred, not read off the amp.
- Imported tones are named apart, a second "Lead" becoming "Lead 2", from each other and from the
  tones kept, since one file often repeats a name. `hk tone import --replace` replaces instead.

## Consequences

- An artist's file recalls onto the amp as tones; it does not land in the amp's presets.
- A preset file from a later version of the app (its `version` is 1 today) is refused whole.
- The modulation type numbering and the power soak step order are read from H&K's files, not from
  the amp; a tone that recalls the wrong step is the sign either is off.
