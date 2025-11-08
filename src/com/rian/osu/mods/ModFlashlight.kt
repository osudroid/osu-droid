package com.rian.osu.mods

import com.reco1l.toolkt.roundBy
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
        get() = usesDefaultSettings

    override val scoreMultiplier
        get() = if (usesDefaultSettings) 1.12f else 1f

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
        precision = 2,
        orderPosition = 0
    )

    /**
     * The multiplier applied to the default Flashlight size.
     */
    var sizeMultiplier by FloatModSetting(
        name = "Flashlight size",
        valueFormatter = { "${it.roundBy(1)}x" },
        defaultValue = DEFAULT_SIZE_MULTIPLIER,
        minValue = 0.5f,
        maxValue = 2f,
        step = 0.1f,
        precision = 1,
        orderPosition = 1
    )

    /**
     * Whether to decrease the Flashlight size as combo increases.
     */
    @get:JvmName("isComboBasedSize")
    var comboBasedSize by BooleanModSetting(
        name = "Change size based on combo",
        defaultValue = true,
        orderPosition = 2
    )

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        followDelay = settings.optDouble("areaFollowDelay", followDelay.toDouble()).toFloat()
        sizeMultiplier = settings.optDouble("sizeMultiplier", sizeMultiplier.toDouble()).toFloat()
        comboBasedSize = settings.optBoolean("comboBasedSize", comboBasedSize)
    }

    override fun serializeSettings() = JSONObject().apply {
        put("areaFollowDelay", followDelay)
        put("sizeMultiplier", sizeMultiplier)
        put("comboBasedSize", comboBasedSize)
    }

    override val extraInformation
        get() = if (usesDefaultSettings) super.extraInformation else buildString {
            if (followDelay != DEFAULT_FOLLOW_DELAY) {
                append("${(followDelay * 1000).roundBy(0).toInt()}ms, ")
            }

            if (sizeMultiplier != DEFAULT_SIZE_MULTIPLIER) {
                append("${sizeMultiplier.roundBy(1)}x, ")
            }
        }.substringBeforeLast(", ")

    override fun deepCopy() = ModFlashlight().also {
        it.followDelay = followDelay
        it.sizeMultiplier = sizeMultiplier
        it.comboBasedSize = comboBasedSize
    }

    companion object {
        /**
         * The default amount of seconds until the flashlight reaches the cursor.
         */
        const val DEFAULT_FOLLOW_DELAY = 0.12f

        /**
         * The default multiplier applied to the default Flashlight size.
         */
        const val DEFAULT_SIZE_MULTIPLIER = 1f
    }
}