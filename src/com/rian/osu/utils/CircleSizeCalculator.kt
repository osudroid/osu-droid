package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import kotlin.math.max
import ru.nsu.ccfit.zuev.osu.Config

/**
 * A utility for converting circle sizes across [GameMode]s.
 */
object CircleSizeCalculator {
    // These constants are used for scale calculations of replay version 6 and below.
    // This was not the real height that is used in the game, but rather an assumption so that we can treat circle sizes
    // similarly across all devices. This is used in difficulty calculation.
    private const val OLD_ASSUMED_DROID_HEIGHT = 681f
    private const val OLD_DROID_SCALE_MULTIPLIER = (0.5 * (11 - 5.2450170716245195) / 5).toFloat()

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
     * The offset used to convert between osu!droid and osu!standard circle sizes.
     *
     * `6.8556344386` was derived by converting the old osu!droid gameplay scale unit into osu!pixels (by dividing it
     * with ([Config.RES_HEIGHT] / 480)) and then fitting the function to the osu!standard scale function. The height in
     * the old osu!droid gameplay scale function was set to 576, which was chosen after sampling the top 100 most used
     * devices by players from Firebase. This is done to ensure that the new scale is as close to the old scale as
     * possible for most players.
     *
     * The fitting of both functions can be found under the following [graph](https://www.desmos.com/calculator/rjfxqc3yic).
     * Note that `6.855634` is used here instead of `6.8556344386` due to single precision floating point limitation.
     */
    private const val DROID_STANDARD_CS_OFFSET = 6.855634f

    /**
     * Converts osu!droid circle size to osu!droid scale.
     *
     * @param cs The circle size to convert.
     * @return The calculated osu!droid scale.
     */
    @JvmStatic
    fun droidCSToDroidScale(cs: Float) = max(1e-3f, standardCSToStandardScale(cs - DROID_STANDARD_CS_OFFSET, true))

    /**
     * Converts osu!droid circle size to osu!droid difficulty scale before replay version 7.
     *
     * @param cs The circle size to convert.
     * @return The calculated osu!droid difficulty scale in osu!pixels.
     */
    @JvmStatic
    fun droidCSToOldDroidDifficultyScale(cs: Float) =
        max(1e-3f, OLD_ASSUMED_DROID_HEIGHT / 480 * (54.42f - cs * 4.48f) / HitObject.OBJECT_RADIUS + OLD_DROID_SCALE_MULTIPLIER)

    /**
     * Converts osu!droid circle size to osu!droid gameplay scale before replay version 7.
     *
     * @param cs The circle size to convert.
     * @return The calculated osu!droid gameplay scale in osu!pixels.
     */
    @JvmStatic
    fun droidCSToOldDroidGameplayScale(cs: Float) =
        max(1e-3f, (54.42f - cs * 4.48f) / HitObject.OBJECT_RADIUS + OLD_DROID_SCALE_MULTIPLIER * 480 / max(1, Config.getRES_HEIGHT()))

    /**
     * Converts osu!droid scale to osu!droid circle size.
     *
     * @param scale The osu!droid scale to convert in osu!pixels.
     * @return The calculated osu!droid circle size.
     */
    @JvmStatic
    fun droidScaleToDroidCS(scale: Float) =standardScaleToStandardCS(max(1e-3f, scale), true) + DROID_STANDARD_CS_OFFSET

    /**
     * Converts osu!droid difficulty scale before replay version 7 to osu!droid circle size.
     *
     * @param scale The osu!droid scale to convert in osu!pixels.
     * @return The calculated osu!droid circle size.
     */
    @JvmStatic
    fun droidOldDifficultyScaleToDroidCS(scale: Float) =
        (54.42f - (max(1e-3f, scale) - OLD_DROID_SCALE_MULTIPLIER) * HitObject.OBJECT_RADIUS * 480 / OLD_ASSUMED_DROID_HEIGHT) / 4.48f

    /**
     * Converts osu!droid gameplay scale before replay version 7 to osu!droid circle size.
     *
     * @param scale The osu!droid scale to convert in osu!pixels.
     * @return The calculated osu!droid circle size.
     */
    @JvmStatic
    fun droidOldGameplayScaleToDroidCS(scale: Float) =
        (54.42f - (max(1e-3f, scale) - OLD_DROID_SCALE_MULTIPLIER * 480 / max(1, Config.getRES_HEIGHT())) * HitObject.OBJECT_RADIUS) / 4.48f

    /**
     * Converts old osu!droid difficulty scale that is in **screen pixels** to **osu!pixels**.
     *
     * @param scale The osu!droid scale to convert.
     * @return The converted scale.
     */
    @JvmStatic
    fun droidOldDifficultyScaleScreenPixelsToOsuPixels(scale: Float) = scale * 480 / OLD_ASSUMED_DROID_HEIGHT

    /**
     * Converts old osu!droid difficulty scale that is in **osu!pixels** to **screen pixels**.
     *
     * @param scale The osu!droid scale to convert.
     * @return The converted scale.
     */
    @JvmStatic
    fun droidOldDifficultyScaleOsuPixelsToScreenPixels(scale: Float) = scale * OLD_ASSUMED_DROID_HEIGHT / 480

    /**
     * Converts old osu!droid gameplay scale that is in **screen pixels** to **osu!pixels**.
     *
     * @param scale The osu!droid scale to convert.
     * @return The converted scale.
     */
    @JvmStatic
    fun droidOldGameplayScaleScreenPixelsToOsuPixels(scale: Float) = scale * 480 / max(1, Config.getRES_HEIGHT())

    /**
     * Converts old osu!droid scale that is in **osu!pixels** to **screen pixels**.
     *
     * @param scale The osu!droid scale to convert.
     * @return The converted scale.
     */
    @JvmStatic
    fun droidOldGameplayScaleOsuPixelsToScreenPixels(scale: Float) = scale * max(1, Config.getRES_HEIGHT()) / 480

    /**
     * Converts osu!droid scale to osu!standard radius.
     *
     * @param scale The osu!droid scale to convert.
     * @return The osu!standard scale of the given radius.
     */
    @JvmStatic
    fun droidScaleToStandardRadius(scale: Float) =
        HitObject.OBJECT_RADIUS * max(1e-3f, scale) / (OLD_ASSUMED_DROID_HEIGHT * 0.85 / 384)

    /**
     * Converts osu!standard radius to osu!droid difficulty scale before replay version 7.
     *
     * @param radius The osu!standard radius to convert.
     * @return The osu!droid difficulty scale of the given osu!standard radius, in osu!pixels.
     */
    @JvmStatic
    fun standardRadiusToOldDroidDifficultyScale(radius: Double) = max(1e-3,
        radius * OLD_ASSUMED_DROID_HEIGHT * 0.85 / 384 / HitObject.OBJECT_RADIUS).toFloat()

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
     * Converts osu!standard scale to osu!standard circle size.
     *
     * @param scale The osu!standard scale to convert.
     * @param applyFudge Whether to apply a fudge that was historically applied to osu!standard.
     * @return The osu!standard circle size of the given osu!standard scale.
     */
    @JvmStatic
    @JvmOverloads
    fun standardScaleToStandardCS(scale: Float, applyFudge: Boolean = false) =
        5 + 5 * (1 - 2 * scale / if (applyFudge) BROKEN_GAMEFIELD_ROUNDING_ALLOWANCE else 1f) / 0.7f

    /**
     * Converts osu!standard scale to osu!droid difficulty scale before replay version 7.
     *
     * @param scale The osu!standard scale to convert.
     * @param applyFudge Whether to apply a fudge that was historically applied to osu!standard.
     * @return The osu!droid difficulty scale (that was used before replay version 7) of the given osu!standard scale.
     */
    @JvmStatic
    @JvmOverloads
    fun standardScaleToOldDroidDifficultyScale(scale: Float, applyFudge: Boolean = false) =
        standardRadiusToOldDroidDifficultyScale(HitObject.OBJECT_RADIUS.toDouble() * scale /
                if (applyFudge) BROKEN_GAMEFIELD_ROUNDING_ALLOWANCE else 1f)
}