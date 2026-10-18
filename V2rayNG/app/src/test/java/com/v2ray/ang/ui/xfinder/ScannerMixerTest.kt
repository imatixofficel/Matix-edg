package com.v2ray.ang.ui.xfinder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerMixerTest {
    private val vless = "vless://uuid@sub.example.com:443?security=tls&type=ws&path=%2Fw#cdn"

    @Test
    fun mixesVlessIntoOneCopyPerIpAndKeepsTheDomainAsSniAndHost() {
        val out = ScannerMixer.mix(vless, listOf("1.1.1.1", "2.2.2.2", "3.3.3.3"), 2)
        assertEquals(2, out.size)
        assertTrue(out[0].startsWith("vless://uuid@1.1.1.1:443?"))
        assertTrue(out[0].contains("sni=sub.example.com"))
        assertTrue(out[0].contains("host=sub.example.com"))
        assertTrue(out[0].endsWith("#cdn | 1.1.1.1"))
    }

    @Test
    fun anUnsupportedLinkOrZeroCountGivesNothing() {
        assertTrue(ScannerMixer.mix("ss://abc@h:1#x", listOf("1.1.1.1"), 3).isEmpty())
        assertTrue(ScannerMixer.mix(vless, listOf("1.1.1.1"), 0).isEmpty())
        assertTrue(ScannerMixer.mix("", listOf("1.1.1.1"), 3).isEmpty())
    }

    @Test
    fun duplicateIpsAreMixedOnce() {
        assertEquals(1, ScannerMixer.mix(vless, listOf("1.1.1.1", "1.1.1.1"), 5).size)
    }
}
