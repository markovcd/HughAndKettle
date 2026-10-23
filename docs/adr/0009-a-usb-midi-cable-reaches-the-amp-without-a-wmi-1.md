# 9. A USB MIDI cable reaches the amp without a WMI-1

Date: 2026-10-13

## Context

The WMI-1 does nothing but carry MIDI between WiFi and the amp's MIDI sockets (docs/wmi-1.md).
Without one, the app and `hk` cannot reach the amp at all, and the user's box is missing. A
class-compliant USB MIDI cable plugged into the phone, with its 5-pin plugs in the amp's MIDI In and
MIDI Out/Thru, carries the same messages. Android has had a MIDI service (`android.media.midi`) since
API 23 that exposes such a cable with no permission prompt, and the JDK carries `javax.sound.midi`.

## Decision

- The cable is a second way to the amp beside the WMI-1, not a replacement. `LinkHolder` holds one
  or the other; taking one drops the other. Everything above it (the panel, tones, presets, the amp
  state) sends and hears `MidiMessage`s as before and does not know which way they went.
- The cable has its own `CableState` (none, opening, open, failed) rather than a place in
  `LinkState`, whose states are about addresses, retries and the box's one TCP client. A cable is not
  retried: it is there while it is plugged in.
- `CableJack` is the boundary to Android's MIDI service, so `LinkHolder` and the panel are tested
  with a stand-in on the JVM. `MidiCableJack` is the one class that touches `android.media.midi`.
- Only USB devices the app can send to are offered, so virtual ports other apps publish do not show
  up as cables. Messages heard on the cable go through the same `MidiStreamParser` as the WMI-1's.
- `hk cable` drives a cable plugged into a laptop through `javax.sound.midi`: list, send, listen.
  It is the quickest way to capture what the amp sends, which is still unknown.

## Consequences

- The app works with no WMI-1 at all, at the cost of a cable from the phone to the amp.
- The amp has one MIDI In, so the FSM-432 footswitch is unplugged while the cable is in. The WMI-1
  merges the two; the cable cannot.
- A cable pulled out mid-song is shown failed and is picked again by hand once plugged back in.
- The amp is not remembered per cable as it is per WMI-1; the panel keeps whichever amp is picked.

## Amendment (2026-10-23)

Below `ui`, a WMI-1 link and a cable are both an `AmpLink`: `WmiLink` in `wmi`, `JavaCableLink` in `midi`.
Every `hk` command that drives the amp takes `cable:<name>` where it takes a WMI-1's address, so the
tempo lock, the trim, songs and randomizing work over a cable from a laptop too. The desktop app opens its
cables through the same `JavaCableLink`. `hk listen --reconnect` stays a WMI-1's alone: a cable is not
retried.
