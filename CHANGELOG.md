# Changelog

User-facing changes only, one terse bullet per feature (the `changelog` skill). The `Unreleased`
section becomes a version's section when `release.sh` ships it.

## Unreleased

### Features

- `hk` command line: find a WMI-1 on the network, send it a message, listen to what the amp says, or run a fake one.
- App: connect to a WMI-1 by address and move the controls of a Black Spirit 200.
- A dropped link to a WMI-1 comes back by itself (`hk listen --reconnect`).
- App: lists the WMI-1s on the network and connects to one with a tap.
- App: the link stays up with the screen off, shown in a notification that can end it.
