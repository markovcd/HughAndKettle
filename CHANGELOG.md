# Changelog

User-facing changes only, one terse bullet per feature (the `changelog` skill). The `Unreleased`
section becomes a version's section when `release.sh` ships it.

## Unreleased

### Features

- `hk` command line: find a WMI-1 on the network, send it a message, listen to what the amp says, or run a fake one.
- App: connect to a WMI-1 by address and move the controls of a Black Spirit 200.
- `hk set` moves a control by name or recalls a preset, and `hk state` prints what the amp reported.
- A dropped link to a WMI-1 comes back by itself (`hk listen --reconnect`).
- App: lists the WMI-1s on the network, the last one used first, and connects to one with a tap.
- App: step through the amp's presets, shown as bank and slot on the GrandMeister.
- App: the link stays up with the screen off, shown in a notification that can end it.
- `hk tone` saves, recalls, lists, renames, reorders, deletes, exports and imports tones.
- App: save the panel as a named tone, recall it with a tap, and move tones between phones as files.
- App: dark by default, with light or the phone's own setting in the settings menu, and remembered.
- App: the panel is three tabs (Amp, FX, Tones) with the preset, channel, boost and mute always in view; in landscape the amp and its effects sit side by side.
- App: opens on a splash of its kettle mark on the dark stage.
- App: the amp is picked in the settings menu, asked for on first linking to a WMI-1, and remembered for each.
- `hk latency` measures how long a control change takes to reach a fake WMI-1 on the same machine.
- Delay time shows in milliseconds, takes a tapped beat in the app, and `hk set` takes `520ms`.
- App: a tap on the preset opens a grid of all 128 to jump to.
- App: connects by itself to the last WMI-1 used or the only one heard, and once linked the connection folds into a status glyph.
- Gain and Volume move the amp's own knobs, landing where the amp reports them.
- App: a control the amp reports moving is outlined until it is set from the phone.
- App: each effect is a card that opens to its controls while it is on, one at a time.
- The GrandMeister 36 can be picked in the app and named in `hk` as `grandmeister_36`.
- `hk fake-wmi --seconds N` runs the fake for N seconds, for scripts that hold no stdin open.
- App and `hk cable`: drive the amp over a USB MIDI cable, with no WMI-1.
- App and `hk preset`: name the amp's presets, shown in the preset grid and under the current preset.
- App and `hk tone read`: read the amp's presets into tones named after them.
- The app and `hk state` follow the knobs, switches and selectors turned on the amp, as the amp reports them.
- The GrandMeister Deluxe 40 power soak shows its steps by wattage.
- After a preset recall or channel switch, the app and `hk state` show the knobs the amp reports it is set to.
- The release APK is shrunk by R8.
- App: shows which WiFi network the phone is on, named once location is allowed.
- App and `hk tone import`: import H&K's GrandMeister Deluxe 40 preset files (`.gm40memory`) as tones.
- App and `hk lock`: lock the delay time so a preset recalled from the footswitch keeps the tempo; the app shows the delay time in BPM too.
- App and `hk trim`: a volume trim moves every recalled preset's and channel's volume by the same amount, standing in for Master; a preset recalled from the app goes behind the mute so its own volume never sounds.
- App and `hk song`: a song recalls the amp's presets on their beats at one tempo, with the delay time set to that tempo.
- App: the song has a tab of its own, with a tapped tempo, the preset names on its steps and a big beat count while it plays; the volume trim sits on a dial that lights when it trims a recall.
- App and `hk song play`: a song counts in a bar of clicks before its first step, unless its count-in is switched off.

### Fixes

- A real WMI-1 is found and reached: its ports are 48437 and 1470, and the link pings it so it stays up and passes on what the amp says.
- App: a phone on the WMI-1's own WiFi finds and reaches it while mobile data is on.
- App: asks for Android 17's local network access, without which it cannot reach the WMI-1.
- App: finds a WMI-1 that has been reset, which broadcasts on a different port.
- App: the address starts as 192.168.1.1, where a WMI-1 sits on its own WiFi.
- App: the amp picked before connecting is kept for the box the app then connects to.
- A WMI-1 that never answers is reported as such, with a hint to check the phone's WiFi.
