# 03. Desktop app: the same screens on Windows and Linux

- **Severity:** —
- **Status:** Open; step 1 done
- **Found:** 2026-10-14, on `main` at `d10cd81`

The user asked for the app on Windows and Linux. This is the plan, one step per task, in order. The
standing rules hold throughout: each step lands green on `main` and leaves the phone app as it was;
the desktop build stays dark (no release, no changelog entry) until step 6 ships it.

## The shape

Compose Multiplatform, JetBrains' build of Compose, draws the same composables on Android and on the
desktop JVM and keeps the `androidx.compose.*` package names, so the screens move rather than get
rewritten. What changes is the module shape, which ADR 0001 deliberately left at Android-only:

- `midi`, `amp`, `wmi`: unchanged, already shared by `cli` and `app`.
- `ui` (new): a Kotlin Multiplatform module with an Android target and a desktop JVM target. It holds
  the theme, the building blocks (knob, effect card, switch row, tone row) and the state behind them.
- `app`: the Android shell. Activity, foreground service, WiFi binding, permissions, DataStore,
  `android.media.midi`, and the phone layouts.
- `desktop` (new): the desktop shell. A window, the desktop layout, `javax.sound.midi` for the cable,
  files for settings, and packaging.

The UI splits where the platforms differ in how they are used, not only in what they can reach:
the building blocks are shared, the layout and the input are per platform.

## What exists

From reading `app` at `d10cd81` (not measured by a trial build): about 4,100 lines in 50 files.
Roughly 3,300 import only `androidx.compose.*` and move as they are. Twelve files reach Android:
`MainActivity`, `HughAndKettleApplication`, `LinkService`, `LinkWords`, `WifiRoute`, `FoundWmis`,
`MidiCableJack`, `ToneShelfCard`, `Settings`, `AmpChoiceStore`, `LastWmiStore`, `ThemeStore`. Text
comes from `strings.xml` through 15 `stringResource` calls, fonts from `res/font`. The 22 UI tests run
on Robolectric.

## Step 1: the toolchain, proved with the theme and one knob

- A convention plugin `hughandkettle.compose-multiplatform` for the `ui` module: Kotlin Multiplatform,
  AGP's `com.android.kotlin.multiplatform.library` for the Android target, a `desktop` JVM target,
  the Compose Multiplatform plugin, ktlint, locking, warnings as errors.
- `ui` holds the palette, the typography with the IBM Plex fonts as Compose resources, and `Knob`.
  `app` depends on `ui` and draws the same knob as before.
- `desktop` module with `./gradlew :desktop:run` opening a window that shows a knob in the app's
  theme. Not packaged.
- A test of the knob runs on the desktop JVM, with no Robolectric.
- ADR 0012: the screens are shared with a desktop build through Compose Multiplatform. It amends the
  Kotlin Multiplatform paragraph of ADR 0001.

**Done when** the gate is green with all three building, and the phone app is unchanged. Done:
the source keeps its `io.github.markovcd.hughandkettle.app` packages in `ui`, so a file moves without
any other file changing.

## Step 2: the screens move into `ui`

- `strings.xml` becomes Compose resources; the `stringResource` imports change.
- The Compose-only files move into `ui`, verbatim, a few per commit, so another session's edits in
  `app` rebase cleanly. `PanelState` and its siblings move with them.
- UI tests that need nothing from Android move to the desktop JVM and drop Robolectric.

**Done when** `app` holds only the twelve Android files and the phone layouts.

## Step 3: the seams the platforms fill differently

Each becomes a small interface in `ui` with one implementation per shell, since both are real:

- settings (theme, amp per WMI-1, last WMI-1): DataStore on Android, a file in the user's config
  folder on the desktop;
- holding the link: the foreground service on Android, the process on the desktop (ADR 0007);
- finding a WMI-1: the WiFi name and its permissions on Android, plain discovery on the desktop;
- the cable: `MidiCableJack` on Android, the `javax.sound.midi` code `hk cable` uses on the desktop;
- tone import and export: the Android file picker, a desktop file dialog;
- haptics: none on the desktop.

## Step 4: the desktop app runs and links

The phone's landscape layout in a desktop window, linking to a WMI-1, to `hk fake-wmi`, or to a cable.
A desktop UI test drives it against the fake WMI-1. `./gradlew :desktop:run` and a runnable folder in
the gate artifacts are how it is tried.

## Step 5: a desktop layout

Amp, FX and Tones side by side in a resizable window, without tabs. Mouse wheel on knobs, hover hints,
right-click menus, a menu bar in place of the ⋮ menu, keyboard shortcuts (step through tones, tap
tempo). The phone layouts stay as they are. The user may want to design this in Claude Design first.

## Step 6: packaging and release

- Linux: a `.deb` and a portable folder with a bundled runtime, built in the gate container.
- Windows: an `.msi` built on a Windows runner in `release.yml`, since jpackage cannot cross-build.
- Both checksummed and signed with the release key like the APK (ADR 0006). An unsigned Windows
  installer shows a SmartScreen warning; buying a code-signing certificate is the user's call.
- Changelog, README and site name the desktop builds.
