package com.rian.osu.utils

import com.rian.osu.mods.*

/**
 * A [HashSet] of [Mod]s that has additional utilities specifically for [Mod]s.
 */
class ModHashSet : HashSet<Mod> {
    constructor() : super()
    constructor(mods: Collection<Mod>) : super(mods)
    constructor(mods: Iterable<Mod>) : super() { addAll(mods) }
    constructor(initialCapacity: Int) : super(initialCapacity)
    constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)

    override fun add(element: Mod): Boolean {
        // If all difficulty statistics are set, all other difficulty adjusting mods are irrelevant, so we remove them.
        // This prevents potential abuse cases where score multipliers from non-affecting mods stack (i.e., forcing
        // all difficulty statistics while using the Hard Rock mod).
        val removeDifficultyAdjustmentMods =
            element is ModDifficultyAdjust &&
            element.cs != null &&
            element.ar != null &&
            element.od != null &&
            element.hp != null

        for (m in this) {
            // Ensure the mod itself is not a duplicate.
            if (m::class == element::class) {
                remove(m)
                continue
            }

            if (removeDifficultyAdjustmentMods && (m is IModApplicableToDifficulty || m is IModApplicableToDifficultyWithSettings)) {
                remove(m)
                continue
            }

            // Check if there is any mod that is incompatible with the new mod.
            if (element.incompatibleMods.any { it.isInstance(m) }) {
                remove(m)
            }
        }

        return super.add(element)
    }

    /**
     * Converts this [ModHashSet] to a [String] that can be displayed to the player.
     */
    fun toReadable(): String {
        if (isEmpty())
            return "None"

        return buildString {
            for (m in this@ModHashSet) when (m) {
                is ModFlashlight -> {
                    if (m.followDelay == ModFlashlight.DEFAULT_FOLLOW_DELAY)
                        append("${m.acronym}, ")
                    else
                        append("${m.acronym} ${(m.followDelay * 1000).toInt()}ms, ")
                }

                is IModUserSelectable -> append("${m.acronym}, ")

                is ModDifficultyAdjust -> {
                    if (m.ar != null) {
                        append("AR ${m.ar}, ")
                    }

                    if (m.od != null) {
                        append("OD ${m.od}, ")
                    }

                    if (m.cs != null) {
                        append("CS ${m.cs}, ")
                    }

                    if (m.hp != null) {
                        append("HP ${m.hp}, ")
                    }
                }

                is ModCustomSpeed -> append("${m.trackRateMultiplier}x, ")

                else -> Unit
            }
        }.substringBeforeLast('/')
    }

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