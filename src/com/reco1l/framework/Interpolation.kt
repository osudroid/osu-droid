package com.reco1l.framework

import com.edlplan.framework.easing.*
import com.rian.osu.math.Precision

object Interpolation {

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

    fun floatLerp(deltaTime: Float, duration: Float, start: Float, end: Float, easing: Easing = Easing.None): Float {
        return floatAt(deltaTime.coerceIn(0f, duration), start, end, 0f, duration, easing)
    }

    fun floatLerpWithSnap(deltaTime: Float, duration: Float, start: Float, end: Float, snapBy: Float = 0f, easing: Easing = Easing.None): Float {
        if (Precision.almostEquals(start, end, snapBy)) {
            return end
        }
        return floatAt(deltaTime.coerceIn(0f, duration), start, end, 0f, duration, easing)
    }


    fun colorAt(time: Float, start: Color4, end: Color4, startTime: Float, endTime: Float, easing: Easing = Easing.None): Color4 {
        val r = floatAt(time, start.red, end.red, startTime, endTime, easing)
        val g = floatAt(time, start.green, end.green, startTime, endTime, easing)
        val b = floatAt(time, start.blue, end.blue, startTime, endTime, easing)
        val a = floatAt(time, start.alpha, end.alpha, startTime, endTime, easing)
        return Color4(r, g, b, a)
    }

    fun colorLerp(deltaTime: Float, duration: Float, start: Color4, end: Color4, easing: Easing = Easing.None): Color4 {
        return colorAt(deltaTime.coerceIn(0f, duration), start, end, 0f, duration, easing)
    }

}