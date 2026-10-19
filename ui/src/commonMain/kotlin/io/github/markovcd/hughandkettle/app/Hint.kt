package io.github.markovcd.hughandkettle.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Whether controls say what they do while the pointer rests on them: on the desktop, not on the phone. */
val LocalHints = staticCompositionLocalOf { false }

/** [content], saying [text] while the pointer rests on it where [LocalHints] is on. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Hint(
    text: String,
    content: @Composable () -> Unit,
) {
    if (!LocalHints.current) return content()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        content = content,
    )
}
