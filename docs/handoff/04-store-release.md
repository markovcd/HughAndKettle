# 04. Store release: the app on F-Droid and Google Play

- **Severity:** —
- **Status:** Open; ADR 0015, store text, privacy policy and the signed bundle done
- **Found:** 2026-11-09, on `main` at `2add1ff`

The user asked for the app on F-Droid and Google Play. The decisions are in ADR 0015: the name stays
Hugh & Kettle, and one key signs every build. This is what is left, in order.

## Steps

1. **The keys and the first release** (the user). Make the Android keystore and the release SSH key,
   add the secrets, commit `release/allowed_signers` (`release/README.md`), then cut `v0.1.0` from the
   `Release` workflow. It refuses to publish without the Windows certificate's two secrets as well.
2. **Screenshots.** Phone screenshots of the Amp, FX, Tones and Song tabs into
   `fastlane/metadata/android/en-US/images/phoneScreenshots/`, from the emulator, and Play's
   1024×500 feature graphic as `featureGraphic.png`.
3. **The F-Droid recipe.** `metadata/io.github.markovcd.hughandkettle.yml` for `fdroiddata`: build
   from the tag with `gradleprops: [releaseVersion=X.Y.Z]`, subdir `app`, `AllowedAPKSigningKeys` set to
   the release key's certificate hash and `Binaries` pointing at the GitHub release APK so F-Droid
   checks the build reproduces it. The version is not written in any file, so the update check has to
   take it from the tag; settle how with `fdroidserver` before writing it. Run `fdroid build` in a
   container against `v0.1.0` and compare with the release APK.
4. **F-Droid and IzzyOnDroid** (the user). Open the merge request to `fdroiddata` on GitLab with the
   recipe from step 3, and ask IzzyOnDroid to list the GitHub release APKs, which takes days rather
   than F-Droid's weeks.
5. **Google Play** (the user, with drafted answers). Create the app in the existing developer account
   with Play App Signing set to the release key, upload `hughandkettle-X.Y.Z.aab` from the GitHub
   release, link `PRIVACY.md`, and fill in data safety (nothing collected or shared), the content
   rating, the target audience (not children), ads (none), and the foreground service declaration for
   the `connectedDevice` link service with a short video of the link holding while the screen is off.
   A personal account opened after 13 November 2023 must first run a closed test with 12 testers opted
   in for 14 days in a row.

## Risks

- **Reproducible builds.** The release APK is built in the gate image (Temurin 21, build tools 37.0.0,
  R8). If F-Droid's build server cannot reproduce it byte for byte, F-Droid signs with its own key and
  its users cannot move to the other channels without reinstalling.
- **The name.** Play's intellectual property policy lets Hughes & Kettner have a listing taken down;
  the user chose to keep the name, with every listing saying the app is unofficial.
- **Developer verification.** From 2027 Google plans to require a registered developer for every app
  on certified phones. The Play account already registers the app id with the release key.
