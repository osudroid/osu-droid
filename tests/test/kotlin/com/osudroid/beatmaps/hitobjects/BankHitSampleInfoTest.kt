package com.osudroid.beatmaps.hitobjects

import org.junit.Assert
import org.junit.Test

class BankHitSampleInfoTest {
    @Test
    fun `Test useBeatmapSample is false when customSampleBank is 0`() {
        val sample = BankHitSampleInfo(BankHitSampleInfo.HIT_NORMAL)

        Assert.assertFalse(sample.useBeatmapSample)
    }

    @Test
    fun `Test useBeatmapSample is true when customSampleBank is at least 1`() {
        val sample = BankHitSampleInfo(BankHitSampleInfo.HIT_NORMAL, customSampleBank = 1)

        Assert.assertTrue(sample.useBeatmapSample)
    }
}
