package com.rian.osu.difficulty.evaluators

import com.rian.osu.difficulty.DifficultyHitObject
import kotlin.math.abs
import kotlin.math.max

internal class Island(epsilon: Double) {
    private val deltaDifferenceEpsilon = epsilon

    var delta = Int.MAX_VALUE
        private set

    var deltaCount = 0
        private set

    constructor(delta: Int, deltaDifferenceEpsilon: Double) : this(deltaDifferenceEpsilon) {
        this.addDelta(delta)
    }

    fun addDelta(delta: Int) {
        if (this.delta == Int.MAX_VALUE) {
            this.delta = max(delta, DifficultyHitObject.MIN_DELTA_TIME)
        }

        ++deltaCount
    }

    fun isSimilarPolarity(other: Island): Boolean {
        // Single delta islands should not be compared.
        if (deltaCount == 1 || other.deltaCount == 1) {
            return false
        }

        return abs(delta - other.delta) < deltaDifferenceEpsilon && deltaCount % 2 == other.deltaCount % 2
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is Island) {
            return false
        }

        return abs(delta - other.delta) < deltaDifferenceEpsilon && deltaCount == other.deltaCount
    }

    override fun hashCode(): Int {
        var result = delta
        result = 31 * result + deltaCount

        return result
    }
}
