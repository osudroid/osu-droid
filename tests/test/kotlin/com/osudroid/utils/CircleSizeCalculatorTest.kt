package com.osudroid.utils

import org.junit.Assert
import org.junit.Test

class CircleSizeCalculatorTest {
    @Test
    fun `Test osu!droid circle size to osu!droid scale conversion`() {
        listOf(
            0f to 1.3304397f,
            2f to 1.1903822f,
            3.5f to 1.0853392f,
            4f to 1.0503248f,
            5f to 0.98029613f,
            6f to 0.91026735f,
            8f to 0.77020997f,
            10f to 0.6301526f,
            12f to 0.49009523f,
            14f to 0.35003784f,
            16f to 0.2099805f,
            17f to 0.13995178f,
            18f to 0.06992312f,
            19f to 0.001f,
            20f to 0.001f
        ).forEach { (cs, scale) ->
            Assert.assertEquals(
                "Invalid scale for CS $cs",
                scale,
                CircleSizeCalculator.droidCSToDroidScale(cs),
                1e-5f
            )
        }
    }

    @Test
    fun `Test osu!droid scale to osu!droid circle size conversion`() {
        listOf(
            1.3304397f to 0f,
            1.1903822f to 2f,
            1.0853392f to 3.5f,
            1.0503248f to 4f,
            0.98029613f to 5f,
            0.91026735f to 6f,
            0.77020997f to 8f,
            0.6301526f to 10f,
            0.49009523f to 12f,
            0.35003784f to 14f,
            0.2099805f to 16f,
            0.13995178f to 17f,
            0.06992312f to 18f,
            0.001f to 18.984211f
        ).forEach { (scale, cs) ->
            Assert.assertEquals(
                "Invalid CS for scale $scale",
                cs,
                CircleSizeCalculator.droidScaleToDroidCS(scale),
                1e-5f
            )
        }
    }

    @Test
    fun `Test osu!droid circle size to old osu!droid difficulty scale conversion`() {
        listOf(
            0f to 1.7818792f,
            2f to 1.5832541f,
            3.5f to 1.4342854f,
            4f to 1.3846292f,
            5f to 1.2853167f,
            6f to 1.1860042f,
            8f to 0.9873792f,
            10f to 0.7887542f,
            12f to 0.5901292f,
            14f to 0.3915042f,
            16f to 0.1928792f,
            17f to 0.0935667f,
            18f to 0.001f,
            19f to 0.001f,
            20f to 0.001f
        ).forEach { (cs, scale) ->
            Assert.assertEquals(
                "Invalid scale for CS $cs",
                scale,
                CircleSizeCalculator.droidCSToOldDroidDifficultyScale(cs),
                1e-5f
            )
        }
    }

    @Test
    fun `Test old osu!droid difficulty scale to osu!droid circle size conversion`() {
        listOf(
            1.7818792f to 0f,
            1.5832541f to 2f,
            1.4342854f to 3.5f,
            1.3846292f to 4f,
            1.2853167f to 5f,
            1.1860042f to 6f,
            0.9873792f to 8f,
            0.7887542f to 10f,
            0.5901292f to 12f,
            0.3915042f to 14f,
            0.1928792f to 16f,
            0.0935667f to 17f,
            0.001f to 17.932074f
        ).forEach { (scale, cs) ->
            Assert.assertEquals(
                "Invalid CS for scale $scale",
                cs,
                CircleSizeCalculator.droidOldDifficultyScaleToDroidCS(scale),
                1e-5f
            )
        }
    }

