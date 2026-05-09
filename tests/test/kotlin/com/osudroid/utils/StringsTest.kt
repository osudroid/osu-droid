package com.osudroid.utils

import org.junit.Assert
import org.junit.Test

class StringsTest {
    @Test
    fun `Test contiguous search`() {
        Assert.assertTrue("".searchContiguously(""))
        Assert.assertTrue("a".searchContiguously(""))
        Assert.assertFalse("".searchContiguously("a"))

        val haystack = "Synesthesia"

        Assert.assertTrue(haystack.searchContiguously("sy h", true))
        Assert.assertTrue(haystack.searchContiguously("S Y N", true))
        Assert.assertTrue(haystack.searchContiguously("s y n", true))
        Assert.assertTrue(haystack.searchContiguously("S y n", false))
        Assert.assertTrue(haystack.searchContiguously("synesthesia", true))

        Assert.assertFalse(haystack.searchContiguously("s y n h", false))
        Assert.assertFalse(haystack.searchContiguously("synesthesia", false))
        Assert.assertFalse(haystack.searchContiguously("xyz", true))
        Assert.assertFalse(haystack.searchContiguously("xyz", false))
        Assert.assertFalse(haystack.searchContiguously("sy h", false))
    }

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