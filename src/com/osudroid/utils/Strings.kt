@file:JvmName("StringUtils")
package com.osudroid.utils

/**
 * Performs a search of [needle] in this [String] considering cases where other characters exist between consecutive
 * characters in [needle].
 *
 * For example, when searching for `Synesthesia`, `sy h` will match provided that cases are ignored.
 *
 * @param needle The substring to search for.
 * @param ignoreCase Whether to ignore case when searching. Defaults to `false`.
 * @returns `true` if [needle] is found in this [String] contiguously, `false` otherwise.
 */
@JvmOverloads
fun String.searchContiguously(needle: String, ignoreCase: Boolean = false): Boolean {
    val needleIsBlank = needle.isBlank()

    // If the haystack is blank, only a blank needle should match.
    // If the needle is blank, it should match everything.
    if (isBlank() || needleIsBlank) {
        return needleIsBlank
    }

    val terms = needle.split(' ').filter { it.isNotBlank() }
    var index = 0

    for (term in terms) {
        val found = indexOf(term, index, ignoreCase)

        if (found == -1) {
            return false
        }

        index = found + 1
    }

    return true
}