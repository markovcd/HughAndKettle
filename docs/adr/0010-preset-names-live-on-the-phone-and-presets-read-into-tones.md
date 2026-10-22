# 10. Preset names live on the phone, per amp model, and the amp's presets are read into tones

Date: 2026-10-13

## Context

The amp's 128 presets carry no names, and storing into one over MIDI does not work (docs/wmi-1.md,
"Storing a preset"). A recall makes the amp send its value block, which holds the knobs of the preset
recalled but not yet the channel or the switches. The user asked for presets to be named, exported
and imported.

## Decision

- A preset name is kept on the phone, or by `hk`, per amp model: `PresetNameStore` holds one
  `<amp-id>.json` per model, `"version": 1`, mapping the preset counted from 1 to its name. A damaged
  or newer file names nothing, is reported, and is never written over.
- Names belong to the amp model, not to a WMI-1, so they hold over a cable and follow the amp
  picked on the panel. Two amps of one model share them.
- Reading presets is `PresetReading`, a value in `amp` that the app and `hk tone read` both drive:
  send its program change, hand it what the amp says, and skip a preset whose value block does not
  come in time. Each preset read becomes a tone called by its number and name, so export and import
  are the tones' own.
- The app replaces tones of the same name, so a second reading refreshes the first; `hk` replaces
  only with `--replace`, as `tone save` and `tone import` do.

## Consequences

- A tone read from a preset holds only knobs. Recalling it sets them on whatever channel the amp is
  on, with the switches as they are. Decoding the value block's flags would add the channel and
  switches.
- Writing a tone back into a preset still waits on the amp's store gesture over MIDI.
- Names are not in a tone file and are not exported; they are a few lines to type again.

## Amendment (2026-10-22)

The value block's flags are decoded (docs/wmi-1.md, "The value block"): a tone read from a preset
holds its channel, boost, noise gate, FX loop and power soak as well as its knobs. The effect
switches are not in the block, so one the amp did not report during the recall is taken as on when
its level (delay volume, reverb volume, modulation intensity) is above 0, as the amp stores an effect
switched off.
