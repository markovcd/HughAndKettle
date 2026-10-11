# Colors

Material 3 roles, dark first, light derived from the same hues with the tones flipped. Dynamic color
is off. Ratios are the role's text on its container (WCAG 2).

## Dark, the stage scheme

| Role | Hex | On | Hex | Ratio |
|---|---|---|---|---|
| primary | `#FFB547` | onPrimary | `#422C00` | 7.5:1 |
| primaryContainer | `#5E4100` | onPrimaryContainer | `#FFDDAF` | 7.3:1 |
| secondary | `#D8C3A8` | onSecondary | `#3B2E1C` | 7.7:1 |
| secondaryContainer | `#534430` | onSecondaryContainer | `#F5DFC3` | 7.3:1 |
| tertiary | `#A7C9D6` | onTertiary | `#0E3440` | 7.6:1 |
| tertiaryContainer | `#284B58` | onTertiaryContainer | `#C6E7F3` | 7.2:1 |
| error | `#FF8FAE` | onError | `#5C0024` | 6.6:1 |
| errorContainer | `#861B3F` | onErrorContainer | `#FFD9E1` | 7.3:1 |

| Surface role | Hex | onSurface `#ECE4DA` ratio |
|---|---|---|
| background, surface | `#14110F` | 14.9 |
| surfaceVariant | `#3D3731` | 9.3 (onSurfaceVariant `#C4B9AC`: 6.1) |
| surfaceContainerLowest | `#0F0D0B` | 15.4 |
| surfaceContainerLow | `#1C1916` | 13.9 |
| surfaceContainer | `#211D1A` | 13.3 |
| surfaceContainerHigh | `#2B2723` | 11.8 |
| surfaceContainerHighest | `#36312C` | 10.2 |
| outline | `#8E8377` | 5.1 |
| outlineVariant | `#4A433C` | decoration only |

## Light, derived

| Role | Hex | On | Hex | Ratio |
|---|---|---|---|---|
| primary | `#7A5200` | onPrimary | `#FFFFFF` | 6.9:1 |
| primaryContainer | `#FFDDAF` | onPrimaryContainer | `#281800` | 13.3:1 |
| secondary | `#6B5B45` | onSecondary | `#FFFFFF` | 6.5:1 |
| secondaryContainer | `#F5DFC3` | onSecondaryContainer | `#251A0B` | 13.2:1 |
| tertiary | `#39616F` | onTertiary | `#FFFFFF` | 6.7:1 |
| tertiaryContainer | `#C6E7F3` | onTertiaryContainer | `#001F28` | 13.1:1 |
| error | `#A8274F` | onError | `#FFFFFF` | 6.8:1 |
| errorContainer | `#FFD9E1` | onErrorContainer | `#3F0016` | 13.3:1 |

| Surface role | Hex | onSurface `#1F1B17` ratio |
|---|---|---|
| background, surface | `#FBF7F2` | 16.0 |
| surfaceVariant | `#EDE3D6` | 13.5 (onSurfaceVariant `#4E463D`: 7.3) |
| surfaceContainerLowest | `#FFFFFF` | 17.1 |
| surfaceContainerLow | `#F6F0E9` | 15.1 |
| surfaceContainer | `#F0E9E1` | 14.2 |
| surfaceContainerHigh | `#EAE3DA` | 13.4 |
| surfaceContainerHighest | `#E4DCD2` | 12.6 |
| outline | `#7F7468` | 4.3 |
| outlineVariant | `#D2C6B8` | decoration only |

## The two signals

Live: a round dot, a pill, amber (primary). Wrong: a triangle or octagon, squared corners, rose
(error). Each carries a word, so neither relies on hue, and they differ in lightness as well as hue.
