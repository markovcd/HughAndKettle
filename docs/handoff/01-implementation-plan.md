# 01. Implementation plan: from these foundations to an app on a phone driving an amp

- **Severity:** —
- **Status:** Open
- **Found:** 2026-10-11, at the first commit

This is the plan an agent follows, one phase per task, in order. Each phase says what it delivers,
what proves it, and what "done" means. The standing rules apply throughout: a behavior change lands
with a test that failed first; `hk` learns a thing before the app does; the changelog and the docs
move in the same commit; a shape decision gets an ADR; work asked for and not started goes to TODO.md.

## What exists

The foundations, built and tested on the JVM:

- `midi`: messages and a stream parser (`MidiStreamParser`) that tolerate everything a serial bridge
  can do to a byte stream.
- `amp`: `AmpModels` with the Black Spirit 200 and GrandMeister Deluxe 40 charts, `Control`,
  `ControlKind`, `Sectors`.
- `wmi`: `WmiDiscovery`, `WmiBroadcast`, `WmiLink`, `FakeWmi`, `WmiDefaults`.
- `cli`: `hk discover | send | listen | fake-wmi`.
- `app`: one Compose screen (`AmpPanel`, `PanelState`) that connects by address and sends controls.
- The gate (`gate.sh`, `gate.Dockerfile`), the release script, the two workflows, Dependabot,
  dependency locking, ktlint, Lint with warnings as errors.

## What is not known

Everything below "confirmed" in [docs/wmi-1.md](../wmi-1.md). In one line: the WMI-1 is almost
certainly an RN-171 serial-to-WiFi bridge speaking raw MIDI on TCP 2000 and announcing itself on
UDP 55555, and nobody has yet pointed `hk` at one. Also unknown: which amp the user owns, whether the
amp reports knob moves and preset changes on its MIDI Out, and how (if at all) presets are read back.

## Phase 0: confirm the link on hardware

**Who:** the user, with a laptop on the WMI-1's network, following the commands in
[docs/wmi-1.md](../wmi-1.md#how-to-confirm-it-phase-0). An agent prepares the build and reads the
output.

**Deliverables (agent, before the user runs anything):**

- [x] `hk listen <host[:port]> [--seconds N]`: opens the link and prints every message heard as a JSON
  line until the time is up or stdin closes. Test: against `FakeWmi.emit`.
- [x] `hk discover` prints the raw packet as hex (`--raw`) so an unexpected layout can be read.
- [x] `hk send` accepts `--keep-open N` to hold the link N seconds after sending, so a `*HELLO*` banner
  or a reply can be seen with `--verbose` on stderr.

**The user records,** in the table in `docs/wmi-1.md`:

- [ ] Whether `discover` hears the box, and the exact JSON line (device id, port, firmware).
- [ ] Whether `send ... cc 9 127` mutes the amp on port 2000; if not, the port or the failure.
- [ ] The box's address in access-point mode.
- [ ] What `hk listen` prints while a knob is turned, a channel is switched and a preset is changed on
  the amp, with the FSM footswitch and without. This is the single most valuable capture.
- [ ] Whether switching the channel within a preset changes the gain, EQ and volume the amp plays.
  The manuals say "your choice of channel determines the knob's assignment", which may mean each
  channel keeps values of its own; if so, `AmpState` forgets those values on a channel switch.
- [x] Which amp model it is: a GrandMeister Deluxe 40 (the user, 2026-10-12).

**Done when:** every "inferred" row in `docs/wmi-1.md` is "confirmed" or replaced, `WmiDefaults` and
`WmiBroadcast` match the hardware, and ADR 0002 is amended or superseded if the transport differs.

**If it is not TCP 2000:** `nmap -sT -sU -p 1-65535 <address>` from the laptop, then a capture of the
iPad app's traffic (`rvictl` on a Mac, `tcpdump -i rvi0`). The capture is the spec; the
`wmi` module is the only thing that changes.

## Phase 1: the link on the phone

**Deliverables:**

- [x] Discovery screen: the app listens for broadcasts (`WmiDiscovery` on `Dispatchers.IO`, holding
  a `WifiManager.MulticastLock` so the radio delivers broadcasts), lists each box by device id and
  address, and connects on tap. Manual address entry stays as the fallback.
- [x] The link survives the app: a connected link is held in a foreground service with a
  notification ("Connected to WMI-1_A1B2C3"), so turning the screen off on stage does not drop it.
  ADR: a service over a bound ViewModel, and why.
- [x] Reconnect: a lost link retries with backoff until cancelled, and the panel shows the state
  (connecting, connected, retrying, refused). "Refused" says in words that something else, such as the
  iPad app, may hold the box's single connection.
- [x] The last box used is remembered (DataStore preferences, one key, versioned per the saved-data
  rule) and offered first.
- [x] The link's state is a pure class in `wmi` or `amp` (`LinkState`, a state machine with a test per
  transition), and the service and the panel both render it. Nothing about retries lives in Android code.

**Tests:** state machine transitions on the JVM; `PanelState` against `FakeWmi`; one Compose test on
Robolectric that the discovery list shows a broadcasting fake (this is where Robolectric enters the
build, in a commit of its own).

**Done when:** on a phone, the app finds the real box, connects, survives screen-off, and recovers
from the box being power-cycled.

## Phase 2: the amp's state

Shaped by what phase 0 heard.

**Deliverables:**

- [x] `AmpState`: an immutable snapshot of every control's value and the current preset, replaced
  whole on every change (the "immutable, then swapped" rule). Sent controls update it optimistically;
  heard messages update it authoritatively.
- [x] Preset selection: a program change for presets 1 to 128, shown as the amp shows them (bank and
  slot if the amp does; the manual says 32 banks of 4 on the GrandMeister). A tap on the preset opens a
  grid of all of them.
