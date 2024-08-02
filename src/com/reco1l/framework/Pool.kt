package com.reco1l.framework

/**
 * A simple object pool implementation.
 *
 * @param T The type of object to pool.
 * @param factory The factory to use to create new objects.
 * @author Reco1l
 */
class Pool<T : Any>(private val factory: (Pool<T>) -> T) {

    private val objects = mutableListOf<T>()


    /**
     * The number of objects in the pool.
     */
    val size: Int
        get() = objects.size


    /**
     * Obtain an object from the pool. If the pool is empty, a new object is created using the
     * factory and its arguments.
     */
    fun obtain() = if (objects.isEmpty()) factory(this) else objects.removeFirst()

    /**
     * Return an object to the pool.
     */
    fun free(obj: T) {
        objects.add(obj)
    }

    /**
     * Clear the pool.
     */
    fun clear() = objects.clear()
}