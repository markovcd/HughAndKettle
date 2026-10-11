# The WMI-1 link

What is known about the box the app talks to, where each fact comes from, and what is still to be
confirmed on hardware. Phase 0 of the [implementation plan](handoff/01-implementation-plan.md) turns
every "inferred" below into "confirmed" or replaces it.

## What the WMI-1 is

Hughes & Kettner's WMI-1 is a WiFi MIDI interface powered by the amp's 7-pin MIDI socket. It has MIDI
In 1 (from the amp's Out), MIDI Out (to the amp's In) and MIDI In 2 (for a footswitch, merged). It runs
either as its own access point (`WMI-1_Network_XXXXXX`, password `hknetwork`, XXXXXX from its MAC) or
as a client on a home router joined by WPS. The iPad side is two apps: **WMI Connect**, which finds
and configures the box, and an amp app (GM36 Remote, GrandMeister Deluxe 40, Black Spirit 200), which
talks CoreMIDI.

| Fact | Status | Source |
|---|---|---|
| The companion app talks to the box over TCP | confirmed by the manual | WMI-1 manual: "the WPS/TCP LED stays on when the WMI Connect app is connected to the WMI-1 via TCP" |
| The box contains a Roving Networks RN-171 WiFly module | inferred, strong | Label text in the manual's drawing decodes to "Contains FCC ID: T9J-RN171"; T9J-RN171 is the RN-171's FCC ID |
| A raw TCP server on port 2000 bridges to the module's serial port, and the serial port is the MIDI port at 31250 baud | inferred from the RN-171 defaults | WiFly command reference DS50002230B, "set ip localport", default 2000; "set uart raw" allows 31250 |
| The box broadcasts a 110-byte status packet to UDP 255.255.255.255:55555 every 7 s, carrying its device id and TCP port | inferred from the RN-171 defaults | WiFly command reference, 3.5.4.3 and figure 3-12 |
| In access-point mode the box is at a fixed address | inferred, address unknown | the RN-171 soft AP serves DHCP; its own address is to be read off the phone's gateway |
| The module sends `*HELLO*` to a new TCP client and `*OPEN*`/`*CLOS*` to the serial side unless configured not to | inferred | WiFly command reference, "set comm remote/open/close" |
| Only one TCP client is served at a time | inferred from the module | RN-171 serves a single TCP connection |
| The amps use MIDI control changes for every control and program changes for presets | confirmed by the manuals | Black Spirit 200 manual 8.3; GrandMeister Deluxe 40 manual 8.3 |
| Whether the amp reports knob moves and preset changes on its MIDI Out, and whether presets are read back by system exclusive | unknown | the iPad apps display amp state, so something flows back; the manuals do not say what |
| The RTP-MIDI / Apple network MIDI protocol is **not** what the box speaks | inferred, strong | TCP in the manual; no Bonjour or session wording anywhere; the module has no RTP-MIDI firmware |

## What the code assumes, and where

- `WmiDefaults`: TCP port 2000, UDP port 55555, 7-second interval, 5-second connect timeout.
- `WmiBroadcast`: the 110/120-byte layout (AP MAC at 0, RSSI at 7, port at 8 big-endian, firmware
  string at 32, device id at 60).
- `MidiStreamParser`: text banners and any other stray data bytes are dropped; running status and
  interleaved real-time bytes are handled; a system exclusive is capped at 64 KiB.
- `WmiLink`: every message is sent whole, so `$$$` (the module's command-mode escape) can never be
  sent on its own.

## How to confirm it (phase 0)

With a laptop on the WMI-1's network (join `WMI-1_Network_XXXXXX`, or the home network the box is a
client of) and the amp on:

```sh
./gradlew :cli:installDist
cli/build/install/hk/bin/hk discover --seconds 20
```

Expected: one JSON line naming the box, its address and the TCP port. Then, with that address:

```sh
cli/build/install/hk/bin/hk send <address>:<port> cc 9 127     # mute on
cli/build/install/hk/bin/hk send <address>:<port> cc 9 0       # mute off
cli/build/install/hk/bin/hk send <address>:<port> pc 3         # preset 4
```

Expected: the amp mutes, unmutes and changes preset. If `discover` hears nothing, read the laptop's
gateway address in access-point mode and try `send` against it on 2000; if that is refused, the port
has been changed or the transport is not what is inferred, and a packet capture of the iPad app
(`tcpdump` on a Mac with the iPad's traffic mirrored by `rvictl`) is the next step. Record the outcome
in the table above, in the same commit as any constant it changes.

Then listen for what the amp says back: `hk fake-wmi` cannot do this against the real box, so the
next command to add is `hk listen <address>:<port>`, which prints every message heard while a person
turns a knob and changes a preset on the amp. What it prints decides phase 2.
