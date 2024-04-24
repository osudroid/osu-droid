package com.reco1l.legacy.ui

import android.graphics.Color
import org.anddev.andengine.util.modifier.ease.EaseLinear
import org.anddev.andengine.util.modifier.ease.IEaseFunction
import kotlin.math.max
import kotlin.math.pow


object Colors {

    fun toLinear(color: Float): Float {
        if (color == 1f) {
            return 1f
        }

        return if (color <= 0.04045) color / 12.92f else ((color + 0.055f) / 1.055f).pow(2.4f)
    }

    fun toSRGB(color: Float): Float {
        if (color == 1f) {
            return 1f
        }

        return if (color < 0.0031308f) 12.92f * color else 1.055f * color.pow(1f / 2.4f) - 0.055f
    }

    fun packARGB(red: Float, green: Float, blue: Float, alpha: Float): Int {

        return ((alpha * 255f + 0.5f).toInt() shl 24)
            .or((red * 255f + 0.5f).toInt() shl 16)
            .or((green * 255f + 0.5f).toInt() shl 8)
            .or((blue * 255f + 0.5f).toInt())
    }

    fun interpolateAt(
        time: Double,
        startColour: Int,
        endColour: Int,
        startTime: Float,
        endTime: Float,
        easing: IEaseFunction = EaseLinear.getInstance()
    ): Int {

        if (startColour == endColour) {
            return startColour
        }

        val current = time - startTime
        val duration = endTime - startTime

        if (duration == 0f || current == 0.0) {
            return startColour
        }

        val t = easing.getPercentage(current.toFloat(), duration).coerceIn(0.0f, 1.0f)

        val startRed = toLinear(Color.red(startColour) / 255f)
        val startGreen = toLinear(Color.green(startColour) / 255f)
        val startBlue = toLinear(Color.blue(startColour) / 255f)
        val startAlpha = Color.alpha(startColour) / 255f

        val endRed = toLinear(Color.red(endColour) / 255f)
        val endGreen = toLinear(Color.green(endColour) / 255f)
        val endBlue = toLinear(Color.blue(endColour) / 255f)
        val endAlpha = Color.alpha(endColour) / 255f

        return packARGB(
            toSRGB(startRed + t * (endRed - startRed)),
            toSRGB(startGreen + t * (endGreen - startGreen)),
            toSRGB(startBlue + t * (endBlue - startBlue)),
            startAlpha + t * (endAlpha - startAlpha)
        )
    }

    fun lighten(color: Int, amount: Float): Int {

        val scalar = max(1f, 1f + amount)

        return packARGB(
            Color.red(color) / 255f * scalar,
            Color.green(color) / 255f * scalar,
            Color.blue(color) / 255f * scalar,
            Color.alpha(color) / 255f
        )
    }
}


object OsuColors {

    // Source from: https://github.com/ppy/osu/blob/b9b341affd4faea65cd29c9678385176628ace5d/osu.Game/Graphics/OsuColour.cs#L26

    val STAR_RATING_GRADIENT = listOf(
        0.1f to 0xFFaaaaaa.toInt(),
        0.1f to 0xFF4290fb.toInt(),
        1.25f to 0xFF4fc0ff.toInt(),
        2.0f to 0xFF4fffd5.toInt(),
        2.5f to 0xFF7cff4f.toInt(),
        3.3f to 0xFFf6f05c.toInt(),
        4.2f to 0xFFff8068.toInt(),
        4.9f to 0xFFff4e6f.toInt(),
        5.8f to 0xFFc645b8.toInt(),
        6.7f to 0xFF6563de.toInt(),
        7.7f to 0xFF18158e.toInt(),
        9.0f to Color.BLACK
    )

    fun getStarRatingColor(point: Double): Int {

        if (point < 0.1f) {
            return 0xAAAAAA
        }

        for (i in 0 until STAR_RATING_GRADIENT.size - 1) {

            val (startDomain, startColor) = STAR_RATING_GRADIENT[i]
            val (endDomain, endColor) = STAR_RATING_GRADIENT[i + 1]

            if (point >= endDomain) {
                continue
            }

            return Colors.interpolateAt(point, startColor, endColor, startDomain, endDomain)
        }

        return Color.BLACK
    }


}





