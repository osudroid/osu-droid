package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import kotlin.math.max
import ru.nsu.ccfit.zuev.osu.Config

/**
 * A utility for converting circle sizes across [GameMode]s.
 */
object CircleSizeCalculator {
    // This is not the real height that is used in the game, but rather an assumption so that we can treat
    // circle sizes similarly across all devices. This is used in difficulty calculation.
    private const val ASSUMED_DROID_HEIGHT = 681f
    private const val DROID_SCALE_MULTIPLIER = (0.5 * (11 - 5.2450170716245195) / 5).toFloat()

    /**
     * The following comment is copied verbatim from osu!lazer and osu!stable:
     *
     *  Builds of osu! up to 2013-05-04 had the gamefield being rounded down, which caused incorrect radius calculations
     *  in widescreen cases. This ratio adjusts to allow for old replays to work post-fix, which in turn increases the lenience
     *  for all plays, but by an amount so small it should only be effective in replays.
     *
     * To match expectations of gameplay we need to apply this multiplier to circle scale. It's weird but is what it is.
     * It works out to under 1 game pixel and is generally not meaningful to gameplay, but is to replay playback accuracy.
     */
    private const val BROKEN_GAMEFIELD_ROUNDING_ALLOWANCE = 1.00041f

    /**
     * Converts osu!droid circle size to osu!droid difficulty scale.
     *
     * @param cs The circle size to convert.
     * @return The calculated osu!droid difficulty scale.
     */
    @JvmStatic
    fun droidCSToDroidDifficultyScale(cs: Float) = droidCSToDroidScale(cs, ASSUMED_DROID_HEIGHT)

    /**
     * Converts osu!droid circle size to osu!droid gameplay scale.
     *
     * @param cs The circle size to convert.
     * @return The calculated osu!droid difficulty scale.
     */
    @JvmStatic
    fun droidCSToDroidGameplayScale(cs: Float) = droidCSToDroidScale(cs, Config.getRES_HEIGHT().toFloat())

    /**
     * Converts osu!droid difficulty scale to osu!droid circle size.
     *
     * @param scale The osu!droid scale to convert.
     * @return The calculated osu!droid circle size.
     */
    @JvmStatic
    fun droidDifficultyScaleToDroidCS(scale: Float) = droidScaleToDroidCS(scale, ASSUMED_DROID_HEIGHT)

    /**
     * Converts osu!droid gameplay scale to osu!droid circle size.
     *
     * @param scale The osu!droid scale to convert.
     * @return The calculated osu!droid circle size.
     */
    @JvmStatic
    fun droidGameplayScaleToDroidCS(scale: Float) = droidScaleToDroidCS(scale, Config.getRES_HEIGHT().toFloat())

    /**
     * Converts osu!droid scale to osu!standard radius.
     *
     * @param scale The osu!droid scale to convert.
     * @return The osu!standard scale of the given radius.
     */
    @JvmStatic
    fun droidScaleToStandardRadius(scale: Float) =
        HitObject.OBJECT_RADIUS * max(1e-3f, scale) / (ASSUMED_DROID_HEIGHT * 0.85 / 384)

    /**
     * Converts osu!standard radius to osu!droid scale.
     *
     * @param radius The osu!standard radius to convert.
     * @return The osu!droid scale of the given osu!standard radius.
     */
    @JvmStatic
    fun standardRadiusToDroidDifficultyScale(radius: Double) = max(1e-3,
        radius * ASSUMED_DROID_HEIGHT * 0.85 / 384 / HitObject.OBJECT_RADIUS).toFloat()

    /**
     * Converts osu!standard radius to osu!standard circle size.
     *
     * @param radius The osu!standard radius to convert.
     * @return The osu!standard circle size at the given radius.
     */
    @JvmStatic
    fun standardRadiusToStandardCS(radius: Double, applyFudge: Boolean = false) =
        5 + (1 - radius.toFloat() / (HitObject.OBJECT_RADIUS / 2) / if (applyFudge) BROKEN_GAMEFIELD_ROUNDING_ALLOWANCE else 1f) * 5 / 0.7f

    /**
     * Converts osu!standard circle size to osu!standard scale.
     *
     * @param cs The osu!standard circle size to convert.
     * @param applyFudge Whether to apply a fudge that was historically applied to osu!standard.
     * @return The osu!standard scale of the given circle size.
     */
    @JvmStatic
    @JvmOverloads
    fun standardCSToStandardScale(cs: Float, applyFudge: Boolean = false) =
        (1 - 0.7f * (cs - 5) / 5) / 2 * if (applyFudge) BROKEN_GAMEFIELD_ROUNDING_ALLOWANCE else 1f

    /**
     * Converts osu!standard scale to osu!droid difficulty scale.
     *
     * @param scale The osu!standard scale to convert.
     * @param applyFudge Whether to apply a fudge that was historically applied to osu!standard.
     * @return The osu!droid difficulty scale of the given osu!standard scale.
     */
    @JvmStatic
    @JvmOverloads
    fun standardScaleToDroidDifficultyScale(scale: Float, applyFudge: Boolean = false) =
        standardRadiusToDroidDifficultyScale(HitObject.OBJECT_RADIUS.toDouble() * scale /
                if (applyFudge) BROKEN_GAMEFIELD_ROUNDING_ALLOWANCE else 1f)

    /**
     * Converts osu!droid circle size to osu!droid scale.
     *
     * @param cs The circle size to convert.
     * @param height The height to use.
     * @return The calculated osu!droid scale.
     */
    private fun droidCSToDroidScale(cs: Float, height: Float) =
        max(1e-3f, height / 480 * (54.42f - cs * 4.48f) * 2 / 128 + DROID_SCALE_MULTIPLIER)

    /**
     * Converts osu!droid scale to osu!droid circle size.
     *
     * @param scale The osu!droid scale to convert.
     * @param height The height to use.
     * @return The calculated osu!droid circle size.
     */
    private fun droidScaleToDroidCS(scale: Float, height: Float) =
        (54.42f - (max(1e-3f, scale) - DROID_SCALE_MULTIPLIER) * 128 / 2 * 480 / height) / 4.48f
}