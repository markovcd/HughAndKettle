package io.github.markovcd.hughandkettle.desktop

import io.github.markovcd.hughandkettle.app.Cable
import io.github.markovcd.hughandkettle.app.CableJack
import io.github.markovcd.hughandkettle.app.CableLink
import io.github.markovcd.hughandkettle.midi.JavaCable
import io.github.markovcd.hughandkettle.midi.JavaCableLink
import io.github.markovcd.hughandkettle.midi.JavaCables
import io.github.markovcd.hughandkettle.midi.MidiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * The MIDI cables Java's MIDI system sees, the ones `hk cable` uses. Java tells nobody when a cable
 * comes or goes, so the shell calls [look] every so often.
 */
class JavaCableJack(
    private val find: () -> Result<List<JavaCable>> = JavaCables::all,
) : CableJack {
    private val ids = ConcurrentHashMap<String, Int>()
    private val plugged = ConcurrentHashMap<Int, JavaCable>()
    private val lost = ConcurrentHashMap<Int, (String) -> Unit>()
    private val found = MutableStateFlow(emptyList<Cable>())

    override val cables: StateFlow<List<Cable>> = found.asStateFlow()

    /** Looks again at what is plugged in. A cable keeps its id for as long as the app runs. */
    @Synchronized
    fun look() {
        // Only a cable the app can send to counts; one that only listens cannot drive the amp.
        val now = find().getOrDefault(emptyList()).filter { it.toAmp != null }
        val seen = now.associateBy { ids.getOrPut(it.name) { ids.size + 1 } }
        plugged.keys.filterNot(seen::containsKey).forEach { id ->
            plugged.remove(id)
            lost.remove(id)?.invoke("unplugged")
        }
        plugged.putAll(seen)
        found.value = seen.map { (id, cable) -> Cable(id, cable.name) }.sortedBy { it.id }
    }

    override fun open(
        cable: Cable,
        onMessage: (MidiMessage) -> Unit,
        onLost: (reason: String) -> Unit,
        onOpened: (Result<CableLink>) -> Unit,
    ) {
        val java = plugged[cable.id]
        if (java?.toAmp == null) {
            onOpened(Result.failure(IllegalStateException("not plugged in")))
            return
        }
        val link =
            JavaCableLink.open(java, onMessage).getOrElse {
                onOpened(Result.failure(it))
                return
            }
        lost[cable.id] = onLost
        onOpened(
            Result.success(
                CableLink(
                    cable,
                    write = { link.write(it).getOrThrow() },
                    release = {
                        lost.remove(cable.id)
                        link.close()
                    },
                ),
            ),
        )
    }
}
