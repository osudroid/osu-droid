package com.rian.util

import org.junit.Assert
import org.junit.Test

class StringsTest {
    @Test
    fun `Test string conversion to float with comma separator`() {
        Assert.assertEquals(123.45f, "123,45".toFloatWithCommaSeparator())
        Assert.assertEquals(123.4567f, "123,4567".toFloatWithCommaSeparator())
        Assert.assertEquals(1023.456f, "1.023,456".toFloatWithCommaSeparator())
        Assert.assertEquals(0.99f, "0,99".toFloatWithCommaSeparator())
        Assert.assertEquals(10f, "10,0".toFloatWithCommaSeparator())
    }
}