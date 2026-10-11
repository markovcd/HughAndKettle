---
name: dependencies
description: Use before starting a feature, refactor or anything else that will touch a lot of files - checking whether the project's packages and its language toolchain are current, what an upgrade costs, and when to take it on the spot versus hand it off.
---

# Packages and the toolchain

## Check them before starting anything big

Before building a feature, a refactor or anything else that will touch a lot of files, find out whether the dependencies are current: `npm outdated`, `pip list --outdated`, `cargo outdated`, `dotnet list <project> package --outdated`, `go list -u -m all`. Check that the command actually reports: some report nothing at all against a solution or workspace file and have to be run per project. Never include prereleases.

**Why:** building on a version that is about to move means writing the code twice, and the upgrade's own breakages then arrive mixed into a diff that is about something else. Finding out costs one command.

## Cheap upgrades are taken on the spot

An upgrade is cheap when everything builds and every test passes after changing the version numbers, or after a change a compiler error walks you straight to. Take it, in a commit of its own, before the real work starts.

An upgrade is expensive when it wants a decision rather than a fix:

- a license or a fee, which is the user's to accept, not yours;
- a package that has to be vendored, replaced or dropped because something downstream has not caught up;
- a shipped behavior that would change, rather than a call that moved;
- a breakage in something no test covers, so the port cannot be checked.

Say what it costs and what it buys, recommend one, and let the user choose. Then do the work they picked before the feature, not alongside it.

## The toolchain, the same way

Check the language runtime or SDK in the same breath, because the answer changes what a big piece of work is written against. Know every place that names the version (the project file, the container image, CI, a version file) so a retarget changes all of them.

- **Cheap, so just do it:** a new version that is already released *and* already installed. Retarget, build, run every test. Passing is the whole test.
- **Not yours to do:** installing a toolchain. That is a download onto the user's machine, possibly needing elevation, and not the task they asked for. Being a patch behind changes nothing in the repository; say it in one line and carry on.
- **Expensive, so hand it off:** a version that needs work rather than a retarget (new analyzer errors, a runtime behavior that moved, a dependency with no build for the new target). Do not start it in the middle of a feature. Say what it would take and offer it as a separate task.

## The upgrade is its own commit

Never in the same commit as the feature it was cleared out of the way for. A version bump that turns out to be what broke something has to be revertable on its own, and a feature diff with a migration folded into it is not reviewable.

## Adding a dependency

A new package is a long-term cost. Prefer the standard library, then what the project already depends on, then a page of code. A first-party build-time tool (an analyzer, a formatter, a code generator that ships nothing) is a much smaller question than a runtime dependency; propose it rather than hand-building a substitute. A third-party runtime dependency is a question to raise, not a line to add.
