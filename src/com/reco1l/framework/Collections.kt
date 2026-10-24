package com.reco1l.framework

/**
 * Per-thread, depth-indexed pool of reusable snapshot buffers backing [forEachTolerant] and
 * [forEachIndexedTolerant].
 *
 * A plain index-based loop over the live list tolerates it shrinking (bounds check) but not
 * skipping or double-visiting elements. Removing the current element shifts the next one into its
 * slot and past the loop unseen, and reordering (e.g. moving an element to the end) can revisit a
 * slot. Snapshotting into a buffer first and iterating that instead avoids both.
 *
 * A single buffer per thread would still break under reentrancy. [action] can itself trigger a
 * nested call to [forEachTolerant] or [forEachIndexedTolerant] on the same thread (directly, or
 * through a chain of calls) before the outer call has finished with its buffer, so the inner
 * call's `clear()` would corrupt the outer iteration mid-flight. Indexing each thread's pool by
 * nesting depth gives every active call its own buffer instead.
 *
 * [forEachTolerant] and [forEachIndexedTolerant] are public extensions on any `List`, so callers
 * can be on any thread. The pool is thread-local so each thread owns an independent buffer stack.
 */
private class TolerantBufferPool {
    val buffers = ArrayList<ArrayList<Any?>>()
    var depth = 0
}

private val tolerantBufferPool = object : ThreadLocal<TolerantBufferPool>() {
    override fun initialValue() = TolerantBufferPool()
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T> acquireTolerantBuffer(): ArrayList<T> {
    // Android's ThreadLocal.get() is declared as a platform-nullable T, but initialValue() always
    // supplies a non-null pool, and it's never set(null), so this is never actually null.
    val pool = tolerantBufferPool.get()!!

    val buffer = if (pool.depth < pool.buffers.size) {
        pool.buffers[pool.depth]
    } else {
        ArrayList<Any?>().also { pool.buffers.add(it) }
    }

    pool.depth++
    return buffer as ArrayList<T>
}

@PublishedApi
internal fun releaseTolerantBuffer() {
    val pool = tolerantBufferPool.get()!!
    pool.depth--
    // Cleared on release, not acquire, so an unused object isn't kept alive by a stale
    // snapshot reference until this depth is reused.
    pool.buffers[pool.depth].clear()
}

/**
 * Iterates over a snapshot of the receiver taken at call time, so that a structural mutation
 * caused by [action] (e.g. removing or reordering elements) can neither skip nor duplicate
 * elements, unlike a live index-based loop or [Iterable.forEach] (the latter also fails fast on
 * such mutation for [ArrayList]-backed collections).
 *
 * The snapshot is drawn from a per-thread pool rather than allocated per call.
 *
 * Only use this in place of [Iterable.forEach] where that mutation tolerance is actually needed. In addition to that,
 * do not "simplify" a call site back to [Iterable.forEach] without first confirming the callback can't structurally
 * mutate the same [List] it's iterating.
 */
inline fun <T> List<T>.forEachTolerant(action: (T) -> Unit) {
    val buffer = acquireTolerantBuffer<T>()

    try {
        buffer.addAll(this)

        for (i in buffer.indices) {
            action(buffer[i])
        }
    } finally {
        releaseTolerantBuffer()
    }
}

/**
 * Iterates over a snapshot of the receiver taken at call time.
 *
 * @see forEachTolerant
 */
inline fun <T> List<T>.forEachIndexedTolerant(action: (Int, T) -> Unit) {
    val buffer = acquireTolerantBuffer<T>()

    try {
        buffer.addAll(this)

        for (i in buffer.indices) {
            action(i, buffer[i])
        }
    } finally {
        releaseTolerantBuffer()
    }
}
