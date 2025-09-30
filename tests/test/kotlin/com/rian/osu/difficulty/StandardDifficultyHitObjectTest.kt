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
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class StandardDifficultyHitObjectTest {
    private val hidden = listOf(ModHidden())

    @Test
    fun `Test previous index`() {
        Assert.assertNull(obj.previous(0))
    }

    @Test
    fun `Test next index`() {
        Assert.assertNull(obj.next(0))
    }

    @Test
    fun `Test No Mod opacity before hit time`() {
        Assert.assertEquals(obj.opacityAt(400.0, listOf()), 0.0, 0.0)
        Assert.assertEquals(obj.opacityAt(600.0, listOf()), 0.5, 1e-2)
        Assert.assertEquals(obj.opacityAt(800.0, listOf()), 1.0, 0.0)
        Assert.assertEquals(obj.opacityAt(1000.0, listOf()), 1.0, 0.0)
    }

    @Test
    fun `Test Hidden opacity before hit time`() {
        Assert.assertEquals(obj.opacityAt(400.0, hidden), 0.0, 0.0)
        Assert.assertEquals(obj.opacityAt(600.0, hidden), 0.5, 1e-2)
        Assert.assertEquals(obj.opacityAt(800.0, hidden), 1.0, 0.0)
        Assert.assertEquals(obj.opacityAt(900.0, hidden), 0.44, 1e-2)
        Assert.assertEquals(obj.opacityAt(1000.0, hidden), 0.0, 0.0)
    }

    @Test
    fun `Test opacity after hit time`() {
        Assert.assertEquals(obj.opacityAt(1100.0, listOf()), 0.0, 0.0)
        Assert.assertEquals(obj.opacityAt(1100.0, hidden), 0.0, 0.0)
    }

    companion object {
        private lateinit var obj: StandardDifficultyHitObject

        @BeforeClass
        @JvmStatic
        fun setup() {
            val hitObjects = listOf(
                HitCircle(500.0, Vector2(100), true, 0),
                Slider(
                    1000.0,
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

            obj = StandardDifficultyHitObject(
                hitObjects[1],
                hitObjects[0],
                1.0,
                arrayOf(),
                0
            ).also { it.computeProperties(1.0) }
        }
    }
}