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

class DroidPlayableBeatmapTest : PlayableBeatmapTest() {
    @Test
    fun testCreationWithoutMods() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap().apply {
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
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap().apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.difficultyScale, 0.852658f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1200.0, 0.0)
        }
    }

    @Test
    fun testCreationWithHardRock() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(listOf(ModHardRock())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.position.y, 192f, 0f)
            Assert.assertEquals(firstObject.difficultyScale, 0.769736f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 900.0, 0.0)
        }
    }

    @Test
    fun testCreationWithHidden() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(listOf(ModHidden())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.timePreempt, 1200.0, 1e-2)
            Assert.assertEquals(firstObject.timeFadeIn, 480.0, 1e-2)
        }
    }

    @Test
    fun testCreationWithCustomSpeed() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(listOf(ModCustomSpeed(2f))).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.timePreempt, 1200.0, 1e-2)
        }
    }

    @Test
    fun testCreationWithReallyEasy() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(
            listOf(
                ModReallyEasy(),
                ModCustomSpeed(1.25f)
            )
        ).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.difficultyScale, 0.935198f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1290.0, 1e-2)
        }
    }

    @Test
    fun testCreationWithDifficultyAdjustNoOverride() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(
            listOf(
                ModReallyEasy(),
                ModCustomSpeed(1.25f),
                ModDifficultyAdjust()
            )
        ).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.difficultyScale, 0.935198f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1290.0, 1e-2)
        }
    }

    @Test
    fun testCreationWithDifficultyAdjustOverride() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(
            listOf(
                ModReallyEasy(),
                ModCustomSpeed(1.25f),
                ModHardRock(),
                ModDifficultyAdjust(4f)
            )
        ).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.difficultyScale, 0.918165f, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1012.5, 1e-2)
        }
    }

    @Test
    fun testHitWindow() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap().apply {
            Assert.assertEquals(hitWindow.greatWindow, 75f, 0f)
            Assert.assertEquals(hitWindow.okWindow, 150f, 0f)
            Assert.assertEquals(hitWindow.mehWindow, 250f, 0f)
        }
    }
}