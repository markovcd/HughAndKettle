package io.github.markovcd.hughandkettle.amp

/** One amp the app knows how to drive: its name and the controls its MIDI chart lists. */
public data class AmpModel(
    val id: String,
    val name: String,
    val controls: List<Control>,
    val presetCount: Int = 128,
) {
    init {
        val duplicateIds = controls.groupBy(Control::id).filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) { "$name repeats control ids $duplicateIds" }
        val duplicateControllers = controls.groupBy(Control::controller).filterValues { it.size > 1 }.keys
        require(duplicateControllers.isEmpty()) { "$name repeats controller numbers $duplicateControllers" }
    }

    public fun control(id: String): Control? = controls.firstOrNull { it.id == id }
}
