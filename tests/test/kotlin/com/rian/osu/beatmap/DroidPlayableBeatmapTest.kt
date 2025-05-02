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
    fun `Test creation without mods`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap().apply {
            Assert.assertEquals(formatVersion, 14)
            Assert.assertEquals(hitObjects.objects.size, 3)

            hitObjects.objects[0].apply {
                Assert.assertEquals(startTime, 1000.0, 0.0)
                Assert.assertEquals(position, Vector2(256, 192))
                Assert.assertEquals(0.98029613f, difficultyScale, 1e-2f)
                Assert.assertEquals(0.98029613f, gameplayScale, 1e-2f)
                Assert.assertEquals(timePreempt, 1200.0, 0.0)
            }

            hitObjects.objects[1].apply {
                Assert.assertEquals(startTime, 2000.0, 0.0)
                Assert.assertEquals(position, Vector2(320, 192))
                Assert.assertEquals(0.98029613f, difficultyScale, 1e-2f)
                Assert.assertEquals(0.98029613f, gameplayScale, 1e-2f)
                Assert.assertEquals(timePreempt, 1200.0, 0.0)
            }

            hitObjects.objects[2].apply {
                Assert.assertEquals(startTime, 3000.0, 0.0)
                Assert.assertEquals(position, Vector2(384, 192))
                Assert.assertEquals(0.98029613f, difficultyScale, 1e-2f)
                Assert.assertEquals(0.98029613f, gameplayScale, 1e-2f)
                Assert.assertEquals(timePreempt, 1200.0, 0.0)
            }
        }
    }

    @Test
    fun `Test creation with Hard Rock`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(listOf(ModHardRock())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.position.y, 192f, 0f)
            Assert.assertEquals(0.8752531f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(0.8752531f, firstObject.gameplayScale, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 900.0, 0.0)
        }
    }

    @Test
    fun `Test creation with Hidden`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(listOf(ModHidden())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.timePreempt, 1200.0, 1e-2)
            Assert.assertEquals(firstObject.timeFadeIn, 480.0, 1e-2)
        }
    }

    @Test
    fun `Test creation with Custom Speed`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(listOf(ModCustomSpeed(2f))).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(firstObject.timePreempt, 1200.0, 1e-2)
        }
    }

    @Test
    fun `Test creation with Really Easy`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(
            listOf(
                ModReallyEasy(),
                ModCustomSpeed(1.25f)
            )
        ).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(1.0684379f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(1.0684379f, firstObject.gameplayScale, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1290.0, 1e-2)
        }
    }

    @Test
    fun `Test creation with Difficulty Adjust no override`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(
            listOf(
                ModReallyEasy(),
                ModCustomSpeed(1.25f),
                ModDifficultyAdjust()
            )
        ).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(firstObject.startTime, 1000.0, 1e-2)
            Assert.assertEquals(1.0684379f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(1.0684379f, firstObject.gameplayScale, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1290.0, 1e-2)
        }
    }

    @Test
    fun `Test creation with Difficulty Adjust override`() {
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
            Assert.assertEquals(1.0503248f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(1.0503248f, firstObject.gameplayScale, 1e-2f)
            Assert.assertEquals(firstObject.timePreempt, 1012.5, 1e-2)
        }
    }

    @Test
    fun `Test hit window`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap().apply {
            Assert.assertEquals(hitWindow.greatWindow, 75f, 0f)
            Assert.assertEquals(hitWindow.okWindow, 150f, 0f)
            Assert.assertEquals(hitWindow.mehWindow, 250f, 0f)
        }
    }
}