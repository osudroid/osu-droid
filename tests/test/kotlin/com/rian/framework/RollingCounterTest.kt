package com.rian.framework

import com.edlplan.framework.easing.Easing
import org.junit.Assert
import org.junit.Test

class RollingCounterTest {
    @Test
    fun `Test rolling counter interpolation`() {
        val counter = TestRollingCounter(0)

        counter.rollingEasing = Easing.None
        counter.rollingDuration = 1f

        counter.targetValue = 100
        counter.onUpdate(0.5f)

        Assert.assertTrue(counter.isRolling)
        Assert.assertEquals(50, counter.currentValue)

        counter.onUpdate(0.5f)

        Assert.assertFalse(counter.isRolling)
        Assert.assertEquals(100, counter.currentValue)
    }

    @Test
    fun `Test mid-roll retarget starts from current value`() {
        val counter = TestRollingCounter(0)

        counter.rollingEasing = Easing.None
        counter.rollingDuration = 1f

        counter.targetValue = 100
        counter.onUpdate(0.5f)

        Assert.assertEquals(50, counter.currentValue)

        // Retarget mid-roll: should start a fresh lerp from 50, not from 0
        counter.targetValue = 200
        counter.onUpdate(0.5f)

        Assert.assertTrue(counter.isRolling)
        Assert.assertEquals(125, counter.currentValue)

        counter.onUpdate(0.5f)

        Assert.assertFalse(counter.isRolling)
        Assert.assertEquals(200, counter.currentValue)
    }

    @Test
    fun `Test zero rolling duration snaps immediately`() {
        val counter = TestRollingCounter(0)

        // rollingDuration defaults to 0, so update should snap without dividing by zero.
        counter.targetValue = 100
        counter.onUpdate(0.016f)

        Assert.assertFalse(counter.isRolling)
        Assert.assertEquals(100, counter.currentValue)
    }

    @Test
    fun `Test setting value without rolling`() {
        val counter = TestRollingCounter(0)

        counter.rollingDuration = 0.5f
        counter.setValueWithoutRolling(100)

        Assert.assertFalse(counter.isRolling)
        Assert.assertEquals(100, counter.currentValue)
        Assert.assertEquals(100, counter.targetValue)
    }
}

private class TestRollingCounter(initialValue: Int) : RollingCounter<Int>(initialValue) {
    override fun interpolate(startValue: Int, endValue: Int, progress: Float): Int {
        return (startValue + (endValue - startValue) * progress).toInt()
    }
}