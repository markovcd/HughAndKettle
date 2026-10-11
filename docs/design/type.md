# Type

IBM Plex Sans Condensed for everything, IBM Plex Mono for addresses and ports. Both under the SIL
Open Font License 1.1 ([fonts/OFL.txt](fonts/OFL.txt)), bundled as `.ttf` in `app/src/main/res/font/`
(Sans Condensed 400, 500, 600; Mono 400). Condensed width leaves room for labels 30% longer. Every
number uses tabular figures (`fontFeatureSettings = "tnum"`), right-aligned in a fixed three-digit box
so 99 to 100 does not shift the knob above it. A control's name wraps to two lines and the row grows;
no ellipsis on a name.

| Role | Use | Family and weight | Size / line | Note |
|---|---|---|---|---|
| displaySmall | preset number | Sans Condensed SemiBold 600 | 40 / 48 sp | tnum |
| headlineSmall | a control's value | Sans Condensed SemiBold 600 | 28 / 36 sp | tnum |
| titleLarge | amp name, section | Sans Condensed Medium 500 | 22 / 28 sp | |
| labelLarge | a control's name, buttons | Sans Condensed Medium 500 | 15 / 20 sp | +0.1 sp tracking |
| bodyMedium | status line, hints | Sans Condensed Regular 400 | 15 / 20 sp | |
| bodyLarge | address and port fields | Plex Mono Regular 400 | 16 / 24 sp | |

Section labels are `labelMedium`-sized uppercase in onSurfaceVariant with 0.6 sp tracking.
