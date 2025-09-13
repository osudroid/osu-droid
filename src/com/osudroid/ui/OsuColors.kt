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

    private val STAR_RATING_GRADIENT = listOf(
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

    /**
     * Retrieves the colour for a given point in the star range.
     */
    fun getStarRatingColor(point: Double): Color4 {

        // Mimics the MidpointRounding.AwayFromZero rounding from C#
        val sr = ceil(point).toFloat().roundBy(2)

        if (sr < 0.1f) {
            return (0xAAAAAA).toColor4()
        }

        for (i in 0 until STAR_RATING_GRADIENT.size - 1) {

            val (startDomain, startColor) = STAR_RATING_GRADIENT[i]
            val (endDomain, endColor) = STAR_RATING_GRADIENT[i + 1]

            if (sr >= endDomain) {
                continue
            }

            return Colors.interpolate(sr, startColor, endColor, startDomain, endDomain)
        }

        return Color.BLACK.toColor4()
    }

    val green = Color4(0xFF88B300)
    val red = Color4(0xFFED1121)
    val blue = Color4(0xFF66CCFF)

    val purple = Color4(0xFF8866EE)
    val purpleDark = Color4(0xFF441188)

    val yellow = Color4(0xFFFFCC22)
    val yellowDark = Color4(0xFFEEAA00)
    val yellowDarker = Color4(0xFFCC6600)
}