# 13. The desktop app ships as a .deb, a Linux folder with its own runtime, and an MSIX signed with a certificate of its own

Date: 2026-10-19

## Context

The desktop app (ADR 0012) needs packages a person installs without Java. jpackage, which Compose
Multiplatform drives, bundles a trimmed runtime but builds only for the system it runs on. The plan
named an `.msi` for Windows; the user asked for an MSIX instead. Windows installs an MSIX only if its
signature chains to a certificate the machine trusts, and the SSH key that signs `SHA256SUMS`
(ADR 0006) cannot sign one.

## Decision

- The gate container builds the `.deb` and a portable folder, packed as
  `hughandkettle-linux-x64.tar.gz`, after `./gradlew build` passes; both are gate artifacts.
- A Windows runner builds the MSIX from jpackage's app folder with the Windows SDK's `makeappx` and
  `signtool` (`desktop/packaging/msix.ps1`). The Gate workflow signs it with a throwaway certificate;
  the Release workflow signs it with the real one from secrets, checks it built the same commit, and
  hands it to `release.sh`.
- The MSIX certificate's subject is `CN=Hugh and Kettle`, the manifest's publisher. Its public half is
  committed as `release/msix.cer`, and a private one that does not pair with it is refused.
- `release.sh` puts the `.deb`, the tarball and the MSIX beside the APK and the `hk` zip in
  `SHA256SUMS`, and refuses to publish without the MSIX.

## Consequences

- The MSIX is not built inside the gate image: it is built on Windows from the same commit, which the
  release checks, rather than promoted from the gate.
- A self-signed certificate means each Windows machine imports `msix.cer` once before installing. A
  certificate from a code-signing authority, or Azure Trusted Signing, removes that and is the user's call.
- The `.deb` depends on Ubuntu 24.04's package names (`libasound2t64` and the like); an older Debian
  or Ubuntu takes the portable folder.
- The lock files carry Skia for Linux and Windows, so both runners restore against them.

## Amendment (2026-10-26)

- A Mac builds an app bundle and a `.dmg` (`createDistributable`, `packageDmg`); no release job makes them
  yet. macOS refuses a version whose first number is 0, so builds that are not a release are 1.0.0 there.
- The lock files carry Skia for macOS on arm64 as well, so a Mac restores against them too.
