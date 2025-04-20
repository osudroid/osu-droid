package com.osudroid.multiplayer.api.data

import com.osudroid.multiplayer.Multiplayer
import com.rian.osu.mods.ModCustomSpeed
import com.rian.osu.mods.ModDifficultyAdjust
import com.rian.osu.mods.ModDoubleTime
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModHalfTime
import com.rian.osu.mods.ModNightCore
import com.rian.osu.utils.ModHashMap
import com.rian.osu.utils.ModUtils
import org.json.JSONArray
import kotlin.collections.iterator

class RoomMods @JvmOverloads constructor(json: JSONArray? = null) : ModHashMap(ModUtils.deserializeMods(json)) {

    /**
     * Converts this [RoomMods] to a [String] that can be displayed to the player.
     */
    fun toReadable(): String {
        if (isEmpty())
            return "None"

        return buildString {
            val difficultyAdjust = ofType<ModDifficultyAdjust>()
            val customSpeed = ofType<ModCustomSpeed>()

            for ((_, m) in this@RoomMods) when (m) {
                is ModFlashlight -> {
                    if (m.followDelay == ModFlashlight.DEFAULT_FOLLOW_DELAY)
                        append("${m.acronym}, ")
                    else
                        append("${m.acronym} ${(m.followDelay * 1000).toInt()}ms, ")
                }

                else -> append("${m.acronym}, ")
            }

            if (customSpeed != null) {
                append("%.2fx, ".format(customSpeed.trackRateMultiplier))
            }

            if (difficultyAdjust != null) {
                if (difficultyAdjust.ar != null) {
                    append("AR%.1f, ".format(difficultyAdjust.ar))
                }

                if (difficultyAdjust.od != null) {
                    append("OD%.1f, ".format(difficultyAdjust.od))
                }

                if (difficultyAdjust.cs != null) {
                    append("CS%.1f, ".format(difficultyAdjust.cs))
                }

                if (difficultyAdjust.hp != null) {
                    append("HP%.1f, ".format(difficultyAdjust.hp))
                }
            }
        }.substringBeforeLast(',')
    }

    fun toString(room: Room): String {
        if (isEmpty()) {
            return if (room.gameplaySettings.isFreeMod) "Free mods" else "None"
        }

        return if (room.gameplaySettings.isFreeMod) buildString {

            append("Free mods, ")

            val doubleTime = ofType<ModDoubleTime>()
            val nightCore = ofType<ModNightCore>()
            val halfTime = ofType<ModHalfTime>()
            val customSpeed = ofType<ModCustomSpeed>()

            if (doubleTime != null) {
                append("${doubleTime.acronym}, ")
            }

            if (nightCore != null) {
                append("${nightCore.acronym}, ")
            }

            if (halfTime != null) {
                append("${halfTime.acronym}, ")
            }

            if (customSpeed != null) {
                append("%.2fx, ".format(customSpeed.trackRateMultiplier))
            }

        }.substringBeforeLast(',') else toString()
    }


    override fun equals(other: Any?): Boolean {

        if (other === this) {
            return true
        }

        if (other !is RoomMods) {
            return false
        }

        val gameplaySettings = Multiplayer.room?.gameplaySettings

        if (gameplaySettings?.isFreeMod == true) {
            val requiredMods = filter { !it.value.isValidForMultiplayerAsFreeMod }
            val otherRequiredMods = other.filter { !it.value.isValidForMultiplayerAsFreeMod }

            return requiredMods.size == otherRequiredMods.size && requiredMods.values.containsAll(otherRequiredMods.values)
        }

        return super.equals(other)
    }

    // Auto-generated this will help to check instance equality aka ===
    override fun hashCode() = super.hashCode()
}