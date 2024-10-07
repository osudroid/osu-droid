package com.reco1l.ibancho.data

import com.reco1l.osu.multiplayer.Multiplayer
import com.rian.osu.mods.ModCustomSpeed
import com.rian.osu.mods.ModDifficultyAdjust
import com.rian.osu.mods.ModDoubleTime
import com.rian.osu.mods.ModHalfTime
import com.rian.osu.mods.ModNightCore
import com.rian.osu.utils.ModHashSet
import com.rian.osu.utils.ModUtils

data class RoomMods(@JvmField val set: ModHashSet)
{
    constructor(modString: String) : this(ModUtils.convertModString(modString))

    override fun toString() = set.toReadable()

    fun toString(room: Room): String
    {
        if (set.isEmpty())
            return buildString {

                if (room.gameplaySettings.isFreeMod) {
                    append("Free mods")

                    if (room.gameplaySettings.allowForceDifficultyStatistics) {
                        append(", Force diffstat")
                    }
                } else append("None")

            }

        return if (room.gameplaySettings.isFreeMod) buildString {

            append("Free mods, ")

            val doubleTime = ModDoubleTime()
            val halfTime = ModHalfTime()

            if (doubleTime in set || ModNightCore() in set)
                append("${doubleTime.acronym}, ")

            if (halfTime in set)
                append("${halfTime.acronym}, ")

            val customSpeed = set.find { it is ModCustomSpeed } as? ModCustomSpeed

            if (customSpeed != null)
                append("%.2fx, ".format(customSpeed.trackRateMultiplier))

            if (room.gameplaySettings.allowForceDifficultyStatistics)
                append("Force diffstat, ")

        }.substringBeforeLast(',') else toString()
    }

    override fun equals(other: Any?): Boolean
    {
        if (other === this)
            return true

        if (other !is RoomMods)
            return false

        val gameplaySettings = Multiplayer.room?.gameplaySettings

        if (gameplaySettings?.isFreeMod == true) {
            // Under free mods, force difficulty statistics is still not allowed unless the setting is explicitly set.
            if (!gameplaySettings.allowForceDifficultyStatistics && set.firstOrNull { it is ModDifficultyAdjust } != other.set.firstOrNull { it is ModDifficultyAdjust })
                return false

            val requiredMods = set.filter { !it.isValidForMultiplayerAsFreeMod }
            val otherRequiredMods = other.set.filter { !it.isValidForMultiplayerAsFreeMod }

            return requiredMods.size == otherRequiredMods.size && requiredMods.containsAll(otherRequiredMods)
        }

        return set == other.set
    }

    // Auto-generated this will help to check instance equality aka ===
    override fun hashCode() = set.hashCode()
}