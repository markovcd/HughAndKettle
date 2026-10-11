# 4. The gate is a container build, and every test runs on the JVM without an emulator

Date: 2026-10-11

## Context

An Android build depends on a JDK, an SDK platform, build tools and a Gradle distribution, any of
which can differ between a laptop, a cloud session and CI. The pipeline rule asks for one gate that
means the same thing everywhere. Android instrumented tests need an emulator, which is slow, flaky on
CI and invisible to an agent.

## Decision

- `gate.sh` builds `gate.Dockerfile`: a pinned JDK 21 image, the Android command-line tools pinned by
  checksum, the SDK packages the build needs, then `./gradlew build`. CI runs `./gate.sh`, nothing
  else. The `artifacts` stage of the same file is how APKs, the CLI distribution and test reports are
  copied out.
- `./gradlew build` on a machine with `ANDROID_HOME` set is the iteration loop; it runs the same tasks
  and is what the fast loop uses, but only the container run is the gate.
- Every test is a JVM test: `kotlin.test` on JUnit 5 in the four JVM modules. The app module is kept
  thin enough that its logic is tested through `PanelState` and the modules below it. When Compose UI
  needs its own tests they run on the JVM through Robolectric, never on an emulator in the gate.
- Dependencies are locked: every module's configurations are locked to `gradle.lockfile`, and
  Gradle, the command-line tools and the base image are pinned by checksum or digest. A version that
  moved fails the gate instead of building.

## Consequences

- "Passing" is one thing: the container run. A machine missing the SDK cannot be green about the app.
- The gate downloads the SDK on every CI run until a layer cache is added; that is a measured
  optimization for later, not a correctness problem.
- Android Lint runs in `build` with warnings as errors. Two checks that go red when a newer dependency
  is published are turned off with the reason beside them; Dependabot owns upgrades.
- A test that needs a real amp or a real WMI-1 does not exist in the suite. Phase 0 of the plan is a
  person running `hk` against the hardware and recording the output in `docs/wmi-1.md`.
