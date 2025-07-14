package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import org.junit.Assert
import org.junit.Test

class ModSmallCircleTest {
    @Test
    fun `Test migration`() {
        ModSmallCircle().migrate(BeatmapDifficulty(cs = 4f)).apply {
            Assert.assertNotNull(cs)
            Assert.assertEquals(8f, cs!!, 0f)
        }
    }

    @Test
    fun `Test beatmap setting adjustment in osu!droid game mode`() {
        BeatmapDifficulty(cs = 3f).apply {
            ModSmallCircle().applyToDifficulty(GameMode.Droid, this, listOf())

            Assert.assertEquals(7f, gameplayCS, 0f)
        }
    }

    @Test
    fun `Test beatmap setting adjustment in osu!standard game mode`() {
        BeatmapDifficulty(cs = 3f).apply {
            ModSmallCircle().applyToDifficulty(GameMode.Standard, this, listOf())

            Assert.assertEquals(7f, difficultyCS, 0f)
            Assert.assertEquals(7f, gameplayCS, 0f)
        }
    }

    @Test
    fun `Test compatibility with Difficulty Adjust mod`() {
        val smallCircle = ModSmallCircle()
        val difficultyAdjust = ModDifficultyAdjust()

        Assert.assertTrue(smallCircle.isCompatibleWith(difficultyAdjust))

        difficultyAdjust.cs = 4f
        Assert.assertFalse(smallCircle.isCompatibleWith(difficultyAdjust))
    }
}