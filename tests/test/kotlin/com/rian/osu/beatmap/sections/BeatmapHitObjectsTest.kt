package com.rian.osu.beatmap.sections

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.hitobject.SliderPathType
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class BeatmapHitObjectsTest {
    private val controlPoints = BeatmapControlPoints()
    private val difficulty = BeatmapDifficulty()

    @Test
    fun `Test add hit objects without existing ones`() {
        BeatmapHitObjects().apply {
            add(createCircle())

            Assert.assertEquals(1, objects.size)
        }
    }

    @Test
    fun `Test add hit objects before 2 hit objects`() {
        BeatmapHitObjects().apply {
            add(listOf(createCircle(), createCircle(1500.0)))
            add(createCircle(500.0))

            Assert.assertEquals(3, objects.size)
            Assert.assertEquals(500.0, objects[0].startTime, 0.0)
        }
    }

    @Test
    fun `Test add hit objects between 2 hit objects`() {
        BeatmapHitObjects().apply {
            add(listOf(createCircle(500.0), createCircle(1500.0)))
            add(createCircle())

            Assert.assertEquals(3, objects.size)
            Assert.assertEquals(1000.0, objects[1].startTime, 0.0)
        }
    }

    @Test
    fun `Test add hit objects after 2 hit objects`() {
        BeatmapHitObjects().apply {
            add(listOf(createCircle(500.0), createCircle(1000.0)))
            add(createCircle(1500.0))

            Assert.assertEquals(3, objects.size)
            Assert.assertEquals(1500.0, objects[2].startTime, 0.0)
        }
    }

    @Test
    fun `Test circle counter`() {
        BeatmapHitObjects().apply {
            add(createCircle())
            Assert.assertEquals(1, circleCount)

            add(createCircle(1500.0))
            Assert.assertEquals(2, circleCount)

            add(createSpinner(2000.0))
            Assert.assertEquals(3, objects.size)
            Assert.assertEquals(2, circleCount)
        }
    }

    @Test
    fun `Test slider counter`() {
        BeatmapHitObjects().apply {
            add(createSlider())
            Assert.assertEquals(1, sliderCount)

            add(createSlider(1500.0))
            Assert.assertEquals(2, sliderCount)

            add(createSpinner(2000.0))
            Assert.assertEquals(3, objects.size)
            Assert.assertEquals(2, sliderCount)
        }
    }

    @Test
    fun `Test spinner counter`() {
        BeatmapHitObjects().apply {
            add(createSpinner())
            Assert.assertEquals(1, spinnerCount)

            add(createSpinner(1500.0))
            Assert.assertEquals(2, spinnerCount)

            add(createCircle(2000.0))
            Assert.assertEquals(3, objects.size)
            Assert.assertEquals(2, spinnerCount)
        }
    }

    @Test
    fun `Test remove hit objects without existing ones`() {
        Assert.assertNull(BeatmapHitObjects().remove(0))
    }

    @Test
    fun `Test remove circle`() {
        BeatmapHitObjects().apply {
            add(createCircle())
            add(createSpinner(1500.0))

            Assert.assertTrue(remove(0) is HitCircle)
            Assert.assertEquals(1, objects.size)
            Assert.assertEquals(0, circleCount)
        }
    }

    @Test
    fun `Test remove slider`() {
        BeatmapHitObjects().apply {
            add(createCircle())
            add(createSlider(1500.0))

            Assert.assertTrue(remove(1) is Slider)
            Assert.assertEquals(1, objects.size)
            Assert.assertEquals(0, sliderCount)
        }
    }

    @Test
    fun `Test remove spinner`() {
        BeatmapHitObjects().apply {
            add(createCircle())
            add(createSpinner(1500.0))

            Assert.assertTrue(remove(1) is Spinner)
            Assert.assertEquals(1, objects.size)
            Assert.assertEquals(0, spinnerCount)
        }
    }

    private fun createCircle(startTime: Double = 1000.0) = HitCircle(startTime, Vector2(0), true, 0).apply {
        applyDefaults(controlPoints, difficulty, GameMode.Standard)
        applySamples(controlPoints)
    }

    private fun createSlider(startTime: Double = 1000.0) = Slider(
        startTime,
        Vector2(100, 192),
        1,
        SliderPath(
            SliderPathType.Linear,
            listOf(Vector2(0), Vector2(200, 0)),
            Vector2(200, 0).getDistance(Vector2(100, 192)).toDouble()
        ),
        true,
        0,
        mutableListOf()
    ).apply {
        applyDefaults(controlPoints, difficulty, GameMode.Standard)
        applySamples(controlPoints)
    }

    private fun createSpinner(startTime: Double = 1000.0) = Spinner(startTime, startTime + 100, true).apply {
        applyDefaults(controlPoints, difficulty, GameMode.Standard)
        applySamples(controlPoints)
    }
}