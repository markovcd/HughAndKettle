package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.amp.ControlKind.Knob
import io.github.markovcd.hughandkettle.amp.ControlKind.Selector
import io.github.markovcd.hughandkettle.amp.ControlKind.Switch

/**
 * The amps and their MIDI charts, as data.
 *
 * Controller numbers come from each manual's "MIDI implementation chart / controller list"
 * (Black Spirit 200 manual chapter 8.3; GrandMeister Deluxe 40 manual chapter 8.3). Selector
 * option names are the manual's where it gives them; TODO.md tracks the ones still to be read
 * off the hardware.
 */
public object AmpModels {
    private val modulationTypes = Selector(listOf("Chorus", "Flanger", "Phaser", "Tremolo"))
    private val channels = Selector(listOf("Clean", "Crunch", "Lead", "Ultra"))

    /** The controls both amps share, with the same numbers. */
    private val common =
        listOf(
            Control("modulation_intensity", "Modulation intensity", 1, Knob),
            Control("delay_time", "Delay time", 4, Knob),
            Control("volume", "Volume", 7, Knob),
            Control("mute", "Mute", 9, Switch),
            Control("modulation_type", "Modulation type", 12, modulationTypes),
            Control("gain", "Gain", 20, Knob),
            Control("bass", "Bass", 21, Knob),
            Control("mid", "Mid", 22, Knob),
            Control("treble", "Treble", 23, Knob),
            Control("resonance", "Resonance", 24, Knob),
            Control("presence", "Presence", 25, Knob),
            Control("modulation_speed", "Modulation speed", 26, Knob),
            Control("delay_feedback", "Delay feedback", 27, Knob),
            Control("delay_volume", "Delay volume", 28, Knob),
            Control("reverb_volume", "Reverb volume", 29, Knob),
            Control("channel", "Channel", 31, channels),
            Control("modulation", "Modulation", 52, Switch),
            Control("delay", "Delay", 53, Switch),
            Control("reverb", "Reverb", 54, Switch),
            Control("fx_loop", "FX loop", 55, Switch),
            Control("gain_hard", "Gain (hard)", 56, Knob),
            Control("volume_hard", "Volume (hard)", 57, Knob),
            Control("noise_gate", "Noise gate", 63, Switch),
            Control("boost", "Boost", 64, Switch),
        )

    public val blackSpirit200: AmpModel =
        AmpModel(
            id = "black_spirit_200",
            name = "Black Spirit 200",
            controls =
                common +
                    listOf(
                        Control("cabinet", "Cabinet", 58, Selector(List(8) { "Cabinet ${it + 1}" })),
                        Control("sagging", "Sagging", 59, Selector(List(8) { "Sagging ${it + 1}" })),
                        Control("noise_gate_sensitivity", "Noise gate sensitivity", 62, Knob),
                    ),
        )

    public val grandMeisterDeluxe40: AmpModel =
        AmpModel(
            id = "grandmeister_deluxe_40",
            name = "GrandMeister Deluxe 40",
            controls =
                common +
                    listOf(
                        Control("power_soak", "Power soak", 30, Selector(List(5) { "Power soak ${it + 1}" })),
                    ),
        )

    public val all: List<AmpModel> = listOf(blackSpirit200, grandMeisterDeluxe40)

    public fun byId(id: String): AmpModel? = all.firstOrNull { it.id == id }
}
