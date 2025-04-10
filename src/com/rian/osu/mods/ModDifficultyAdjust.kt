package com.rian.osu.mods

import com.reco1l.toolkt.*
import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.ModUtils
import kotlin.math.exp
import kotlin.math.pow
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible
import org.json.JSONObject

/**
 * Represents the Difficulty Adjust mod. Serves as a container for forced difficulty statistics.
 */
class ModDifficultyAdjust @JvmOverloads constructor(
    cs: Float? = null,
    ar: Float? = null,
    od: Float? = null,
    hp: Float? = null
) : Mod(), IModApplicableToDifficultyWithSettings, IModApplicableToHitObjectWithSettings, IModRequiresOriginalBeatmap {

    /**
     * The circle size to enforce.
     */
    var cs by NullableFloatModSetting(
        name = "Circle size",
        valueFormatter = { it!!.roundBy(1).toString() },
        defaultValue = cs,
        minValue = 0f,
        maxValue = 15f,
        step = 0.5f
    )

    /**
     * The approach rate to enforce.
     */
    var ar by NullableFloatModSetting(
        name = "Approach rate",
        valueFormatter = { it!!.roundBy(1).toString() },
        defaultValue = ar,
        minValue = 0f,
        maxValue = 12.5f,
        step = 0.5f
    )

    /**
     * The overall difficulty to enforce.
     */
    var od by NullableFloatModSetting(
        name = "Overall difficulty",
        valueFormatter = { it!!.roundBy(1).toString() },
        defaultValue = od,
        minValue = 0f,
        maxValue = 11f,
        step = 0.5f
    )

    /**
     * The health drain rate to enforce.
     */
    var hp by NullableFloatModSetting(
        name = "Health drain",
        valueFormatter = { it!!.roundBy(1).toString() },
        defaultValue = hp,
        minValue = 0f,
        maxValue = 11f,
        step = 0.5f
    )


    override val name = "Difficulty Adjust"
    override val acronym = "DA"
    override val type = ModType.Conversion
    override val textureNameSuffix = "difficultyadjust"

    override val isRelevant
        get() = cs != null || ar != null || od != null || hp != null

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty): Float {
        // Graph: https://www.desmos.com/calculator/yrggkhrkzz
        var multiplier = 1f

        if (cs != null) {
            val diff = cs!! - difficulty.difficultyCS

            multiplier *=
                if (diff >= 0) 1 + 0.0075f * diff.pow(1.5f)
                else 2 / (1 + exp(-0.5f * diff))
        }

        if (od != null) {
            val diff = od!! - difficulty.od

            multiplier *=
                if (diff >= 0) 1 + 0.005f * diff.pow(1.3f)
                else 2 / (1 + exp(-0.25f * diff))
        }

        return multiplier
    }

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        cs = settings.optDouble("cs").toFloat().takeUnless { it.isNaN() }
        ar = settings.optDouble("ar").toFloat().takeUnless { it.isNaN() }
        od = settings.optDouble("od").toFloat().takeUnless { it.isNaN() }
        hp = settings.optDouble("hp").toFloat().takeUnless { it.isNaN() }
    }

    override fun serializeSettings(): JSONObject? {
        if (!isRelevant) {
            return null
        }

        return JSONObject().apply {
            if (cs != null) {
                put("cs", cs)
            }

            if (ar != null) {
                put("ar", ar)
            }

            if (od != null) {
                put("od", od)
            }

            if (hp != null) {
                put("hp", hp)
            }
        }
    }

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty, mods: Iterable<Mod>) =
        difficulty.let {
            it.difficultyCS = getValue(cs, it.difficultyCS)
            it.gameplayCS = getValue(cs, it.gameplayCS)
            it.ar = getValue(ar, it.ar)
            it.od = getValue(od, it.od)
            it.hp = getValue(hp, it.hp)

            // Special case for force AR, where the AR value is kept constant with respect to game time.
            // This makes the player perceive the AR as is under all speed multipliers.
            if (ar != null) {
                val preempt = BeatmapDifficulty.difficultyRange(ar!!.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN)
                val trackRate = ModUtils.calculateRateWithMods(mods)

                it.ar = BeatmapDifficulty.inverseDifficultyRange(preempt * trackRate, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN).toFloat()
            }
        }

    override fun applyToHitObject(mode: GameMode, hitObject: HitObject, mods: Iterable<Mod>) {
        // Special case for force AR, where the AR value is kept constant with respect to game time.
        // This makes the player perceive the fade in animation as is under all speed multipliers.
        if (ar == null) {
            return
        }

        applyFadeAdjustment(hitObject, mods)

        if (hitObject is Slider) {
            hitObject.nestedHitObjects.forEach { applyFadeAdjustment(it, mods) }
        }
    }

    override fun applyFromBeatmap(beatmap: Beatmap) {
        val difficulty = beatmap.difficulty

        updateDefaultValue(::cs, difficulty.gameplayCS)
        updateDefaultValue(::ar, difficulty.ar)
        updateDefaultValue(::od, difficulty.od)
        updateDefaultValue(::hp, difficulty.hp)
    }

    private fun updateDefaultValue(property: KProperty0<*>, value: Float) {
        property.isAccessible = true

        val delegate = property.getDelegate() as NullableFloatModSetting
        delegate.defaultValue = value

        property.isAccessible = false
    }

    private fun applyFadeAdjustment(hitObject: HitObject, mods: Iterable<Mod>) {
        val initialTrackRate = ModUtils.calculateRateWithMods(mods)
        val currentTrackRate = ModUtils.calculateRateWithMods(mods, hitObject.startTime)

        // Cancel the rate that was initially applied to timePreempt (via applyToDifficulty above and
        // HitObject.applyDefaults) and apply the current one.
        hitObject.timePreempt *= currentTrackRate / initialTrackRate

        hitObject.timeFadeIn *= currentTrackRate
    }

    private fun getValue(value: Float?, fallback: Float) = value ?: fallback

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModDifficultyAdjust) {
            return false
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()

        result = 31 * result + cs.hashCode()
        result = 31 * result + ar.hashCode()
        result = 31 * result + od.hashCode()
        result = 31 * result + hp.hashCode()

        return result
    }

    override fun deepCopy() = ModDifficultyAdjust(cs, ar, od, hp)
}