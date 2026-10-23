package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.Composable
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.song_preset
import org.jetbrains.compose.resources.stringResource

/** What a step is called on the Song tab: "Preset N" for a preset step, the tone's name for a tone step. */
@Composable
internal fun songStepTitle(step: SongStep): String =
    step.preset?.let { stringResource(Res.string.song_preset, it) } ?: step.tone.orEmpty()
