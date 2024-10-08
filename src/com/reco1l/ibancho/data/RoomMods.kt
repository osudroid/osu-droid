package com.reco1l.ibancho.data

import com.reco1l.osu.multiplayer.Multiplayer
import com.rian.osu.mods.ModCustomSpeed
import com.rian.osu.mods.ModDifficultyAdjust
import com.rian.osu.mods.ModDoubleTime
import com.rian.osu.mods.ModHalfTime
import com.rian.osu.mods.ModNightCore
import com.rian.osu.utils.ModHashMap
import com.rian.osu.utils.ModUtils

data class RoomMods(@JvmField val map: ModHashMap)
{
    constructor(modString: String) : this(ModUtils.convertModString(modString))

    override fun toString() = map.toReadable()

    fun toString(room: Room): String
    {
        if (map.isEmpty())
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

            val doubleTime = map.ofType<ModDoubleTime>()
            val nightCore = map.ofType<ModNightCore>()
            val halfTime = map.ofType<ModHalfTime>()
            val customSpeed = map.ofType<ModCustomSpeed>()

            if (doubleTime != null)
                append("${doubleTime.acronym}, ")

            if (nightCore != null)
                append("${nightCore.acronym}, ")

            if (halfTime != null)
                append("${halfTime.acronym}, ")

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
            if (!gameplaySettings.allowForceDifficultyStatistics &&
                map.ofType<ModDifficultyAdjust>() != other.map.ofType<ModDifficultyAdjust>()) {
                return false
            }

            val requiredMods = map.filter { !it.value.isValidForMultiplayerAsFreeMod }
            val otherRequiredMods = other.map.filter { !it.value.isValidForMultiplayerAsFreeMod }

            return requiredMods.size == otherRequiredMods.size && requiredMods.values.containsAll(otherRequiredMods.values)
        }

        return map == other.map
    }

    // Auto-generated this will help to check instance equality aka ===
    override fun hashCode() = map.hashCode()
}