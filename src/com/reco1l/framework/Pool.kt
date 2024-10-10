package com.reco1l.framework

import java.util.LinkedList

/**
 * A simple object pool implementation.
 *
 * @param T The type of object to pool.
 * @param maxSize The maximum size of the pool, by default -1 (no limit).
 * @param factory The factory to use to create new objects.
 * @author Reco1l
 */
class Pool<T : Any> @JvmOverloads constructor(

    private val maxSize: Int = -1,

    private val factory: (Pool<T>) -> T

) {


    private val objects = LinkedList<T>()


    /**
     * The number of objects in the pool.
     */
    val size: Int
        get() = objects.size


    /**
     * Obtain an object from the pool. If the pool is empty, a new object is created using the
     * factory and its arguments.
     */
    fun obtain() = objects.poll() ?: factory(this)

    /**
     * Return an object to the pool.
     */
    fun free(obj: T): Boolean {

        if (maxSize > 0 && objects.size >= maxSize) {
            return false
        }

        return objects.add(obj)
    }

    /**
     * Clear the pool.
     */
    fun clear() {
        objects.clear()
    }

    /**
     * Fills the pool up to the specified number of objects using the factory defined in the constructor.
     */
    fun fill(size: Int) {

        if (size <= 0) {
            throw IllegalArgumentException("Size must be greater than 0.")
        }

        for (i in objects.size until size) {
            objects.add(factory(this))
        }
    }

    /**
     * Clears the pool and fills it up to the specified number of objects using the factory defined in the constructor.
     */
    fun renew(size: Int) {
        objects.clear()
        fill(size)
    }
}