package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.hitobject.SliderPathType
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class BeatmapTest {
    @Test
    fun `Test beatmap version time offset`() {
        val beatmap = Beatmap(GameMode.Standard)

        beatmap.formatVersion = 3
        Assert.assertEquals(beatmap.getOffsetTime(1000), 1024)

        beatmap.formatVersion = 4
        Assert.assertEquals(beatmap.getOffsetTime(1000), 1024)

        beatmap.formatVersion = 10
        Assert.assertEquals(beatmap.getOffsetTime(1000), 1000)
    }

    @Test
    fun `Test max combo getter`() {
        fun test(expectedMaxCombo: Int, vararg objects: HitObject) {
            val beatmap = Beatmap(GameMode.Standard)

            for (obj in objects) {
                obj.applyDefaults(beatmap.controlPoints, beatmap.difficulty, beatmap.mode)
                beatmap.hitObjects.add(obj)
            }

            Assert.assertEquals(expectedMaxCombo, beatmap.maxCombo)
        }

        test(0)
        test(1, createCircle())
        test(4, createCircle(), createSlider())
        test(5, createCircle(), createSlider(), createSpinner())
    }

    private fun createCircle(startTime: Double = 0.0) = HitCircle(startTime, Vector2(0), true, 0)

    private fun createSlider(startTime: Double = 0.0) = Slider(
        startTime,
        Vector2(0),
        0,
        SliderPath(SliderPathType.Linear, listOf(Vector2(0), Vector2(200, 0)), 200.0),
        true,
        0,
        mutableListOf()
    )

    private fun createSpinner(startTime: Double = 1000.0, endTime: Double = startTime + 100.0) =
        Spinner(startTime, endTime, true)
}