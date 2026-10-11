# Testing

## What a change carries

A behavior change lands with a test that fails before it and passes after. A bug fix lands with the failing test that confirmed the bug. Prove the new test fails without the fix by running that one test, not the suite.

A test goes in the project or module that owns the behavior, not the one that is convenient.

**Why:** a test written after the fix, never seen red, may be testing nothing. And a finding is not a finding until something fails: a red test, or a command whose output can be pasted.

## Names state the rule

The file, the class and the subject share a name. A test's name is a sentence that states the rule it holds:

```text
A_printed_config_reads_back_as_the_same_config
A_chunk_that_lies_about_its_length_costs_nothing
An_expired_token_is_refused_before_the_body_is_read
```

No `Should_`, no `Given_When_Then`, no `Test` suffix, no `Bug3`. Name the rule, not the bug: the rule outlives the hunt that found it.

## Shape

- A private helper named for what it observes (`Heard`, `Seen`, `Run`), and cases that read as a table.
- Floats are compared with a tolerance; two implementations that must agree are compared bit for bit.
- A claim about every item of a set (every opcode, every endpoint, every shipped config) is a parameterized test over the real set, not a hand-written list, so the next one added is covered without anybody remembering. Exceptions are named one by one with a comment, so adding one is a decision. Collect failures and assert once, with the first few in the message.
- Doubles are small private classes in the test file: a fake device, a canned HTTP handler, a function of two string writers returning an exit code. Reach for a mocking library only if the codebase already uses one.
- A temp folder is a field, created per test and deleted in teardown.
- Assert on the smallest thing that holds the behavior. Logic reachable as a pure function needs no window, server or process.
- A UI element a test drives is found by a stable name or id, never by its caption or the glyph it draws: those are the look, and they change without the behavior changing.

## Skips say why

A test that needs something the machine may lack (a GPU, ffmpeg, a platform) skips with a reason: `skip when no ffmpeg on this machine`. Never a silent pass and never an early return that reports green.

**Why:** a local run with things missing can be green about code it never ran. The gate, run in a clean environment that has everything, is the truth.

## Snapshots

Accept a changed snapshot only after reading at least one diff and agreeing with it. A snapshot change that was not the point of the commit is a finding, not a chore.

## Waiting

Never `sleep` a fixed time to let something happen. Wait on the condition with a deadline, and fail with a message when the deadline passes. A wait that proves nothing happened is capped hard, and a comment says why the cap is enough.
