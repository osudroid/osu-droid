package com.osudroid.utils

import org.junit.Assert
import org.junit.Test

class SynchronizedPoolTest {
    @Test
    fun `Test acquiring from an empty pool`() {
        val pool = SynchronizedPool<TestPoolable>(2)
        Assert.assertNull(pool.acquire())
    }

    @Test
    fun `Test releasing and acquiring an object`() {
        val pool = SynchronizedPool<TestPoolable>(2)
        val obj = TestPoolable()

        Assert.assertTrue(pool.release(obj))
        val acquired = pool.acquire()

        Assert.assertNotNull(acquired)
        Assert.assertEquals(obj, acquired)
        Assert.assertFalse(acquired!!.isRecycled)
    }

    @Test
    fun `Test releasing the same object twice throws exception`() {
        val pool = SynchronizedPool<TestPoolable>(2)
        val obj = TestPoolable()

        pool.release(obj)
        Assert.assertThrows(IllegalStateException::class.java) { pool.release(obj) }
    }

    @Test
    fun `Test pool does not exceed max size`() {
        val pool = SynchronizedPool<TestPoolable>(2)
        val obj1 = TestPoolable()
        val obj2 = TestPoolable()
        val obj3 = TestPoolable()

        Assert.assertTrue(pool.release(obj1))
        Assert.assertTrue(pool.release(obj2))

        // Pool is full, should return false
        Assert.assertFalse(pool.release(obj3))
    }
}

private class TestPoolable : IPoolable {
    override var isRecycled = false
}