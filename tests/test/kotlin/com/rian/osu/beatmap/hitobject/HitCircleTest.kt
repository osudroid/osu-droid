package com.rian.osu.beatmap.hitobject

import com.rian.osu.GameMode
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class HitCircleTest : HitObjectTest() {
    @Test
    fun testStartAndEndTime() {
        createCircle().apply {
            Assert.assertEquals(startTime, endTime, 0.0)
        }
    }

    @Test
    fun testEndPosition() {
        createCircle().apply {
            Assert.assertEquals(position, endPosition)
        }
    }

    @Test
    fun testStackedPositionWithoutHeight() {
        createCircle().apply {
            Assert.assertEquals(position, difficultyStackedPosition)
            Assert.assertEquals(endPosition, difficultyStackedEndPosition)
        }
    }

    @Test
    fun testStackedPositionWithHeight() {
        testStackedPositions(createCircle(), GameMode.Droid, { position }, { difficultyStackedPosition })
        testStackedPositions(createCircle(), GameMode.Droid, { endPosition }, { difficultyStackedEndPosition })

        testStackedPositions(createCircle(), GameMode.Standard, { position }, { difficultyStackedPosition })
        testStackedPositions(createCircle(), GameMode.Standard, { endPosition }, { difficultyStackedEndPosition })
    }

    @Test
    fun testRadius() {
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