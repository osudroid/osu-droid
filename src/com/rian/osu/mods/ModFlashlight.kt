package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlin.math.round
import org.json.JSONObject

/**
 * Represents the Flashlight mod.
 */
class ModFlashlight : Mod() {
    override val name = "Flashlight"
    override val acronym = "FL"
    override val description = "Restricted view area."
    override val type = ModType.DifficultyIncrease

    override val isRanked
        get() = followDelay == DEFAULT_FOLLOW_DELAY

    /**
     * The amount of seconds until the flashlight reaches the cursor.
     */
    var followDelay by FloatModSetting(
        name = "Flashlight follow delay",
        valueFormatter = { "${round(it * 1000).toInt()}ms" },
        defaultValue = DEFAULT_FOLLOW_DELAY,
        minValue = DEFAULT_FOLLOW_DELAY,
        maxValue = DEFAULT_FOLLOW_DELAY * 10,
        step = DEFAULT_FOLLOW_DELAY,
        precision = 2
    )

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.12f

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        followDelay = settings.optDouble("areaFollowDelay", followDelay.toDouble()).toFloat()
    }

    override fun serializeSettings() = JSONObject().apply {
        put("areaFollowDelay", followDelay)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModFlashlight) {
            return false
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()

        result = 31 * result + followDelay.hashCode()

        return result
    }

    override fun toString(): String {
        if (followDelay == DEFAULT_FOLLOW_DELAY) {
            return super.toString()
        }

        return "${super.toString()} (%.2fs)".format(followDelay)
    }

    override fun deepCopy() = ModFlashlight().also { it.followDelay = followDelay }

    companion object {
        /**
         * The default amount of seconds until the flashlight reaches the cursor.
         */
        const val DEFAULT_FOLLOW_DELAY = 0.12f
    }
}