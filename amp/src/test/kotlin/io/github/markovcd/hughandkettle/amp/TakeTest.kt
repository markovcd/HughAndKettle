package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TakeTest {
    // At 120 BPM a beat is half a second.
    private val take = Take(120)

    private fun Take.preset(
        preset: Int,
        at: Duration,
    ) = after(SoundSwitch(preset), at)

    private fun Take.reported(
        preset: Int,
        at: Duration,
    ) = after(SoundSwitch(preset, reported = true), at)

    @Test
    fun `Each switch starts a step held until the next, the last until the end`() {
        val taken = take.preset(5, Duration.ZERO).preset(7, 8.seconds).preset(5, 12.seconds)
        assertEquals(listOf(SongStep(5, 16), SongStep(7, 8), SongStep(5, 8)), taken.steps(16.seconds))
    }

    @Test
    fun `A switch lands on the nearest beat`() {
        val taken = take.preset(5, Duration.ZERO).preset(7, 2240.milliseconds)
        assertEquals(listOf(SongStep(5, 4), SongStep(7, 4)), taken.steps(4.seconds))
        val later = take.preset(5, Duration.ZERO).preset(7, 2260.milliseconds)
        assertEquals(listOf(SongStep(5, 5), SongStep(7, 3)), later.steps(4.seconds))
    }

    @Test
    fun `Of switches landing on the same beat only the last is kept`() {
        val taken = take.preset(5, Duration.ZERO).preset(7, 2.seconds).preset(9, 2100.milliseconds)
        assertEquals(listOf(SongStep(5, 4), SongStep(9, 4)), taken.steps(4.seconds))
    }

    @Test
    fun `A switch during the count-in is the sound on the first beat`() {
        val taken = take.preset(5, Duration.ZERO).preset(7, (-1).seconds)
        assertEquals(listOf(SongStep(7, 8)), taken.steps(4.seconds))
    }

    @Test
    fun `A switch back to the same sound is still a step of its own`() {
        val taken = take.preset(5, Duration.ZERO).reported(5, 4120.milliseconds)
        assertEquals(listOf(SongStep(5, 8), SongStep(5, 4)), taken.steps(6.seconds))
    }

    @Test
    fun `A reported switch is moved back by how late the amp reports it`() {
        val taken = take.preset(5, Duration.ZERO).reported(7, 2.seconds + Take.REPORT_LAG + 200.milliseconds)
        assertEquals(listOf(SongStep(5, 4), SongStep(7, 4)), taken.steps(4.seconds))
    }

    @Test
    fun `The amp's report of a preset the app just recalled is that same switch`() {
        val taken =
            take
                .preset(5, Duration.ZERO)
                .preset(7, 2.seconds)
                .reported(7, 2.seconds + Take.REPORT_LAG + 30.milliseconds)
        assertEquals(listOf(SongStep(5, 4), SongStep(7, 4)), taken.steps(4.seconds))
    }

    @Test
    fun `A tone switch keeps its tone and the note the delay played at`() {
        val note = NoteLength.of("1/8d")
        val taken = take.preset(5, Duration.ZERO).after(SoundSwitch(tone = "Lead solo"), 2.seconds, note)
        assertEquals(
            listOf(SongStep(5, 4), SongStep(beats = 4, tone = "Lead solo", note = note)),
            taken.steps(4.seconds),
        )
    }

    @Test
    fun `The last step lasts a beat at least, and a take with no switch has no steps`() {
        val stopped = take.preset(5, Duration.ZERO).preset(7, 2.seconds)
        assertEquals(listOf(SongStep(5, 4), SongStep(7, 1)), stopped.steps(2.seconds))
        assertEquals(emptyList<SongStep>(), take.steps(10.seconds))
    }

    @Test
    fun `A sound held past the most a step lasts goes on in a step after it`() {
        val end = take.preset(5, Duration.ZERO).steps(500.milliseconds * (SongStep.MOST_BEATS + 10))
        assertEquals(listOf(SongStep(5, SongStep.MOST_BEATS), SongStep(5, 10)), end)
    }

    @Test
    fun `A take is named by the first number not taken`() {
        assertEquals("Take 1", Take.name { false })
        assertEquals("Take 3", Take.name { it in setOf("Take 1", "Take 2") })
    }
}
