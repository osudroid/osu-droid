package com.rian.osu.mods

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.mods.settings.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import org.json.JSONObject

/**
 * Represents the Hidden mod.
 */
class ModHidden : ModWithVisibilityAdjustment() {
    override val name = "Hidden"
    override val acronym = "HD"
    override val description = "Play with no approach circles and fading circles/sliders."
    override val type = ModType.DifficultyIncrease

    override val isRanked
        get() = usesDefaultSettings

    override val scoreMultiplier: Float
        get() = if (usesDefaultSettings) 1.06f else 1f

    override val incompatibleMods = super.incompatibleMods + arrayOf(ModApproachDifferent::class, ModTraceable::class)

    /**
     * Whether to only fade approach circles.
     *
     * The main object body will not fade when enabled.
     */
    @get:JvmName("isOnlyFadeApproachCircles")
    var onlyFadeApproachCircles by BooleanModSetting(
        name = "Only fade approach circles",
        defaultValue = false
    )

    override fun isFirstAdjustableObject(hitObject: HitObject) = hitObject !is Spinner

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        onlyFadeApproachCircles = settings.optBoolean("onlyFadeApproachCircles", onlyFadeApproachCircles)
    }

    override fun serializeSettings(): JSONObject? {
        if (usesDefaultSettings) {
            return null
        }

        return JSONObject().apply {
            put("onlyFadeApproachCircles", onlyFadeApproachCircles)
        }
    }

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        super.applyToBeatmap(beatmap, scope)

        fun applyFadeInAdjustment(hitObject: HitObject) {
            scope?.ensureActive()

            hitObject.timeFadeIn = hitObject.timePreempt * FADE_IN_DURATION_MULTIPLIER

            if (hitObject is Slider) {
                hitObject.nestedHitObjects.forEach { applyFadeInAdjustment(it) }
            }
        }

        beatmap.hitObjects.objects.forEach { applyFadeInAdjustment(it) }
    }

    override fun deepCopy() = ModHidden().also {
        it.onlyFadeApproachCircles = onlyFadeApproachCircles
    }

    companion object {
        const val FADE_IN_DURATION_MULTIPLIER = 0.4
        const val FADE_OUT_DURATION_MULTIPLIER = 0.3
    }
}