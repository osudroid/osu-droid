package com.osudroid.ui

import android.graphics.Color
import com.reco1l.framework.Color4
import com.reco1l.framework.Colors
import com.reco1l.framework.toColor4
import com.reco1l.toolkt.roundBy
import kotlin.math.ceil

/**
 * A collection of colors used in osu!.
 */
object OsuColors {

    // Source from: https://github.com/ppy/osu/blob/b9b341affd4faea65cd29c9678385176628ace5d/osu.Game/Graphics/OsuColour.cs#L26
    private val starRatingColorSpectrum = LinearColorScale(
        listOf(
            0.1f to Color4(0xFFaaaaaa.toInt()),
            0.1f to Color4(0xFF4290fb.toInt()),
            1.25f to Color4(0xFF4fc0ff.toInt()),
            2.0f to Color4(0xFF4fffd5.toInt()),
            2.5f to Color4(0xFF7cff4f.toInt()),
            3.3f to Color4(0xFFf6f05c.toInt()),
            4.2f to Color4(0xFFff8068.toInt()),
            4.9f to Color4(0xFFff4e6f.toInt()),
            5.8f to Color4(0xFFc645b8.toInt()),
            6.7f to Color4(0xFF6563de.toInt()),
            7.7f to Color4(0xFF18158e.toInt()),
            9.0f to Color4(Color.BLACK)
        )
    )

    // Sourced from https://github.com/ppy/osu-web/blob/e510645e16af97997d3ce97624472b3f1fbe12a2/resources/js/utils/beatmap-helper.ts#L28.
    // Do note that the source has one more color in the range than the domain, which is unused.
    // In addition to that, gamma correction is ignored, following similar spectrum behavior of the star rating color itself.
    private val starRatingTextColorSpectrum = LinearColorScale(
        listOf(
            9.0f to Color4(0xFFF6F05C.toInt()),
            9.9f to Color4(0xFFFF8068.toInt()),
            10.6f to Color4(0xFFFF4E6F.toInt()),
            11.5f to Color4(0xFFC645B8.toInt()),
            12.4f to Color4(0xFF6563DE.toInt())
        )
    )

    /**
     * Retrieves the colour for a given point in the star range.
     */
    fun getStarRatingColor(point: Double): Color4 {
        // Mimics the MidpointRounding.AwayFromZero rounding from C#
        val sr = ceil(point).toFloat().roundBy(2)

        if (sr < 0.1f) {
            return Color4(0xAAAAAA)
        }

        return starRatingColorSpectrum.get(sr)
    }

    /**
     * Retrieves the text colour for a given point in the star range.
     */
    fun getStarRatingTextColor(point: Double): Color4 {
        // Mimics the MidpointRounding.AwayFromZero rounding from C#
        val sr = ceil(point).toFloat().roundBy(2)

        if (sr < 6.5) {
            return Color.BLACK.toColor4()
        }

        return starRatingTextColorSpectrum.get(sr)
    }

    val green = Color4(0xFF88B300)
    val red = Color4(0xFFED1121)
    val blue = Color4(0xFF66CCFF)

    val redLight = Color4(0xFFED7787)
    val redDark = Color4(0xFFBA0011)

    val purple = Color4(0xFF8866EE)
    val purpleDark = Color4(0xFF441188)

    val yellow = Color4(0xFFFFCC22)
    val yellowDark = Color4(0xFFEEAA00)
    val yellowDarker = Color4(0xFFCC6600)
}

private class LinearColorScale(
    private val gradient: List<Pair<Float, Color4>>,
    private val clamp: Boolean = true
) {
    fun get(point: Float): Color4 {
        if (clamp) {
            if (point <= gradient.first().first) {
                return gradient.first().second
            }

            if (point >= gradient.last().first) {
                return gradient.last().second
            }
        }

        for (i in 0 until gradient.size - 1) {
            val (startDomain, startColor) = gradient[i]
            val (endDomain, endColor) = gradient[i + 1]

            if (point >= endDomain) {
                continue
            }

            return Colors.interpolate(point, startColor, endColor, startDomain, endDomain)
        }

        return gradient.last().second
    }
}