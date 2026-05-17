package com.osudroid.utils

import org.junit.Assert
import org.junit.Test

class LRUCacheTest {
    @Test
    fun `Test LRU cache`() {
        val cache = LRUCache<Int, Int>(3)

        cache[1] = 1
        cache[2] = 2
        cache[3] = 3

        Assert.assertEquals(1, cache.get(1))
        Assert.assertEquals(2, cache.get(2))
        Assert.assertEquals(3, cache.get(3))

        cache[4] = 4

        Assert.assertNull(cache.get(1))
        Assert.assertEquals(2, cache.get(2))
        Assert.assertEquals(3, cache.get(3))
        Assert.assertEquals(4, cache.get(4))

        cache[5] = 5

        Assert.assertNull(cache.get(1))
        Assert.assertNull(cache.get(2))
        Assert.assertEquals(3, cache.get(3))
        Assert.assertEquals(4, cache.get(4))
        Assert.assertEquals(5, cache.get(5))
    }
}