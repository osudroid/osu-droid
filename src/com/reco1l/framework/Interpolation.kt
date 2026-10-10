package com.reco1l.framework

import com.edlplan.framework.easing.*
import com.osudroid.math.Precision

object Interpolation {

    @JvmStatic
    fun intAt(time: Float, start: Int, end: Int, startTime: Float, endTime: Float, easing: Easing = Easing.None): Int {

        if (start == end) {
            return start
        }

        val current = time - startTime
        val duration = endTime - startTime

        if (duration == 0f || current == 0f) {
            return start
        }

        val t = easing.interpolate(current / duration).coerceIn(0f, 1f)

        return start + (t * (end - start)).toInt()
    }

    @JvmStatic
    fun floatAt(time: Float, start: Float, end: Float, startTime: Float, endTime: Float, easing: Easing = Easing.None): Float {

        if (start == end) {
            return start
        }

        val current = time - startTime
        val duration = endTime - startTime

        if (duration == 0f || current == 0f) {
            return start
        }

        val t = easing.interpolate(current / duration).coerceIn(0f, 1f)

        return start + t * (end - start)
    }

    /**
     * Interpolates towards [end] starting from [start] using [deltaTime] as the elapsed time of a [duration] long transition.
     */
    @JvmStatic
    fun floatLerp(deltaTime: Float, duration: Float, start: Float, end: Float, easing: Easing = Easing.None): Float {
        return floatAt(deltaTime.coerceIn(0f, duration), start, end, 0f, duration, easing)
    }

    /**
     * Same as [floatLerp] but snaps to [end] once the difference between [start] and [end] is within [snapBy].
     */
    @JvmStatic
    fun floatLerpWithSnap(deltaTime: Float, duration: Float, start: Float, end: Float, snapBy: Float = 0f, easing: Easing = Easing.None): Float {
        if (Precision.almostEquals(start, end, snapBy)) {
            return end
        }
        return floatAt(deltaTime.coerceIn(0f, duration), start, end, 0f, duration, easing)
    }

    @JvmStatic
    fun colorAt(time: Float, start: Color4, end: Color4, startTime: Float, endTime: Float, easing: Easing = Easing.None): Color4 {
        val r = floatAt(time, start.red, end.red, startTime, endTime, easing)
        val g = floatAt(time, start.green, end.green, startTime, endTime, easing)
        val b = floatAt(time, start.blue, end.blue, startTime, endTime, easing)
        val a = floatAt(time, start.alpha, end.alpha, startTime, endTime, easing)
        return Color4(r, g, b, a)
    }

    @JvmStatic
    fun colorLerp(deltaTime: Float, duration: Float, start: Color4, end: Color4, easing: Easing = Easing.None): Color4 {
        return colorAt(deltaTime.coerceIn(0f, duration), start, end, 0f, duration, easing)
    }

    /**
     * Same as [colorLerp] but snaps to [end] once a step makes no progress anymore. [Color4] stores 8 bits per
     * channel, so small steps (e.g. at high frame rates) round back to [start] and would never reach [end].
     */
    @JvmStatic
    fun colorLerpWithSnap(deltaTime: Float, duration: Float, start: Color4, end: Color4, easing: Easing = Easing.None): Color4 {
        val color = colorLerp(deltaTime, duration, start, end, easing)
        return if (color == start) end else color
    }

}
