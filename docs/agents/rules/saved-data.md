# Saved data doesn't break

## What was written stays readable

Users' files, databases, settings, saved URLs, public APIs and command-line interfaces outlive the code that wrote them. A change to the shape of any of them carries its migration in the same commit, with a test that loads the old shape.

**Why:** a user's saved work is the one thing they cannot regenerate. A format change that breaks it is found by the user, after release, with no way back.

**How to apply:**

- **Version the format.** A persisted format carries a version stamp. Raise it only when an older reader would get the new shape wrong (a field renamed, a unit or index base changed, a list that means something new), not for an addition an old reader can ignore or report.
- **Every raise owes an upgrade step.** Steps run in order, each standing alone, so a file from version 1 opened by version 4 runs 1→2, 2→3, 3→4. Keep a fixture of each old version in the tests and load it.
- **A file from a newer version is refused whole, with a message**, rather than half-read and silently saved back in the older shape.
- **Persisted identifiers are forever.** An id, enum number, key or type name that is written to disk or sent over the wire is never renamed, renumbered or reused once shipped. Add a new one; retire the old one by reading it.
- **What is missing is reported, not fatal.** A saved file that names something no longer available (a removed feature, a missing plugin) opens with a report of what is missing, not an exception.
- **Settings load tolerant.** A settings file that is missing, empty, damaged or from another version loads as defaults for what it cannot read, and never stops the program starting.
- **Contracts only grow.** A public API, a plugin interface, a command-line argument another version calls, a URL someone bookmarked: add to them, don't change or remove. When one must break, it is versioned and the break is a decision the user makes, recorded in an ADR.
- **Shipped examples are data too.** Sample files, seed data and default presets kept as files do not follow a refactor the way code does. A change that would stop one loading rewrites it in the same commit, and a test loads every one.
- **Database migrations are forward-only and tested.** One per change, run against a copy of the previous schema with data in it, never edited once applied anywhere.
