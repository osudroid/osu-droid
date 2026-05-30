package com.osudroid.mods

import com.reco1l.toolkt.*
import com.osudroid.GameMode
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.settings.*
import com.osudroid.utils.ModUtils
import kotlin.math.exp
import kotlin.math.pow
import kotlin.reflect.KProperty0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the Difficulty Adjust mod. Serves as a container for forced difficulty statistics.
 */
class ModDifficultyAdjust @JvmOverloads constructor(
    cs: Float? = null,
    ar: Float? = null,
    od: Float? = null,
    hp: Float? = null
) : Mod(), IModApplicableToDifficultyWithMods, IModApplicableToHitObjectWithMods, IModRequiresBeatmapDifficulty {

    /**
     * The circle size to enforce.
     */
    var cs by DifficultyAdjustModSetting(
        name = "Circle size",
        key = "cs",
        valueFormatter = { (it ?: defaultValue)?.roundBy(1)?.toString() ?: "None" },
        minValue = 0f,
        maxValue = 15f,
        step = 0.1f,
        precision = 1,
        orderPosition = 0
    )

    /**
     * The approach rate to enforce.
     */
    var ar by DifficultyAdjustModSetting(
        name = "Approach rate",
        key = "ar",
        valueFormatter = { (it ?: defaultValue)?.roundBy(1)?.toString() ?: "None" },
        minValue = 0f,
        maxValue = 12.5f,
        step = 0.1f,
        precision = 1,
        orderPosition = 1
    )

    /**
     * The overall difficulty to enforce.
     */
    var od by DifficultyAdjustModSetting(
        name = "Overall difficulty",
        key = "od",
        valueFormatter = { (it ?: defaultValue)?.roundBy(1)?.toString() ?: "None" },
        minValue = 0f,
        maxValue = 11f,
        step = 0.1f,
        precision = 1,
        orderPosition = 2
    )

    /**
     * The health drain rate to enforce.
     */
    var hp by DifficultyAdjustModSetting(
        name = "Health drain",
        key = "hp",
        valueFormatter = { (it ?: defaultValue)?.roundBy(1)?.toString() ?: "None" },
        minValue = 0f,
        maxValue = 11f,
        step = 0.1f,
        precision = 1,
        orderPosition = 3
    )

    init {
        // We set the default values here so that resetting the settings would reset them to null.
        updateDefaultValue(::cs, cs)
        updateDefaultValue(::ar, ar)
        updateDefaultValue(::od, od)
        updateDefaultValue(::hp, hp)

        this.cs = cs
        this.ar = ar
        this.od = od
        this.hp = hp
    }

    override val name = "Difficulty Adjust"
    override val acronym = "DA"
    override val description = "Override a beatmap's difficulty settings."
    override val type = ModType.Conversion
    override val requiresConfiguration = true

    // This mod has a different default than others as the default value of settings change based on the beatmap.
    override val usesDefaultSettings
        get() = settings.all { it.value == it.initialValue }

    override val scoreMultiplier: Float
        get() {
            // Graph: https://www.desmos.com/calculator/yrggkhrkzz
            var multiplier = 1f
            val cs = getModSettingDelegate<DifficultyAdjustModSetting>(::cs)
            val od = getModSettingDelegate<DifficultyAdjustModSetting>(::od)

            if (cs.value != null) {
                val original = cs.originalValue ?: cs.defaultValue

                if (original != null) {
                    val diff = cs.value!! - original

                    multiplier *=
                        if (diff >= 0) 1 + 0.0075f * diff.pow(1.5f)
                        else 2 / (1 + exp(-0.5f * diff))
                }
            }

            if (od.value != null) {
                val original = od.originalValue ?: od.defaultValue

                if (original != null) {
                    val diff = od.value!! - original

                    multiplier *=
                        if (diff >= 0) 1 + 0.005f * diff.pow(1.3f)
                        else 2 / (1 + exp(-0.25f * diff))
                }
            }

            return multiplier
        }

    override fun isCompatibleWith(other: Mod): Boolean {
        if (!super.isCompatibleWith(other)) {
            return false
        }

        if (other is ModSmallCircle && cs != null) {
            return false
        }

        if (cs != null && ar != null && od != null && hp != null) {
            return other !is ModEasy && other !is ModHardRock && other !is ModReallyEasy
        }

        return true
    }

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty, mods: Iterable<Mod>) =
        difficulty.let {
            it.difficultyCS = getValue(cs, it.difficultyCS)
            it.gameplayCS = getValue(cs, it.gameplayCS)
            it.ar = getValue(ar, it.ar)
            it.od = getValue(od, it.od)
            it.hp = getValue(hp, it.hp)

            // Special case for force AR in replay version 6 and older, where the AR value is kept constant with respect
            // to game time. This makes the player perceive the AR as is under all speed multipliers.
            if (ar != null && mods.any { m -> m is ModReplayV6 }) {
                val preempt = BeatmapDifficulty.difficultyRange(ar!!.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN)
                val trackRate = ModUtils.calculateRateWithMods(mods)

                it.ar = BeatmapDifficulty.inverseDifficultyRange(preempt * trackRate, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN).toFloat()
            }
        }

    override fun applyToHitObject(mode: GameMode, hitObject: HitObject, mods: Iterable<Mod>, scope: CoroutineScope?) {
        // Special case for force AR in replay version 6 and older, where the AR value is kept constant with respect to
        // game time. This makes the player perceive the fade in animation as is under all speed multipliers.
        if (ar == null || mods.none { it is ModReplayV6 }) {
            return
        }

        applyOldFadeAdjustment(hitObject, mods)

        if (hitObject is Slider) {
            hitObject.nestedHitObjects.forEach {
                scope?.ensureActive()

                applyOldFadeAdjustment(it, mods)
            }
        }
    }

    override fun applyFromBeatmapDifficulty(difficulty: BeatmapDifficulty) {
        updateBeatmapValue(::cs, difficulty.gameplayCS)
        updateBeatmapValue(::ar, difficulty.ar)
        updateBeatmapValue(::od, difficulty.od)
        updateBeatmapValue(::hp, difficulty.hp)
    }

    private fun updateDefaultValue(property: KProperty0<Float?>, value: Float?) {
        getModSettingDelegate<DifficultyAdjustModSetting>(property).defaultValue = value
    }

    private fun updateBeatmapValue(property: KProperty0<Float?>, value: Float?) {
        val delegate = getModSettingDelegate<DifficultyAdjustModSetting>(property)

        delegate.defaultValue = value
        delegate.originalValue = value
    }

    private fun applyOldFadeAdjustment(hitObject: HitObject, mods: Iterable<Mod>) {
        val initialTrackRate = ModUtils.calculateRateWithMods(mods)
        val currentTrackRate = ModUtils.calculateRateWithMods(mods, hitObject.startTime)

        // Cancel the rate that was initially applied to timePreempt (via applyToDifficulty above and
        // HitObject.applyDefaults) and apply the current one.
        hitObject.timePreempt *= currentTrackRate / initialTrackRate

        hitObject.timeFadeIn *= currentTrackRate
    }

    private fun getValue(value: Float?, fallback: Float) = value ?: fallback

    override val extraInformation: String
        get() {
            val settings = mutableListOf<String>()

            if (cs != null) {
                settings += "CS%.1f".format(cs)
            }

            if (ar != null) {
                settings += "AR%.1f".format(ar)
            }

            if (od != null) {
                settings += "OD%.1f".format(od)
            }

            if (hp != null) {
                settings += "HP%.1f".format(hp)
            }

            return settings.joinToString(", ")
        }
}