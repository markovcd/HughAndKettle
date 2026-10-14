# Code style

## Match the file

The style is what the code already does. New code should be indistinguishable from the file it lands in: its naming, its idiom, its comment density, its error handling. Read the neighbors before writing, and copy how the codebase already does this kind of thing before inventing a way.

A formatter or linter configuration, where there is one, wins. Where there is none, the surrounding code is the configuration.

## Warnings are errors

A warning is fixed, not silenced. No blanket suppression pragmas, no `// eslint-disable`, `#pragma warning disable`, `@SuppressWarnings` or `# noqa` to get a build through. No `TODO` comments either: work not done is an item in `TODO.md`, not a note in the code. If a suppression is truly right, it is scoped to one line and the reason is written beside it.

Build and tool configuration is commented like code. If a line of build config is not obvious, the reason is written above it.

## Design habits

- **A failure is a value.** Code that reads a file, parses input, loads a plugin or talks to the network reports what went wrong as a result the caller can inspect (an issue list, a result type, a problem record). It does not throw for the expected ways the world is broken. Exceptions are for broken invariants: states the code promised could not happen.
- **Say where it went wrong.** An error names the file, the line, the field, the item or the second it concerns, so the next step is a fix rather than a bisect.
- **One place knows a thing.** A measurement, a mapping, a color, a limit, a way of opening a file: each has one owner, and everything else asks it. The same few lines in three places is a missing owner.
- **No interface without a second implementation.** Abstractions exist at real boundaries (plugins, platforms, the network, the device) and almost nowhere else. Code calls concrete types. No layer, factory or option added for a caller that does not exist yet.
- **No parameter with one value.** A parameter that every caller passes the same way, a flag threaded through four layers, a setting nobody asked for: each is a decision not made. Make it.
- **Data over types.** Prefer a value in a table to a subclass per case. What a thing carries is a part it has, not a subtype it is.
- **Immutable, then swapped.** Shared state that is read on a hot path is an immutable snapshot replaced whole, not a structure mutated under a lock.
- **Time is an argument.** Logic that needs the time is passed it. Nothing below the edge of the program reads a wall clock, which is what makes it testable and repeatable.
- **Closed by default.** The narrowest visibility that works, final/sealed classes, read-only fields. Widening is a decision; narrowing later breaks callers.
- **No dependency where a page of code will do.** A package is a long-term cost: updates, licenses, supply chain, a second way of doing things. A first-party build-time tool (an analyzer, a formatter) is not the same as a runtime dependency; do not hand-build a substitute for one on "no dependencies" grounds.
- **Split a big class by region, not by pattern.** A part that owns its own state becomes a class of its own. Do not reach for a pattern (MVVM, repository, mediator) the codebase does not already use.
- **An unsafe or clever construct is earned.** Pointer tricks, unchecked access and manual memory appear only where a measured hot loop needs them, with the check that makes them safe done once up front.

## Names

Use the domain's words, not the framework's, and the same word for the same thing everywhere: code, UI, docs, tests, commits. `docs/glossary.md` lists them. A name that says `get` does not write. A persisted identifier (a file format key, a public API name, an id saved in user data) is never renamed once shipped.

## Keep the codebase's own decisions

Before changing the shape of something, check `docs/adr/` and the comment at the top of the file. A shape an ADR chose, or a header that says "these are separate on purpose", is not drift. Where an ADR and the code disagree, that is a finding to raise, not a license to pick one.

## Kotlin and Android specifics

- Everything that can run without Android lives in a pure-Kotlin JVM module (`midi`, `amp`, `wmi`, `cli`) and is tested there in milliseconds. Compose that both apps draw lives in `ui`, a Kotlin Multiplatform module whose common code uses no Android class. The `app` and `desktop` modules are thin shells: they render state and hand events on. No Android class below `app`.
- Library modules compile with `explicitApi()`: every declaration states its visibility, and the answer is `internal` unless another module needs it. Classes are final unless `open` is a decision.
- `data class` for data, `@JvmInline value class` for a small value with a rule (a MIDI channel), `sealed interface` for a closed set of shapes, `object` for the one place that knows a thing. Nested types stay inside their owner.
- An expected failure is a value: `Result<T>` where the caller needs the reason, a nullable where "not one of these" is the whole answer. `require` and `error` are for broken invariants only.
- The JVM modules use plain threads and blocking sockets, so the CLI carries no runtime dependency. Coroutines appear in `app`, where the UI needs them, and nowhere below it.
- Warnings are errors in the Kotlin compiler (`allWarningsAsErrors`) and in Android Lint (`warningsAsErrors`). The two lint checks turned off, with the reason beside them, are the ones that go red when a newer dependency is published.
- ktlint's official style, configured in `.editorconfig`, is the formatter; `./gradlew ktlintFormat` applies it and `./gradlew ktlintCheck` is part of `build`.
- Tests use `kotlin.test` on JUnit 5. A test's name is a sentence in backticks: `` fun `A control change and a program change read back as themselves`() ``.
- Composables are named like types, found in tests by `Modifier.testTag`, never by their text.
- One Gradle convention plugin per kind of module, in `build-logic/`; a module's build file says which kind it is and what it depends on, nothing else. Versions live only in `gradle/libs.versions.toml`.
