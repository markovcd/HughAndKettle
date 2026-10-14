# 1. The app is Kotlin and Jetpack Compose, and everything but the screen lives in pure JVM modules

Date: 2026-10-11

## Context

Hughes & Kettner ship iPad-only apps for their MIDI amps; the WMI-1 that connects them is sold as
iOS-only. The user wants the same on Android. Nearly all of the work is protocol and model code (a MIDI
codec, a TCP link, tables of controllers, preset handling), and most of it will be written, tested and
debugged by agents without eyes on a screen.

Options weighed: native Kotlin with Jetpack Compose; Flutter; React Native; Kotlin Multiplatform with
Compose Multiplatform so an iOS build could follow.

## Decision

Native Android in Kotlin, with Jetpack Compose for the UI, built with the Android Gradle Plugin's
built-in Kotlin. The code is split so that the Android module is as thin as possible:

- `midi`: MIDI 1.0 messages and a stream parser. Pure Kotlin, JVM, no dependencies.
- `amp`: amp models, their controls and how a control becomes a message. Depends on `midi`.
- `wmi`: discovery of a WMI-1 and the TCP link to it, and the fake WMI-1. Depends on `midi`.
- `cli`: the `hk` command line over `amp` and `wmi`.
- `app`: Compose screens that render state from the modules above and hand events back.

Android classes never appear below `app`. Coroutines are used in `app` only; the JVM modules use
threads and blocking sockets so the CLI has no runtime dependency.

## Consequences

- Every rule, every protocol detail and every amp table is testable in milliseconds with `kotlin.test`
  on the JVM, and reachable from a command line. The gate needs no emulator.
- Flutter and React Native were set aside because the hard part is the network and MIDI code, where a
  JVM module with sockets is the shortest path, and because an agent drives Gradle and `adb` more
  reliably than two toolchains.
- Kotlin Multiplatform is not adopted now: an iOS app already exists, and the Android toolchain is
  simpler without it. The split above keeps that door open: the JVM modules use nothing a KMP
  `commonMain` could not, apart from `java.net` sockets, which would move behind an expect/actual pair.
- The Android module compiles with AGP 9's built-in Kotlin, so the Kotlin Android plugin is never
  applied; a session that adds it will get a build error, which is the intended signal.

## Amendment (2026-10-14)

Kotlin Multiplatform is adopted for the shared screens, so a desktop app can draw them: ADR 0012.
The JVM modules stay as they are.
