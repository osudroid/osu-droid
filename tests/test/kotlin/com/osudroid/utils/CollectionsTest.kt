package com.osudroid.utils

import org.junit.Assert
import org.junit.Test

class CollectionsTest {
    @Test
    fun `Test collections standard deviation`() {
        data class TestCase(val data: Collection<Double>, val expected: Double)

        listOf(
            TestCase(listOf(), 0.0),
            TestCase(listOf(1.0), 0.0),
            TestCase(listOf(1.0, 5.0), 2.0),
            TestCase(listOf(1.0, 5.0, 9.0), 3.27),
            TestCase(listOf(1.0, 5.0, 9.0, 3.0, 12.0), 4.0)
        ).forEach { (data, expected) ->
            Assert.assertEquals(
                "Failed for data: $data",
                expected,
                data.standardDeviation(),
                1e-2
            )
        }
    }
}