# 02. Design brief: a logo, an icon and the look of the whole app

- **Severity:** —
- **Status:** Landed for what the app has (icon, theme, panel, link card, knob, switch and selector rows); the discovery list, the preset strip and the retrying state wait on phases 1 and 2. The record is in [docs/design/](../design/README.md).
- **Found:** 2026-10-11, at the first commit

A handoff for a design session (Claude Design, or any designer): what to make, what it must obey,
and what to hand back so an agent can land it in the app without a second round. The app's
placeholder look (`AmpPanel.kt`, `ic_launcher.xml`, `themes.xml`) is what this replaces.

## What the product is

Hugh & Kettle is an Android app that controls a Hughes & Kettner tube amp from the stage or the
sofa, over the amp's WMI-1 WiFi box, the way the maker's iPad apps do. The name is a pun on
"Hughes & Kettner"; the app is independent of the maker. One person uses it today: a guitarist
with a Black Spirit 200 or a GrandMeister Deluxe 40, a phone on a mic stand or in a hand, often in a
dark room, often with a guitar in the other hand.

The words the app uses are fixed by [docs/glossary.md](../glossary.md): **amp**, **WMI-1**, **link**,
**control** (a **knob**, a **switch** or a **selector** with **sectors**), **channel** (the amp's
Clean, Crunch, Lead, Ultra), **preset** (one of 128), **panel** (the screen for one amp). Use these
in every label, never "parameter", "patch", "dongle" or "connection".

## Deliverables

### 1. Logo and app icon

- A mark that works at 48 dp on a launcher and at 24 dp in a notification, and reads as the app when
  there is no text next to it.
- The pun is the brief: a kettle, or a kettle's silhouette, that is also an amp or an amp's knob. Keep
  it to one idea; two merged objects read as a blob at 48 dp.
- It must not imitate Hughes & Kettner's trademarks: not their logo, not their lettering, not the
  blue-lit front panel that is their signature. An amp-flavored look is fine; their look is not.
- Deliver as an **Android adaptive icon**: a 108 dp foreground layer (safe zone: the inner 66 dp
  circle), a background layer (a flat color or a simple gradient), and a **monochrome** layer for
  themed icons on Android 13 and later. Each as an SVG that converts cleanly to a vector drawable (no
  gradients with more than two stops, no filters, no text as text).
- Also: a 512 px PNG for a store listing or the README, and a one-color version for dark and light
  backgrounds.

### 2. A color system

- Material 3 color roles, both schemes, as hex values: `primary`, `onPrimary`, `primaryContainer`,
  `onPrimaryContainer`, `secondary` (and its three), `tertiary` (and its three), `error` (and its
  three), `background`, `onBackground`, `surface`, `onSurface`, `surfaceVariant`,
  `onSurfaceVariant`, `outline`, `outlineVariant`, `surfaceContainer` (lowest to highest).
- **Dark first.** The dark scheme is the one people will see on stage; design it, then derive the
  light one. Contrast of text on its container at least 4.5:1, of large text and controls at least
  3:1, in both schemes.
- One accent that says "live": the color of a connected link, a selected sector, a knob being
  moved. One that says "something is wrong": a lost link, a refused connection. These two must be
  distinguishable by people who cannot tell red from green, so give each a shape or a label too.
- Dynamic color (Material You) is off; the app has its own colors so a knob looks the same on every
  phone.

### 3. Type

- Two text styles carry almost everything: a control's name (small, above its knob) and the number
  or option it shows (large, glanceable from a meter away). Pick a typeface with tabular figures so a
  value that changes from 99 to 100 does not shift its neighbors.
- Specify the Material 3 type scale roles the app uses (`titleLarge`, `labelLarge`, `bodyMedium`,
  `displaySmall` for the preset number) with family, weight and size. A font that is not a system
  font must be licensed for bundling in an open-source app and delivered as a `.ttf`.

### 4. Components, as they appear in the app

Everything below exists today as a plain Material 3 default and needs its look. Keep each one a
Material 3 component with a theme and modifiers, not a custom drawing, unless the brief says so.

- **The panel**: the amp's name, the link row, then one row per control, scrolling. Design the
  landscape layout for a tablet too: controls in a grid grouped by section (Amp: gain, bass, mid,
  treble, resonance, presence, volume; FX: modulation, delay, reverb, FX loop; Switching: channel,
  boost, noise gate, mute; and the amp's own extras: cabinet and sagging, or power soak).
- **The link row**: an address field, a port field, one button that is Connect or Disconnect, and a
  status line that is one of: not connected, connecting, connected to a named box, retrying, refused
  ("something else is connected"). A box found on the network appears here as a tappable name.
- **A knob row**: name above, a slider for 0 to 127 below, the value at the right. This is the one
  component that may be a custom drawing: a real rotary knob, if it is as fast to drag as a slider
  and as readable. Design both and say which to build first.
- **A switch row**: name and a switch; the on state uses the live accent.
- **A selector row**: name and one chip or segmented button per option (two to eight options; the
  four-channel selector is the one that matters most and wants big targets).
- **The preset strip** (phase 2): the current preset's number and name, large, with previous and
  next; and a grid of 128 to jump to one.
- **A discovery list** (phase 1): each WMI-1 heard on the network as a card with its name and
  address, and a manual-entry fallback.
- **Touch targets** at least 48 dp; a knob row at least 64 dp tall. A thumb on a mic stand, not a
  mouse.

### 5. States and motion

- Every control has: enabled, disabled (the control does not apply on the current channel), and
  "moved on the amp" (the value just changed from the amp's side; a brief highlight, then normal).
- No animation on a value change: latency is the feature, and a slider that eases lags the amp. A
  sector change may have a short haptic tick; design the visual for that moment.
- A lost link greys the whole panel and keeps the status line readable; it does not dismiss anything.

## Constraints that are not negotiable

- Jetpack Compose with Material 3; the theme is delivered as values a `MaterialTheme` takes.
- Tests find elements by `testTag`, so every interactive element keeps a stable tag; a redesign may
  change what it looks like and never what it is called.
- The app talks to a box on the local network and nothing else: no account, no sign-in, no onboarding
  that asks for anything, no analytics. The first screen is the panel or the discovery list.
- One language (English) to start; strings live in `strings.xml` and labels must survive being 30%
  longer in another language.

## What to hand back

A design session should return, in this order, so an agent can land each in its own commit:

1. `icon/` with `foreground.svg`, `background.svg`, `monochrome.svg`, `icon-512.png`, and a line
   saying the safe zone was respected.
2. `theme/colors.md`: the Material 3 roles for dark and light as a table of hex values, with the
   contrast ratios noted. (An agent turns this into `Color.kt` and `Theme.kt`.)
3. `theme/type.md`: the type scale roles used, with family, weight, size and line height, and the
   font file if one is bundled.
4. Screens as images, at phone (412×915 dp) and tablet landscape (1280×800 dp): the panel in both
   schemes, the link row in each of its five states, a knob row in each of its three states, the
   selector row, the discovery list, the preset strip. Name each file for the screen and state.
5. One page of notes on anything the agent would otherwise have to guess: spacing scale, corner
   radii, elevation use, where the accent is allowed and where it is not.

Keep the artifacts in `docs/design/` when they land, and link them from this file; the images are
the record of what was decided, so a later screenshot test can be compared against them.

## Out of scope

A website, a store listing beyond the icon, marketing material, an iOS design, and anything that
needs the maker's assets.
