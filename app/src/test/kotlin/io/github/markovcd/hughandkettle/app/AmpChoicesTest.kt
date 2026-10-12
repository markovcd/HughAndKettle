package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import org.junit.Test
import kotlin.test.assertEquals

class AmpChoicesTest {
    private val choices =
        mapOf(
            WmiAddress("192.168.1.1", 2000) to AmpModels.grandMeisterDeluxe40,
            WmiAddress("10.0.0.7", 2001) to AmpModels.blackSpirit200,
        )

    @Test
    fun `Choices read back as written`() {
        assertEquals(choices, AmpChoices.read(AmpChoices.write(choices)))
    }

    @Test
    fun `A line that does not read or names an unknown amp is left out`() {
        val text = "1\n192.168.1.1:2000 grandmeister_deluxe_40\nnot a line\n10.0.0.7:2001 jcm800\n:99 black_spirit_200"
        assertEquals(mapOf(WmiAddress("192.168.1.1", 2000) to AmpModels.grandMeisterDeluxe40), AmpChoices.read(text))
    }

    @Test
    fun `Nothing saved or a newer version reads as no choices`() {
        assertEquals(
            listOf(emptyMap(), emptyMap()),
            listOf(null, "2\n192.168.1.1:2000 black_spirit_200").map(AmpChoices::read),
        )
    }
}
