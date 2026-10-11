---
name: running-the-app
description: Use when launching the real app (a desktop window, a dev server, a long-running process) to check something or take a screenshot - waiting for an instance you did not start, marking yours so it is plainly not to be touched, and driving only the process you started.
---

# Running the app yourself

Prefer a headless check: a test, a CLI command, a still render. Launch the real app only for what those cannot answer.

## Another instance may be running, and it is not yours

Before starting one, list what is running with its process id, start time, title and path. An instance you did not start belongs to the user or to another session, whichever checkout or build its path points at.

- **If one is running, wait for it to close.** Check again every minute or so. Do not click, type into, move, resize, capture or stop it meanwhile.
- **After a long wait (ten minutes or so), close it yourself** by its id, then tell the user which instance it was (path, title, start time) and when you closed it.
- **Then start yours**, keep the process id the launcher hands back, and act only on that process: its window handle for input and captures, its id to stop it. Never pick "the first window of that name", and never kill every process by name.

Before sending keystrokes, bring your window to the front and check the foreground window really is yours, since synthetic keys go to whatever is in front.

**Why:** a capture script once took the first matching window it found and began by stopping every process of that name. The window it then drove was a build from another checkout, possibly the user's own.

A dev server binds a port: check the port is free first, and if it is taken, that is someone else's server, not a stale one to kill.

## Mark your window

A window a session started looks exactly like one the user opened. Mark yours as soon as it has a handle, before any input: a title such as `CLAUDE IS DRIVING - DO NOT CLICK`, and on Windows 11 a red caption via `DwmSetWindowAttribute` (attributes 34, 35, 36: border, caption, text color; `COLORREF` is `0x00BBGGRR`). Restore the real title only for a screenshot that would show it, and mark it again right after.

## Sound and side effects

If the app makes sound, sends messages, writes to shared locations or talks to a real service when it starts, start it with those off (muted, pointed at a local fake) unless they are the point.

## When done

Stop what you started, by its id. Leave everything else running.
