package com.rian.osu.utils

import org.junit.Assert
import org.junit.Test

class CachedTest {
    @Test
    fun `Test cache invalidation`() {
        val cached = Cached(0)

        Assert.assertTrue(cached.isValid)

        cached.invalidate()

        Assert.assertFalse(cached.isValid)
    }

    @Test
    fun `Test cache access when invalid`() {
        val cached = Cached(0)
        cached.invalidate()

        Assert.assertThrows(UnsupportedOperationException::class.java) { cached.value }
    }
}