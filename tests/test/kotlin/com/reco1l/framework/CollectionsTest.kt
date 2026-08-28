package com.reco1l.framework

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class CollectionsTest {
    @Test
    fun `Test forEachTolerant visits every element in order`() {
        val visited = mutableListOf<Int>()
        listOf(1, 2, 3, 4).forEachTolerant { visited.add(it) }

        Assert.assertEquals(listOf(1, 2, 3, 4), visited)
    }

    @Test
    fun `Test forEachIndexedTolerant reports snapshot indices`() {
        val collected = mutableListOf<Pair<Int, String>>()
        val list = mutableListOf("a", "b", "c")

        list.forEachIndexedTolerant { index, item ->
            collected.add(index to item)

            // Mutating the live list should not affect indices already reported for the snapshot.
            if (item == "a") {
                list.add("d")
            }
        }

        Assert.assertEquals(listOf(0 to "a", 1 to "b", 2 to "c"), collected)
    }

    @Test
    fun `Test forEachTolerant on an empty list does nothing`() {
        val visited = mutableListOf<Int>()
        emptyList<Int>().forEachTolerant { visited.add(it) }

        Assert.assertTrue(visited.isEmpty())
    }

    @Test
    fun `Test forEachTolerant tolerates element removal during iteration`() {
        val list = mutableListOf(1, 2, 3, 4)
        val visited = mutableListOf<Int>()

        list.forEachTolerant { item ->
            visited.add(item)

            if (item == 2) {
                list.remove(2)
            }
        }

        // A live index-based loop would skip 3 here (removing 2 shifts 3 into the
        // slot the loop just finished with, so the next index lands on 4 instead).
        Assert.assertEquals(listOf(1, 2, 3, 4), visited)
        Assert.assertEquals(listOf(1, 3, 4), list)
    }

    @Test
    fun `Test forEachTolerant tolerates reordering during iteration`() {
        val list = mutableListOf(1, 2, 3, 4)
        val visited = mutableListOf<Int>()

        list.forEachTolerant { item ->
            visited.add(item)

            if (item == 2) {
                list.remove(2)
                list.add(2)
            }
        }

        // A live index-based loop would revisit 2 here, since moving it
        // to the end shifts it into an index the loop hasn't reached yet.
        Assert.assertEquals(listOf(1, 2, 3, 4), visited)
        Assert.assertEquals(listOf(1, 3, 4, 2), list)
    }

    @Test
    fun `Test nested call on a different list does not corrupt the outer snapshot`() {
        val outerList = listOf(1, 2, 3, 4, 5)
        val innerList = listOf(10, 20)

        val outerVisited = mutableListOf<Int>()
        val innerVisited = mutableListOf<Int>()

        outerList.forEachTolerant { outer ->
            outerVisited.add(outer)

            // A single shared buffer would have its clear() + refill here stomp on the outer call's
            // still-in-progress snapshot, since this call is on the same thread while the outer
            // call hasn't finished with its buffer.
            if (outer == 3) {
                innerList.forEachTolerant { inner -> innerVisited.add(inner) }
            }
        }

        Assert.assertEquals(listOf(1, 2, 3, 4, 5), outerVisited)
        Assert.assertEquals(listOf(10, 20), innerVisited)
    }

    @Test
    fun `Test a released buffer does not leak stale elements into the next call at the same depth`() {
        val firstVisited = mutableListOf<Int>()
        listOf(1, 2, 3).forEachTolerant { firstVisited.add(it) }

        val secondVisited = mutableListOf<Int>()
        listOf(9, 8).forEachTolerant { secondVisited.add(it) }

        Assert.assertEquals(listOf(1, 2, 3), firstVisited)
        Assert.assertEquals(listOf(9, 8), secondVisited)
    }

    @Test
    fun `Test buffer is released even when action throws`() {
        val list = listOf(1, 2, 3)

        Assert.assertThrows(RuntimeException::class.java) {
            list.forEachTolerant { if (it == 2) throw RuntimeException("boom") }
        }

        // If the depth counter weren't decremented on the failed call, this call would be handed
        // a buffer one level deeper than it should be.
        val visited = mutableListOf<Int>()
        list.forEachTolerant { visited.add(it) }

        Assert.assertEquals(listOf(1, 2, 3), visited)
    }

    @Test
    fun `Test thread-local pools do not interfere across threads`() {
        val error = AtomicReference<Throwable>()

        fun repeatedlyIterate(list: List<Int>) {
            repeat(50) {
                val visited = mutableListOf<Int>()
                list.forEachTolerant { visited.add(it) }

                if (visited != list) {
                    throw AssertionError("Expected $list but visited $visited")
                }
            }
        }

        val threads = listOf(
            Thread { runCatching { repeatedlyIterate(List(500) { it }) }.onFailure { error.compareAndSet(null, it) } },
            Thread { runCatching { repeatedlyIterate(List(500) { it + 10_000 }) }.onFailure { error.compareAndSet(null, it) } }
        )

        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        error.get()?.let { throw it }
    }
}
