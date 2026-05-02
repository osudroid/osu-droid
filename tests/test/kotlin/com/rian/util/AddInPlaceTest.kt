package com.rian.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AddInPlaceTest {
    private lateinit var list: MutableList<Int>

    @Before
    fun setUp() {
        list = mutableListOf()
    }

    // Insertion into an empty list

    @Test
    fun `addInPlace into empty list inserts at index 0`() {
        val index = list.addInPlace(5)

        assertEquals(0, index)
        assertEquals(listOf(5), list)
    }

    // Return-value correctness

    @Test
    fun `addInPlace returns correct index for head insertion`() {
        list.addAll(listOf(10, 20, 30))

        val index = list.addInPlace(5)

        assertEquals(0, index)
    }

    @Test
    fun `addInPlace returns correct index for tail insertion`() {
        list.addAll(listOf(10, 20, 30))

        val index = list.addInPlace(35)

        assertEquals(3, index)
    }

    @Test
    fun `addInPlace returns correct index for middle insertion`() {
        list.addAll(listOf(10, 20, 30))

        val index = list.addInPlace(15)

        assertEquals(1, index)
    }

    // List-state correctness after insertion

    @Test
    fun `addInPlace preserves sorted order after head insertion`() {
        list.addAll(listOf(10, 20, 30))
        list.addInPlace(5)

        assertEquals(listOf(5, 10, 20, 30), list)
    }

    @Test
    fun `addInPlace preserves sorted order after tail insertion`() {
        list.addAll(listOf(10, 20, 30))
        list.addInPlace(40)

        assertEquals(listOf(10, 20, 30, 40), list)
    }

    @Test
    fun `addInPlace preserves sorted order after middle insertion`() {
        list.addAll(listOf(10, 20, 30))
        list.addInPlace(25)

        assertEquals(listOf(10, 20, 25, 30), list)
    }

    @Test
    fun `addInPlace increments list size by one`() {
        list.addAll(listOf(1, 2, 3))
        val sizeBefore = list.size

        list.addInPlace(2)

        assertEquals(sizeBefore + 1, list.size)
    }

    // Duplicate values

    @Test
    fun `addInPlace handles duplicate value, list stays sorted`() {
        list.addAll(listOf(10, 20, 30))
        list.addInPlace(20)

        // Both copies of 20 must be adjacent and the list sorted
        assertEquals(4, list.size)
        assertTrue(list == list.sorted())
        assertEquals(2, list.count { it == 20 })
    }

    @Test
    fun `addInPlace on single-element list with same value inserts correctly`() {
        list.add(5)
        list.addInPlace(5)

        assertEquals(listOf(5, 5), list)
    }

    // Single-element list: boundary probes

    @Test
    fun `addInPlace inserts before sole element`() {
        list.add(10)
        val index = list.addInPlace(5)

        assertEquals(0, index)
        assertEquals(listOf(5, 10), list)
    }

    @Test
    fun `addInPlace inserts after sole element`() {
        list.add(10)
        val index = list.addInPlace(15)

        assertEquals(1, index)
        assertEquals(listOf(10, 15), list)
    }

    // Multiple sequential insertions

    @Test
    fun `addInPlace keeps list sorted across many random insertions`() {
        val values = listOf(50, 10, 80, 30, 70, 20, 90, 40, 60)

        for (v in values) {
            list.addInPlace(v)
        }

        assertEquals(values.sorted(), list)
    }

    // String list (verifies generic Comparable constraint)

    @Test
    fun `addInPlace works correctly with String elements`() {
        val list = mutableListOf("apple", "cherry", "elderberry")

        val index = list.addInPlace("banana")

        assertEquals(1, index)
        assertEquals(listOf("apple", "banana", "cherry", "elderberry"), list)
    }

    @Test
    fun `addInPlace inserts String at head correctly`() {
        val list = mutableListOf("banana", "cherry")

        val index = list.addInPlace("apple")

        assertEquals(0, index)
        assertEquals(listOf("apple", "banana", "cherry"), list)
    }

    @Test
    fun `addInPlace inserts String at tail correctly`() {
        val list = mutableListOf("apple", "banana")

        val index = list.addInPlace("cherry")

        assertEquals(2, index)
        assertEquals(listOf("apple", "banana", "cherry"), list)
    }

    // Double list (another Comparable type)

    @Test
    fun `addInPlace works correctly with Double elements`() {
        val list = mutableListOf(1.1, 2.2, 3.3)

        val index = list.addInPlace(2.5)

        assertEquals(2, index)
        assertEquals(listOf(1.1, 2.2, 2.5, 3.3), list)
    }

    // Negative numbers

    @Test
    fun `addInPlace handles negative integers correctly`() {
        list.addAll(listOf(-30, -20, -10, 0))

        val index = list.addInPlace(-15)

        assertEquals(2, index)
        assertEquals(listOf(-30, -20, -15, -10, 0), list)
    }

    @Test
    fun `addInPlace handles mix of negative and positive integers`() {
        list.addAll(listOf(-10, 0, 10))

        val index = list.addInPlace(-5)

        assertEquals(1, index)
        assertEquals(listOf(-10, -5, 0, 10), list)
    }
}