package io.github.markovcd.hughandkettle.app

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ThemeStoreTest {
    private val app = ApplicationProvider.getApplicationContext<HughAndKettleApplication>()

    private fun choice(): ThemeChoice = runBlocking { app.theme.choice.first() }

    @Test
    fun `A phone that never picked wears dark, and a picked theme reads back`() {
        assertEquals(ThemeChoice.Dark, choice())
        runBlocking { app.theme.pick(ThemeChoice.Light) }
        assertEquals(ThemeChoice.Light, choice())
    }
}
