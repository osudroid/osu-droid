package com.rian.osu.difficulty.evaluators

import com.rian.osu.difficulty.DifficultyHitObject
import kotlin.math.abs
import kotlin.math.max

internal class Island(epsilon: Double) {
    private val deltaDifferenceEpsilon = epsilon

    var delta = Int.MAX_VALUE
        private set(value) {
            if (field == Int.MAX_VALUE) {
                field = max(value, DifficultyHitObject.MIN_DELTA_TIME)
            }

            ++deltaCount
        }

    var deltaCount = 0
        private set

    constructor(delta: Int, deltaDifferenceEpsilon: Double) : this(deltaDifferenceEpsilon) {
        this.delta = max(delta, DifficultyHitObject.MIN_DELTA_TIME)
    }

    fun addDelta(delta: Int) {
        this.delta = delta
    }

    fun isSimilarPolarity(other: Island) =
    // TODO: consider islands to be of similar polarity only if they're having the same average delta (we don't want to consider 3 singletaps similar to a triple)
        // naively adding delta check here breaks _a lot_ of maps because of the flawed ratio calculation
        deltaCount % 2 == other.deltaCount % 2

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is Island) {
            return false
        }

        return abs(delta - other.delta) < deltaDifferenceEpsilon && deltaCount == other.deltaCount
    }

    override fun hashCode() = super.hashCode()
}
