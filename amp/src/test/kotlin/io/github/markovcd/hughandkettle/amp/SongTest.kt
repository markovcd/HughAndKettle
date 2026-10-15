package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SongTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val time = amp.control("delay_time")!!
    private val song = Song(amp, 120, listOf(SongStep(5, 8), SongStep(7, 4)))

    @Test
    fun `A beat at 120 BPM lasts half a second and the song lasts its beats`() {
        assertEquals(500.milliseconds, song.beat)
        assertEquals(6.seconds, song.length)
        assertEquals(4.seconds, song.startOf(1))
    }

    @Test
    fun `The song stands on the step and beat its elapsed time falls in`() {
        assertEquals(Song.Position(0, 1), song.at(0.seconds))
        assertEquals(Song.Position(0, 8), song.at(3999.milliseconds))
        assertEquals(Song.Position(1, 1), song.at(4.seconds))
        assertEquals(Song.Position(1, 4), song.at(5900.milliseconds))
    }

    @Test
    fun `A song is over once its last beat is out, and not yet started before its first`() {
        assertNull(song.at(6.seconds))
        assertNull(song.at((-1).milliseconds))
        assertNull(Song(amp).at(0.seconds))
    }

    @Test
    fun `A step starts with its preset, then the delay time at one repeat a beat`() {
        assertEquals(listOf(amp.recall(7), time.set(time.scale!!.value(500))), song.cue(1))
    }

    @Test
    fun `A count-in is a bar of beats before the first step, and nothing when off`() {
        assertEquals(2.seconds, Song(amp, 120).lead)
        assertEquals(Duration.ZERO, Song(amp, 120, countIn = false).lead)
    }

    @Test
    fun `The delay time reads in the milliseconds the knob reaches`() {
        assertEquals(505, Song(amp, 120).delayMs)
    }

    @Test
    fun `A tempo slower than the longest delay time sets the delay to its longest`() {
        assertEquals(time.set(127), Song(amp, 40).delayTime)
    }

    @Test
    fun `A step on a preset the amp lacks, or a tempo out of range, is refused`() {
        assertFailsWith<IllegalArgumentException> { Song(amp, 120, listOf(SongStep(129, 4))) }
        assertFailsWith<IllegalArgumentException> { Song(amp, 401) }
        assertFailsWith<IllegalArgumentException> { SongStep(5, 0) }
    }
}
