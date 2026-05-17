package com.osudroid.mods

import com.osudroid.GameMode
import com.osudroid.beatmaps.EmptyHitWindow
import com.osudroid.beatmaps.PreciseDroidHitWindow
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.SliderPath
import com.osudroid.beatmaps.hitobjects.SliderPathType
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.beatmaps.sections.BeatmapControlPoints
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.math.Vector2
import org.junit.Assert
import org.junit.Test

class ModPreciseTest {
    @Test
    fun `Test hit window application to circle`() {
        HitCircle(0.0, Vector2(0), true, 0).apply {
            applyDefaults(BeatmapControlPoints(), BeatmapDifficulty(), GameMode.Droid)
            ModPrecise().applyToHitObject(GameMode.Droid, this, listOf())

            Assert.assertTrue(hitWindow is PreciseDroidHitWindow)
            Assert.assertEquals(5.0, hitWindow!!.overallDifficulty, 0.0)
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
            ModPrecise().applyToHitObject(GameMode.Droid, this, listOf())

            // Ensure that the hit window is not applied to the slider itself
            Assert.assertTrue(hitWindow is EmptyHitWindow)
            Assert.assertTrue(head.hitWindow is PreciseDroidHitWindow)
            Assert.assertEquals(5.0, head.hitWindow!!.overallDifficulty, 0.0)
        }
    }

    @Test
    fun `Test hit window application to spinner`() {
        Spinner(0.0, 1000.0, true).apply {
            applyDefaults(BeatmapControlPoints(), BeatmapDifficulty(), GameMode.Droid)
            ModPrecise().applyToHitObject(GameMode.Droid, this, listOf())

            Assert.assertTrue(hitWindow is EmptyHitWindow)
        }
    }
}