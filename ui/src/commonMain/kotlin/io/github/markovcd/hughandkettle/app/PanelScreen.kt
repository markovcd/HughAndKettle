package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration

/**
 * What a window of either app shows and acts on, over one [link] and what [home] keeps: the panel, the WMI-1s
 * heard through [discovery], the preset names, the tone shelf and the songs. A song counts in with [click],
 * played the sound output's [lag] ahead. The shelf hands a renamed tone to the songs, and the songs look
 * tones up on the shelf.
 */
class PanelScreen(
    home: AppHome,
    link: LinkHolder,
    scope: CoroutineScope,
    val discovery: DiscoveryState,
    click: () -> Unit,
    lag: suspend () -> Duration = { Duration.ZERO },
) {
    val panel: PanelState =
        PanelState(
            AmpModels.all.first(),
            link,
            scope,
            home.lastWmi.last,
            home.amps.choices,
            home.amps::remember,
        )
    val presetNames: PresetNamesState = PresetNamesState(home.presetNames, panel, scope)
    val songs: SongState =
        SongState(
            home.songs,
            panel,
            scope,
            click = click,
            lag = lag,
            tones = { shelf.tones },
            recordClick = home.recordClick.on,
            keepRecordClick = home.recordClick::keep,
        )
    val shelf: ToneShelf =
        ToneShelf(
            home.tones,
            panel,
            scope,
            heard = link.heard,
            names = { presetNames.names },
            renamed = songs::renameTone,
            named = presetNames::name,
        )
}
