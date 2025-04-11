package com.rian.osu.utils

import org.junit.Assert
import org.junit.Test

class CachedTest {
    @Test
    fun testCacheInvalidation() {
        val cached = Cached(0)

        Assert.assertTrue(cached.isValid)

        cached.invalidate()

        Assert.assertFalse(cached.isValid)
    }

    @Test
    fun testCacheAccessWhenInvalid() {
        val cached = Cached(0)
        cached.invalidate()

        Assert.assertThrows(UnsupportedOperationException::class.java) { cached.value }
    }
}