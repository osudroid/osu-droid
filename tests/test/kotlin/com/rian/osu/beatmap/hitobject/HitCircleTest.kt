package com.rian.osu.beatmap.hitobject

import com.rian.osu.GameMode
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class HitCircleTest : HitObjectTest() {
    @Test
    fun `Test start time and end time`() {
        createCircle().apply {
            Assert.assertEquals(endTime, startTime, 0.0)
        }
    }

    @Test
    fun `Test end position`() {
        createCircle().apply {
            Assert.assertEquals(endPosition, position)
        }
    }

    @Test
    fun `Test stacked position without stack height`() {
        createCircle().apply {
            Assert.assertEquals(difficultyStackedPosition, position)
            Assert.assertEquals(difficultyStackedEndPosition, endPosition)
        }
    }

    @Test
    fun `Test stacked position with stack height`() {
        testStackedPositions(createCircle(), GameMode.Droid, { position }, { difficultyStackedPosition })
        testStackedPositions(createCircle(), GameMode.Droid, { endPosition }, { difficultyStackedEndPosition })

        testStackedPositions(createCircle(), GameMode.Standard, { position }, { difficultyStackedPosition })
        testStackedPositions(createCircle(), GameMode.Standard, { endPosition }, { difficultyStackedEndPosition })
    }

    @Test
    fun `Test radius`() {
        createCircle().apply {
            difficultyScale = 1f
            Assert.assertEquals(difficultyRadius, HitObject.OBJECT_RADIUS.toDouble(), 0.0)

            difficultyScale = 0.5f
            Assert.assertEquals(difficultyRadius, HitObject.OBJECT_RADIUS * 0.5, 0.0)

            difficultyScale = 2f
            Assert.assertEquals(difficultyRadius, HitObject.OBJECT_RADIUS * 2.0, 0.0)
        }
    }

    private fun createCircle() = HitCircle(1000.0, Vector2(256, 192), true, 0)
}