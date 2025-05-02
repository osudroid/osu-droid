package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.hitobject.SliderPathType
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class ModHardRockTest {
    @Test
    fun `Test beatmap setting adjustment osu!droid game mode`() {
        BeatmapDifficulty(cs = 4f, ar = 9f, od = 7f, hp = 6f).apply {
            ModHardRock().applyToDifficulty(GameMode.Droid, this, listOf())

            Assert.assertEquals(5.2f, difficultyCS, 1e-2f)
            Assert.assertEquals(5.2f, gameplayCS, 1e-2f)
            Assert.assertEquals(10f, ar, 1e-2f)
            Assert.assertEquals(9.8f, od, 1e-2f)
            Assert.assertEquals(8.4f, hp, 1e-2f)
        }
    }

    @Test
    fun `Test beatmap setting adjustment osu!droid game mode with mods`() {
        BeatmapDifficulty(cs = 4f, ar = 9f, od = 7f, hp = 6f).apply {
            ModHardRock().applyToDifficulty(GameMode.Droid, this, listOf(ModReplayV6()))

            Assert.assertEquals(5.26f, difficultyCS, 1e-2f)
            Assert.assertEquals(10f, ar, 1e-2f)
            Assert.assertEquals(9.8f, od, 1e-2f)
            Assert.assertEquals(8.4f, hp, 1e-2f)
        }
    }

    @Test
    fun `Test beatmap setting adjustment osu!standard game mode`() {
        BeatmapDifficulty(cs = 4f, ar = 9f, od = 7f, hp = 6f).apply {
            ModHardRock().applyToDifficulty(GameMode.Standard, this, listOf())

            Assert.assertEquals(5.2f, difficultyCS, 1e-2f)
            Assert.assertEquals(5.2f, gameplayCS, 1e-2f)
            Assert.assertEquals(10f, ar, 1e-2f)
            Assert.assertEquals(9.8f, od, 1e-2f)
            Assert.assertEquals(8.4f, hp, 1e-2f)
        }
    }

    @Test
    fun `Test object application`() {
        val difficulty = BeatmapDifficulty(cs = 4f, ar = 9f, od = 7f, hp = 6f)
        val hardRock = ModHardRock()

        hardRock.applyToDifficulty(GameMode.Droid, difficulty, listOf())

        Slider(
            0.0, Vector2(100, 100), 0, SliderPath(
                SliderPathType.Linear, listOf(Vector2(0), Vector2(200, 200)), 282.842712
            ), true, 0, mutableListOf()
        ).apply {
            applyDefaults(BeatmapControlPoints(), difficulty, GameMode.Droid)
            hardRock.applyToHitObject(GameMode.Droid, this, listOf())

            Assert.assertEquals(Vector2(100, 284), position)
            Assert.assertEquals(450.0, timePreempt, 1e-2)
            Assert.assertEquals(Vector2(200, -200), path.controlPoints[1])
            Assert.assertTrue(nestedHitObjects.size > 2)

            val tick = nestedHitObjects[1]

            Assert.assertEquals(170.71068f, tick.position.x, 1e-4f)
            Assert.assertEquals(213.28932f, tick.position.y, 1e-4f)
        }
    }
}