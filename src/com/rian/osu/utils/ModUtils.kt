package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.*
import kotlin.reflect.full.createInstance
import org.json.JSONArray

/**
 * A set of utilities to handle [Mod] combinations.
 */
object ModUtils {

    /**
     * Returns a list of all available [Mod]s.
     */
    val allModsInstances
        get() = arrayOf(
            ModAutoplay(),
            ModAutopilot(),
            ModCustomSpeed(),
            ModDeflate(),
            ModDifficultyAdjust(),
            ModDoubleTime(),
            ModEasy(),
            ModFlashlight(),
            ModGrow(),
            ModHalfTime(),
            ModHardRock(),
            ModHidden(),
            ModMirror(),
            ModMuted(),
            ModNightCore(),
            ModNoFail(),
            ModPerfect(),
            ModPrecise(),
            ModRandom(),
            ModReallyEasy(),
            ModRelax(),
            ModReplayV6(),
            ModScoreV2(),
            ModSmallCircle(),
            ModSuddenDeath(),
            ModSynesthesia(),
            ModTraceable(),
            ModWindDown(),
            ModWindUp()
        )

    private val allModsClassesByAcronym = allModsInstances.associateBy({ it.acronym }, { it::class })

    /**
     * Serializes a list of [Mod]s into a [JSONArray].
     *
     * The serialized [Mod]s can be deserialized using [deserializeMods].
     *
     * @param mods The list of [Mod]s to serialize.
     * @param includeNonUserPlayable Whether to serialize non-user-playable [Mod]s. Defaults to true.
     * @return The serialized [Mod]s in a [JSONArray].
     */
    @JvmStatic
    @JvmOverloads
    fun serializeMods(mods: Iterable<Mod>, includeNonUserPlayable: Boolean = true) = JSONArray().also {
        for (mod in mods) {
            if (!includeNonUserPlayable && !mod.isUserPlayable) {
                continue
            }

            it.put(mod.serialize())
        }
    }

    /**
     * Deserializes a list of [Mod]s from a [JSONArray] received from [serializeMods].
     *
     * @param json The [JSONArray] containing the serialized [Mod]s.
     * @return The deserialized [Mod]s in a [ModHashMap].
     */
    @JvmStatic
    @JvmOverloads
    fun deserializeMods(json: JSONArray? = null) = ModHashMap().also {
        if (json == null) {
            return@also
        }

        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            val acronym = obj.optString("acronym") ?: continue
            val settings = obj.optJSONObject("settings")

            val mod = allModsClassesByAcronym[acronym]?.createInstance() ?: continue

            if (settings != null) {
                mod.copySettings(settings)
            }

            it.put(mod)
        }
    }

    /**
     * Calculates the playback rate for the track with the selected [Mod]s at [time].
     *
     * @param mods The list of selected [Mod]s.
     * @param time The time at which the playback rate is queried, in milliseconds. Defaults to 0.
     * @return The rate with [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateRateWithMods(mods: Iterable<Mod>, time: Double = 0.0) = mods.fold(1f) { rate, mod ->
        (mod as? IModApplicableToTrackRate)?.applyToRate(time, rate) ?: rate
    }

    /**
     * Calculates the playback rate for the track with the selected [IModApplicableToTrackRate]s at [time].
     *
     * @param mods The list of selected [IModApplicableToTrackRate]s.
     * @param time The time at which the playback rate is queried, in milliseconds. Defaults to 0.
     * @return The rate with [IModApplicableToTrackRate]s.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateRateWithTrackRateMods")
    fun calculateRateWithMods(mods: Iterable<IModApplicableToTrackRate>, time: Double = 0.0) =
        mods.fold(1f) { rate, mod ->
            mod.applyToRate(time, rate)
        }

    /**
     * Applies the selected [Mod]s to a [BeatmapDifficulty].
     *
     * @param difficulty The [BeatmapDifficulty] to apply the [Mod]s to.
     * @param mode The [GameMode] to apply the [Mod]s for.
     * @param mods The selected [Mod]s.
     * @param withRateChange Whether to apply rate changes to the [BeatmapDifficulty].
     */
    @JvmStatic
    @JvmOverloads
    fun applyModsToBeatmapDifficulty(
        difficulty: BeatmapDifficulty,
        mode: GameMode,
        mods: Iterable<Mod>,
        withRateChange: Boolean = false
    ) {
        @Suppress("UNCHECKED_CAST")
        val adjustmentMods = mods.filter { it is IModFacilitatesAdjustment } as Iterable<IModFacilitatesAdjustment>

        for (mod in mods) {
            if (mod is IModApplicableToDifficulty) {
                mod.applyToDifficulty(mode, difficulty, adjustmentMods)
            }
        }

        for (mod in mods) {
            if (mod is IModApplicableToDifficultyWithMods) {
                mod.applyToDifficulty(mode, difficulty, mods)
            }
        }

        if (!withRateChange) {
            return
        }

        // Apply rate adjustments
        val trackRate = calculateRateWithMods(mods, Double.POSITIVE_INFINITY)

        val preempt = BeatmapDifficulty.difficultyRange(
            difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ) / trackRate

        difficulty.ar = BeatmapDifficulty.inverseDifficultyRange(
            preempt, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ).toFloat()

        val isPreciseMod = mods.any { it is ModPrecise }
        val hitWindow = if (isPreciseMod) PreciseDroidHitWindow(difficulty.od) else DroidHitWindow(difficulty.od)
        val greatWindow = hitWindow.greatWindow / trackRate

        difficulty.od =
            if (isPreciseMod) PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow)
            else DroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow)
    }
}