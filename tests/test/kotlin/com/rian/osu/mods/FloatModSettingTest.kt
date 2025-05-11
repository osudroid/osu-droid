package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test

class FloatModSettingTest {
    @Test
    fun `Test boundaries`() {
        fun test(action: () -> Unit) {
            Assert.assertThrows(IllegalArgumentException::class.java, action)
        }

        // Test min > max
        test { FloatModSetting(name = "Test", defaultValue = 0.12f, minValue = 0.12f, maxValue = 0.1f) }

        // Test defaultValue > max
        test { FloatModSetting(name = "Test", defaultValue = 0.24f, minValue = 0.1f, maxValue = 0.12f) }

        // Test defaultValue < min
        test { FloatModSetting(name = "Test", defaultValue = 0.1f, minValue = 0.12f, maxValue = 0.24f) }

        // Test step < 0
        test { FloatModSetting(name = "Test", defaultValue = 0.12f, step = -0.1f) }

        // Test precision < 0
        test { FloatModSetting(name = "Test", defaultValue = 0.12f, precision = -1) }
    }

    @Test
    fun `Test step without precision`() {
        var setting by FloatModSetting(name = "Test", defaultValue = 0.12f, step = 0.12f)
        Assert.assertEquals(0.12f, setting, 1e-5f)

        setting = 0.24f
        Assert.assertEquals(0.24f, setting, 1e-5f)

        setting = 0.36f
        Assert.assertEquals(0.36f, setting, 1e-5f)

        setting = 0.48f
        Assert.assertEquals(0.48f, setting, 1e-5f)
    }

    @Test
    fun `Test step with precision`() {
        var setting by FloatModSetting(
            name = "Test",
            defaultValue = 0.12f,
            step = 0.12f,
            precision = 2,
        )

        // These need to be exactly equal to prevent serialization differences.
        Assert.assertEquals(0.12f, setting)

        setting = 0.24f
        Assert.assertEquals(0.24f, setting)

        setting = 0.36f
        Assert.assertEquals(0.36f, setting)

        setting = 0.48f
        Assert.assertEquals(0.48f, setting)
    }

    @Test
    fun `Test value cap without precision`() {
        var setting by FloatModSetting(
            name = "Test",
            defaultValue = 0.12f,
            minValue = 0.12f,
            maxValue = 1.2f,
            step = 0.12f,
        )

        Assert.assertEquals(0.12f, setting, 1e-5f)

        setting = 0f
        Assert.assertEquals(0.12f, setting, 1e-5f)

        setting = 1.2f
        Assert.assertEquals(1.2f, setting, 1e-5f)

        setting = 1.5f
        Assert.assertEquals(1.2f, setting, 1e-5f)
    }

    @Test
    fun `Test value cap with precision`() {
        var setting by FloatModSetting(
            name = "Test",
            defaultValue = 0.12f,
            minValue = 0.12f,
            // Intentional multiplication to introduce floating point errors.
            maxValue = 0.12f * 10,
            step = 0.12f,
            precision = 2,
        )

        // These need to be exactly equal to prevent serialization differences.
        Assert.assertEquals(0.12f, setting)

        setting = 0f
        Assert.assertEquals(0.12f, setting)

        setting = 1.2f
        Assert.assertEquals(1.2f, setting)

        setting = 1.5f
        Assert.assertEquals(1.2f, setting)
    }
}