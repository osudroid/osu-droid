package com.rian.osu.utils

import org.junit.Assert
import org.junit.Test

class CircleSizeCalculatorTest {
    @Test
    fun `Test osu!droid circle size to osu!droid scale conversion`() {
        fun test(cs: Float, scale: Float) =
            Assert.assertEquals(scale, CircleSizeCalculator.droidCSToDroidScale(cs), 1e-5f)

        test(0f, 1.3304397f)
        test(2f, 1.1903822f)
        test(3.5f, 1.0853392f)
        test(4f, 1.0503248f)
        test(5f, 0.98029613f)
        test(6f, 0.91026735f)
        test(8f, 0.77020997f)
        test(10f, 0.6301526f)
        test(12f, 0.49009523f)
        test(14f, 0.35003784f)
        test(16f, 0.2099805f)
        test(17f, 0.13995178f)
        test(18f, 0.06992312f)
        test(19f, 0.001f)
        test(20f, 0.001f)
    }

    @Test
    fun `Test osu!droid scale to osu!droid circle size conversion`() {
        fun test(scale: Float, cs: Float) =
            Assert.assertEquals(cs, CircleSizeCalculator.droidScaleToDroidCS(scale), 1e-5f)

        test(1.3304397f, 0f)
        test(1.1903822f, 2f)
        test(1.0853392f, 3.5f)
        test(1.0503248f, 4f)
        test(0.98029613f, 5f)
        test(0.91026735f, 6f)
        test(0.77020997f, 8f)
        test(0.6301526f, 10f)
        test(0.49009523f, 12f)
        test(0.35003784f, 14f)
        test(0.2099805f, 16f)
        test(0.13995178f, 17f)
        test(0.06992312f, 18f)
        test(0.001f, 18.984211f)
    }

    @Test
    fun `Test osu!droid circle size to old osu!droid difficulty scale conversion`() {
        fun test(cs: Float, scale: Float) =
            Assert.assertEquals(scale, CircleSizeCalculator.droidCSToOldDroidDifficultyScale(cs), 1e-5f)

        test(0f, 1.7818792f)
        test(2f, 1.5832541f)
        test(3.5f, 1.4342854f)
        test(4f, 1.3846292f)
        test(5f, 1.2853167f)
        test(6f, 1.1860042f)
        test(8f, 0.9873792f)
        test(10f, 0.7887542f)
        test(12f, 0.5901292f)
        test(14f, 0.3915042f)
        test(16f, 0.1928792f)
        test(17f, 0.0935667f)
        test(18f, 0.001f)
        test(19f, 0.001f)
        test(20f, 0.001f)
    }

    @Test
    fun `Test old osu!droid difficulty scale to osu!droid circle size conversion`() {
        fun test(scale: Float, cs: Float) =
            Assert.assertEquals(CircleSizeCalculator.droidOldDifficultyScaleToDroidCS(scale), cs, 1e-5f)

        test(1.7818792f, 0f)
        test(1.5832541f, 2f)
        test(1.4342854f, 3.5f)
        test(1.3846292f, 4f)
        test(1.2853167f, 5f)
        test(1.1860042f, 6f)
        test(0.9873792f, 8f)
        test(0.7887542f, 10f)
        test(0.5901292f, 12f)
        test(0.3915042f, 14f)
        test(0.1928792f, 16f)
        test(0.0935667f, 17f)
        test(0.001f, 17.932074f)
    }

    @Test
    fun `Test osu!standard radius to old osu!droid difficulty scale conversion`() {
        fun test(radius: Double, scale: Float) =
            Assert.assertEquals(
                CircleSizeCalculator.standardRadiusToOldDroidDifficultyScale(radius),
                scale,
                1e-5f
            )

        // CS 0 osu!droid
        test(75.65252145594813, 1.7818792f)
        // CS 2 osu!droid
        test(67.21958027947754, 1.5832541f)
        // CS 4 osu!droid
        test(58.786639103006955, 1.3846292f)
        // CS 6 osu!droid
        test(50.35369792653637, 1.1860042f)
        // CS 8 osu!droid
        test(41.920756750065785, 0.9873792f)
        // CS 10 osu!droid
        test(33.48781557359519, 0.7887542f)
        // CS 12 osu!droid
        test(25.054874397124607, 0.5901292f)
        // CS 14 osu!droid
        test(16.621933220654018, 0.3915042f)
        // CS 16 osu!droid
        test(8.188992044183431, 0.1928792f)
        // CS 17 osu!droid
        test(3.9725214559481308, 0.0935667f)
        // Beyond CS 17.62 osu!droid
        test(0.042456594972790876, 0.001f)
    }

    @Test
    fun `Test osu!standard radius to osu!standard circle size conversion with fudge`() {
        fun test(radius: Double, cs: Float) =
            Assert.assertEquals(
                CircleSizeCalculator.standardRadiusToStandardCS(radius, true),
                cs,
                1e-5f
            )

        test(100.0, -10.169425f)
        test(90.0, -7.938197f)
        test(80.0, -5.7069683f)
        test(70.0, -3.4757395f)
        test(60.0, -1.2445126f)
        test(50.0, 0.9867163f)
        test(40.0, 3.2179446f)
        test(30.0, 5.4491725f)
        test(20.0, 7.680401f)
        test(10.0, 9.911629f)
        test(5.0, 11.027243f)
    }

    @Test
    fun `Test osu!standard circle size to osu!standard scale conversion with fudge`() {
        fun test(cs: Float, scale: Float) =
            Assert.assertEquals(
                CircleSizeCalculator.standardCSToStandardScale(cs, true),
                scale,
                1e-5f
            )

        test(0f, 0.8503485f)
        test(2f, 0.7102911f)
        test(4f, 0.5702337f)
        test(6f, 0.4301763f)
        test(8f, 0.2901189f)
        test(10f, 0.1500615f)
    }

    @Test
    fun `Test osu!standard scale to old osu!droid difficulty scale conversion with fudge`() {
        fun test(standardScale: Float, droidScale: Float) =
            Assert.assertEquals(
                CircleSizeCalculator.standardScaleToOldDroidDifficultyScale(standardScale, true),
                droidScale,
                1e-5f
            )

        test(0.8503485f, 1.2813087f)
        test(0.7102911f, 1.0702696f)
        test(0.5702337f, 0.8592305f)
        test(0.4301763f, 0.6481914f)
        test(0.2901189f, 0.43715236f)
        test(0.1500615f, 0.22611329f)
    }
}