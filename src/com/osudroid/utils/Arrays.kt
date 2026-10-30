package com.osudroid.utils

/**
 * Finds the first index whose value is at or after [value], assuming this [Array] is sorted in ascending order.
 *
 * @param value The value to search for.
 * @return The first index whose value is at or after [value], or [Array.size] if there is none.
 */
fun <T : Comparable<T>> Array<out T>.firstIndexAtOrAfter(value: T): Int =
    partitionPoint(size) { this[it] < value }

/**
 * Finds the last index whose value is at or before [value], assuming this [Array] is sorted in ascending order.
 *
 * @param value The value to search for.
 * @return The last index whose value is at or before [value], or -1 if there is none.
 */
fun <T : Comparable<T>> Array<out T>.lastIndexAtOrBefore(value: T): Int =
    partitionPoint(size) { this[it] <= value } - 1

/**
 * Finds the first index whose value is at or after [value], assuming this [IntArray] is sorted in ascending order.
 *
 * This overload does not box the elements, so it is suitable for hot paths.
 *
 * @param value The value to search for.
 * @return The first index whose value is at or after [value], or [IntArray.size] if there is none.
 */
fun IntArray.firstIndexAtOrAfter(value: Int): Int =
    partitionPoint(size) { this[it] < value }

/**
 * Finds the last index whose value is at or before [value], assuming this [IntArray] is sorted in ascending order.
 *
 * This overload does not box the elements, so it is suitable for hot paths.
 *
 * @param value The value to search for.
 * @return The last index whose value is at or before [value], or -1 if there is none.
 */
fun IntArray.lastIndexAtOrBefore(value: Int): Int =
    partitionPoint(size) { this[it] <= value } - 1

/**
 * Binary searches the first index in `[0, size)` for which [isBefore] is false.
 *
 * [isBefore] must be true for a prefix of the indices and false for the rest. The lambda is inlined, so it does not
 * allocate.
 *
 * @param size The number of indices to search.
 * @param isBefore Whether the element at the given index lies before the partition point.
 * @return The first index for which [isBefore] is false, or [size] if there is none.
 */
private inline fun partitionPoint(size: Int, isBefore: (index: Int) -> Boolean): Int {
    var low = 0
    var high = size

    while (low < high) {
        val mid = low + high ushr 1

        if (isBefore(mid)) {
            low = mid + 1
        } else {
            high = mid
        }
    }

    return low
}
