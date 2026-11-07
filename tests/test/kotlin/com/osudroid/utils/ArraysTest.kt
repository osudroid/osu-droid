package com.osudroid.utils

import org.junit.Assert
import org.junit.Test

class ArraysTest {
    @Test
    fun `IntArray firstIndexAtOrAfter`() {
        val a = intArrayOf(0, 100, 100, 200)

        Assert.assertEquals(0, a.firstIndexAtOrAfter(-50))
        Assert.assertEquals(0, a.firstIndexAtOrAfter(0))
        Assert.assertEquals(1, a.firstIndexAtOrAfter(50))
        Assert.assertEquals(1, a.firstIndexAtOrAfter(100))
        Assert.assertEquals(3, a.firstIndexAtOrAfter(200))
        Assert.assertEquals(4, a.firstIndexAtOrAfter(201))
        Assert.assertEquals(0, intArrayOf().firstIndexAtOrAfter(0))
    }

    @Test
    fun `IntArray lastIndexAtOrBefore`() {
        val a = intArrayOf(0, 100, 100, 200)

        Assert.assertEquals(-1, a.lastIndexAtOrBefore(-1))
        Assert.assertEquals(0, a.lastIndexAtOrBefore(0))
        Assert.assertEquals(0, a.lastIndexAtOrBefore(50))
        Assert.assertEquals(2, a.lastIndexAtOrBefore(100))
        Assert.assertEquals(3, a.lastIndexAtOrBefore(200))
        Assert.assertEquals(3, a.lastIndexAtOrBefore(500))
        Assert.assertEquals(-1, intArrayOf().lastIndexAtOrBefore(0))
    }

    @Test
    fun `Array firstIndexAtOrAfter`() {
        val a = arrayOf("b", "d", "d", "f")

        Assert.assertEquals(0, a.firstIndexAtOrAfter("a"))
        Assert.assertEquals(1, a.firstIndexAtOrAfter("c"))
        Assert.assertEquals(1, a.firstIndexAtOrAfter("d"))
        Assert.assertEquals(4, a.firstIndexAtOrAfter("g"))
        Assert.assertEquals(0, emptyArray<String>().firstIndexAtOrAfter("a"))
    }

    @Test
    fun `Array lastIndexAtOrBefore`() {
        val a = arrayOf("b", "d", "d", "f")

        Assert.assertEquals(-1, a.lastIndexAtOrBefore("a"))
        Assert.assertEquals(0, a.lastIndexAtOrBefore("c"))
        Assert.assertEquals(2, a.lastIndexAtOrBefore("d"))
        Assert.assertEquals(3, a.lastIndexAtOrBefore("g"))
        Assert.assertEquals(-1, emptyArray<String>().lastIndexAtOrBefore("a"))
    }
}
