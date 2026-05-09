package com.osudroid.mods

import com.osudroid.GameMode
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.BeatmapProcessor
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.SliderPath
import com.osudroid.beatmaps.hitobjects.SliderPathType
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.math.Vector2
import org.junit.Assert
import org.junit.Test

class ModFreezeFrameTest {
    @Test
    fun `Test fade in adjustment`() {
        val beatmap = createBeatmap()

        ModFreezeFrame().applyToBeatmap(beatmap)

        val firstCircle = beatmap.hitObjects.objects[0] as HitCircle
        val secondCircle = beatmap.hitObjects.objects[1] as HitCircle
        val slider = beatmap.hitObjects.objects[2] as Slider
        val spinner = beatmap.hitObjects.objects[3] as Spinner

        Assert.assertEquals(450.0, firstCircle.timePreempt, 1e-3)
        Assert.assertEquals(550.0, secondCircle.timePreempt, 1e-3)
        Assert.assertEquals(650.0, slider.timePreempt, 1e-3)
        Assert.assertEquals(9, slider.nestedHitObjects.size)

        // Slider head
        Assert.assertEquals(650.0, slider.nestedHitObjects[0].timePreempt, 1e-3)

        // Slider ticks should not be adjusted.
        val tick = slider.nestedHitObjects[1] as SliderTick
        Assert.assertEquals(797.0, tick.timePreempt, 1e-3)

        // First two repeats should be adjusted.
        Assert.assertEquals(4650.0, slider.nestedHitObjects[2].timePreempt, 1e-3)
        Assert.assertEquals(8200.0, slider.nestedHitObjects[4].timePreempt, 1e-3)

        // Any more than 2 repeats should not be adjusted.
        Assert.assertEquals(4000.0, slider.nestedHitObjects[6].timePreempt, 1e-3)

        // Slider tail
        Assert.assertEquals(700.0, slider.nestedHitObjects[7].timePreempt, 1e-3)

        // Spinners should not be adjusted.
        Assert.assertEquals(450.0, spinner.timePreempt, 1e-3)
    }

    private fun createBeatmap(): Beatmap {
        val beatmap = Beatmap(GameMode.Droid).apply {
            difficulty.ar = 10f

            hitObjects.add(listOf(
                HitCircle(startTime = 0.0, position = Vector2(0), isNewCombo = true, comboOffset = 0),
                HitCircle(startTime = 100.0, position = Vector2(0), isNewCombo = false, comboOffset = 0),
                Slider(
                    startTime = 200.0,
                    position = Vector2(0),
                    repeatCount = 3,
                    path = SliderPath(
                        pathType = SliderPathType.Linear,
                        controlPoints = listOf(Vector2(0), Vector2(200)),
                        expectedDistance = 200.0
                    ),
                    isNewCombo = false,
                    comboOffset = 0,
                    nodeSamples = mutableListOf()
                ),
                Spinner(startTime = 20000.0, endTime = 25000.0, isNewCombo = false)
            ))
        }

        val processor = BeatmapProcessor(beatmap)
        processor.preProcess()

        beatmap.hitObjects.objects.forEach { it.applyDefaults(beatmap.controlPoints, beatmap.difficulty, beatmap.mode) }

        processor.postProcess()

        return beatmap
    }
}