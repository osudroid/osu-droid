package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.math.Vector2
import com.rian.osu.mods.ModCustomSpeed
import com.rian.osu.mods.ModDifficultyAdjust
import com.rian.osu.mods.ModHardRock
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModReallyEasy
import org.junit.Assert
import org.junit.Test

class StandardPlayableBeatmapTest : PlayableBeatmapTest() {
    @Test
    fun `Test creation without mods`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap().apply {
            Assert.assertEquals(14, formatVersion)
            Assert.assertEquals(3, hitObjects.objects.size)

            hitObjects.objects[0].apply {
                Assert.assertEquals(1000.0, startTime, 0.0)
                Assert.assertEquals(Vector2(256, 192), position)
                Assert.assertEquals(0.500205f, difficultyScale, 1e-2f)
                Assert.assertEquals(1200.0, timePreempt, 0.0)
            }

            hitObjects.objects[1].apply {
                Assert.assertEquals(2000.0, startTime, 0.0)
                Assert.assertEquals(Vector2(320, 192), position)
                Assert.assertEquals(0.500205f, difficultyScale, 1e-2f)
                Assert.assertEquals(1200.0, timePreempt, 0.0)
            }

            hitObjects.objects[2].apply {
                Assert.assertEquals(3000.0, startTime, 0.0)
                Assert.assertEquals(Vector2(384, 192), position)
                Assert.assertEquals(0.500205f, difficultyScale, 1e-2f)
                Assert.assertEquals(1200.0, timePreempt, 0.0)
            }
        }
    }

    @Test
    fun `Test creation with Hard Rock`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(listOf(ModHardRock())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(192f, firstObject.position.y, 0f)
            Assert.assertEquals(0.39516196f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(900.0, firstObject.timePreempt, 0.0)
        }
    }

    @Test
    fun `Test creation with Hidden`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(listOf(ModHidden())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(1200.0, firstObject.timePreempt, 1e-2)
            Assert.assertEquals(480.0, firstObject.timeFadeIn, 1e-2)
        }
    }

    @Test
    fun `Test creation with Custom Speed`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(listOf(ModCustomSpeed(2f))).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(1000.0, firstObject.startTime, 1e-2)
            Assert.assertEquals(1200.0, firstObject.timePreempt, 1e-2)
        }
    }

    @Test
    fun `Test creation with Really Easy`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(
            listOf(
                ModReallyEasy(),
                ModCustomSpeed(1.25f)
            )
        ).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(1000.0, firstObject.startTime, 1e-2)
            Assert.assertEquals(0.67527676f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(1290.0, firstObject.timePreempt, 1e-2)
        }
    }

    @Test
    fun `Test creation with Difficulty Adjust no override`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(
            listOf(
                ModReallyEasy(),
                ModCustomSpeed(1.25f),
                ModDifficultyAdjust()
            )
        ).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(1000.0, firstObject.startTime, 1e-2)
            Assert.assertEquals(0.67527676f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(1290.0, firstObject.timePreempt, 1e-2)
        }
    }

    @Test
    fun `Test creation with Difficulty Adjust override`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(
            listOf(
                ModReallyEasy(),
                ModCustomSpeed(1.25f),
                ModHardRock(),
                ModDifficultyAdjust(4f)
            )
        ).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(1000.0, firstObject.startTime, 1e-2)
            Assert.assertEquals(0.57023364f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(1012.5, firstObject.timePreempt, 1e-2)
        }
    }

    @Test
    fun `Test hit window`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap().apply {
            Assert.assertEquals(50f, hitWindow.greatWindow, 0f)
            Assert.assertEquals(100f, hitWindow.okWindow, 0f)
            Assert.assertEquals(150f, hitWindow.mehWindow, 0f)
        }
    }
}