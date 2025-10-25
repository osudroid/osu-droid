package com.rian.osu.beatmap.timings

import org.junit.Assert
import org.junit.Test

class ControlPointManagerTest {
    @Test
    fun `Test add control points to empty manager`() {
        val manager = DifficultyControlPointManager()

        // Redundant control point
        Assert.assertFalse(manager.add(createControlPoint(speedMultiplier = 1.0)))
        Assert.assertTrue(manager.controlPoints.isEmpty())

        // Not redundant control point
        Assert.assertTrue(manager.add(createControlPoint()))
        Assert.assertEquals(1, manager.controlPoints.size)
        Assert.assertEquals(1000.0, manager.controlPoints[0].time, 0.0)
        Assert.assertEquals(0.5, manager.controlPoints[0].speedMultiplier, 0.0)

        manager.clear()

        // At default control point
        Assert.assertTrue(manager.add(createControlPoint(time = 0.0)))
        Assert.assertEquals(1, manager.controlPoints.size)
        Assert.assertEquals(0.0, manager.controlPoints[0].time, 0.0)
        Assert.assertEquals(0.5, manager.controlPoints[0].speedMultiplier, 0.0)
    }

    @Test
    fun `Test add control points to manager with 1 control point`() {
        val manager = DifficultyControlPointManager()
        manager.add(createControlPoint())

        // Redundant control point
        Assert.assertFalse(manager.add(createControlPoint(1500.0)))
        Assert.assertEquals(1, manager.controlPoints.size)

        // Before control point
        Assert.assertTrue(manager.add(createControlPoint(500.0)))
        Assert.assertEquals(2, manager.controlPoints.size)
        Assert.assertEquals(500.0, manager.controlPoints[0].time, 0.0)
        Assert.assertEquals(0.5, manager.controlPoints[0].speedMultiplier, 0.0)
        Assert.assertEquals(1000.0, manager.controlPoints[1].time, 0.0)
        Assert.assertEquals(0.5, manager.controlPoints[1].speedMultiplier, 0.0)
    }

    @Test
    fun `Test add control points to manager with 2 control points`() {
        val manager = DifficultyControlPointManager()
        manager.add(createControlPoint())
        manager.add(createControlPoint(1500.0, 0.75))

        // Before both control points
        Assert.assertTrue(manager.add(createControlPoint(500.0, 0.8)))
        Assert.assertEquals(3, manager.controlPoints.size)
        Assert.assertEquals(500.0, manager.controlPoints[0].time, 0.0)
        Assert.assertEquals(0.8, manager.controlPoints[0].speedMultiplier, 0.0)

        manager.remove(0)

        // Between both control points
        Assert.assertTrue(manager.add(createControlPoint(1250.0, 1.0)))
        Assert.assertEquals(3, manager.controlPoints.size)
        Assert.assertEquals(1250.0, manager.controlPoints[1].time, 0.0)
        Assert.assertEquals(1.0, manager.controlPoints[1].speedMultiplier, 0.0)

        manager.remove(1)

        // After both control points
        Assert.assertTrue(manager.add(createControlPoint(2000.0, 1.0)))
        Assert.assertEquals(3, manager.controlPoints.size)
        Assert.assertEquals(2000.0, manager.controlPoints[2].time, 0.0)
        Assert.assertEquals(1.0, manager.controlPoints[2].speedMultiplier, 0.0)
    }

    @Test
    fun `Test remove control points`() {
        val manager = DifficultyControlPointManager()

        Assert.assertNull(manager.remove(0))
        Assert.assertFalse(manager.remove(createControlPoint()))

        manager.add(createControlPoint())
        manager.add(createControlPoint(1500.0, 0.75))

        Assert.assertEquals(2, manager.controlPoints.size)
        Assert.assertTrue(manager.remove(manager.controlPoints[1]))
        Assert.assertEquals(1, manager.controlPoints.size)

        manager.add(createControlPoint(1500.0, 0.75))
        val removed = manager.remove(1)

        Assert.assertEquals(1, manager.controlPoints.size)
        Assert.assertNotNull(removed)
        Assert.assertEquals(1500.0, removed!!.time, 0.0)
        Assert.assertEquals(0.75, removed.speedMultiplier, 0.0)

        // Before all control points
        Assert.assertFalse(manager.remove(createControlPoint(500.0, 0.75)))
    }

    @Test
    fun `Test control point search`() {
        val manager = DifficultyControlPointManager()
        manager.add(createControlPoint(speedMultiplier = 0.9))

        manager.controlPointAt(0.0).let {
            Assert.assertEquals(0.0, it.time, 0.0)
            Assert.assertEquals(1.0, it.speedMultiplier, 0.0)
        }

        manager.controlPointAt(1000.0).let {
            Assert.assertEquals(1000.0, it.time, 0.0)
            Assert.assertEquals(0.9, it.speedMultiplier, 0.0)
        }

        manager.controlPointAt(3000.0).let {
            Assert.assertEquals(1000.0, it.time, 0.0)
            Assert.assertEquals(0.9, it.speedMultiplier, 0.0)
        }

        manager.controlPointAt(7000.0).let {
            Assert.assertEquals(1000.0, it.time, 0.0)
            Assert.assertEquals(0.9, it.speedMultiplier, 0.0)
        }

        Assert.assertTrue(manager.add(createControlPoint(5000.0)))

        manager.controlPointAt(7000.0).let {
            Assert.assertEquals(5000.0, it.time, 0.0)
            Assert.assertEquals(0.5, it.speedMultiplier, 0.0)
        }
    }

    private fun createControlPoint(time: Double = 1000.0, speedMultiplier: Double = 0.5) =
        DifficultyControlPoint(time, speedMultiplier, true)
}