- [ ] If the amp reports state (phase 0 says): the panel follows the amp when a knob is turned on it.
  Built against the fake: a control the amp moves follows and stays outlined until the panel sets it.
  If presets come back as system exclusive: a `SysExPreset` codec in `amp` with a captured dump as
  the fixture, and `hk preset dump <n>`.
- No channel-dependent controls. Both manuals (Black Spirit 200 and GrandMeister Deluxe 40, chapters 2
  and 3) say all four channels share one set of knobs with "full access to all sound parameters on
  every channel", and the 8.3 charts mark nothing per channel, so the panel greys out nothing.
- [x] `hk set <host> <amp-id> <control-id> <value|on|off|option>` sets a control by name, and
  `hk state <host> <amp-id>` prints the snapshot as JSON (after a request, if the amp answers one).

**Tests:** `AmpState` transitions on the JVM; every control of every model round-trips through
`set` and `heard`; the sysex codec, if any, against the captured fixture.

**Done when:** the panel shows what the amp has, and `hk state` prints the same.

## Phase 3: tones on the phone

A sound kept on the phone is a **tone**, so "preset" keeps meaning one of the amp's 128 ([ADR 0008](../adr/0008-tones-are-files-of-raw-control-values-apart-from-the-amps-presets.md)).

**Deliverables:**

- [x] A tone file format: JSON, with `"version": 1`, the amp model id, a name, and control ids to
  values (the ids are forever; see ADR 0003). `kotlinx-serialization-json` is the one dependency this
  adds, in its own commit. The saved-data rule applies: a fixture of version 1 lives in the tests from
  the first commit, and every later version carries its step.
- [x] Store the current state as a named tone on the phone; recall it (which sends every control);
  rename, delete, reorder. The list is a file per tone in app storage.
- [x] Export and import as files (the system's save and open pickers, which reach Drive and other
  providers) so tones move between phones and to the user's backup. Import checks the version, refuses a newer one whole with a message, and reports
  a control id the model does not have rather than failing.
- [ ] Write a preset into one of the amp's 128 slots: recall it, then whatever the amp's store
  gesture is over MIDI (phase 0 or the manual says; if there is none, the UI says to press Store).
- [x] `hk tone list|save|recall|rename|move|delete|export|import` over the same code.

**Tests:** round trip through the format; loading the version-1 fixture; a newer version refused;
an unknown control reported.

**Done when:** a sound made on the amp can be saved on the phone, sent to another phone, and recalled.

## Phase 4: the panel people will use on stage

**Deliverables:**

- [x] Layout per amp from the table: sections (Amp, FX, Channel, Boost), big targets, a landscape
  layout for tablets, and the four-channel selector as four large buttons.
- [x] Dark theme by default, with the user's choice persisted; large text for the current preset.
- [x] Knobs show the amp's scale where the manual gives one (delay time 51 ms to 1360 ms in 128
  steps, tap tempo for it), the raw 0 to 127 elsewhere.
- [x] Haptics on a sector change; no animation on the sliders (latency is the feature).
- [x] Accessibility: every control has a content description from its name; tests find things by
  `testTag`, never by text.
- [x] A latency measurement: `hk` sends a burst and the fake WMI-1 stamps arrival; the number lands
  in `docs/performance.md` (the performance skill) so a later change can be compared.

**Tests:** Compose tests on Robolectric for the layout rules that matter (a selector shows every
option; a disabled control is not clickable); screenshots retaken by a test, never drawn by hand.

**Done when:** the user plays a rehearsal with it and does not reach for the iPad.

## Phase 5: more amps and the WMI-1 itself

**Deliverables:**

- [ ] GrandMeister 36 (and any other amp whose chart exists) as rows in `AmpModels`, from the manual,
  with the same tests. Amp choice in the app, remembered per box (done: the amp's name on the panel
  opens the list).
- [ ] Selector option names and order read off the hardware (the TODO item).
- [ ] WMI-1 configuration, replacing WMI Connect: read the module's settings and set access-point
  or client mode, the WiFi password and the device name. This speaks the RN-171 command mode (`$$$`
  with its guard time, `set wlan ...`, `save`, `reboot`) on the same TCP port and must never be
  mixed with a MIDI link; it is its own screen and its own `hk wmi` command, behind an "are you sure"
  because a wrong setting strands the box until a factory reset. ADR first.

**Done when:** a second amp model is verified by its owner, and a box can be moved from AP mode to
the home network from the app.

## Phase 6: release

**Deliverables:**

- [ ] The user makes the keys (`release/README.md`) and adds the five secrets the `Release` workflow
  names; `release/allowed_signers` is committed.
- [ ] A first `./release.sh` with real keys on the user's machine, then `v0.1.0` from the workflow.
- [ ] The README's install section: download the APK from Releases, verify, sideload. An ADR for a
  store (Play, F-Droid) if and when the user wants one.
- [ ] A layer cache for the gate image in CI, measured first (how long the SDK download costs per
  run) and taken only if it pays.

**Done when:** a tagged release installs on the user's phone from the GitHub release page and the
checksum verifies with the two lines in `release/README.md`.

## Cross-cutting

- **Latency budget:** a knob move reaches the amp within 30 ms on the local network. The parser and
  link allocate nothing per message beyond the message; measure before optimizing.
- **Security:** the app talks to the local network only; no telemetry, no analytics, no crash
  reporting that leaves the phone. The WiFi password, when phase 5 handles it, is typed, sent to the
  box, and not stored.
- **Honest unknowns:** anything a manual does not state and the hardware has not shown stays a
  placeholder or a TODO item, never a guess in code.
- **Hacks become features:** a session that needs a throwaway program to answer a question about the
  system ends by proposing the `hk` command that would have answered it.
