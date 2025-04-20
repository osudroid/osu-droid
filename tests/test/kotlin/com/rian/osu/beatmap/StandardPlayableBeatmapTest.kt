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
            Assert.assertEquals(formatVersion, 14)
            Assert.assertEquals(hitObjects.objects.size, 3)

            hitObjects.objects[0].apply {
                Assert.assertEquals(startTime, 1000.0, 0.0)
                Assert.assertEquals(position, Vector2(256, 192))
                Assert.assertEquals(difficultyScale, 0.500205f, 1e-2f)
                Assert.assertEquals(timePreempt, 1200.0, 0.0)
            }

            hitObjects.objects[1].apply {
                Assert.assertEquals(startTime, 2000.0, 0.0)
                Assert.assertEquals(position, Vector2(320, 192))
                Assert.assertEquals(difficultyScale, 0.500205f, 1e-2f)
                Assert.assertEquals(timePreempt, 1200.0, 0.0)
            }

            hitObjects.objects[2].apply {
                Assert.assertEquals(startTime, 3000.0, 0.0)
                Assert.assertEquals(position, Vector2(384, 192))
                Assert.assertEquals(difficultyScale, 0.500205f, 1e-2f)
                Assert.assertEquals(timePreempt, 1200.0, 0.0)
            }
        }
    }

    @Test
    fun `Test creation with Hard Rock`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(listOf(ModHardRock())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.position.y, 192f, 0f)
            Assert.assertEquals(firstObject.difficultyScale, 0.39516196f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 900.0, 0.0)
        }
    }

    @Test
    fun `Test creation with Hidden`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(listOf(ModHidden())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.timePreempt, 1200.0, 1e-2)
            Assert.assertEquals(firstObject.timeFadeIn, 480.0, 1e-2)
        }
    }

    @Test
    fun `Test creation with Custom Speed`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(listOf(ModCustomSpeed(2f))).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.timePreempt, 1200.0, 1e-2)
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

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.difficultyScale, 0.67527676f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1290.0, 1e-2)
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

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.difficultyScale, 0.67527676f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1290.0, 1e-2)
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

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.difficultyScale, 0.57023364f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1012.5, 1e-2)
        }
    }

    @Test
    fun `Test hit window`() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap().apply {
            Assert.assertEquals(hitWindow.greatWindow, 50f, 0f)
            Assert.assertEquals(hitWindow.okWindow, 100f, 0f)
            Assert.assertEquals(hitWindow.mehWindow, 150f, 0f)
        }
    }
}