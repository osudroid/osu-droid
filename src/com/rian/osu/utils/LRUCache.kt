package com.rian.osu.utils

import kotlin.collections.Map.Entry
import kotlin.math.ceil

/**
 * An implementation of least-recently-used cache using [LinkedHashMap]s.
 *
 * @param <K> The key of the cache.
 * @param <V> The value to cache.
 */
class LRUCache<K, V>(
    private val maxSize: Int
) : LinkedHashMap<K, V>(ceil(maxSize / 0.75f).toInt(), 0.75f, true) {
    override fun removeEldestEntry(eldest: Entry<K, V>) = size > maxSize
}