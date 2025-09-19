package com.osudroid.utils

import androidx.core.util.Pools

/**
 * An interface for objects that can be pooled and recycled.
 */
interface IPoolable {
    /**
     * Whether this [IPoolable] is currently recycled (in the pool). Must always be `false` initially.
     *
     * **This property is managed by the pool itself and should not be modified manually**.
     */
    var isRecycled: Boolean
}

/**
 * A synchronized [Pools.Pool] for reusing instances of [IPoolable] objects.
 *
 * Unlike [Pools.SimplePool] and its descendants, this pool determines whether an object is already recycled by checking
 * [IPoolable.isRecycled] instead of performing a linear search through the pool.
 *
 * @param maxPoolSize The maximum number of objects to keep in the pool.
 * @param T The type of objects in the pool, which must implement [IPoolable].
 */
open class SynchronizedPool<T : IPoolable>(maxPoolSize: Int) : Pools.Pool<T> {
    private val pool: Array<IPoolable?>
    private var size = 0
    private val lock = Any()

    init {
        require(maxPoolSize > 0) { "The max pool size must be > 0" }
        pool = arrayOfNulls<IPoolable>(maxPoolSize)
    }

    override fun acquire(): T? = synchronized(lock) {
        if (size == 0) {
            return@synchronized null
        }

        @Suppress("UNCHECKED_CAST")
        val instance = pool[--size] as T

        pool[size] = null
        instance.isRecycled = false
        instance
    }

    override fun release(instance: T) = synchronized(lock) {
        if (instance.isRecycled) {
            throw IllegalStateException("This instance is already recycled.")
        }

        if (size < pool.size) {
            instance.isRecycled = true
            pool[size++] = instance
            true
        } else {
            false
        }
    }
}