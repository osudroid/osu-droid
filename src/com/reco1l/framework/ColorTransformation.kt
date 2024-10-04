package com.reco1l.framework

import android.graphics.*
import com.edlplan.framework.easing.*
import kotlin.math.*
import kotlin.random.*


/**
 * Returns a random color.
 */
@JvmOverloads
fun Random.nextColor(until: Int = 255, alpha: Int = until): Int {
    return Color.argb(alpha, nextInt(until), nextInt(until), nextInt(until))
}


fun ColorARGB.toLinear() = ColorARGB(
    Colors.toLinear(red),
    Colors.toLinear(green),
    Colors.toLinear(blue),
    alpha
)

fun ColorARGB.toSRGB() = ColorARGB(
    Colors.toSRGB(red),
    Colors.toSRGB(green),
    Colors.toSRGB(blue),
    alpha
)


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


    /**
     * Interpolates between two sRGB colors in a linear (gamma-correct) RGB space.
     *
     * [Information regarding linear interpolation](https://blog.johnnovak.net/2016/09/21/what-every-coder-should-know-about-gamma/#gradients)
     */
    fun interpolate(time: Float, startColour: ColorARGB, endColour: ColorARGB, startTime: Float, endTime: Float, easing: Easing = Easing.None): ColorARGB {

        if (startColour == endColour) {
            return startColour
        }

        val current = time - startTime
        val duration = endTime - startTime

        if (duration == 0f || current == 0f) {
            return startColour
        }

        val t = EasingManager.apply(easing, (current / duration).toDouble()).toFloat().coerceIn(0f, 1f)

        val startLinear = startColour.toLinear()
        val endLinear = endColour.toLinear()

        return ColorARGB(
            startLinear.red + t * (endLinear.red - startLinear.red),
            startLinear.green + t * (endLinear.green - startLinear.green),
            startLinear.blue + t * (endLinear.blue - startLinear.blue),
            startColour.alpha + t * (endColour.alpha - startColour.alpha)
        ).toSRGB()
    }

    /**
     * Interpolates between two sRGB colors directly in sRGB space.
     */
    fun interpolateNonLinear(time: Float, startColour: ColorARGB, endColour: ColorARGB, startTime: Float, endTime: Float, easing: Easing = Easing.None): ColorARGB {

        if (startColour == endColour) {
            return startColour
        }

        val current = time - startTime
        val duration = endTime - startTime

        if (duration == 0f || current == 0f) {
            return startColour
        }

        val t = EasingManager.apply(easing, (current / duration).toDouble()).toFloat().coerceIn(0f, 1f)

        return ColorARGB(
            startColour.red + t * (endColour.red - startColour.red),
            startColour.green + t * (endColour.green - startColour.green),
            startColour.blue + t * (endColour.blue - startColour.blue),
            startColour.alpha + t * (endColour.alpha - startColour.alpha)
        )
    }

}



