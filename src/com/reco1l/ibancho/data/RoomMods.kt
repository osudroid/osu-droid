package com.reco1l.ibancho.data

import com.reco1l.osu.multiplayer.Multiplayer
import com.reco1l.osu.multiplayer.modsToReadable
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.*
import java.util.*

data class RoomMods(

    /**
     * The mods set.
     */
    var set: EnumSet<GameMod>,

    /**
     * The speed multiplier.
     */
    var speedMultiplier: Float,

    /**
     * The flashlight follow delay time in seconds.
     */
    var flFollowDelay: Float,

    /**
     * The custom approach rate.
     */
    var customAR: Float?,

    /**
     * The custom overall difficulty.
     */
    var customOD: Float?,

    /**
     * The custom circle size.
     */
    var customCS: Float?,

    /**
     * The custom health points.
     */
    var customHP: Float?

) {


    override fun toString(): String {
        return modsToReadable(set, speedMultiplier, flFollowDelay, customAR, customOD, customCS, customHP)
    }

    fun toString(room: Room): String {

        if (set.isEmpty()) {
            return buildString {

                if (room.gameplaySettings.isFreeMod) {
                    append("Free mods")

                    if (room.gameplaySettings.allowForceDifficultyStatistics) {
                        append(", Force diffstat")
                    }
                } else append("None")

            }
        }

        return if (room.gameplaySettings.isFreeMod) buildString {

            append("Free mods, ")

            if (MOD_DOUBLETIME in set || MOD_NIGHTCORE in set) {
                append("DT, ")
            }

            if (MOD_HALFTIME in set) {
                append("HT, ")
            }

            if (speedMultiplier != 1f) {
                append("%.2fx, ".format(speedMultiplier))
            }

            if (room.gameplaySettings.allowForceDifficultyStatistics) {
                append("Force diffstat, ")
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

        val sameMods = Multiplayer.room?.gameplaySettings?.isFreeMod == true

            // DoubleTime and NightCore, comparing them as one.
            && (MOD_DOUBLETIME in set || MOD_NIGHTCORE in set) == (MOD_DOUBLETIME in other.set || MOD_NIGHTCORE in other.set)
            // HalfTime
            && MOD_HALFTIME in set == MOD_HALFTIME in other.set
            // ScoreV2
            && MOD_SCOREV2 in set == MOD_SCOREV2 in other.set

            || set == other.set

        return sameMods
            && speedMultiplier == other.speedMultiplier
            && flFollowDelay == other.flFollowDelay
            && customAR == other.customAR
            && customOD == other.customOD
            && customCS == other.customCS
            && customHP == other.customHP
    }

    override fun hashCode(): Int {
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