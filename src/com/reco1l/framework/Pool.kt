package com.reco1l.framework

/**
 * A simple object pool implementation.
 *
 * @param T The type of object to pool.
 * @param initialSize The initial size of the pool.
 * @param factory The factory to use to create new objects.
 * @author Reco1l
 */
class Pool<T : Any>(initialSize: Int = 0, private val maxSize: Int, private val factory: (Pool<T>) -> T) {


    private val objects = mutableListOf<T>()


    init {
        repeat(initialSize) {
            objects.add(factory(this))
        }
    }


    /**
     * The number of objects in the pool.
     */
    val size: Int
        get() = objects.size


    /**
     * Obtain an object from the pool. If the pool is empty, a new object is created using the
     * factory and its arguments.
     */
    fun obtain() = objects.removeFirstOrNull() ?: factory(this)

    /**
     * Return an object to the pool.
     */
    fun free(obj: T): Boolean {

        if (objects.size >= maxSize) {
            return false
        }

        return objects.add(obj)
    }

    /**
     * Clear the pool.
     */
    fun clear() = objects.clear()
}