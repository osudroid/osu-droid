package com.rian.osu.utils

import com.rian.osu.mods.ILegacyMod
import com.rian.osu.mods.IModUserSelectable
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModCustomSpeed
import com.rian.osu.mods.ModDifficultyAdjust
import com.rian.osu.mods.ModFlashlight

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

    override fun toString() = buildString {
        var difficultyAdjust: ModDifficultyAdjust? = null
        var customSpeed: ModCustomSpeed? = null
        var flashlight: ModFlashlight? = null

        for (m in this@ModHashSet) when (m) {
            is ModDifficultyAdjust -> difficultyAdjust = m
            is ModCustomSpeed -> customSpeed = m
            is ILegacyMod -> continue

            is IModUserSelectable -> {
                if (m is ModFlashlight) {
                    flashlight = m
                }

                append(m.droidChar)
            }
        }

        append('|')

        if (difficultyAdjust == null && customSpeed == null && flashlight == null) {
            // Append another pipe character so that the substringBeforeLast call below takes that pipe into account.
            // This is weird, but it is the behavior of the original implementation.
            append('|')
            return@buildString
        }

        // Convert container mods
        if (customSpeed != null) {
            append(String.format("x%.2f|", customSpeed.trackRateMultiplier))
        }

        difficultyAdjust?.let {
            if (customSpeed != null) {
                append('|')
            }

            if (it.ar != null) {
                append(String.format("AR%.1f|", it.ar))
            }

            if (it.od != null) {
                append(String.format("OD%.1f|", it.od))
            }

            if (it.cs != null) {
                append(String.format("CS%.1f|", it.cs))
            }

            if (it.hp != null) {
                append(String.format("HP%.1f|", it.hp))
            }
        }

        if (flashlight != null && flashlight.followDelay != ModFlashlight.DEFAULT_FOLLOW_DELAY) {
            append(String.format("FLD%.2f|", flashlight.followDelay))
        }
    }.substringBeforeLast('|')
}