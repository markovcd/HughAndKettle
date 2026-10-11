# Design

The record of what the design session decided, so a later screenshot test and a later session can be
compared against it. The source is the canvas at
<https://claude.ai/artifact/YVnSyknwXv1ipS6gtUE85n> (twelve artboards: icon, colors, type, the panel
on a phone in both schemes and on a tablet, the link row in six states, the knob, switch and selector
rows, the discovery list, the preset strip, and notes). It answers the brief in
[docs/handoff/02-design-brief.md](../handoff/02-design-brief.md).

What lands where:

| Design | In the app |
|---|---|
| Icon layers ([icon/](icon/)) | `app/src/main/res/drawable/ic_launcher_*.xml`, `drawable-anydpi/ic_launcher.xml` |
| Colors ([colors.md](colors.md)) | `app/src/main/kotlin/.../app/theme/Palette.kt`, `HughAndKettleTheme.kt` |
| Type ([type.md](type.md)), fonts under [fonts/](fonts/) | `app/src/main/res/font/`, `.../app/theme/Type.kt` |
| Components and states | `.../app/AmpPanel.kt` and the row composables beside it |

## The idea

One mark: a kettle that is an amp head, with a carry handle, a spout and one large knob with a
notch. Amber (`#FFB547`) on a near-black warm ground. The knob is also the one custom-drawn control
in the app, so the icon and the panel share a shape. Nothing of the maker's: not their logo, not
their lettering, not the blue-lit front panel.

## Spacing, radii, elevation

- Spacing scale 4, 8, 12, 16, 24, 32 dp. Screen edge to card 12; text inset in a card 16. Rows stack
  with no gap; 16 above an uppercase section label, 8 below.
- Radii: text fields 10; segments, chips and preset cells 12; a selector group's outline 14; cards,
  the link row, previous/next 16; buttons a full pill at 48 tall; a bottom sheet 28 on its top corners.
- Tonal elevation only, no shadows (they vanish on a dark stage). Screen = surface; link row and
  section cards = surfaceContainer; knob arc track and preset cells = surfaceContainerHighest; a
  bottom sheet = surfaceContainerHigh over a 55% black scrim.

## Where the accent is allowed

- **primary (live):** the connected dot in the status line; a selected sector; the current preset; a
  switch's on track; a knob's cap and value arc; the Connect button, the one filled button per screen;
  `primaryContainer` for the 600 ms "moved on the amp" row.
- **error (wrong):** only a lost link (retrying) and a refused link, always with its icon (triangle,
  octagon) and words. Never for a disabled control, never for a value.
- **tertiary (heard):** a WMI-1 found by discovery, nothing else.
- Not allowed: headings, the amp name, icons that are not state, decoration, the app bar. If a screen
  has more than one thing in amber besides values, one is wrong.

## States and motion

- Values never animate: notch, arc and number jump. A value is sent on every step.
- Moved on the amp: the row's background holds 600 ms, fades over 300 ms.
- Sector change: a `CLOCK_TICK` haptic and a 3 dp ring for about 100 ms.
- Disabled: the row at 38% with the reason in words ("not on Clean").
- Lost link: every control row at 38%; the link row and status line stay at full contrast. Nothing is
  dismissed.

## The knob

The one custom drawing. A 270° arc from 135° (seven o'clock) clockwise; the track in
surfaceContainerHighest, the value arc and the cap in primary, a notch cut from the cap to its rim at
135° + 270° × value / 127, the number below in `headlineSmall`.

- Turning is a straight drag, not a circle: up or right turns up, down or left turns down, starting
  anywhere in the cell. One value per 1.5 dp, so a full sweep is 190 dp. Hold still 400 ms before
  dragging for fine mode, one value per 6 dp.
- Haptic tick at 0, 64 and 127.
- Semantics: a range 0 to 127 with `setProgress`; hardware arrows ±1, Page Up and Page Down ±8.
- Phone: 76 dp dial in a 3-column grid, cells 150 dp tall. Tablet: 96 dp dial, seven across.

## Compose mapping

| Component | Build |
|---|---|
| Knob | `Canvas` plus `Modifier.pointerInput` for the drag; `progressBarRangeInfo` semantics. No sliders anywhere. |
| Switch row | Material 3 `Switch` with a check icon as thumb content when on; row 56 tall |
| Selector, 2 to 4 options | `SingleChoiceSegmentedButtonRow`, 56 tall on a phone, 64 on a tablet |
| Selector, 5 to 8 options | `FlowRow` of toggleable buttons, 3 per row on a phone, 48 tall |
| Link row | two `OutlinedTextField` in Plex Mono 16, one `Button` (Connect, filled) or `OutlinedButton` (Disconnect) |

Every existing `testTag` stays; the redesign changes themes and modifiers only.
