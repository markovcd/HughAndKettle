package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.EffectRoll
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.fx_randomize_undo
import io.github.markovcd.hughandkettle.ui.fx_randomized
import org.jetbrains.compose.resources.stringResource

/** The bar after a randomize, naming the effects [roll] changed, with the undo tests find by `fx_randomize_undo`. */
@Composable
internal fun UndoBar(
    roll: EffectRoll,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.heightIn(min = 52.dp).padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.fx_randomized, names(roll.effects)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUndo, modifier = Modifier.testTag("fx_randomize_undo")) {
                Text(
                    stringResource(Res.string.fx_randomize_undo),
                    color = colors.inversePrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
