package com.rian.framework

import com.edlplan.framework.easing.Easing
import org.junit.Assert
import org.junit.Test

class RollingCounterTest {
    @Test
    fun `Test rolling counter interpolation`() {
        val counter = TestRollingCounter(0)

        counter.rollingEasing = Easing.None
        counter.rollingDuration = 1000f

        counter.targetValue = 100
        counter.update(500f)

        Assert.assertEquals(50, counter.currentValue)

        counter.update(500f)
        Assert.assertEquals(100, counter.currentValue)
    }

    @Test
    fun `Test setting value without rolling`() {
        val counter = TestRollingCounter(0)

        counter.rollingDuration = 500f
        counter.setValueWithoutRolling(100)

        Assert.assertEquals(100, counter.currentValue)
        Assert.assertEquals(100, counter.targetValue)
    }
}

private class TestRollingCounter(initialValue: Int) : RollingCounter<Int>(initialValue) {
    override fun interpolate(startValue: Int, endValue: Int, progress: Float): Int {
        return (startValue + (endValue - startValue) * progress).toInt()
    }
}