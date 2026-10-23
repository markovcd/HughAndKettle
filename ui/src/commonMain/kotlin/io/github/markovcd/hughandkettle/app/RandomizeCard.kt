package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.fx_randomize
import io.github.markovcd.hughandkettle.ui.fx_randomize_changes
import io.github.markovcd.hughandkettle.ui.fx_randomize_kept
import io.github.markovcd.hughandkettle.ui.fx_randomize_locked
import io.github.markovcd.hughandkettle.ui.fx_randomize_none
import io.github.markovcd.hughandkettle.ui.names_and
import org.jetbrains.compose.resources.stringResource

/**
 * The FX tab's dice: a button that gives the effects that are on new values, over a line naming them and
 * the locked time it keeps. With no effect on it is disabled and says to turn one on. Tests find the
 * button by `fx_randomize`.
 */
@Composable
internal fun RandomizeCard(
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val effects = panel.rollable
    val kept = panel.lock?.control?.takeIf { time -> effects.any { it.id == time.needs } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            onClick = { panel.randomize() },
            enabled = enabled && effects.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.primary,
                ),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("fx_randomize"),
        ) {
            val tint = if (enabled && effects.isNotEmpty()) colors.primary else colors.onSurface.copy(alpha = 0.38f)
            Dice(tint)
            Text(
                stringResource(Res.string.fx_randomize),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val style = MaterialTheme.typography.bodyMedium
            if (effects.isEmpty()) {
                Note(stringResource(Res.string.fx_randomize_none))
                return@Row
            }
            val changes = stringResource(Res.string.fx_randomize_changes, names(effects))
            if (kept == null) {
                Note(changes)
                return@Row
            }
            Note("$changes ${stringResource(Res.string.fx_randomize_kept, kept.name)}")
            Padlock(open = false, color = colors.primary, size = 13.dp)
            Text(stringResource(Res.string.fx_randomize_locked), style = style, color = colors.onSurfaceVariant)
        }
    }
}

/** The names of [effects] as a sentence lists them: "Modulation", "Modulation and Delay", "Modulation, Delay and Reverb". */
@Composable
internal fun names(effects: List<Control>): String {
    val words = effects.map { it.name }
    if (words.size < 2) return words.joinToString()
    return stringResource(Res.string.names_and, words.dropLast(1).joinToString(", "), words.last())
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** A die showing five. */
@Composable
private fun Dice(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val u = size.width / 24
        drawRoundRect(color, Offset(3 * u, 3 * u), Size(18 * u, 18 * u), CornerRadius(4 * u), style = Stroke(2 * u))
        listOf(8 to 8, 16 to 8, 12 to 12, 8 to 16, 16 to 16).forEach { (x, y) ->
            drawCircle(color, 1.6f * u, Offset(x * u, y * u))
        }
    }
}
