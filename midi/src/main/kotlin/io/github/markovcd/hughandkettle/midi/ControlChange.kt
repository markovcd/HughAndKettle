package io.github.markovcd.hughandkettle.midi

public data class ControlChange(
    val channel: MidiChannel,
    val controller: Int,
    val value: Int,
) : MidiMessage {
    init {
        require(controller in 0..127) { "controller must be 0..127, got $controller" }
        require(value in 0..127) { "value must be 0..127, got $value" }
    }

    override fun encode(): ByteArray = byteArrayOf((0xB0 or channel.wire).toByte(), controller.toByte(), value.toByte())
}
