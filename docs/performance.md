# Performance

The budget: a knob move reaches the amp within 30 ms on the local network
([plan, cross-cutting](handoff/01-implementation-plan.md#cross-cutting)).

## Where the time goes

`hk latency --count N` times the app's own share: from handing a control change to `WmiLink` to a
fake WMI-1 on the same machine having parsed it, one message at a time, after 200 untimed sends to
warm the JIT. WiFi, the WMI-1's serial bridge and the amp are not in it.

| Date | Commit | Machine | Count | min | median | p95 | max |
|---|---|---|---|---|---|---|---|
| 2026-10-12 | 93bc8c6 | cloud container, 4 vCPU Xeon, OpenJDK 21.0.12, loopback | 10 000 × 3 runs | 2 µs | 18–19 µs | 28–33 µs | 5–15 ms |

The app's share is three orders of magnitude inside the budget at the median. The maximum is a
scheduler or GC pause on a shared machine, seen once per run, not a pattern.

### The gate in CI

From `build/gate.log` in the uploaded `gate` artifact, two runs on `ubuntu-latest`:

| Date | Commit | Gate step | Image layers (base, apt, SDK, wrapper) | `./gradlew build` |
|---|---|---|---|---|
| 2026-10-12 | 269f88c | ~6 min | 5.7 + 6.3 + 11.1 + 3.4 = 26.5 s | 313.7 s |
| 2026-10-12 | 9b94115 | 6 min 27 s | 5.7 + 7.3 + 11.4 + 3.8 = 28.2 s | 339.3 s |

Nine tenths of the job is `./gradlew build` starting cold: no Gradle cache, every dependency
downloaded, `build-logic` compiled from nothing (its first jar lands 90 s in). The same build warm on
a 4 vCPU cloud container takes about 30 s.

## Open leads

- The gate's `./gradlew build` cold in CI (above). Keeping Gradle's caches between runs is where the
  minutes are, but they live inside `docker build`, so they would need a BuildKit cache mount carried
  across runs, which takes a third-party action or a hand-written export step. Not tried.
- The real number: the same measurement can't stamp arrival on a real WMI-1. On hardware, the
  budget is checked by ear or by `hk listen` on a second link if the amp echoes; phase 0 says which.

## Ruled out

- Caching the gate image's layers in CI (2026-10-13). The layers a cache could keep cost 26 to 28 s
  of a 6.5 min job, and saving and restoring a JDK-plus-SDK image through the Actions cache costs time
  of its own. `COPY . .` comes before `./gradlew build`, so every commit rebuilds the step that matters
  whatever is cached. Not added.
