# 6. A release is a signed APK and a CLI zip on GitHub Releases, versioned from a tag and signed with an SSH key

Date: 2026-10-11

## Context

The pipeline rule asks for a release that is one script, refuses before moving anything, builds once,
ships a signed checksum file, and takes its version from a tag. The product is an Android app for one
person to start with; a store listing is a later decision.

## Decision

- `release.sh` is the release. Run on a machine it builds everything with a throwaway Android keystore
  and a throwaway signing key and publishes nothing; the `Release` workflow runs the same script with
  `--publish` and the real keys from secrets.
- The version is the next patch after the latest `vX.Y.Z` tag, or the one given. It reaches the build
  as `-PreleaseVersion`; every other build is `0.0.0-dev+<commit>` with version code 1 and can never
  pass for a release.
- The APK is signed inside the gate image with `apksigner` from the pinned build tools. The checksum
  file `SHA256SUMS` is signed with `ssh-keygen -Y sign` and verified against the public key committed
  as `release/allowed_signers`; the script refuses a private key that does not pair with it before
  building anything.
- Artifacts (the APK, the `hk` distribution zip, `SHA256SUMS`, `SHA256SUMS.sig`) go to a GitHub
  Release whose notes are the version's section of `CHANGELOG.md`, created with the `gh` CLI the
  runners already carry.

## Consequences

- No GPG, minisign or cosign: OpenSSH is on every machine the project touches and its signature format
  is stable. A user verifies with one `ssh-keygen -Y verify` line that the README gives.
- Until the user makes the keys and commits `release/allowed_signers`, `release.sh` runs only in its
  throwaway mode and says so. That is a TODO item, not a placeholder key in the tree.
- Google Play or F-Droid would add their own signing and metadata; either is an ADR of its own when
  the user wants it, and nothing here blocks it.
