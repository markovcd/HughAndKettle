# Glossary

One word for each thing, and the same word everywhere a person reads: the UI,
the docs, ADRs, comments, test names and commit messages.

- **Say** is the word. Use it in prose, whatever the code calls the thing.
- **Code** is the name in source. Where it differs from **Say**, it differs
  because renaming it would break something persisted or public (a file
  format, an API). A comment on it still uses **Say**.
- **Not** lists words that must not stand for the thing, and why where it
  helps.

Prefer the domain's words over the framework's.

| Say | Means | Code | Not |
|---|---|---|---|
| **amp** | The Hughes & Kettner amplifier being driven. | `AmpModel` for the kind of amp, `amp` for one | amplifier, head, device, unit |
| **amp model** | One kind of amp and its MIDI chart: Black Spirit 200, GrandMeister Deluxe 40, GrandMeister 36. | `AmpModel`, `AmpModels` | profile, product |
| **WMI-1** | Hughes & Kettner's WiFi MIDI interface box that plugs into the amp's MIDI ports. Always spelled `WMI-1`. | `Wmi*` | dongle, wifi thingy, interface box, adapter, the wireless |
| **link** | An open TCP connection between the app (or `hk`) and a WMI-1. | `WmiLink` | connection, session (a MIDI word), socket |
| **link state** | Where the link stands: idle, connecting, connected, or retrying after a failure. | `LinkState`, kept by `LinkKeeper` | status (the WMI-1's broadcast), connection state |
| **discovery** | Hearing a WMI-1's status broadcast on the network. | `WmiDiscovery`, `WmiBroadcast` | scan, pairing |
| **fake WMI-1** | The in-process stand-in used by tests and by `hk fake-wmi`. | `FakeWmi` | mock, simulator, emulator (Android's word) |
| **control** | One thing on the amp the app can turn or switch: a knob, a switch or a selector. | `Control` | parameter, param, setting, knob (when it is not a knob) |
| **knob** | A control swept over 0 to 127. | `ControlKind.Knob` | slider, fader, pot |
| **switch** | A control that is off or on. | `ControlKind.Switch` | toggle, button |
| **selector** | A control whose 0 to 127 is cut into a few equal sectors, one per option. | `ControlKind.Selector` | enum, mode, picker |
| **sector** | One slice of a selector's 0 to 127. | `Sectors` | range, bucket, zone |
| **scale** | What a knob's 0 to 127 means on the amp, where the manual gives it: delay time from 51 ms to 1360 ms. | `Scale` | range, mapping, curve |
| **tap tempo** | Setting a time knob by tapping its beat. | `TapTempo` | tap delay, BPM |
| **controller** | The MIDI control change number a control moves on, 0 to 127. | `controller` | CC (fine in a table), parameter number |
| **MIDI channel** | The 1 to 16 a message is addressed on. Always with the word MIDI, because the amp has channels too. | `MidiChannel` | channel (alone) |
| **channel** | The amp's Clean, Crunch, Lead or Ultra. | `channel` control | mode, voice, sound |
| **preset** | One of the amp's 128 stored sounds, recalled by a program change. | `preset`; `program` for the MIDI number | patch, program (in prose), slot, bank entry |
| **tone** | A named set of control values kept on the phone or by `hk`, apart from the amp's presets, and recalled by sending each value. | `Tone`, `ToneFile`, `ToneStore` | preset (the amp's), patch, sound, favorite |
| **tone shelf** | The panel's list of tones, with save, recall, rename, move, delete, export and import. | `ToneShelf`, `ToneShelfCard` | library, bank (an amp word), favorites |
| **message** | One complete MIDI message. | `MidiMessage` | event, packet (a network word), command |
| **amp state** | What the app knows the amp is set to: a value per control and the current preset, replaced whole on every message. | `AmpState` | status, settings, snapshot (fine in prose for one printed copy) |
| **MIDI chart** | A manual's table of controllers per amp model. | the `AmpModels` tables | implementation chart, CC map |
| **panel** | The app's screen for one amp: link row and one row per control. | `AmpPanel`, `PanelState` | dashboard, view, page |
| **hk** | The command line, one command per thing the app can do. | `cli` module, `Cli` | tool, script |
| **the gate** | `./gate.sh`: the container build that is the only definition of passing. | `gate.sh`, `gate.Dockerfile` | CI, the pipeline, the build (alone) |
