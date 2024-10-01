package com.rian.osu.utils

import com.rian.osu.mods.*

/**
 * A set of utilities to handle [Mod] combinations.
 */
object ModUtils {
    /**
     * Calculates the rate for the track with the selected [Mod]s.
     *
     * @param mods The list of selected [Mod]s.
     * @return The rate with [Mod]s.
     */
    @JvmStatic
    fun calculateRateWithMods(mods: Iterable<Mod>) = mods.fold(1f) {
        rate, mod -> rate * (if (mod is IModApplicableToTrackRate) mod.trackRateMultiplier else 1f)
    }
}