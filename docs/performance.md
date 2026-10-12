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

## Open leads

- The real number: the same measurement can't stamp arrival on a real WMI-1. On hardware, the
  budget is checked by ear or by `hk listen` on a second link if the amp echoes; phase 0 says which.

## Ruled out

Nothing yet.
