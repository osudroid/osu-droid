package com.rian.osu.beatmap.timings

import org.junit.Assert
import org.junit.Test

class ControlPointManagerTest {
    @Test
    fun `Test add control points to empty manager`() {
        val manager = DifficultyControlPointManager()

        // Redundant control point
        Assert.assertFalse(manager.add(createControlPoint(speedMultiplier = 1.0)))
        Assert.assertEquals(manager.controlPoints.size, 0)

        // Not redundant control point
        Assert.assertTrue(manager.add(createControlPoint()))
        Assert.assertEquals(manager.controlPoints.size, 1)
        Assert.assertEquals(manager.controlPoints[0].time, 1000.0, 0.0)
        Assert.assertEquals(manager.controlPoints[0].speedMultiplier, 0.5, 0.0)
        manager.clear()

        // At default control point
        Assert.assertTrue(manager.add(createControlPoint(time = 0.0)))
        Assert.assertEquals(manager.controlPoints.size, 1)
        Assert.assertEquals(manager.controlPoints[0].time, 0.0, 0.0)
        Assert.assertEquals(manager.controlPoints[0].speedMultiplier, 0.5, 0.0)
    }

    @Test
    fun `Test add control points to manager with 1 control point`() {
        val manager = DifficultyControlPointManager()
        manager.add(createControlPoint())

        // Redundant control point
        Assert.assertFalse(manager.add(createControlPoint(1500.0)))
        Assert.assertEquals(manager.controlPoints.size, 1)

        // Before control point
        Assert.assertTrue(manager.add(createControlPoint(500.0)))
        Assert.assertEquals(manager.controlPoints.size, 2)
        Assert.assertEquals(manager.controlPoints[0].time, 500.0, 0.0)
        Assert.assertEquals(manager.controlPoints[0].speedMultiplier, 0.5, 0.0)
        Assert.assertEquals(manager.controlPoints[1].time, 1000.0, 0.0)
        Assert.assertEquals(manager.controlPoints[1].speedMultiplier, 0.5, 0.0)
    }

    @Test
    fun `Test add control points to manager with 2 control points`() {
        val manager = DifficultyControlPointManager()
        manager.add(createControlPoint())
        manager.add(createControlPoint(1500.0, 0.75))

        // Before both control points
        Assert.assertTrue(manager.add(createControlPoint(500.0, 0.8)))
        Assert.assertEquals(manager.controlPoints.size, 3)
        Assert.assertEquals(manager.controlPoints[0].time, 500.0, 0.0)
        Assert.assertEquals(manager.controlPoints[0].speedMultiplier, 0.8, 0.0)
        manager.remove(0)

        // Between both control points
        Assert.assertTrue(manager.add(createControlPoint(1250.0, 1.0)))
        Assert.assertEquals(manager.controlPoints.size, 3)
        Assert.assertEquals(manager.controlPoints[1].time, 1250.0, 0.0)
        Assert.assertEquals(manager.controlPoints[1].speedMultiplier, 1.0, 0.0)
        manager.remove(1)

        // After both control points
        Assert.assertTrue(manager.add(createControlPoint(2000.0, 1.0)))
        Assert.assertEquals(manager.controlPoints.size, 3)
        Assert.assertEquals(manager.controlPoints[2].time, 2000.0, 0.0)
        Assert.assertEquals(manager.controlPoints[2].speedMultiplier, 1.0, 0.0)
    }

    @Test
    fun `Test remove control points`() {
        val manager = DifficultyControlPointManager()

        Assert.assertNull(manager.remove(0))
        Assert.assertFalse(manager.remove(createControlPoint()))

        manager.add(createControlPoint())
        manager.add(createControlPoint(1500.0, 0.75))

        Assert.assertEquals(manager.controlPoints.size, 2)
        Assert.assertTrue(manager.remove(manager.controlPoints[1]))
        Assert.assertEquals(manager.controlPoints.size, 1)

        manager.add(createControlPoint(1500.0, 0.75))
        val removed = manager.remove(1)
        Assert.assertEquals(manager.controlPoints.size, 1)
        Assert.assertNotNull(removed)
        Assert.assertEquals(removed!!.time, 1500.0, 0.0)
        Assert.assertEquals(removed.speedMultiplier, 0.75, 0.0)

        // Before all control points
        Assert.assertFalse(manager.remove(createControlPoint(500.0, 0.75)))
    }

    @Test
    fun `Test control point search`() {
        val manager = DifficultyControlPointManager()
        manager.add(createControlPoint(speedMultiplier = 0.9))

        manager.controlPointAt(0.0).let {
            Assert.assertEquals(it.time, 0.0, 0.0)
            Assert.assertEquals(it.speedMultiplier, 1.0, 0.0)
        }

        manager.controlPointAt(1000.0).let {
            Assert.assertEquals(it.time, 1000.0, 0.0)
            Assert.assertEquals(it.speedMultiplier, 0.9, 0.0)
        }

        manager.controlPointAt(3000.0).let {
            Assert.assertEquals(it.time, 1000.0, 0.0)
            Assert.assertEquals(it.speedMultiplier, 0.9, 0.0)
        }

        manager.controlPointAt(7000.0).let {
            Assert.assertEquals(it.time, 1000.0, 0.0)
            Assert.assertEquals(it.speedMultiplier, 0.9, 0.0)
        }

        Assert.assertTrue(manager.add(createControlPoint(5000.0)))

        manager.controlPointAt(7000.0).let {
            Assert.assertEquals(it.time, 5000.0, 0.0)
            Assert.assertEquals(it.speedMultiplier, 0.5, 0.0)
        }
    }

    private fun createControlPoint(time: Double = 1000.0, speedMultiplier: Double = 0.5) =
        DifficultyControlPoint(time, speedMultiplier, true)
}