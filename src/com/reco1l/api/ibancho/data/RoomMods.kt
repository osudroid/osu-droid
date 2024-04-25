package com.reco1l.api.ibancho.data

import com.reco1l.osu.conversion.modsToReadable
import com.reco1l.osu.Multiplayer
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.*
import java.util.*

data class RoomMods(

    var set: EnumSet<GameMod>,

    var speedMultiplier: Float,

    var flFollowDelay: Float,

    var customAR: Float?,

    var customOD: Float?,

    var customCS: Float?,

    var customHP: Float?
)
{

    override fun toString() = modsToReadable(set, speedMultiplier, flFollowDelay, customAR, customOD, customCS, customHP)

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

            if (MOD_DOUBLETIME in set || MOD_NIGHTCORE in set)
                append("DT, ")

            if (MOD_HALFTIME in set)
                append("HT, ")

            if (speedMultiplier != 1f)
                append("%.2fx, ".format(speedMultiplier))

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

        val sameMods = Multiplayer.room?.gameplaySettings?.isFreeMod == true

                // DoubleTime and NightCore, comparing them as one.
                && (MOD_DOUBLETIME in set || MOD_NIGHTCORE in set) == (MOD_DOUBLETIME in other.set || MOD_NIGHTCORE in other.set)
                // HalfTime
                && (MOD_HALFTIME in set == MOD_HALFTIME in other.set)
                // ScoreV2
                && (MOD_SCOREV2 in set == MOD_SCOREV2 in other.set)

                || set == other.set

        return sameMods
                && speedMultiplier == other.speedMultiplier
                && flFollowDelay == other.flFollowDelay
                && customAR == other.customAR
                && customOD == other.customOD
                && customCS == other.customCS
                && customHP == other.customHP
    }

    // Auto-generated this will help to check instance equality aka ===
    override fun hashCode(): Int
    {
        var result = set.hashCode()
        result = 31 * result + speedMultiplier.hashCode()
        result = 31 * result + flFollowDelay.hashCode()
        result = 31 * result + (customAR?.hashCode() ?: 0)
        result = 31 * result + (customOD?.hashCode() ?: 0)
        result = 31 * result + (customCS?.hashCode() ?: 0)
        result = 31 * result + (customHP?.hashCode() ?: 0)
        return result
    }
}