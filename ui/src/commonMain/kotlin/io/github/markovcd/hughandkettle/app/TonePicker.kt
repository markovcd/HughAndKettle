package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.tone_picker_empty
import io.github.markovcd.hughandkettle.ui.tone_picker_empty_what
import io.github.markovcd.hughandkettle.ui.tone_picker_for
import io.github.markovcd.hughandkettle.ui.tone_picker_manage
import io.github.markovcd.hughandkettle.ui.tone_picker_title
import org.jetbrains.compose.resources.stringResource

/**
 * The pinned tones for [amp], numbered in the Tones tab's order, the one loaded ticked. A tap loads one and
 * closes the picker; the link at the foot goes to the Tones tab through [onManage]. Tests find the picker by
 * `tone_picker`, a tone by `tone_pick:<name>`, the close by `tone_picker_close` and the link by
 * `tone_picker_manage`.
 */
@Composable
internal fun TonePicker(
    shelf: ToneShelf,
    amp: AmpModel,
    onClose: () -> Unit,
    onManage: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val pinned = shelf.pinned
    val loaded = shelf.loaded
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.testTag("tone_picker"),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PickerTitle(
                    stringResource(Res.string.tone_picker_title),
                    stringResource(Res.string.tone_picker_for, amp.name),
                    onClose,
                    "tone_picker_close",
                )
                if (pinned.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PinMark(colors.onSurfaceVariant, filled = false, size = 32.dp)
                        Text(stringResource(Res.string.tone_picker_empty), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(Res.string.tone_picker_empty_what),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    pinned.forEachIndexed { index, tone ->
                        val now = tone == loaded
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    shelf.recall(tone)
                                    onClose()
                                }.padding(vertical = 12.dp, horizontal = 4.dp)
                                .testTag("tone_pick:${tone.name}"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (now) colors.primary else colors.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(24.dp),
                            )
                            Text(
                                tone.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (now) colors.primary else colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (now) TickMark(colors.primary)
                        }
                    }
                }
                TextButton(onClick = onManage, modifier = Modifier.testTag("tone_picker_manage")) {
                    Text(stringResource(Res.string.tone_picker_manage))
                }
            }
        }
    }
}
