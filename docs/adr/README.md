# Architecture decision records

One file per decision, in Michael Nygard's shape: context, decision, consequences. A title is a
sentence stating the decision. Check here before proposing a change of shape (the `adrs` skill).

| # | Decision |
|---|---|
| [0001](0001-kotlin-and-compose-over-pure-jvm-modules.md) | The app is Kotlin and Jetpack Compose, and everything but the screen lives in pure JVM modules |
| [0002](0002-the-wmi-1-is-a-tcp-midi-bridge-driven-by-first-party-code.md) | The WMI-1 is driven as a transparent TCP MIDI bridge, with first-party code and no MIDI library |
| [0003](0003-amp-midi-charts-are-data.md) | An amp's MIDI chart is a table of controls, one per amp model, with ids that are persisted |
| [0004](0004-the-gate-is-a-container-and-the-tests-run-on-the-jvm.md) | The gate is a container build, and every test runs on the JVM without an emulator |
| [0005](0005-the-cli-is-the-first-client-of-every-feature.md) | `hk` is the first client of every feature, and the fake WMI-1 is how the app is driven without an amp |
| [0006](0006-releases-are-signed-apks-on-github-releases.md) | A release is a signed APK and a CLI zip on GitHub Releases, versioned from a tag and signed with an SSH key |
| [0007](0007-the-link-is-held-by-the-process-and-kept-alive-by-a-foreground-service.md) | The link is held by the process and kept alive by a foreground service while it is up |
| [0008](0008-tones-are-files-of-raw-control-values-apart-from-the-amps-presets.md) | A tone is a file of raw control values, kept apart from the amp's presets |
| [0009](0009-a-usb-midi-cable-reaches-the-amp-without-a-wmi-1.md) | A USB MIDI cable reaches the amp without a WMI-1 |
| [0010](0010-preset-names-live-on-the-phone-and-presets-read-into-tones.md) | Preset names live on the phone, per amp model, and the amp's presets are read into tones |
| [0011](0011-the-grandmeister-apps-preset-files-import-as-tones.md) | The GrandMeister app's preset files import as tones |
