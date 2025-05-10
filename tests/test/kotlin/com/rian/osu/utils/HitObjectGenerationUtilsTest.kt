package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.hitobject.SliderPathType
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class HitObjectGenerationUtilsTest {
    @Test
    fun `Test horizontal reflection`() {
        createSlider().apply {
            HitObjectGenerationUtils.reflectHorizontallyAlongPlayfield(this)

            Assert.assertEquals(position, Vector2(412, 100))
            Assert.assertEquals(endPosition, Vector2(212, 100))
            Assert.assertEquals(nestedHitObjects[1].position, Vector2(312, 100))
        }
    }

    @Test
    fun `Test vertical reflection`() {
        createSlider().apply {
            HitObjectGenerationUtils.reflectVerticallyAlongPlayfield(this)

            Assert.assertEquals(position, Vector2(100, 284))
            Assert.assertEquals(endPosition, Vector2(300, 284))
            Assert.assertEquals(nestedHitObjects[1].position, Vector2(200, 284))
        }
    }

    private fun createSlider() = Slider(
        0.0, Vector2(100, 100), 0, SliderPath(
            SliderPathType.Linear, listOf(Vector2(0), Vector2(200, 0)), 200.0
        ), true, 0, mutableListOf()
    ).apply { applyDefaults(BeatmapControlPoints(), BeatmapDifficulty(), GameMode.Droid) }
}