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
- App: dark by default, with light or the phone's own setting a tap away, and remembered.
- App: a tablet in landscape shows the controls in a pane of their own, six knobs to a row.
- `hk latency` measures how long a control change takes to reach a fake WMI-1 on the same machine.
- Delay time shows in milliseconds, takes a tapped beat in the app, and `hk set` takes `520ms`.
