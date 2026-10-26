# 14. A tone is written into a preset by answering the amp's STORE button

Date: 2026-10-26

## Context

The amp stores a preset only from its own STORE button: controller 80 sent over MIDI does not arm a store
(docs/wmi-1.md, "Storing a preset"). Once STORE is pressed, a program change from the app stores the panel
into that preset, and the amp reports the press as controller 80 at 127, then answers the store with the
stored preset's value block. The user asked to write tones into presets so the footswitch recalls them
with no phone.

## Decision

- A write is `PresetWriting`, a value in `amp` that the app and `hk tone write` both drive, as
  `PresetReading` is driven: recall the preset and keep it as a backup tone, send the tone, wait for the
  amp's STORE report, answer it with the preset's program change, and compare the value block that comes
  back with the tone.
- The person presses STORE; the app never asks them to pick the preset on the amp. STORE is asked for only
  once the backup is kept and the tone sent: pressed earlier, the backup's own recall would store into the
  preset before it is kept, so a STORE heard then ends the write as unknown.
- The backup is always kept, as a tone named after the preset ("Backup of Bank 12 · 3"), apart from the
  tones already kept.
- The tempo lock and the volume trim answer nothing while a write runs, so the amp stores the tone's own
  delay time and volume.
- A level stored at 0 for an effect the tone has off is how the amp keeps an effect switched off, and is
  not reported as a difference. The effect switches are not in the value block and are not compared.
- A write that saw STORE but no value block ends as unknown, and is checked by recalling the preset.

## Consequences

- One STORE press per preset; writing several presets in one go is a later step.
- Whether the amp stores a value sent over MIDI as faithfully as one turned by hand is checked by the
  comparison each write makes; one store under a volume trim kept the untrimmed volume.
- A preset written is named after the tone on the phone, and in `hk`'s preset names.
