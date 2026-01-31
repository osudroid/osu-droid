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
            Assert.assertEquals(14, formatVersion)
            Assert.assertEquals(3, hitObjects.objects.size)

            hitObjects.objects[0].apply {
                Assert.assertEquals(1000.0, startTime, 0.0)
                Assert.assertEquals(Vector2(256, 192), position)
                Assert.assertEquals(0.98029613f, difficultyScale, 1e-2f)
                Assert.assertEquals(0.98029613f, gameplayScale, 1e-2f)
                Assert.assertEquals(1200.0, timePreempt, 0.0)
            }

            hitObjects.objects[1].apply {
                Assert.assertEquals(2000.0, startTime, 0.0)
                Assert.assertEquals(Vector2(320, 192), position)
                Assert.assertEquals(0.98029613f, difficultyScale, 1e-2f)
                Assert.assertEquals(0.98029613f, gameplayScale, 1e-2f)
                Assert.assertEquals(1200.0, timePreempt, 0.0)
            }

            hitObjects.objects[2].apply {
                Assert.assertEquals(3000.0, startTime, 0.0)
                Assert.assertEquals(Vector2(384, 192), position)
                Assert.assertEquals(0.98029613f, difficultyScale, 1e-2f)
                Assert.assertEquals(0.98029613f, gameplayScale, 1e-2f)
                Assert.assertEquals(1200.0, timePreempt, 0.0)
            }
        }
    }

    @Test
    fun `Test creation with Hard Rock`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(listOf(ModHardRock())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(192f, firstObject.position.y, 0f)
            Assert.assertEquals(0.8752531f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(0.8752531f, firstObject.gameplayScale, 1e-2f)
            Assert.assertEquals(900.0, firstObject.timePreempt, 0.0)
        }
    }

    @Test
    fun `Test creation with Hidden`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(listOf(ModHidden())).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(1200.0, firstObject.timePreempt, 1e-2)
            Assert.assertEquals(480.0, firstObject.timeFadeIn, 1e-2)
        }
    }

    @Test
    fun `Test creation with Custom Speed`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap(listOf(ModCustomSpeed(2f))).apply {
            val firstObject = hitObjects.objects[0]

            Assert.assertEquals(1000.0, firstObject.startTime, 1e-2)
            Assert.assertEquals(1200.0, firstObject.timePreempt, 1e-2)
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

            Assert.assertEquals(1000.0, firstObject.startTime, 1e-2)
            Assert.assertEquals(1.1553679f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(1.1553679f, firstObject.gameplayScale, 1e-2f)
            Assert.assertEquals(1290.0, firstObject.timePreempt, 1e-2)
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

            Assert.assertEquals(1000.0, firstObject.startTime, 1e-2)
            Assert.assertEquals(1.1553679f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(1.1553679f, firstObject.gameplayScale, 1e-2f)
            Assert.assertEquals(1290.0, firstObject.timePreempt, 1e-2)
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

            Assert.assertEquals(1000.0, firstObject.startTime, 1e-2)
            Assert.assertEquals(1.0503248f, firstObject.difficultyScale, 1e-2f)
            Assert.assertEquals(1.0503248f, firstObject.gameplayScale, 1e-2f)
            Assert.assertEquals(1012.0, firstObject.timePreempt, 1e-2)
        }
    }

    @Test
    fun `Test hit window`() {
        createBasePlayableBeatmap(GameMode.Droid).createDroidPlayableBeatmap().apply {
            Assert.assertEquals(75f, hitWindow.greatWindow, 0f)
            Assert.assertEquals(150f, hitWindow.okWindow, 0f)
            Assert.assertEquals(250f, hitWindow.mehWindow, 0f)
        }
    }
}