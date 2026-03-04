package com.rian.util

import org.junit.Assert
import org.junit.Test

class StringsTest {
    @Test
    fun `Test string conversion to float with comma separator`() {
        listOf(
            "123,45" to 123.45f,
            "123,4567" to 123.4567f,
            "1.023,456" to 1023.456f,
            "0,99" to 0.99f,
            "10,0" to 10f,
        ).forEach { (input, expected) ->
            Assert.assertEquals(expected, input.toFloatWithCommaSeparator())
        }
    }
}