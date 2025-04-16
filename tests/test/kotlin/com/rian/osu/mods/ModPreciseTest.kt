package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.EmptyHitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.hitobject.SliderPathType
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class ModPreciseTest {
    @Test
    fun `Test hit window application to circle`() {
        HitCircle(0.0, Vector2(0), true, 0).apply {
            applyDefaults(BeatmapControlPoints(), BeatmapDifficulty(), GameMode.Droid)
            ModPrecise().applyToHitObject(GameMode.Droid, this)

            Assert.assertTrue(hitWindow is PreciseDroidHitWindow)
            Assert.assertEquals(5f, hitWindow!!.overallDifficulty, 0f)
        }
    }

    @Test
    fun `Test hit window application to slider`() {
        Slider(
            0.0,
            Vector2(0),
            0,
            SliderPath(SliderPathType.Linear, listOf(Vector2(0), Vector2(100, 0)), 100.0),
            true,
            0,
            mutableListOf()
        ).apply {
            applyDefaults(BeatmapControlPoints(), BeatmapDifficulty(), GameMode.Droid)
            ModPrecise().applyToHitObject(GameMode.Droid, this)

            // Ensure that the hit window is not applied to the slider itself
            Assert.assertTrue(hitWindow is EmptyHitWindow)
            Assert.assertTrue(head.hitWindow is PreciseDroidHitWindow)
            Assert.assertEquals(5f, head.hitWindow!!.overallDifficulty, 0f)
        }
    }

    @Test
    fun `Test hit window application to spinner`() {
        Spinner(0.0, 1000.0, true).apply {
            applyDefaults(BeatmapControlPoints(), BeatmapDifficulty(), GameMode.Droid)
            ModPrecise().applyToHitObject(GameMode.Droid, this)

            Assert.assertTrue(hitWindow is EmptyHitWindow)
        }
    }
}