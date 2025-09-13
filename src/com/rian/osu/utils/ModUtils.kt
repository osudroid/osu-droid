package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.*
import kotlin.reflect.full.createInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONException

/**
 * A set of utilities to handle [Mod] combinations.
 */
object ModUtils {

    /**
     * Returns a list of all available [Mod]s.
     */
    val allModsInstances by lazy {
        arrayOf(
            ModApproachDifferent(),
            ModAutoplay(),
            ModAutopilot(),
            ModCustomSpeed(),
            ModDifficultyAdjust(),
            ModDoubleTime(),
            ModEasy(),
            ModFlashlight(),
            ModFreezeFrame(),
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
    }

    private val allModsClassesByAcronym = allModsInstances.associateBy({ it.acronym }, { it::class })

    /**
     * Serializes a list of [Mod]s into a [JSONArray].
     *
     * The serialized [Mod]s can be deserialized using [deserializeMods].
     *
     * @param mods The list of [Mod]s to serialize.
     * @param includeNonUserPlayable Whether to serialize non-user-playable [Mod]s. Defaults to `true`.
     * @param includeIrrelevantMods Whether to include [Mod]s that are not relevant to gameplay. Defaults to `false`.
     * @return The serialized [Mod]s in a [JSONArray].
     */
    @JvmStatic
    @JvmOverloads
    fun serializeMods(
        mods: Iterable<Mod>,
        includeNonUserPlayable: Boolean = true,
        includeIrrelevantMods: Boolean = false
    ) = JSONArray().also {
        for (mod in mods) {
            if (!includeNonUserPlayable && !mod.isUserPlayable) {
                continue
            }

            if (!includeIrrelevantMods && !mod.isRelevant) {
                continue
            }

            it.put(mod.serialize())
        }
    }

    /**
     * Deserializes a list of [Mod]s from a JSON string received from [serializeMods].
     *
     * @param str The JSON string containing the serialized [Mod]s.
     * @return The deserialized [Mod]s in a [ModHashMap].
     * @throws JSONException If the string cannot be parsed as a valid JSON array.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun deserializeMods(str: String) = deserializeMods(JSONArray(str))

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
     * This is a faster version that uses [Collection.indices] rather than [Iterable.iterator].
     *
     * @param mods The list of selected [Mod]s.
     * @param time The time at which the playback rate is queried, in milliseconds. Defaults to 0.
     * @return The rate with [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateRateWithMods(mods: List<Mod>, time: Double = 0.0): Float {
        var rate = 1f

        for (i in mods.indices) {
            val mod = mods[i]

            if (mod is IModApplicableToTrackRate) {
                rate = mod.applyToRate(time, rate)
            }
        }

        return rate
    }

    /**
     * Calculates the playback rate for the track with the selected [IModApplicableToTrackRate]s at [time].
     *
     * This is a faster version that uses [Collection.indices] rather than [Iterable.iterator].
     *
     * @param mods The list of selected [IModApplicableToTrackRate]s.
     * @param time The time at which the playback rate is queried, in milliseconds. Defaults to 0.
     * @return The rate with [IModApplicableToTrackRate]s.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateRateWithTrackRateMods")
    fun calculateRateWithMods(mods: List<IModApplicableToTrackRate>, time: Double = 0.0): Float {
        var rate = 1f

        for (i in mods.indices) {
            val mod = mods[i]

            rate = mod.applyToRate(time, rate)
        }

        return rate
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
     * Calculates the score multiplier for the selected [Mod]s.
     *
     * @param mods The selected [Mod]s.
     * @return The score multiplier.
     */
    @JvmStatic
    fun calculateScoreMultiplier(mods: ModHashMap) =
        calculateScoreMultiplier(mods.values)

    /**
     * Calculates the score multiplier for the selected [Mod]s.
     *
     * @param mods The selected [Mod]s.
     * @return The score multiplier.
     */
    @JvmStatic
    fun calculateScoreMultiplier(mods: Iterable<Mod>): Float {
        // Rate-adjusting mods combine their track rate multipliers together, then bunched together.
        var totalRateAdjustTrackRateMultiplier = 1f
        var scoreMultiplier = 1f

        for (mod in mods) {
            if (mod is ModRateAdjust) {
                totalRateAdjustTrackRateMultiplier *= mod.trackRateMultiplier
            } else {
                scoreMultiplier *= mod.scoreMultiplier
            }
        }

        scoreMultiplier *= ModRateAdjustHelper(totalRateAdjustTrackRateMultiplier).scoreMultiplier

        return scoreMultiplier
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
        withRateChange: Boolean = false,
        scope: CoroutineScope? = null
    ) {
        @Suppress("UNCHECKED_CAST")
        val adjustmentMods = mods.filter { it is IModFacilitatesAdjustment } as Iterable<IModFacilitatesAdjustment>

        for (mod in mods) {
            scope?.ensureActive()

            if (mod is IModApplicableToDifficulty) {
                mod.applyToDifficulty(mode, difficulty, adjustmentMods)
            }
        }

        for (mod in mods) {
            scope?.ensureActive()

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