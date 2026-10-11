---
name: docs-in-step
description: Use when a code change touches anything the README, the docs folder or the website describes - commands, flags, settings, shortcuts, names, file formats, public APIs, supported platforms - so the doc edit lands in the same commit.
---

# Keep the docs in step

Any change to functionality the README, `docs/` or a website describes gets the matching doc edit in the same commit: command names and flags, settings, keyboard shortcuts, the names of things users see, file formats, public API and plugin contracts, supported platforms, screenshots.

**Why:** docs published straight from `main` have no build step to catch a stale sentence, and a separate "update docs" commit is the one that never happens.

**How to apply:**

- After a user-visible change, grep the docs and site for the affected names and fix the prose.
- Code samples and snippets in the docs must still run. Check them with the real tool, not by reading.
- Claims about what is published (platforms, versions, download names) must match what CI actually builds.
- Never draw a UI element by hand in HTML, CSS or SVG to illustrate it. A second drawing goes stale the moment the real one changes; take a screenshot of the real thing, ideally from a test that can retake it.
- A screenshot a change makes stale is retaken in the same commit if a script can take it; otherwise list it somewhere a later session will find it.
- Doc changes are not changelog entries.
