package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkHolderTest {
    private val jack = SlowJack()
    private val holder = LinkHolder(jack)

    @After
    fun teardown() = holder.close()

    @Test
    fun `A cable that opens after it was given up on is closed and never used`() {
        holder.connect(jack.doremidi)
        holder.stop()
        jack.answer()
        assertTrue(jack.released)
        assertEquals(CableState.None, holder.cable.value)
        assertTrue(holder.send(ProgramChange(MidiChannel(1), 0)).isFailure)
    }

    @Test
    fun `A cable that will not open is shown failed with the reason`() {
        holder.connect(jack.doremidi)
        jack.answer(Result.failure(IllegalStateException("held by another app")))
        assertEquals(CableState.Failed(jack.doremidi, "held by another app"), holder.cable.value)
    }

    /** One cable whose open is answered only when the test says so. */
    private class SlowJack : CableJack {
        val doremidi = Cable(7, "DOREMIDI")
        var released = false
        private var pending: ((Result<CableLink>) -> Unit)? = null

        override val cables: StateFlow<List<Cable>> = MutableStateFlow(listOf(doremidi))

        override fun open(
            cable: Cable,
            onMessage: (MidiMessage) -> Unit,
            onLost: (reason: String) -> Unit,
            onOpened: (Result<CableLink>) -> Unit,
        ) {
            pending = onOpened
        }

        fun answer(result: Result<CableLink> = Result.success(link())) {
            pending?.invoke(result)
        }

        private fun link() = CableLink(doremidi, write = {}, release = { released = true })
    }
}
