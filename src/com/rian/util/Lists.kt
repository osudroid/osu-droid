package com.rian.util

/**
 * Adds the given item to this [MutableList] according to its [Comparable] implementation.
 *
 * **Do not use on unsorted [MutableList]s**.
 *
 * @param item The item to add.
 * @return The index at which [item] was added.
 */
fun <T : Comparable<T>> MutableList<T>.addInPlace(item: T): Int {
    var index = binarySearch(item)

    if (index < 0) {
        index = -index - 1
    }

    add(index, item)

    return index
}