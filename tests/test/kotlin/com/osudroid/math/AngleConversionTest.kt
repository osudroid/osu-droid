package com.osudroid.math

import com.osudroid.math.toDegrees
import com.osudroid.math.toRadians
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AngleConversionTest(
    private val degrees: Double,
    private val radians: Double
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} degrees = {1} radians")
        fun data() = listOf(
            arrayOf(0.0, 0.0),
            arrayOf(45.0, Math.PI / 4),
            arrayOf(60.0, Math.PI / 3),
            arrayOf(90.0, Math.PI / 2),
            arrayOf(180.0, Math.PI)
        )
    }

    @Test
    fun `Test degrees to radians conversion`() {
        // Float overload
        Assert.assertEquals(radians.toFloat(), degrees.toFloat().toRadians(), 1e-5f)

        // Double overload
        Assert.assertEquals(radians, degrees.toRadians(), 1e-5)

        // Int and long overloads
        Assert.assertEquals(radians.toFloat(), degrees.toInt().toRadians(), 1e-5f)
        Assert.assertEquals(radians, degrees.toLong().toRadians(), 1e-5)
    }

    @Test
    fun `Test radians to degrees conversion`() {
        // Float overload
        Assert.assertEquals(degrees.toFloat(), radians.toFloat().toDegrees(), 1e-5f)
        // Double overload
        Assert.assertEquals(degrees, radians.toDegrees(), 1e-5)

        // Only test int and long conversions if the radian value is a whole number.
        // Otherwise, the conversion will break (e.g., casting Math.PI / 4 to Int becomes 0).
        if (radians % 1.0 == 0.0) {
            Assert.assertEquals(degrees.toFloat(), radians.toInt().toDegrees(), 1e-5f)
            Assert.assertEquals(degrees, radians.toLong().toDegrees(), 1e-5)
        }
    }
}