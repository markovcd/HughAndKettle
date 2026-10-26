package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.preset_picker_banks
import io.github.markovcd.hughandkettle.ui.preset_picker_count
import io.github.markovcd.hughandkettle.ui.write_confirm
import io.github.markovcd.hughandkettle.ui.write_overwrite
import io.github.markovcd.hughandkettle.ui.write_overwrite_named
import io.github.markovcd.hughandkettle.ui.write_pick_title
import io.github.markovcd.hughandkettle.ui.write_where
import io.github.markovcd.hughandkettle.ui.write_where_plain
import org.jetbrains.compose.resources.stringResource

/**
 * The amp's presets to write [tone] into, as the preset picker shows them: a tap picks one, which then says what
 * it holds will be overwritten, and Write starts the write. Tests find the picker by `write_picker`, its list by
 * `write_presets`, a preset by `write_preset:<n>`, the button by `write_confirm` and the close by `write_picker_close`.
 */
@Composable
internal fun WritePicker(
    shelf: ToneShelf,
    tone: Tone,
    onClose: () -> Unit,
) {
    val amp = tone.amp
    var picked by remember { mutableStateOf<Int?>(null) }
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.testTag("write_picker"),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PickerTitle(
                    stringResource(Res.string.write_pick_title, tone.name),
                    amp.presetsPerBank?.let { stringResource(Res.string.preset_picker_banks, amp.presetCount / it, it) }
                        ?: stringResource(Res.string.preset_picker_count, amp.presetCount),
                    onClose,
                    "write_picker_close",
                )
                PresetGrid(
                    amp,
                    shelf.preset,
                    shelf.presetNames,
                    lit = picked,
                    listTag = "write_presets",
                    tileTag = "write_preset",
                    onPick = { picked = it },
                )
                picked?.let { preset ->
                    val where = presetWhere(amp, preset)
                    Text(
                        shelf.presetNames[preset]?.let { stringResource(Res.string.write_overwrite_named, where, it) }
                            ?: stringResource(Res.string.write_overwrite, where),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = {
                            shelf.write(tone, preset)
                            onClose()
                        },
                        enabled = shelf.canWrite(tone),
                        modifier = Modifier.fillMaxWidth().testTag("write_confirm"),
                    ) { Text(stringResource(Res.string.write_confirm)) }
                }
            }
        }
    }
}

/** [preset] as the amp shows it: "Bank 12 · 3" where it has banks, else "Preset 47". */
@Composable
internal fun presetWhere(
    amp: AmpModel,
    preset: Int,
): String =
    amp.slotOf(preset)?.let { stringResource(Res.string.write_where, it.bank, it.slot) }
        ?: stringResource(Res.string.write_where_plain, preset)
