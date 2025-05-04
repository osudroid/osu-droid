package com.rian.osu.math

import kotlin.math.abs

/**
 * A pseudo-random number generator that shares the same implementation with
 * [.NET 5's `System.Random`](https://github.com/dotnet/runtime/blob/v9.0.4/src/libraries/System.Private.CoreLib/src/System/Random.Net5CompatImpl.cs).
 *
 * Used in the Random mod to ensure that a seed generates the result that users expect.
 */
class Random(seed: Int) {
    private var seedArray = IntArray(56)
    private var iNext = 0
    private var iNextP = 21

    init {
        val subtraction = if (seed == Int.MIN_VALUE) Int.MAX_VALUE else abs(seed)
        // Magic number based on Phi (golden ratio).
        var mj = 161803398 - subtraction
        seedArray[55] = mj
        var mk = 1

        var ii = 0
        for (i in 1 until 55) {
            // The range [1..55] is special (Knuth) and so we're wasting the 0'th position.
            ii = (21 * i) % 55
            seedArray[ii] = mk
            mk = mj - mk

            if (mk < 0) {
                mk += Int.MAX_VALUE
            }

            mj = seedArray[ii]
        }

        (1 until 5).forEach { k ->
            for (i in 1 until 56) {
                val n = (i + 30) % 55

                seedArray[i] -= seedArray[n + 1]

                if (seedArray[i] < 0) {
                    seedArray[i] += Int.MAX_VALUE
                }
            }
        }
    }

    fun nextDouble() = sample()

    private fun sample() = internalSample().toDouble() / Int.MAX_VALUE

    private fun internalSample(): Int {
        var locINext = iNext
        if (++locINext >= 56) {
            locINext = 1
        }

        var locINextP = iNextP
        if (++locINextP >= 56) {
            locINextP = 1
        }

        val seedArray = seedArray
        var retVal = seedArray[locINext] - seedArray[locINextP]

        if (retVal == Int.MAX_VALUE) {
            --retVal
        }
        if (retVal < 0) {
            retVal += Int.MAX_VALUE
        }

        seedArray[locINext] = retVal
        iNext = locINext
        iNextP = locINextP

        return retVal
    }

    companion object {
        // Delegates to Kotlin's Random class.
        @JvmStatic
        fun nextInt() = kotlin.random.Random.nextInt()
    }
}