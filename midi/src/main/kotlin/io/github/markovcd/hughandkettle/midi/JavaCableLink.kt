package io.github.markovcd.hughandkettle.midi

import javax.sound.midi.Receiver
import javax.sound.midi.MidiMessage as JavaMidiMessage

/**
 * A [JavaCable] opened through Java's own MIDI: messages go out on its port to the amp, and what its port
 * from the amp gives is cut into whole messages for [onMessage] on Java's thread. Java says nothing when a
 * cable is pulled out, so a link is lost only by closing it.
 */
public class JavaCableLink private constructor(
    public val cable: JavaCable,
    private val receiver: Receiver?,
) : AmpLink {
    override fun send(message: MidiMessage): Result<Unit> = write(message.encode())

    /** Sends [bytes] as the messages they hold, each whole, as the parser cuts them. */
    public fun write(bytes: ByteArray): Result<Unit> {
        val to = receiver ?: return Result.failure(IllegalStateException("${cable.name} takes no messages to send"))
        val messages = MidiStreamParser().feed(bytes)
        return runCatching { synchronized(to) { messages.forEach { to.send(JavaCables.toJava(it), -1) } } }
    }

    override fun close() {
        cable.fromAmp?.close()
        receiver?.close()
        cable.toAmp?.close()
    }

    /** What the amp sends, cut into whole messages. */
    private class Hearing(
        private val onMessage: (MidiMessage) -> Unit,
    ) : Receiver {
        private val parser = MidiStreamParser()

        override fun send(
            message: JavaMidiMessage,
            timeStamp: Long,
        ) = synchronized(parser) { parser.feed(message.message) }.forEach(onMessage)

        override fun close() = Unit
    }

    public companion object {
        /** Opens both ways of [cable] it has; one Java will not open is a failure value, with both closed again. */
        public fun open(
            cable: JavaCable,
            onMessage: (MidiMessage) -> Unit = {},
        ): Result<JavaCableLink> {
            val toAmp = cable.toAmp
            val fromAmp = cable.fromAmp
            if (toAmp == null && fromAmp == null) {
                return Result.failure(IllegalStateException("${cable.name} has no port to open"))
            }
            val opened =
                runCatching {
                    toAmp?.open()
                    val receiver = toAmp?.receiver
                    fromAmp?.open()
                    fromAmp?.transmitter?.receiver = Hearing(onMessage)
                    JavaCableLink(cable, receiver)
                }
            return opened.fold({ Result.success(it) }) {
                fromAmp?.close()
                toAmp?.close()
                Result.failure(
                    IllegalStateException("Java would not open ${cable.name}; another program may hold it", it),
                )
            }
        }
    }
}
