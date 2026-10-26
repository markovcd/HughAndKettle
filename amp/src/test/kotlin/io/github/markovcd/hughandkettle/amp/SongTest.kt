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
        assertEquals(listOf(amp.recall(7), time.set(time.scale!!.value(500))), song.cue(1, NoteLength.QUARTER) { null })
    }

    @Test
    fun `A tone step starts with the tone's values but its delay time, then the song's delay time`() {
        val lead = Tone("Lead solo", amp, mapOf("gain" to 90, "delay_time" to 10))
        val song = Song(amp, 120, listOf(SongStep(beats = 4, tone = "Lead solo")))
        assertEquals(
            listOf(amp.control("gain")!!.set(90), time.set(time.scale!!.value(500))),
            song.cue(0, NoteLength.QUARTER) { name ->
                lead.takeIf {
                    it.name ==
                        name
                }
            },
        )
    }

    @Test
    fun `A step with its own note sets the delay time to that note, the others to the note picked`() {
        val song = Song(amp, 120, listOf(SongStep(5, 4, note = NoteLength.of("1/8d")), SongStep(7, 4)))
        val scale = time.scale!!
        assertEquals(listOf(amp.recall(5), time.set(scale.value(375))), song.cue(0, NoteLength.QUARTER) { null })
        assertEquals(listOf(amp.recall(7), time.set(scale.value(500))), song.cue(1, NoteLength.QUARTER) { null })
        assertEquals(listOf(amp.recall(5)), song.cue(0, null) { null })
    }

    @Test
    fun `An untimed step keeps the delay time of its preset or tone`() {
        val lead = Tone("Lead solo", amp, mapOf("gain" to 90, "delay_time" to 10))
        val song = Song(amp, 120, listOf(SongStep(preset = 7, beats = 4), SongStep(beats = 4, tone = "Lead solo")))
        assertEquals(listOf(amp.recall(7)), song.cue(0, null) { null })
        assertEquals(lead.recall(), song.cue(1, null) { lead })
        assertEquals(emptyList(), song.cue(1, null) { null })
    }

    @Test
    fun `A step on a missing tone, or one kept for another amp, cues the delay time alone`() {
        val song = Song(amp, 120, listOf(SongStep(beats = 4, tone = "Gone")))
        val other = Tone("Gone", AmpModels.blackSpirit200, emptyMap())
        val repeat = time.set(time.scale!!.value(500))
        assertEquals(listOf(repeat), song.cue(0, NoteLength.QUARTER) { null })
        assertEquals(listOf(repeat), song.cue(0, NoteLength.QUARTER) { other })
    }

    @Test
    fun `A count-in is a bar of beats before the first step, and nothing when off`() {
        assertEquals(2.seconds, Song(amp, 120).lead)
        assertEquals(Duration.ZERO, Song(amp, 120, countIn = false).lead)
    }

    @Test
    fun `The delay time reads in the milliseconds the knob reaches`() {
        assertEquals(505, Song(amp, 120).delayMs(NoteLength.QUARTER))
    }

    @Test
    fun `A dotted eighth repeats at three quarters of a beat`() {
        assertEquals(time.set(time.scale!!.value(375)), Song(amp, 120).delayTime(NoteLength.of("1/8d")!!))
    }

    @Test
    fun `A note longer than the longest delay time sets the delay to its longest, and says it is too long`() {
        assertEquals(time.set(127), Song(amp, 40).delayTime(NoteLength.QUARTER))
        assertEquals(NoteReach.TooLong, Song(amp, 60).reach(NoteLength.of("1/2")!!))
        assertEquals(NoteReach.TooShort, Song(amp, 300).reach(NoteLength.of("1/16")!!))
        assertEquals(NoteReach.Fits, Song(amp, 120).reach(NoteLength.of("1/8d")!!))
    }

    @Test
    fun `A step on a preset the amp lacks, or a tempo out of range, is refused`() {
        assertFailsWith<IllegalArgumentException> { Song(amp, 120, listOf(SongStep(129, 4))) }
        assertFailsWith<IllegalArgumentException> { Song(amp, 401) }
        assertFailsWith<IllegalArgumentException> { SongStep(5, 0) }
        assertFailsWith<IllegalArgumentException> { SongStep(5, 4, tone = "Lead solo") }
        assertFailsWith<IllegalArgumentException> { SongStep(beats = 4) }
    }
}
