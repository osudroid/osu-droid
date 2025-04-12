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
    fun testCreationWithoutMods() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap().apply {
            Assert.assertEquals(formatVersion, 14)
            Assert.assertEquals(hitObjects.objects.size, 3)

            Assert.assertEquals(hitObjects.objects[0].startTime, 1000.0, 0.0)
            Assert.assertEquals(hitObjects.objects[1].startTime, 2000.0, 0.0)
            Assert.assertEquals(hitObjects.objects[2].startTime, 3000.0, 0.0)

            Assert.assertEquals(hitObjects.objects[0].position, Vector2(256, 192))
            Assert.assertEquals(hitObjects.objects[1].position, Vector2(320, 192))
            Assert.assertEquals(hitObjects.objects[2].position, Vector2(384, 192))
        }
    }

    @Test
    fun testCreationWithNoMod() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap().apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.difficultyScale, 0.500205f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1200.0, 0.0)
        }
    }

    @Test
    fun testCreationWithHardRock() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(listOf(ModHardRock())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.position.y, 192f, 0f)
            Assert.assertEquals(firstObject.difficultyScale, 0.39516196f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 900.0, 0.0)
        }
    }

    @Test
    fun testCreationWithHidden() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(listOf(ModHidden())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.timePreempt, 1200.0, 1e-2)
            Assert.assertEquals(firstObject.timeFadeIn, 480.0, 1e-2)
        }
    }

    @Test
    fun testCreationWithCustomSpeed() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap(listOf(ModCustomSpeed(2f))).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.timePreempt, 1200.0, 1e-2)
        }
    }

    @Test
    fun testCreationWithReallyEasy() {
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
    fun testCreationWithDifficultyAdjustNoOverride() {
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
    fun testCreationWithDifficultyAdjustOverride() {
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
    fun testHitWindow() {
        createBasePlayableBeatmap(GameMode.Standard).createStandardPlayableBeatmap().apply {
            Assert.assertEquals(hitWindow.greatWindow, 50f, 0f)
            Assert.assertEquals(hitWindow.okWindow, 100f, 0f)
            Assert.assertEquals(hitWindow.mehWindow, 150f, 0f)
        }
    }
}