package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R

/** Address, port, one button and a status line that is never hidden, in any state. */
@Composable
internal fun LinkCard(
    panel: PanelState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusLine(panel)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = panel.host,
                    onValueChange = { panel.host = it },
                    label = { Text(stringResource(R.string.host)) },
                    singleLine = true,
                    enabled = panel.idle,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("host"),
                )
                OutlinedTextField(
                    value = panel.port,
                    onValueChange = { panel.port = it },
                    label = { Text(stringResource(R.string.port)) },
                    singleLine = true,
                    enabled = panel.idle,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(96.dp).testTag("port"),
                )
            }
            if (panel.idle) {
                Button(onClick = panel::connect, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("connect")) {
                    Text(stringResource(R.string.connect))
                }
            } else {
                OutlinedButton(
                    onClick = panel::disconnect,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("disconnect"),
                ) {
                    Text(stringResource(R.string.disconnect))
                }
            }
        }
    }
}

@Composable
private fun StatusLine(panel: PanelState) {
    val failure = panel.failure
    val link = panel.link
    val colors = MaterialTheme.colorScheme
    val band = if (failure != null) Modifier.background(colors.errorContainer, RoundedCornerShape(8.dp)) else Modifier
    val text =
        when {
            failure != null -> {
                failure.words()
            }

            link != null -> {
                stringResource(
                    R.string.connected_to,
                    link.remote.address.hostAddress ?: link.remote.toString(),
                )
            }

            panel.connecting -> {
                stringResource(R.string.connecting_to, "${panel.host.trim()}:${panel.port}")
            }

            else -> {
                stringResource(R.string.not_connected)
            }
        }
    Row(
        modifier = band.defaultMinSize(minHeight = 36.dp).padding(horizontal = 10.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            failure != null -> WarningTriangle(colors.error)
            link != null -> Box(Modifier.size(14.dp).background(colors.primary, CircleShape))
            panel.connecting -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.4.dp)
            else -> Box(Modifier.size(14.dp).border(2.dp, colors.outline, CircleShape))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (failure != null) colors.onErrorContainer else colors.onSurface,
            modifier = Modifier.testTag("status"),
        )
    }
}

@Composable
private fun LinkFailure.words(): String =
    when (this) {
        LinkFailure.NeedsAddress -> stringResource(R.string.link_needs_address)
        is LinkFailure.Refused -> stringResource(R.string.link_refused, target)
        is LinkFailure.Unreachable -> stringResource(R.string.link_unreachable, target, reason)
        is LinkFailure.SendFailed -> stringResource(R.string.send_failed, reason)
    }

@Composable
private fun WarningTriangle(color: Color) {
    Canvas(Modifier.size(18.dp, 16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        val outline =
            Path().apply {
                moveTo(w / 2, stroke / 2)
                lineTo(w - stroke / 2, h - stroke / 2)
                lineTo(stroke / 2, h - stroke / 2)
                close()
            }
        drawPath(outline, color, style = Stroke(stroke, join = StrokeJoin.Round))
        drawLine(color, Offset(w / 2, h * 0.4f), Offset(w / 2, h * 0.62f), strokeWidth = stroke)
        drawCircle(color, radius = stroke * 0.55f, center = Offset(w / 2, h * 0.77f))
    }
}
