# The USB MIDI cable

A class-compliant USB MIDI cable reaches the amp without a WMI-1: USB-C into the phone (or a
laptop), the 5-pin plugs into the amp's MIDI sockets. The app and `hk cable` send the same messages
as over a WMI-1; ADR 0009 says how the two sit side by side.

## Wiring

- The cable's plug marked **OUT** goes into the amp's **MIDI In** (7-pin; a 5-pin plug fits, the
  manual says so). This is the plug the phone talks through.
- The plug marked **IN** goes into the amp's **MIDI Out/Thru**. This is where the amp is heard, if it
  says anything.
- If the amp ignores everything, swap the two plugs. Some cables mark a plug by the socket it goes
  into rather than by what it does; a swap cannot harm anything.
- The amp has one MIDI In, so the FSM-432 footswitch is unplugged for as long as the cable is in.
- The amp leaves the factory on MIDI channel 1 with Omni on, so it takes every MIDI channel; the app
  sends on 1.

## The test

It stands in for phase 0 of the [plan](handoff/01-implementation-plan.md#phase-0-confirm-the-link-on-hardware)
while the WMI-1 is missing. The amp on, in Play, and a guitar plugged in.

### On the phone

Install the latest build, plug the cable in, open the app.

1. The link card lists the cable under "USB MIDI cables plugged in". Tap it; the status reads
   "Connected over USB to …".
2. Open the settings menu (⋮) and pick the GrandMeister Deluxe 40.
3. Move Volume, then Gain. Does the amp follow?
4. Switch the channel with the selector, then switch Delay and Reverb on and off.
5. Open the preset grid and pick preset 4. Does the amp change preset?
6. Turn a knob on the amp itself and change its channel by hand. Does any control on the panel get
   outlined ("Moved on the amp")? That is the amp saying something on its MIDI Out.
7. Drag Gain quickly back and forth. Does the knob stutter or jump back? It would mean the amp echoes
   what it is sent.
8. On one preset, switch channel and back. Do Gain, the EQ and Volume come back where they were,
   or does each channel keep values of its own?
9. Turn the screen off for a minute, back on, and move Volume. Then pull the cable out: the status
   should say the cable was unplugged.

### On a laptop, to capture what the amp sends

With the cable in the laptop instead, from a checkout of the repo (JDK 21 on `PATH` or in
`JAVA_HOME`):

```sh
./gradlew :cli:installDist
hk=cli/build/install/hk/bin/hk
$hk cable list                       # one JSON line per MIDI port; note the cable's name
$hk cable send DOREMIDI cc 9 127     # mute on: the amp goes quiet
$hk cable send DOREMIDI cc 9 0       # mute off
$hk cable send DOREMIDI pc 3         # preset 4
$hk cable listen DOREMIDI > heard.jsonl
```

Any part of the cable's name will do in place of `DOREMIDI`. While `listen` runs, turn a knob on the
amp, switch its channel, change its preset, then press Ctrl-D. `heard.jsonl` is one JSON line per
message heard, with the milliseconds since it started; an empty file means the amp sends nothing of
its own.

### What to send back

- For steps 1 to 9 on the phone: yes or no for each, and the status line's words wherever it shows
  a failure.
- From the laptop, if it was used: the `hk cable list` lines and `heard.jsonl`.
- Which plug went where, if the swap was needed.

The answers settle the "unknown" rows of [docs/wmi-1.md](wmi-1.md) about the amp, the channel
question in phase 0, and what phase 2 builds on.
