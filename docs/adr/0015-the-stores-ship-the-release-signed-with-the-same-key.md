# 15. F-Droid and Google Play ship the app signed with the same key as the GitHub release

Date: 2026-11-09

## Context

The app goes to F-Droid and Google Play as well as GitHub Releases (ADR 0006). Android installs an
update only when it is signed with the same key as the installed app. Left to their defaults, F-Droid
signs what it builds with a key of its own and Play signs with a key Google generates, so the same app
id would exist under three keys. Someone moving from one store to another would have to uninstall
first and lose their tones, songs and settings, and nothing in the app can carry them across.

The app keeps the name Hugh & Kettle and the app id `io.github.markovcd.hughandkettle` everywhere.
It is unofficial, and every listing says it is not made or endorsed by Hughes & Kettner.

## Decision

- One Android signing key signs every build that reaches a user. `release.sh` signs the APK on the
  GitHub release with it, and an Android App Bundle (`.aab`) beside it for Play.
- Play App Signing is set up with that key rather than a key Google generates.
- F-Droid builds from the release tag and, when its build matches the signed APK on the GitHub release
  byte for byte bar the signature (reproducible builds), publishes that APK with its signature.
- Store text lives in the repo as `fastlane/metadata/android/`, which F-Droid and IzzyOnDroid read
  from the tag. The privacy policy is `PRIVACY.md`.

## Consequences

- Losing the key means the app can never be updated in any store. It is kept offline in two places
  and in the CI secret store, nowhere else.
- The release APK has to be reproducible on F-Droid's build server: the same JDK major, build tools
  and R8 as the gate image. If it does not reproduce, F-Droid falls back to its own key and only that
  store's users are cut off from the others.
- Google's developer verification ties an app id to a registered developer and key; one key means one
  registration covers every channel.
