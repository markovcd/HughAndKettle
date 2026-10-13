# The WMI-1 link

What is known about the box the app talks to, where each fact comes from, and what is still to be
confirmed on hardware. Phase 0 of the [implementation plan](handoff/01-implementation-plan.md) turns
every "inferred" below into "confirmed" or replaces it.

## What the WMI-1 is

Hughes & Kettner's WMI-1 is a WiFi MIDI interface powered by the amp's 7-pin MIDI socket. It has MIDI
In 1 (from the amp's Out), MIDI Out (to the amp's In) and MIDI In 2 (for a footswitch, merged). It runs
either as its own access point (`WMI-1_Network_XXXXXX`, password `hknetwork`, XXXXXX from its MAC, as
it leaves the factory) or as a client on a home router joined by WPS. The iPad side is two apps: **WMI
Connect** (bundle id `com.de.synergetik.HKMidiBridge`), which finds and configures the box and holds
the link, and an amp app (GM36 Remote, GrandMeister Deluxe 40, Black Spirit 200), which talks CoreMIDI.

WMI Connect's iOS binary cannot be read: the App Store serves it only to a signed-in Apple account,
and its code is FairPlay-encrypted. There is no Android build. Its App Store screenshots (version
1.2.0) are the source cited below as "WMI Connect screenshots".

| Fact | Status | Source |
|---|---|---|
| The companion app talks to the box over TCP | confirmed by the manual | WMI-1 manual: "the WPS/TCP LED stays on when the WMI Connect app is connected to the WMI-1 via TCP" |
| The box contains a Roving Networks RN-171 WiFly module | inferred, strong | Label text in the manual's drawing decodes to "Contains FCC ID: T9J-RN171"; T9J-RN171 is the RN-171's FCC ID. WMI Connect screenshots show the box's MAC as `00:06:66:…`, the Roving Networks OUI |
| WMI Connect is the bridge: it holds the TCP link and offers the box to amp apps as a CoreMIDI port | inferred | the bundle id `HKMidiBridge`; the App Store text says the box works with "any app for iPad that supports CoreMIDI" |
| A raw TCP server on port 2000 bridges to the module's serial port, and the serial port is the MIDI port at 31250 baud | inferred from the RN-171 defaults | WiFly command reference DS50002230B, "set ip localport", default 2000; "set uart raw" allows 31250 |
| The box broadcasts a 110-byte status packet to UDP 255.255.255.255:55555 every 7 s, carrying its device id and TCP port | inferred from the RN-171 defaults | WiFly command reference, 3.5.4.3 and figure 3-12 |
| In access-point mode the box is at 192.168.1.1 | seen in the vendor's screenshots | WMI Connect screenshots list the box at 192.168.1.1 in "Stand alone AP" mode, and at a DHCP address (192.168.80.43) as a home network client |
| The box has a user-set name, shown in discovery and used as its access point's network name | seen in the vendor's screenshots | WMI Connect lists a found box as "GrandMeister 40" and shows the same as its wireless network; the broadcast's device id field is the likely carrier |
| WMI Connect sets the box's name, access point channel, WPA2-PSK password and home network | seen in the vendor's screenshots; how is unknown | the Configuration screen. The WiFly module is configured in its `$$$` command mode, over TCP or serial |
| The module sends `*HELLO*` to a new TCP client and `*OPEN*`/`*CLOS*` to the serial side unless configured not to | inferred | WiFly command reference, "set comm remote/open/close" |
| Only one TCP client is served at a time | inferred from the module | RN-171 serves a single TCP connection |
| The amps use MIDI control changes for every control and program changes for presets | confirmed by the manuals | Black Spirit 200 manual 8.3; GrandMeister Deluxe 40 manual 8.3 |
| Whether the amp reports knob moves and preset changes on its MIDI Out, and whether presets are read back by system exclusive | unknown | the iPad apps display amp state, so something flows back, and a WMI Connect screenshot counts 42 messages in with none sent out; the manuals do not say what |
| The RTP-MIDI / Apple network MIDI protocol is **not** what the box speaks | inferred, strong | TCP in the manual; no Bonjour or session wording anywhere; the module has no RTP-MIDI firmware |

Without a WMI-1, a USB MIDI cable reaches the amp directly; [cable.md](cable.md) says how, and how
to find out what the amp sends.

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

Expected: one JSON line naming the box, its address and the TCP port. If a line looks wrong (an
empty device id, a port that is not 2000), run it again with `--raw` and paste the packet's hex. Then,
with that address:

```sh
cli/build/install/hk/bin/hk send <address>:<port> cc 9 127 --keep-open 3 --verbose   # mute on
cli/build/install/hk/bin/hk send <address>:<port> cc 9 0       # mute off
cli/build/install/hk/bin/hk send <address>:<port> pc 3         # preset 4
```

Expected: the amp mutes, unmutes and changes preset. The first command holds the link three seconds
and prints on stderr every byte the box sends, hex and text, so a `*HELLO*` banner shows up there if
the module sends one. If `discover` hears nothing, read the laptop's
gateway address in access-point mode and try `send` against it on 2000; if that is refused, the port
has been changed or the transport is not what is inferred, and a packet capture of the iPad app
(`tcpdump` on a Mac with the iPad's traffic mirrored by `rvictl`) is the next step. Record the outcome
in the table above, in the same commit as any constant it changes.

Then listen for what the amp says back, while a person turns a knob, switches a channel and changes a
preset on the amp, once with the footswitch plugged into MIDI In 2 and once without:

```sh
cli/build/install/hk/bin/hk listen <address>:<port> --verbose > heard.jsonl
```

It prints one JSON line per message heard, with the milliseconds since the link opened, until stdin
closes (Ctrl-D) or `--seconds N` pass; `--verbose` adds the raw bytes on stderr. What it prints
decides phase 2.
