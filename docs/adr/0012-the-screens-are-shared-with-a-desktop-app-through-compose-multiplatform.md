# 12. The screens are shared with a desktop app through Compose Multiplatform

Date: 2026-10-14

## Context

The user wants the app on Windows and Linux as well as Android. ADR 0001 kept Kotlin Multiplatform out
because the Android toolchain is simpler without it, and left the door open. The JVM modules already run
on any desktop: `hk` does, and its cable path uses `javax.sound.midi` (ADR 0009). What does not is the
Compose in `app`, about 4,000 lines, almost all of it plain `androidx.compose.*` drawing.

Options weighed: a second UI in Swing or JavaFX; Compose for Desktop in its own module with copies of
the screens; Compose Multiplatform with one shared module.

## Decision

Compose Multiplatform, with the shared screens in `ui`, a Kotlin Multiplatform module with an Android
target (AGP's KMP library plugin, with Android Lint applied) and a `desktop` JVM target.

- `app` is the Android shell and `desktop` the desktop one. Each holds what its platform does
  differently: the activity and service, or the window; settings storage; the cable; file dialogs; and
  its own layouts, since a phone and a desktop are used differently.
- The building blocks and the state behind them are shared; layouts and input are per platform.
- Code moved into `ui` keeps its `io.github.markovcd.hughandkettle.app` package, so moving a file
  changes no other file and another session's edits rebase across it.
- Fonts and, later, text are Compose resources in `ui`.
- The desktop build links Skia's native library for the machine building it; the lock files hold the
  gate's Linux x64 one.

## Consequences

- One change to a shared screen lands on Android and the desktop at once.
- UI tests of shared pieces run on the desktop JVM with no Robolectric.
- The Android app resolves Compose through JetBrains' artifacts, which point at the androidx ones;
  the Compose BOM still sets the androidx versions.
- Packaging for Windows needs a Windows build machine, since jpackage cannot cross-build.
- The plan is [docs/handoff/03-desktop-app.md](../handoff/03-desktop-app.md).
