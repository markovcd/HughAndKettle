package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Text not yet looked up: a string resource and its arguments, read on screen or, off it, suspended. */
data class Words(
    val resource: StringResource,
    val args: List<Any> = emptyList(),
) {
    @Composable
    fun text(): String = stringResource(resource, *args.toTypedArray())

    suspend fun load(): String = getString(resource, *args.toTypedArray())
}
