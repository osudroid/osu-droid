package com.rian.osu.beatmap.hitobject

import com.rian.osu.GameMode
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class SpinnerTest : HitObjectTest() {
    private val center = Vector2(256, 192)

    @Test
    fun `Test duration`() {
        createSpinner(100.0).apply { Assert.assertEquals(duration, 100.0, 0.0) }
        createSpinner(200.0).apply { Assert.assertEquals(duration, 200.0, 0.0) }
        createSpinner(500.0).apply { Assert.assertEquals(duration, 500.0, 0.0) }
    }

    @Test
    fun `Test end time`() {
        createSpinner(100.0).apply { Assert.assertEquals(endTime, 1100.0, 0.0) }
        createSpinner(200.0).apply { Assert.assertEquals(endTime, 1200.0, 0.0) }
        createSpinner(500.0).apply { Assert.assertEquals(endTime, 1500.0, 0.0) }
    }

    @Test
    fun `Test position`() {
        createSpinner().apply { Assert.assertEquals(center, position) }
    }

    @Test
    fun `Test end position`() {
        createSpinner().apply { Assert.assertEquals(center, endPosition) }
    }

    @Test
    fun `Test stacked position without stack height`() {
        createSpinner().apply {
            Assert.assertEquals(center, difficultyStackedPosition)
            Assert.assertEquals(center, difficultyStackedEndPosition)
        }
    }

    @Test
    fun `Test stacked position with stack height`() {
        testStackedPositions(createSpinner(), GameMode.Droid, { position }, { difficultyStackedPosition })
        testStackedPositions(createSpinner(), GameMode.Droid, { endPosition }, { difficultyStackedEndPosition })

        testStackedPositions(createSpinner(), GameMode.Standard, { position }, { difficultyStackedPosition })
        testStackedPositions(createSpinner(), GameMode.Standard, { endPosition }, { difficultyStackedEndPosition })
    }

    override fun testStackedPositions(
        obj: HitObject,
        mode: GameMode,
        positionGetter: HitObject.() -> Vector2,
        stackedPositionGetter: HitObject.() -> Vector2
    ) = obj.apply {
        difficultyStackHeight = 1
        Assert.assertEquals(stackedPositionGetter(), positionGetter())

        difficultyStackHeight = 2
        Assert.assertEquals(stackedPositionGetter(), positionGetter())

        difficultyStackHeight = 4
        Assert.assertEquals(stackedPositionGetter(), positionGetter())
    }

    private fun createSpinner(duration: Double = 100.0) = Spinner(1000.0, 1000.0 + duration, true)
}