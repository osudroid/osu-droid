package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import org.junit.Assert
import org.junit.Test

class ModEasyTest {
    @Test
    fun `Test beatmap setting adjustment in osu!droid game mode`() {
        BeatmapDifficulty(cs = 4f, ar = 9f, od = 7f, hp = 6f).apply {
            ModEasy().applyToDifficulty(GameMode.Droid, this, listOf())

            Assert.assertEquals(2.74f, difficultyCS, 1e-2f)
            Assert.assertEquals(4.5f, ar, 1e-2f)
            Assert.assertEquals(3.5f, od, 1e-2f)
            Assert.assertEquals(3f, hp, 1e-2f)
        }
    }

    @Test
    fun `Test beatmap setting adjustment in osu!standard game mode`() {
        BeatmapDifficulty(cs = 4f, ar = 9f, od = 7f, hp = 6f).apply {
            ModEasy().applyToDifficulty(GameMode.Standard, this, listOf())

            Assert.assertEquals(2f, difficultyCS, 1e-2f)
            Assert.assertEquals(2f, gameplayCS, 1e-2f)
            Assert.assertEquals(4.5f, ar, 1e-2f)
            Assert.assertEquals(3.5f, od, 1e-2f)
            Assert.assertEquals(3f, hp, 1e-2f)
        }
    }
}