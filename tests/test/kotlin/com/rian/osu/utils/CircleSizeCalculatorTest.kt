package com.rian.osu.utils

import org.junit.Assert
import org.junit.Test

class CircleSizeCalculatorTest {
    @Test
    fun testDroidCSToDroidDifficultyScale() {
        fun test(cs: Float, scale: Float) =
            Assert.assertEquals(CircleSizeCalculator.droidCSToDroidDifficultyScale(cs), scale, 1e-5f)

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
        test(20f, 0.001f)
    }

    @Test
    fun testDroidDifficultyScaleToDroidCS() {
        fun test(scale: Float, cs: Float) =
            Assert.assertEquals(CircleSizeCalculator.droidDifficultyScaleToDroidCS(scale), cs, 1e-5f)

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
    fun testDroidScaleToStandardRadius() {
        fun test(scale: Float, radius: Double) =
            Assert.assertEquals(
                CircleSizeCalculator.droidScaleToStandardRadius(scale),
                radius,
                1e-7
            )

        // CS 0 osu!droid
        test(1.7818792f, 75.65252291720653)
        // CS 2 osu!droid
        test(1.5832541f, 67.21957801567763)
        // CS 4 osu!droid
        test(1.3846292f, 58.78664323658978)
        // CS 6 osu!droid
        test(1.1860042f, 50.35369833506089)
        // CS 8 osu!droid
        test(0.9873792f, 41.920758494752526)
        // CS 10 osu!droid
        test(0.7887542f, 33.487818654444155)
        // CS 12 osu!droid
        test(0.5901292f, 25.054876283525523)
        // CS 14 osu!droid
        test(0.3915042f, 16.62193517791202)
        // CS 16 osu!droid
        test(0.1928792f, 8.188994072298522)
        // CS 17 osu!droid
        test(0.0935667f, 3.972523519491772)
        // Beyond CS 17.62 osu!droid
        test(0.001f, 0.042456594972790876)
    }

    @Test
    fun testStandardRadiusToDroidDifficultyScale() {
        fun test(radius: Double, scale: Float) =
            Assert.assertEquals(
                CircleSizeCalculator.standardRadiusToDroidDifficultyScale(radius),
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
    fun testStandardRadiusToStandardCSWithFudge() {
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
    fun testStandardCSToStandardScaleWithFudge() {
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
    fun testStandardScaleToDroidDifficultyScaleWithFudge() {
        fun test(standardScale: Float, droidScale: Float) =
            Assert.assertEquals(
                CircleSizeCalculator.standardScaleToDroidDifficultyScale(standardScale, true),
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