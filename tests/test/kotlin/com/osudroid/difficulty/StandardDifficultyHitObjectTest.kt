package com.osudroid.difficulty

import com.osudroid.GameMode
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.SliderPath
import com.osudroid.beatmaps.hitobjects.SliderPathType
import com.osudroid.beatmaps.sections.BeatmapControlPoints
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.beatmaps.timings.TimingControlPoint
import com.osudroid.math.Vector2
import com.osudroid.mods.ModHidden
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class StandardDifficultyHitObjectTest {
    private val hidden = listOf(ModHidden())

    @Test
    fun `Test previous index`() {
        Assert.assertNull(_root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.previous(0))
    }

    @Test
    fun `Test next index`() {
        Assert.assertNull(_root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.next(0))
    }

    @Test
    fun `Test No Mod opacity before hit time`() {
        Assert.assertEquals(0.0, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(400.0, listOf()), 0.0)
        Assert.assertEquals(0.5, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(600.0, listOf()), 1e-2)
        Assert.assertEquals(1.0, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(800.0, listOf()), 0.0)
        Assert.assertEquals(1.0, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(1000.0, listOf()), 0.0)
    }

    @Test
    fun `Test Hidden opacity before hit time`() {
        Assert.assertEquals(0.0, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(400.0, hidden), 0.0)
        Assert.assertEquals(0.5, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(600.0, hidden), 1e-2)
        Assert.assertEquals(1.0, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(800.0, hidden), 0.0)
        Assert.assertEquals(0.44, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(900.0, hidden), 1e-2)
        Assert.assertEquals(0.0, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(1000.0, hidden), 0.0)
    }

    @Test
    fun `Test opacity after hit time`() {
        Assert.assertEquals(0.0, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(1100.0, listOf()), 0.0)
        Assert.assertEquals(0.0, _root_ide_package_.com.osudroid.difficulty.StandardDifficultyHitObjectTest.Companion.obj.opacityAt(1100.0, hidden), 0.0)
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