package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control

/** On is an amber track with a dark thumb and a check; off is an outlined track with a small grey thumb. */
@Composable
internal fun SwitchRow(
    control: Control,
    value: Int,
    enabled: Boolean,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val on = value >= 64
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(control.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = on,
            onCheckedChange = { onValue(if (it) 127 else 0) },
            enabled = enabled,
            thumbContent =
                if (on) {
                    { CheckMark(MaterialTheme.colorScheme.onPrimary, Modifier.size(SwitchDefaults.IconSize)) }
                } else {
                    null
                },
        )
    }
}

/** A check drawn in place: the one glyph the switch needs is not worth an icon library. */
@Composable
private fun CheckMark(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path =
            Path().apply {
                moveTo(w * 0.2f, h * 0.55f)
                lineTo(w * 0.42f, h * 0.76f)
                lineTo(w * 0.82f, h * 0.3f)
            }
        drawPath(path, color, style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
