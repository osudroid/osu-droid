package com.rian.osu.mods

import com.rian.osu.mods.settings.*
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

    override val scoreMultiplier = 1.12f

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

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        followDelay = settings.optDouble("areaFollowDelay", followDelay.toDouble()).toFloat()
    }

    override fun serializeSettings() = JSONObject().apply {
        put("areaFollowDelay", followDelay)
    }

    override val extraInformation
        get() = if (followDelay == DEFAULT_FOLLOW_DELAY) super.extraInformation else "%.2fs".format(followDelay)

    override fun deepCopy() = ModFlashlight().also { it.followDelay = followDelay }

    companion object {
        /**
         * The default amount of seconds until the flashlight reaches the cursor.
         */
        const val DEFAULT_FOLLOW_DELAY = 0.12f
    }
}