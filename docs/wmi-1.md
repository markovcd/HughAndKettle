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
| The box contains a Roving Networks RN-171 WiFly module | confirmed | its broadcast names its firmware, `wifly-EZX Ver: 4.41`; the label carries FCC ID T9J-RN171 |
| WMI Connect is the bridge: it holds the TCP link and offers the box to amp apps as a CoreMIDI port | inferred | the bundle id `HKMidiBridge`; the App Store text says the box works with "any app for iPad that supports CoreMIDI" |
| A raw TCP server on port **1470** bridges to the module's serial port, which is the MIDI port | confirmed on hardware, 2026-10-13 | `hk send 192.168.1.1:1470 cc 9 127` muted a GrandMeister; port 2000 refused the connection |
| The box broadcasts a 110-byte status packet to UDP 255.255.255.255:**48437** every **2 s**, from port 1470, carrying its device id and TCP port | confirmed on hardware, 2026-10-13 | packet capture on the laptop; `hk discover --port 48437 --raw` decoded `WMI-1_Network_a724e7`, port 1470. The packet: `44 B7 D0 A7 24 E7 01 00 05 BE 00 00 00 CD 0B DD 0D 16 54 69 6D 65 20 4E 4F 54 20 53 45 54 00 00 77 69 66 6C 79 2D 45 5A 58 20 56 65 72 3A 20 34 2E 34 31 20 42 75 69 6C 64 3A 20 72 57 4D 49 2D 31 5F 4E 65 74 77 6F 72 6B 5F 61 37 32 34 65 37 00 32 33 3A 35 36 00 …` |
| In access-point mode the box is at 192.168.1.1 and hands out addresses in 192.168.1.0/24 | confirmed on hardware | the laptop got 192.168.1.11 with 192.168.1.1 as its gateway |
| The box has a user-set name, shown in discovery and used as its access point's network name | seen in the vendor's screenshots | WMI Connect lists a found box as "GrandMeister 40" and shows the same as its wireless network; the broadcast's device id field is the likely carrier |
| The box drops a client that has sent nothing for about 30 s | confirmed on hardware | `hk listen --reconnect` lost the link every 29 to 30 s; a client sending a byte a second kept it for minutes |
| The box passes what the amp sends to the client only while the client keeps sending; what arrives in between is held and delivered in a burst on the client's next byte | confirmed on hardware | a 28 s listen heard nothing while knobs were turned; the next `hk send` received every turn at once. With a one-byte ping each second the reports arrived live |
| The amp echoes everything it receives on its MIDI Out/Thru, including a lone `FD` | confirmed on hardware | every `hk send` got its own message back, and every ping came back |
| A byte goes laptop to box to amp and back in about 11 ms | measured, 2026-10-13 | 100 `FD` echoes over the box's own access point, Windows laptop: min 8.7, median 11.1, p95 13.6, max 31.8 ms |
| WMI Connect sets the box's name, access point channel, WPA2-PSK password and home network | seen in the vendor's screenshots; how is unknown | the Configuration screen. The WiFly module is configured in its `$$$` command mode, over TCP or serial |
| The module sends `*HELLO*` to a new TCP client and `*OPEN*`/`*CLOS*` to the serial side unless configured not to | ruled out for the TCP side | no banner arrived on any of a dozen connections |
| Only one TCP client is served at a time | inferred from the module | RN-171 serves a single TCP connection |
| The amps use MIDI control changes for every control and program changes for presets | confirmed by the manuals | Black Spirit 200 manual 8.3; GrandMeister Deluxe 40 manual 8.3 |
| The amp reports every control it moves, by hand, footswitch or MIDI, as a system exclusive naming the controller | confirmed on hardware, GrandMeister Deluxe 40, firmware 1.16 | every knob, switch and selector turned on the panel came back; see [What the amp reports](#what-the-amp-reports) |
| A channel switch or preset recall is followed by a 47-byte block of the amp's current values | seen on hardware; mostly decoded | see [The value block](#the-value-block) |
| The RTP-MIDI / Apple network MIDI protocol is **not** what the box speaks | inferred, strong | TCP in the manual; no Bonjour or session wording anywhere; the module has no RTP-MIDI firmware |

## After a full reset

Holding SET for ten seconds while the amp powers up resets the box. Read off it on 2026-10-13: the
access point stays at 192.168.1.1 and TCP 1470, its password is gone, and the status broadcast moves to
UDP 55555, the RN-171's own default, still naming port 1470. The app listens on both ports.

## What the amp reports

Read off a GrandMeister Deluxe 40 (amp firmware 1.16, identity reply family `10 00`, member `09 00`).
A control the amp moves comes back as

```text
F0 00 20 44 00 10 00 0A 00 04 <controller> <hi> <lo> <check> F7
```

where the value is `hi * 128 + lo` and `check` is `0A xor controller xor hi xor lo`. Knobs and the
modulation type report 0 to 255, twice MIDI's resolution; switches and the other selectors report
0 to 127. `ControlReport` decodes it and `AmpState` takes it in, halving the 256-step ones.

- The controllers are the MIDI chart's, with one exception: the panel's Gain and Volume report as
  56 and 57 (the chart's "hard" variants), and so does a control change on 20 or 7. A change on 20
  does not land where a change on 56 does (20 at 30 reported 8 of 255; 56 at 100 reported 201), so
  the app's Gain and Volume send on 56 and 57 and leave 20 and 7 alone. Master is not on MIDI.
- Modulation is one knob, 12, whose sweep runs chorus, flanger, phaser, tremolo from the bottom.
  Heard on 2026-10-22: the panel's clearest chorus, flanger, phaser and tremolo reported 0, 73, 167
  and 255, and a change on 12 at the middle of each quarter of 0..127 (15, 47, 79, 111) sounds as
  that effect.
- The power soak, 30, runs off, 1 W, 5 W, 20 W, 40 W from the bottom (reported 0, 32, 64, 95, 127).
- Dly Time is 4 and Feedback 27. A change on 4 at 64 reported 129 of 255, so the sweep looks even.
- Noise gate (63) and FX loop (55) report as their switches. Turning the noise gate's intensity
  knob reports nothing, so like Master it is not on MIDI.
- The cab emulation controls on the back report nothing, as the MIDI chart's lack of them says.
- The footswitch's switches send the chart's controllers (modulation 52, delay 53, boost 64), and the
  box passes them on to the client ahead of the amp's report. Tap tempo sends delay time (4) with
  controller 36 at 0 or 64, which the manual does not list. 36 is 4's low byte, as MIDI pairs
  controllers 0 to 31 with 32 to 63: the amp reports `2 * <4> + 1` when 36 is 64 and `2 * <4>` when
  it is 0, so the two together reach all 256 steps of the delay time. A change on 4 at 50 then 36 at
  64 reported 101.
- MIDI clock (`F8`, with start and stop) does nothing: 20 seconds of it at 40 BPM left the delay
  time where it was, by ear and in the amp's reports.
- Two links opened within a second or two of each other once lost the second link's messages: the
  box neither echoed them nor did the amp report them. Seen once, not reproduced.

## Storing a preset

Pressing STORE on the amp reports controller 80 at 127 and flashes. A footswitch button then sends
its program change (bank 32 is programs 124 to 127), the amp reports 80 at 0, sends the value block
for the preset just stored, and reports controller 16 at the preset's number.

A program change from the client stores just as the footswitch's does: on 2026-10-26, STORE pressed
on the amp and then a preset picked in the app stored the panel there, with no footswitch on the amp.
Under a volume trim of +20 the preset kept its own volume, not the trimmed one, confirmed by a recall
from an app started afresh; why the amp stores the untrimmed volume is not known.

Sending controller 80 at 127 does not arm a store: the amp neither reports it nor stores on the
program change that follows, which recalls the preset as usual. How the iPad apps store is unknown,
though they do: the GrandMeister Deluxe 40 Remote app's App Store listing and the manual's chapter 2.5
say it uploads presets to the amp and moves them between slots.

## The value block

After a channel switch, a preset recall, or a client's first bytes after the amp starts, the amp
sends

```text
F0 00 20 44 00 10 00 0A 00 40 <a> <b> <preset> <15 values as hi lo> <switches> <flags> <check> F7
```

Read on 2026-10-13 by setting known values, switching to Crunch and back to Clean:

| Pair | Control | Steps |
|---|---|---|
| 1 | gain (56) | 256 |
| 2 | bass (21) | 256 |
| 3 | mid (22) | 256 |
| 4 | volume (57) | 256 |
| 5 | treble (23) | 256 |
| 6 | resonance (24) | 256 |
| 7 | presence (25) | 256 |
| 8 | reverb volume (29) | 256 |
| 9 | delay volume (28) | 256 |
| 10 | delay time (4) | 256 |
| 11 | delay feedback (27) | 256 |
| 12 | modulation intensity (1) | 256 |
| 13 | modulation type (12) | 256 |
| 14, 15 | not yet known, 0 in every capture | |

`ValueBlock` decodes it and `AmpState` takes in its knobs and preset.

- Pairs 1 to 7 belong to the channel: Crunch's came back as its own, and Clean's kept the values set
  on it before the switch. Pairs 8 to 13 are shared by every channel.
- The flags byte, read on 2026-10-22 by changing one setting at a time: bits 0 and 1 are the channel
  (0 Clean to 3 Ultra), bit 2 is boost, bit 3 the FX loop, and bits 4 to 6 the power soak's step, 0
  for Speaker Off, then 1 W, 5 W, 20 W and 4 for full power. Bit 0 of `<switches>` is the noise gate;
  its other bits were 0 throughout. Boost is kept per channel, so Crunch's flags kept its boost while
  Clean's was switched; the noise gate, the FX loop and the power soak are shared. The effect switches
  are not in the block: turning modulation or delay on or off changed no byte of it. `<preset>` is the preset counted
  from 0 after a store or recall; `<a> <b>` were `00 00` then, and `7F 7F` after a channel switch,
  likely meaning the panel no longer matches a stored preset. A recall from the footswitch sends the
  block alone, with no separate report of the delay time, though one recall reported the delay and
  modulation switches (53 and 52 at 127) just ahead of it. The block comes about 120 ms after the
  program change, so under a volume trim a footswitch recall plays its stored volume that long, a
  short jump heard as acceptable. The check is `4E` xor every byte from
  `<a>` to the flags.
- Two stored presets carried 0 for delay volume and modulation intensity, which had been 160 and 201
  just before: likely how an effect switched off is stored. H&K's own preset files agree: they keep
  no effect switches, only levels, and a level of 0 in every preset whose effect is off.
- Presence set to 50 reported 127 of 255 and stored 127, not 100: unexplained.
- On the GrandMeister Deluxe 40 a change on 26 (modulation speed in the chart) reports as 12 at half
  its value, so speed and type look like the one modulation knob.

Without a WMI-1, a USB MIDI cable reaches the amp directly; [cable.md](cable.md) says how, and how
to find out what the amp sends.

## What the code assumes, and where

- `WmiDefaults`: TCP port 1470, UDP port 48437, 2-second interval, 5-second connect timeout, a one-byte
  ping (`FD`, an undefined real-time byte every receiver ignores) every second.
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
empty device id, a port that is not 1470), run it again with `--raw` and paste the packet's hex. Then,
with that address:

```sh
cli/build/install/hk/bin/hk send <address>:<port> cc 9 127 --keep-open 3 --verbose   # mute on
cli/build/install/hk/bin/hk send <address>:<port> cc 9 0       # mute off
cli/build/install/hk/bin/hk send <address>:<port> pc 3         # preset 4
```

Expected: the amp mutes, unmutes and changes preset. The first command holds the link three seconds
and prints on stderr every byte the box sends, hex and text, so a `*HELLO*` banner shows up there if
the module sends one. If `discover` hears nothing, read the laptop's
gateway address in access-point mode and try `send` against it on 1470; if that is refused, the port
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
