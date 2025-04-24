package com.rian.osu.difficulty

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.hitobject.SliderPathType
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.beatmap.timings.TimingControlPoint
import com.rian.osu.math.Vector2
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModTraceable
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class DroidDifficultyHitObjectTest {
    private val hidden = listOf(ModHidden())
    private val traceable = listOf(ModTraceable())

    @Test
    fun `Test previous index`() {
        Assert.assertNull(objects[0].previous(0))
        Assert.assertNotNull(objects[1].previous(0))
    }

    @Test
    fun `Test next index`() {
        Assert.assertNotNull(objects[0].next(0))
        Assert.assertNull(objects[1].next(0))
    }

    @Test
    fun `Test No Mod opacity before hit time`() {
        objects[0].apply {
            Assert.assertEquals(opacityAt(400.0, listOf()), 0.0, 0.0)
            Assert.assertEquals(opacityAt(600.0, listOf()), 0.5, 1e-2)
            Assert.assertEquals(opacityAt(800.0, listOf()), 1.0, 0.0)
            Assert.assertEquals(opacityAt(1000.0, listOf()), 1.0, 0.0)
        }
    }

    @Test
    fun `Test Hidden opacity before hit time`() {
        objects[0].apply {
            Assert.assertEquals(opacityAt(400.0, hidden), 0.0, 0.0)
            Assert.assertEquals(opacityAt(600.0, hidden), 0.5, 1e-2)
            Assert.assertEquals(opacityAt(800.0, hidden), 1.0, 0.0)
            Assert.assertEquals(opacityAt(900.0, hidden), 0.44, 1e-2)
            Assert.assertEquals(opacityAt(1000.0, hidden), 0.0, 0.0)
        }
    }

    @Test
    fun `Test Traceable opacity before hit time`() {
        objects[0].apply {
            Assert.assertEquals(opacityAt(400.0, traceable), 0.0, 0.0)
            Assert.assertEquals(opacityAt(700.0, traceable), 0.0, 0.0)
            Assert.assertEquals(opacityAt(1000.0, traceable), 0.0, 0.0)
        }
    }

    @Test
    fun `Test opacity after hit time`() {
        objects[0].apply {
            Assert.assertEquals(opacityAt(1100.0, listOf()), 0.0, 0.0)
            Assert.assertEquals(opacityAt(1100.0, hidden), 0.0, 0.0)
            Assert.assertEquals(opacityAt(1100.0, traceable), 0.0, 0.0)
        }
    }

    companion object {
        private lateinit var objects: Array<DroidDifficultyHitObject>

        @BeforeClass
        @JvmStatic
        fun setup() {
            val hitObjects = listOf(
                HitCircle(1000.0, Vector2(100), true, 0),
                Slider(
                    1500.0,
                    Vector2(150, 100),
                    0,
                    SliderPath(SliderPathType.Linear, listOf(Vector2(0), Vector2(100, 0)), 100.0),
                    true,
                    0,
                    mutableListOf()
                )
            )

            val controlPoints = BeatmapControlPoints().apply {
                timing.add(TimingControlPoint(0.0, 300.0, 4))
            }

            val difficulty = BeatmapDifficulty(ar = 9f)

            for (hitObject in hitObjects) {
                hitObject.applyDefaults(controlPoints, difficulty, GameMode.Droid)
            }

            @Suppress("UNCHECKED_CAST")
            objects = arrayOfNulls<DroidDifficultyHitObject>(hitObjects.size) as Array<DroidDifficultyHitObject>

            for (i in hitObjects.indices) {
                objects[i] = DroidDifficultyHitObject(
                    hitObjects[i],
                    hitObjects.getOrNull(i - 1),
                    1.0,
                    objects,
                    i - 1
                ).also { it.computeProperties(1.0, hitObjects) }
            }
        }
    }
}