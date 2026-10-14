package io.github.markovcd.hughandkettle.app

/** The WiFi network the phone is on, as far as Android will say. */
sealed interface PhoneWifi {
    data object Off : PhoneWifi

    /** On WiFi, with the network's name kept back: Android names it only to an app granted location. */
    data object Unnamed : PhoneWifi

    data class Named(
        val name: String,
    ) : PhoneWifi

    companion object {
        // What Android reports in place of a name it keeps back.
        private const val UNKNOWN_SSID = "<unknown ssid>"

        /** The network an SSID as Android reports it names: quoted when it is text, bare when it is hex. */
        fun of(ssid: String?): PhoneWifi {
            val name = ssid?.removeSurrounding("\"")
            return if (name.isNullOrBlank() || name == UNKNOWN_SSID) Unnamed else Named(name)
        }
    }
}
