# 2. The WMI-1 is driven as a transparent TCP MIDI bridge, with first-party code and no MIDI library

Date: 2026-10-11

## Context

The obvious guess for a WiFi MIDI box is RTP-MIDI (Apple network MIDI). The WMI-1 is not that. Its
manual says the companion app, WMI Connect, is "connected to the WMI-1 via TCP", says nothing of a
network MIDI session, and the label text decodes to "Contains FCC ID: T9J-RN171", the Roving Networks
RN-171 WiFly module: a serial-to-WiFi bridge whose firmware opens a raw TCP server on port 2000 and
broadcasts a 110-byte status packet to UDP 55555 every seven seconds. WMI Connect's job is to find the
module, configure it (access point or client mode, password, name) and bridge its TCP byte stream to a
CoreMIDI port that the amp apps then use. [docs/wmi-1.md](../wmi-1.md) lists what is confirmed and what
is still inference.

Libraries considered: RTP-MIDI implementations (LeovR/rtp-midi, nmj), which speak a protocol the box
does not; ktmidi, which has no network transport; Android's `android.media.midi`, which has no network
transport either.

## Decision

The link is a TCP socket carrying raw MIDI bytes, written and parsed by first-party code:

- `MidiStreamParser` turns any chunking of bytes into messages, drops stray data bytes (the module can
  prefix text banners such as `*HELLO*`), honours running status and interleaved real-time bytes, and
  caps a system exclusive at 64 KiB.
- `WmiLink` opens the socket, sends every message whole with its status byte (so the module can never
  see `$$$`, its command-mode escape, on its own), and parses what comes back on a reader thread.
- `WmiDiscovery` listens for the status broadcast and reads the device id and the TCP port out of it.
- Defaults (port 2000, UDP 55555, seven seconds) live in `WmiDefaults`, the one place that knows them.

No MIDI or networking dependency is added. Until the hardware confirms the ports and the broadcast,
the plan's phase 0 is a verification step with the `hk` command line, and anything it disproves is
changed in `WmiDefaults` and `WmiBroadcast` only.

## Amendment, 2026-10-13: what the hardware does

Phase 0 confirmed the transport and corrected the defaults. The box serves the bridge on TCP 1470 and
broadcasts to UDP 48437 every two seconds. It drops a client that has sent nothing for about 30
seconds, and passes on the amp's messages only while the client keeps sending, so `WmiLink` pings it
with one `FD` byte a second, an undefined real-time byte MIDI receivers ignore. The amp echoes the
ping, and the link drops the echo. [docs/wmi-1.md](../wmi-1.md) has the captures.

## Consequences

- A few hundred lines replace a library that would not have fit anyway, and every byte on the wire is
  covered by tests against a fake WMI-1 that speaks the same bytes.
- The RN-171 serves one TCP client at a time, so the iPad app and this app cannot be connected at once.
  The app must show a refused connection as "something else is connected", not as a bug.
- Configuring the module itself (its WiFi mode and password, what WMI Connect does) is a separate,
  later feature; it talks to the same module through its command mode and is kept out of the MIDI link.
- If phase 0 shows a different transport, this decision is superseded by a new ADR and the
  `wmi` module is the only thing that changes.
