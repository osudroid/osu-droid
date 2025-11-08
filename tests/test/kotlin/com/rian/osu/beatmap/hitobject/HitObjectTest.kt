package com.rian.osu.beatmap.hitobject

import com.rian.osu.GameMode
import com.rian.osu.math.Vector2
import org.junit.Assert

sealed class HitObjectTest {
    protected open fun testStackedPositions(
        obj: HitObject, mode: GameMode,
        positionGetter: HitObject.() -> Vector2,
        stackedPositionGetter: HitObject.() -> Vector2
    ) = obj.apply {
        difficultyScale = 1f

        stackOffsetMultiplier = when (mode) {
            GameMode.Droid -> 4f
            GameMode.Standard -> -6.4f
        }

        fun testOffset() {
            val positionOffset = stackedPositionGetter() - positionGetter()

            Assert.assertEquals(difficultyStackOffset.x, positionOffset.x, 1e-2f)
            Assert.assertEquals(difficultyStackOffset.y, positionOffset.y, 1e-2f)
        }

        difficultyStackHeight = 1
        testOffset()

        difficultyStackHeight = 2
        testOffset()

        difficultyStackHeight = 4
        testOffset()
    }
}