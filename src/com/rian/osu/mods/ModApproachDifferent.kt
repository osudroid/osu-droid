package com.rian.osu.mods

import com.edlplan.framework.easing.Easing
import com.reco1l.toolkt.roundBy
import org.json.JSONObject

/**
 * Represents the Approach Different mod.
 */
class ModApproachDifferent : Mod() {
    override val name = "Approach Different"
    override val acronym = "AD"
    override val description = "Never trust the approach circles..."
    override val type = ModType.Fun

    override val incompatibleMods = super.incompatibleMods + arrayOf(ModHidden::class, ModFreezeFrame::class)

    /**
     * The initial size of the approach circle, relative to hit circles.
     */
    var scale by FloatModSetting(
        name = "Initial size",
        valueFormatter = { it.roundBy(1).toString() },
        defaultValue = 3f,
        minValue = 1.5f,
        maxValue = 10f,
        step = 0.1f,
        precision = 1
    )

    /**
     * The animation style of the approach circles.
     */
    // TODO: change to dropdown input
    var style = AnimationStyle.Gravity

    /**
     * The [Easing] to apply to the approach circle animation.
     */
    val easing
        get() = when (style) {
            AnimationStyle.Linear -> Easing.None
            AnimationStyle.Gravity -> Easing.InBack
            AnimationStyle.InOut1 -> Easing.InOutCubic
            AnimationStyle.InOut2 -> Easing.InOutQuint
            AnimationStyle.Accelerate1 -> Easing.In
            AnimationStyle.Accelerate2 -> Easing.InCubic
            AnimationStyle.Accelerate3 -> Easing.InQuint
            AnimationStyle.Decelerate1 -> Easing.Out
            AnimationStyle.Decelerate2 -> Easing.OutCubic
            AnimationStyle.Decelerate3 -> Easing.OutQuint
        }

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        scale = settings.optDouble("scale", scale.toDouble()).toFloat()
        style = AnimationStyle.entries.getOrNull(settings.optInt("style", style.ordinal)) ?: style
    }

    override fun serializeSettings() = JSONObject().apply {
        put("scale", scale)
        put("style", style.ordinal)
    }

    override fun deepCopy() = ModApproachDifferent()

    enum class AnimationStyle {
        Linear,
        Gravity,
        InOut1,
        InOut2,
        Accelerate1,
        Accelerate2,
        Accelerate3,
        Decelerate1,
        Decelerate2,
        Decelerate3,
    }
}