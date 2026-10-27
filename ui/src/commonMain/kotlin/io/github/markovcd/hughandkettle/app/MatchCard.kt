package io.github.markovcd.hughandkettle.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.match_card
import io.github.markovcd.hughandkettle.ui.match_clear
import io.github.markovcd.hughandkettle.ui.match_clear_all
import io.github.markovcd.hughandkettle.ui.match_corrected
import io.github.markovcd.hughandkettle.ui.match_corrected_one
import io.github.markovcd.hughandkettle.ui.match_none_on
import io.github.markovcd.hughandkettle.ui.match_on
import io.github.markovcd.hughandkettle.ui.trim_applied
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * The loudness match's corrections on the Amp tab, while there are any: the correction of the preset playing, how
 * many presets have one, and clearing one or all. Each recall a correction moves lights the card and reads
 * "Applied" for a second, as the volume trim's does. Tests find the card by `match_row` and its buttons by
 * `match_clear:<n>` and `match_clear_all`.
 */
@Composable
internal fun MatchCard(
    match: MatchState,
    panel: PanelState,
    names: PresetNamesState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val corrections = match.corrections.amounts
    if (corrections.isEmpty()) return
    var applied by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(panel.corrected) }
    LaunchedEffect(panel.corrected) {
        if (panel.corrected == answered) return@LaunchedEffect
        answered = panel.corrected
        applied = true
        delay(1.seconds)
        applied = false
    }
    val ground by animateColorAsState(
        if (applied) colors.primary.copy(alpha = 0.1f) else colors.surfaceContainer,
        tween(if (applied) APPLIED_RISE_MS else APPLIED_FADE_MS),
    )
    val preset = panel.state.preset
    val correction = preset?.let { corrections[it] }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ground)
            .border(1.dp, if (correction != null) colors.primary else colors.outlineVariant, shape)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            .testTag("match_row"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.match_card).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val name = preset?.let { names.names[it] ?: presetWhere(panel.amp, it) }
                Text(
                    when {
                        name == null -> ""
                        correction == null -> stringResource(Res.string.match_none_on, name)
                        else -> stringResource(Res.string.match_on, signed(correction), name)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (applied) {
                    Text(
                        stringResource(Res.string.trim_applied),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(8.dp),
                                ).background(colors.primary)
                                .padding(horizontal = 7.dp),
                    )
                }
            }
            Text(
                if (corrections.size == 1) {
                    stringResource(Res.string.match_corrected_one)
                } else {
                    stringResource(Res.string.match_corrected, corrections.size)
                },
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
        }
        if (preset != null && correction != null) {
            OutlinedButton(
                onClick = { match.clear(preset) },
                enabled = enabled,
                modifier = Modifier.testTag("match_clear:$preset"),
            ) { Text(stringResource(Res.string.match_clear)) }
        }
        TextButton(onClick = match::clearAll, modifier = Modifier.testTag("match_clear_all")) {
            Text(stringResource(Res.string.match_clear_all))
        }
    }
}

/** [amount] with its sign, a true minus for a cut. */
internal fun signed(amount: Int): String = if (amount < 0) "−${-amount}" else "+$amount"

// The lit card rises at once and fades back over most of the second it is lit.
private const val APPLIED_RISE_MS = 120
private const val APPLIED_FADE_MS = 900
