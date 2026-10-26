package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.PresetNames
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.preset_bank
import io.github.markovcd.hughandkettle.ui.preset_bank_now
import io.github.markovcd.hughandkettle.ui.preset_no_name
import org.jetbrains.compose.resources.stringResource

/**
 * Every preset of [amp] in rows of four, a row per bank where the amp has banks, opened on the [current]
 * preset's row, which is marked, and the [lit] preset amber. The list is tagged [listTag], each row
 * `preset_bank:<n>` and each preset `<tileTag>:<n>`; holding a preset calls [onHold] when given.
 */
@Composable
internal fun PresetGrid(
    amp: AmpModel,
    current: Int?,
    names: PresetNames,
    lit: Int?,
    listTag: String,
    tileTag: String,
    onPick: (Int) -> Unit,
    onHold: ((Int) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val perRow = amp.presetsPerBank ?: PER_ROW
    val rows = amp.presetCount / perRow
    val banked = amp.presetsPerBank != null
    val now = current?.let { (it - 1) / perRow } ?: 0
    LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = now),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.heightIn(max = 480.dp).testTag(listTag),
    ) {
        items(rows) { row ->
            Column(
                Modifier.testTag("preset_bank:${row + 1}"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (banked) {
                    val marked = row == now && current != null
                    Text(
                        stringResource(if (marked) Res.string.preset_bank_now else Res.string.preset_bank, row + 1),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (marked) colors.primary else colors.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..perRow).map { row * perRow + it }.forEach { preset ->
                        PresetTile(
                            preset,
                            names[preset],
                            lit = preset == lit,
                            onPick = { onPick(preset) },
                            onHold = onHold?.let { { it(preset) } },
                            tag = "$tileTag:$preset",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** A preset's number large and its name small, or "No name" in italics; amber when [lit]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetTile(
    preset: Int,
    name: String?,
    lit: Boolean,
    onPick: () -> Unit,
    onHold: (() -> Unit)?,
    tag: String,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = if (lit) colors.primary else colors.surfaceContainerHigh,
        border = if (lit) null else BorderStroke(1.dp, colors.outlineVariant),
        modifier =
            modifier
                .clip(shape)
                .combinedClickable(role = Role.Button, onLongClick = onHold, onClick = onPick)
                .testTag(tag),
    ) {
        Column(
            Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$preset",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 20.sp,
                color = if (lit) colors.onPrimary else colors.onSurface,
            )
            Text(
                name ?: stringResource(Res.string.preset_no_name),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                fontStyle = if (name == null) FontStyle.Italic else FontStyle.Normal,
                color = if (lit) colors.onPrimary else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