    @Test
    fun `Test old osu!droid difficulty scale pixels conversion (bidirectional)`() {
        listOf(
            1.7818792f to 1.2559501f,
            1.5832541f to 1.11595f,
            1.4342854f to 1.0109501f,
            1.3846292f to 0.9759502f,
            1.2853167f to 0.9059501f,
            1.1860042f to 0.8359501f,
            0.9873792f to 0.6959501f,
            0.7887542f to 0.5559501f,
            0.5901292f to 0.41595012f,
            0.3915042f to 0.2759501f,
            0.1928792f to 0.1359501f,
            0.0935667f to 0.0659501f,
            0.001f to 7.048458e-4f
        ).forEach { (screenScale, osuScale) ->
            // Screen pixels to osu!pixels
            Assert.assertEquals(
                "Invalid osu!pixels for $screenScale",
                osuScale,
                CircleSizeCalculator.droidOldDifficultyScaleScreenPixelsToOsuPixels(screenScale),
                1e-5f
            )

            // osu! to screen pixels
            Assert.assertEquals(
                "Invalid screen pixels for $osuScale",
                screenScale,
                CircleSizeCalculator.droidOldDifficultyScaleOsuPixelsToScreenPixels(osuScale),
                1e-5f
            )
        }
    }

    @Test
    fun `Test osu!standard radius to old osu!droid difficulty scale conversion`() {
        listOf(
            // CS 0 osu!droid
            75.65252145594813 to 1.7818792f,
            // CS 2 osu!droid
            67.21958027947754 to 1.5832541f,
            // CS 4 osu!droid
            58.786639103006955 to 1.3846292f,
            // CS 6 osu!droid
            50.35369792653637 to 1.1860042f,
            // CS 8 osu!droid
            41.920756750065785 to 0.9873792f,
            // CS 10 osu!droid
            33.48781557359519 to 0.7887542f,
            // CS 12 osu!droid
            25.054874397124607 to 0.5901292f,
            // CS 14 osu!droid
            16.621933220654018 to 0.3915042f,
            // CS 16 osu!droid
            8.188992044183431 to 0.1928792f,
            // CS 17 osu!droid
            3.9725214559481308 to 0.0935667f,
            // Beyond CS 17.62 osu!droid
            0.042456594972790876 to 0.001f
        ).forEach { (radius, scale) ->
            Assert.assertEquals(
                "Invalid scale for radius $radius",
                scale,
                CircleSizeCalculator.standardRadiusToOldDroidDifficultyScale(radius),
                1e-5f
            )
        }
    }

    @Test
    fun `Test osu!standard radius to osu!standard circle size conversion with fudge`() {
        listOf(
            100.0 to -10.169425f,
            90.0 to -7.938197f,
            80.0 to -5.7069683f,
            70.0 to -3.4757395f,
            60.0 to -1.2445126f,
            50.0 to 0.9867163f,
            40.0 to 3.2179446f,
            30.0 to 5.4491725f,
            20.0 to 7.680401f,
            10.0 to 9.911629f,
            5.0 to 11.027243f
        ).forEach { (radius, cs) ->
            Assert.assertEquals(
                "Invalid CS for radius $radius",
                cs,
                CircleSizeCalculator.standardRadiusToStandardCS(radius, true),
                1e-5f
            )
        }
    }

    @Test
    fun `Test osu!standard circle size to osu!standard scale conversion with fudge`() {
        listOf(
            0f to 0.8503485f,
            2f to 0.7102911f,
            4f to 0.5702337f,
            6f to 0.4301763f,
            8f to 0.2901189f,
            10f to 0.1500615f
        ).forEach { (cs, scale) ->
            Assert.assertEquals(
                "Invalid scale for CS $cs",
                scale,
                CircleSizeCalculator.standardCSToStandardScale(cs, true),
                1e-5f
            )
        }
    }

    @Test
    fun `Test osu!standard scale to old osu!droid difficulty scale conversion with fudge`() {
        listOf(
            0.8503485f to 1.2813087f,
            0.7102911f to 1.0702696f,
            0.5702337f to 0.8592305f,
            0.4301763f to 0.6481914f,
            0.2901189f to 0.43715236f,
            0.1500615f to 0.22611329f
        ).forEach { (standardScale, droidScale) ->
            Assert.assertEquals(
                "Invalid osu!droid scale for osu!standard scale $standardScale",
                droidScale,
                CircleSizeCalculator.standardScaleToOldDroidDifficultyScale(standardScale, true),
                1e-5f
            )
        }
    }
}