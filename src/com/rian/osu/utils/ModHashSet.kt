package com.rian.osu.utils

import com.rian.osu.mods.Mod

/**
 * A [HashSet] of [Mod]s that can be compared against each other.
 */
class ModHashSet : HashSet<Mod> {
    constructor() : super()
    constructor(mods: Collection<Mod>) : super(mods)
    constructor(mods: Iterable<Mod>) : super() { addAll(mods) }
    constructor(initialCapacity: Int) : super(initialCapacity)
    constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModHashSet) {
            return false
        }

        return size == other.size && containsAll(other)
    }

    override fun hashCode(): Int {
        var result = 0

        for (mod in this) {
            result = 31 * result + mod.hashCode()
        }

        return result
    }
}