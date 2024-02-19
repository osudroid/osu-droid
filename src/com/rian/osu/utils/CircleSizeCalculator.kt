package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import kotlin.math.max

/**
 * A utility for converting circle sizes across [GameMode]s.
 */
object CircleSizeCalculator {
    // This is not the real height that is used in the game, but rather an assumption so that we can treat
    // circle sizes similarly across all devices.
    private const val ASSUMED_DROID_HEIGHT = 681f

    // The following comment is copied verbatim from osu!lazer and osu!stable:
    //
    //   Builds of osu! up to 2013-05-04 had the gamefield being rounded down, which caused incorrect radius calculations
    //   in widescreen cases. This ratio adjusts to allow for old replays to work post-fix, which in turn increases the lenience
    //   for all plays, but by an amount so small it should only be effective in replays.
    //
    // To match expectations of gameplay we need to apply this multiplier to circle scale. It's weird but is what it is.
    // It works out to under 1 game pixel and is generally not meaningful to gameplay, but is to replay playback accuracy.
    private const val BROKEN_GAMEFIELD_ROUNDING_ALLOWANCE = 1.00041f

    /**
     * Converts osu!droid circle size to osu!droid scale.
     *
     * @param cs The circle size to convert.
     * @return The calculated osu!droid scale.
     */
    @JvmStatic
    fun droidCSToDroidScale(cs: Float) =
        max(1e-3f, ASSUMED_DROID_HEIGHT / 480 * (54.42f - cs * 4.48f) * 2 / 128 + 0.5f * ((11 - 5.2450170716245195) / 5).toFloat())

    /**
     * Converts osu!droid scale to osu!droid circle size.
     *
     * @param scale The osu!droid scale to convert.
     * @return The calculated osu!droid circle size.
     */
    @JvmStatic
    fun droidScaleToDroidCS(scale: Float) =
        (54.42f - (max(1e-3f, scale) - 0.5f * ((11 - 5.2450170716245195) / 5).toFloat()) * 128 / 2 * 480 / ASSUMED_DROID_HEIGHT) / 4.48f

    /**
     * Converts osu!droid scale to osu!standard radius.
     *
     * @param scale The osu!droid scale to convert.
     * @return The osu!standard scale of the given radius.
     */
    @JvmStatic
    fun droidScaleToStandardRadius(scale: Float) =
        HitObject.OBJECT_RADIUS * max(1e-3f, scale) / (ASSUMED_DROID_HEIGHT * 0.85f / 384)

    /**
     * Converts osu!standard radius to osu!droid scale.
     *
     * @param radius The osu!standard radius to convert.
     * @return The osu!droid scale of the given osu!standard radius.
     */
    @JvmStatic
    fun standardRadiusToDroidScale(radius: Double) = max(1e-3f,
        radius.toFloat() * ASSUMED_DROID_HEIGHT * 0.85f / 384 / HitObject.OBJECT_RADIUS)

    /**
     * Converts osu!standard radius to osu!standard circle size.
     *
     * @param radius The osu!standard radius to convert.
     * @return The osu!standard circle size at the given radius.
     */
    @JvmStatic
    fun standardRadiusToStandardCS(radius: Double) =
        5 + ((1 - radius.toFloat() / (HitObject.OBJECT_RADIUS / 2)) * 5) / 0.7f

    /**
     * Converts osu!standard circle size to osu!standard scale.
     *
     * @param cs The osu!standard circle size to convert.
     * @return The osu!standard scale of the given circle size.
     */
    @JvmStatic
    fun standardCSToStandardScale(cs: Float) = (1 - 0.7f * (cs - 5) / 5) / 2 * BROKEN_GAMEFIELD_ROUNDING_ALLOWANCE

    /**
     * Converts osu!standard scale to osu!droid scale.
     *
     * @param scale The osu!standard scale to convert.
     * @return The osu!droid scale of the given osu!standard scale.
     */
    @JvmStatic
    fun standardScaleToDroidScale(scale: Float) =
        standardRadiusToDroidScale(HitObject.OBJECT_RADIUS.toDouble() * scale / BROKEN_GAMEFIELD_ROUNDING_ALLOWANCE)

    /**
     * Converts osu!standard circle size to osu!droid scale.
     *
     * @param cs The osu!standard circle size to convert.
     * @return The osu!droid scale of the given osu!standard circle size.
     */
    @JvmStatic
    fun standardCSToDroidScale(cs: Float) = standardScaleToDroidScale(standardCSToStandardScale(cs))
}