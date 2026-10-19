package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/** Calls [onClick] when the secondary mouse button goes down: a right-click, the desktop's way of asking for a menu. */
internal fun Modifier.onSecondaryClick(onClick: () -> Unit): Modifier =
    pointerInput(onClick) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    event.changes.forEach { it.consume() }
                    onClick()
                }
            }
        }
    }
