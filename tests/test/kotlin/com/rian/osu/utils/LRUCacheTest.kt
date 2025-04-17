package com.rian.osu.utils

import org.junit.Assert
import org.junit.Test

class LRUCacheTest {
    @Test
    fun `Test LRU cache`() {
        val cache = LRUCache<Int, Int>(3)

        cache.put(1, 1)
        cache.put(2, 2)
        cache.put(3, 3)

        Assert.assertEquals(cache.get(1), 1)
        Assert.assertEquals(cache.get(2), 2)
        Assert.assertEquals(cache.get(3), 3)

        cache.put(4, 4)

        Assert.assertEquals(cache.get(1), null)
        Assert.assertEquals(cache.get(2), 2)
        Assert.assertEquals(cache.get(3), 3)
        Assert.assertEquals(cache.get(4), 4)

        cache.put(5, 5)

        Assert.assertEquals(cache.get(1), null)
        Assert.assertEquals(cache.get(2), null)
        Assert.assertEquals(cache.get(3), 3)
        Assert.assertEquals(cache.get(4), 4)
        Assert.assertEquals(cache.get(5), 5)
    }
}