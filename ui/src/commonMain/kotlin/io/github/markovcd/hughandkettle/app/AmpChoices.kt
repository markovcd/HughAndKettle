package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.wmi.WmiAddress

/**
 * Which amp sits behind each WMI-1, as written to the phone: a version line, then one
 * `<host:port> <amp-id>` per box. A line that does not read, or names an amp this version does not
 * know, is left out; a newer version reads as no choices at all.
 */
object AmpChoices {
    const val VERSION: Int = 1

    fun write(choices: Map<WmiAddress, AmpModel>): String =
        (listOf("$VERSION") + choices.map { (box, amp) -> "$box ${amp.id}" }).joinToString("\n")

    fun read(text: String?): Map<WmiAddress, AmpModel> {
        val lines = text?.lines() ?: return emptyMap()
        if (lines.first().trim().toIntOrNull() != VERSION) return emptyMap()
        return lines
            .drop(1)
            .mapNotNull { line ->
                val box = WmiAddress.parse(line.substringBefore(' ').trim()) ?: return@mapNotNull null
                val amp = AmpModels.byId(line.substringAfter(' ', "").trim()) ?: return@mapNotNull null
                box to amp
            }.toMap()
    }
}
