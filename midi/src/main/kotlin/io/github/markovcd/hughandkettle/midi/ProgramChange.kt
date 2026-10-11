package io.github.markovcd.hughandkettle.midi

public data class ProgramChange(
    val channel: MidiChannel,
    val program: Int,
) : MidiMessage {
    init {
        require(program in 0..127) { "program must be 0..127, got $program" }
    }

    override fun encode(): ByteArray = byteArrayOf((0xC0 or channel.wire).toByte(), program.toByte())
}
