package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.amp.ControlKind.Knob
import io.github.markovcd.hughandkettle.amp.ControlKind.Selector
import io.github.markovcd.hughandkettle.amp.ControlKind.Switch
import io.github.markovcd.hughandkettle.amp.Section.Fx
import io.github.markovcd.hughandkettle.amp.Section.Header
import io.github.markovcd.hughandkettle.amp.Section.PowerAmp
import io.github.markovcd.hughandkettle.amp.Section.Preamp
import io.github.markovcd.hughandkettle.amp.Section.Switching

/**
 * The amps and their MIDI charts, as data.
 *
 * An effect's knobs need its on-off switch: the manuals' FX section has them shape an effect that is
 * heard only while switched on.
 *
 * Controller numbers come from each manual's "MIDI implementation chart / controller list"
 * (Black Spirit 200 manual chapter 8.3; GrandMeister Deluxe 40 manual chapter 8.3; GrandMeister 36
 * manual chapter 8.2). The charts list Gain and Volume twice, "soft" on 20 and 7 and "hard" on 56 and
 * 57; only the hard ones are here, since the amp reports its own knobs on them and lands where they say.
 * Selector
 * option names are the manual's where it gives them; TODO.md tracks the ones still to be read
 * off the hardware.
 */
public object AmpModels {
    private val modulationTypes = Selector(listOf("Chorus", "Flanger", "Phaser", "Tremolo"))
    private val powerSoakSteps = Selector(listOf("Off", "1 W", "5 W", "20 W", "40 W"))
    private val powerSoak36Steps = Selector(listOf("Off", "1 W", "5 W", "18 W", "36 W"))
    private val channels = Selector(listOf("Clean", "Crunch", "Lead", "Ultra"))

    /** The controls both amps share, with the same numbers, in the order the panel shows them. */
    private val common =
        listOf(
            Control("mute", "Mute", 9, Switch, Header),
            Control("channel", "Channel", 31, channels, Switching),
            Control("boost", "Boost", 64, Switch, Switching),
            Control("gain", "Gain", 56, Knob, Preamp),
            Control("bass", "Bass", 21, Knob, Preamp),
            Control("mid", "Mid", 22, Knob, Preamp),
            Control("treble", "Treble", 23, Knob, Preamp),
            Control("volume", "Volume", 57, Knob, PowerAmp),
            Control("resonance", "Resonance", 24, Knob, PowerAmp),
            Control("presence", "Presence", 25, Knob, PowerAmp),
            Control("noise_gate", "Noise gate", 63, Switch, PowerAmp),
            Control("modulation", "Modulation", 52, Switch, Fx),
            Control("modulation_type", "Modulation type", 12, modulationTypes, Fx, needs = "modulation", fine = true),
            Control("modulation_intensity", "Modulation intensity", 1, Knob, Fx, needs = "modulation"),
            Control("modulation_speed", "Modulation speed", 26, Knob, Fx, needs = "modulation"),
            Control("delay", "Delay", 53, Switch, Fx),
            Control("delay_time", "Delay time", 4, Knob, Fx, Scale(51, 1360, "ms"), needs = "delay"),
            Control("delay_feedback", "Delay feedback", 27, Knob, Fx, needs = "delay"),
            Control("delay_volume", "Delay volume", 28, Knob, Fx, needs = "delay"),
            Control("reverb", "Reverb", 54, Switch, Fx),
            Control("reverb_volume", "Reverb volume", 29, Knob, Fx, needs = "reverb"),
            Control("fx_loop", "FX loop", 55, Switch, Fx),
        )

    public val blackSpirit200: AmpModel =
        AmpModel(
            id = "black_spirit_200",
            name = "Black Spirit 200",
            controls =
                common +
                    listOf(
                        Control("cabinet", "Cabinet", 58, Selector(List(8) { "Cabinet ${it + 1}" }), PowerAmp),
                        Control("sagging", "Sagging", 59, Selector(List(8) { "Sagging ${it + 1}" }), PowerAmp),
                        Control("noise_gate_sensitivity", "Noise gate sensitivity", 62, Knob, PowerAmp),
                    ),
        )

    public val grandMeisterDeluxe40: AmpModel =
        AmpModel(
            id = "grandmeister_deluxe_40",
            name = "GrandMeister Deluxe 40",
            controls =
                common +
                    listOf(
                        Control("power_soak", "Power soak", 30, powerSoakSteps, PowerAmp),
                    ),
            // The manual's 128 presets are 32 banks of 4.
            presetsPerBank = 4,
        )

    /**
     * The GrandMeister Deluxe 40's chart, row for row, with the power soak's top steps at 18 and 36 watts.
     * The manual names the steps but not their order on cc 30; the Deluxe 40's is taken.
     */
    public val grandMeister36: AmpModel =
        AmpModel(
            id = "grandmeister_36",
            name = "GrandMeister 36",
            controls =
                common +
                    listOf(
                        Control("power_soak", "Power soak", 30, powerSoak36Steps, PowerAmp),
                    ),
            // The manual's 128 presets are 32 banks of 4.
            presetsPerBank = 4,
        )

    public val all: List<AmpModel> = listOf(blackSpirit200, grandMeisterDeluxe40, grandMeister36)

    public fun byId(id: String): AmpModel? = all.firstOrNull { it.id == id }
}
