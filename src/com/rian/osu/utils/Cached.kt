package com.rian.osu.utils

/**
 * Describes a value that can be cached.
 */
class Cached<T>(
    /**
     * The value to cache.
     */
    value: T
) {
    /**
     * The cached value.
     */
    var value: T = value
        get() {
            if (!isValid) {
                throw UnsupportedOperationException("May not query value of an invalid cache.")
            }

            return field
        }
        set(value) {
            field = value
            isValid = true
        }

    /**
     * Whether the cache is valid.
     */
    var isValid: Boolean = true
        private set

    /**
     * Invalidates the cache of this [Cached].
     *
     * @return `true` if the cache was invalidated from a valid state.
     */
    fun invalidate(): Boolean {
        if (isValid) {
            isValid = false

            return true
        }

        return false
    }
}