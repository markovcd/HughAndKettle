package io.github.markovcd.hughandkettle.app

import org.junit.Test
import kotlin.test.assertEquals

class PhoneWifiTest {
    @Test
    fun `A quoted SSID names the network without its quotes`() {
        assertEquals(PhoneWifi.Named("WMI-1_Network_a724e7"), PhoneWifi.of("\"WMI-1_Network_a724e7\""))
    }

    @Test
    fun `An SSID Android keeps back leaves the network unnamed`() {
        assertEquals(PhoneWifi.Unnamed, PhoneWifi.of("<unknown ssid>"))
        assertEquals(PhoneWifi.Unnamed, PhoneWifi.of(null))
        assertEquals(PhoneWifi.Unnamed, PhoneWifi.of("\"\""))
    }
}
