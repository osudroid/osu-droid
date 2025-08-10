package com.rian.osu.mods

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.math.Interpolation
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject

/**
 * Represents a [Mod] that gradually adjusts the track's playback rate over time.
 */
abstract class ModTimeRamp : Mod(), IModApplicableToBeatmap, IModApplicableToTrackRate {
    /**
     * The starting speed of the track.
     */
    abstract var initialRate: Float

    /**
     * The final speed to ramp to.
     */
    abstract var finalRate: Float

    final override val isValidForMultiplayerAsFreeMod = false
    override val incompatibleMods = super.incompatibleMods + ModTimeRamp::class

    private var initialRateTime = 0.0
    private var finalRateTime = 0.0

    // Graph: https://www.desmos.com/calculator/1zp4vwl3o7
    override val scoreMultiplier: Float
        get() =
            Interpolation.linear(
                calculateScoreMultiplierAt(initialRate),
                calculateScoreMultiplierAt(finalRate),
                FINAL_RATE_PROGRESS.toFloat()
            )

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        initialRate = settings.optDouble("initialRate", initialRate.toDouble()).toFloat()
        finalRate = settings.optDouble("finalRate", finalRate.toDouble()).toFloat()
    }

    override fun serializeSettings() = JSONObject().apply {
        put("initialRate", initialRate)
        put("finalRate", finalRate)
    }

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        initialRateTime = beatmap.hitObjects.objects.firstOrNull()?.startTime ?: 0.0

        finalRateTime = Interpolation.linear(
            initialRateTime,
            beatmap.hitObjects.objects.lastOrNull()?.endTime ?: 0.0,
            FINAL_RATE_PROGRESS
        )
    }

    override fun applyToRate(time: Double, rate: Float): Float {
        val amount = (time - initialRateTime) / max(1.0, finalRateTime - initialRateTime)

        return rate * Interpolation.linear(initialRate, finalRate, amount.toFloat().coerceIn(0f, 1f))
    }

    override val extraInformation
        get() = "%.2fx - %.2fx".format(initialRate, finalRate)

    private fun calculateScoreMultiplierAt(rate: Float) = ModRateAdjustHelper(rate).scoreMultiplier

    companion object {
        /**
         * The point in the beatmap at which the final rate should be reached.
         */
        const val FINAL_RATE_PROGRESS = 0.75
    }
}