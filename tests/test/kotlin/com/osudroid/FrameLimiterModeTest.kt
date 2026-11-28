package com.osudroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameLimiterModeTest {
    @Test
    fun `Stored values stay stable`() {
        assertEquals(1, FrameLimiterMode.VSync.value)
        assertEquals(-1, FrameLimiterMode.VSyncLowLatency.value)
        assertEquals(2, FrameLimiterMode.RefreshRate2x.value)
        assertEquals(4, FrameLimiterMode.RefreshRate4x.value)
        assertEquals(8, FrameLimiterMode.RefreshRate8x.value)
        assertEquals(0, FrameLimiterMode.Unlimited.value)
    }

    @Test
    fun `Every mode parses back from its stored value`() {
        FrameLimiterMode.entries.forEach {
            assertEquals(it, FrameLimiterMode.parse(it.value.toString()))
        }
    }

    @Test
    fun `Unknown values fall back to VSync`() {
        listOf(null, "", "3", "16", "abc", "-2").forEach {
            assertEquals(FrameLimiterMode.VSync, FrameLimiterMode.parse(it))
        }
    }

    @Test
    fun `Only the VSync modes are paced by vertical sync`() {
        FrameLimiterMode.entries.forEach {
            val expected = it == FrameLimiterMode.VSync || it == FrameLimiterMode.VSyncLowLatency

            if (expected) {
                assertTrue(it.name, it.isVSync)
            } else {
                assertFalse(it.name, it.isVSync)
            }
        }
    }

    @Test
    fun `Refresh rate multiples cap only the refresh-rate modes`() {
        assertEquals(2, FrameLimiterMode.RefreshRate2x.refreshRateMultiple)
        assertEquals(4, FrameLimiterMode.RefreshRate4x.refreshRateMultiple)
        assertEquals(8, FrameLimiterMode.RefreshRate8x.refreshRateMultiple)
        assertEquals(0, FrameLimiterMode.VSync.refreshRateMultiple)
        assertEquals(0, FrameLimiterMode.VSyncLowLatency.refreshRateMultiple)
        assertEquals(0, FrameLimiterMode.Unlimited.refreshRateMultiple)
    }
}
