package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import org.junit.Assert
import org.junit.Test

class ModReallyEasyTest {
    @Test
    fun `Test beatmap setting adjustment osu!droid game mode`() {
        createBeatmapDifficulty().apply {
            ModReallyEasy().applyToDifficulty(GameMode.Droid, this, listOf())

            Assert.assertEquals(2f, difficultyCS, 1e-2f)
            Assert.assertEquals(2f, gameplayCS, 1e-2f)
            Assert.assertEquals(8.5f, ar, 1e-2f)
            Assert.assertEquals(3.5f, od, 1e-2f)
            Assert.assertEquals(3f, hp, 1e-2f)
        }
    }

    @Test
    fun `Test beatmap setting adjustment osu!droid game mode with adjustment mods`() {
        createBeatmapDifficulty().apply {
            ModReallyEasy().applyToDifficulty(GameMode.Droid, this, listOf(ModReplayV6()))

            Assert.assertEquals(3.1128423f, difficultyCS, 1e-2f)
            Assert.assertEquals(8.5f, ar, 1e-2f)
            Assert.assertEquals(3.5f, od, 1e-2f)
            Assert.assertEquals(3f, hp, 1e-2f)
        }
    }

    @Test
    fun `Test beatmap setting adjustment osu!standard game mode`() {
        createBeatmapDifficulty().apply {
            ModReallyEasy().applyToDifficulty(GameMode.Standard, this, listOf())

            Assert.assertEquals(2f, difficultyCS, 1e-2f)
            Assert.assertEquals(2f, gameplayCS, 1e-2f)
            Assert.assertEquals(8.5f, ar, 1e-2f)
            Assert.assertEquals(3.5f, od, 1e-2f)
            Assert.assertEquals(3f, hp, 1e-2f)
        }
    }

    @Test
    fun `Test beatmap setting adjustment with Difficulty Adjust`() {
        createBeatmapDifficulty().apply {
            ModReallyEasy().applyToDifficulty(
                GameMode.Droid,
                this,
                listOf(ModDifficultyAdjust(cs = 4f, ar = 9f, od = 7f, hp = 6f))
            )

            Assert.assertEquals(4f, difficultyCS, 1e-2f)
            Assert.assertEquals(4f, gameplayCS, 1e-2f)
            Assert.assertEquals(9f, ar, 1e-2f)
            Assert.assertEquals(7f, od, 1e-2f)
            Assert.assertEquals(6f, hp, 1e-2f)
        }
    }

    @Test
    fun `Test beatmap setting adjustment with Custom Speed`() {
        createBeatmapDifficulty().apply {
            ModReallyEasy().applyToDifficulty(
                GameMode.Droid,
                this,
                listOf(ModCustomSpeed(trackRateMultiplier = 1.25f))
            )

            Assert.assertEquals(2f, difficultyCS, 1e-2f)
            Assert.assertEquals(2f, gameplayCS, 1e-2f)
            Assert.assertEquals(8.25f, ar, 1e-2f)
            Assert.assertEquals(3.5f, od, 1e-2f)
            Assert.assertEquals(3f, hp, 1e-2f)
        }
    }

    @Test
    fun `Test AR adjustment with Custom Speed and other rate adjusting mods of same rate`() {
        fun test(expected: Float, mods: List<Mod>) = createBeatmapDifficulty().run {
            ModReallyEasy().applyToDifficulty(GameMode.Droid, this, mods)
            Assert.assertEquals(expected, ar, 1e-2f)
        }

        test(8f, listOf(ModCustomSpeed(trackRateMultiplier = 1.5f)))
        test(8f, listOf(ModDoubleTime()))
    }

    @Test
    fun `Test compatibility with Difficulty Adjust mod`() {
        val reallyEasy = ModReallyEasy()
        val difficultyAdjust = ModDifficultyAdjust(cs = 4f)

        Assert.assertTrue(reallyEasy.isCompatibleWith(difficultyAdjust))

        difficultyAdjust.ar = 9f
        Assert.assertTrue(reallyEasy.isCompatibleWith(difficultyAdjust))

        difficultyAdjust.od = 7f
        Assert.assertTrue(reallyEasy.isCompatibleWith(difficultyAdjust))

        difficultyAdjust.hp = 6f
        Assert.assertFalse(reallyEasy.isCompatibleWith(difficultyAdjust))
    }

    private fun createBeatmapDifficulty() = BeatmapDifficulty(cs = 4f, ar = 9f, od = 7f, hp = 6f)
}