package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals

class TempoTest {
    @Test
    fun `A delay time reads as the tempo of one beat a repeat, to the nearest beat`() {
        assertEquals(120, Tempo.beatsPerMinute(500))
        assertEquals(119, Tempo.beatsPerMinute(505))
        assertEquals(44, Tempo.beatsPerMinute(1360))
        assertEquals(1176, Tempo.beatsPerMinute(51))
    }

    @Test
    fun `Only the delay time knob is a time knob`() {
        val timed = AmpModels.all.flatMap { amp -> amp.controls.filter(Control::timed).map { "${amp.id}/${it.id}" } }
        assertEquals(AmpModels.all.map { "${it.id}/delay_time" }, timed)
    }
}
