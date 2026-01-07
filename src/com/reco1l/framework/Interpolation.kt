package com.reco1l.framework

import com.edlplan.framework.easing.*

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


    fun colorAt(time: Float, start: Color4, end: Color4, startTime: Float, endTime: Float, easing: Easing = Easing.None): Color4 {
        val r = floatAt(time, start.red, end.red, startTime, endTime, easing)
        val g = floatAt(time, start.green, end.green, startTime, endTime, easing)
        val b = floatAt(time, start.blue, end.blue, startTime, endTime, easing)
        val a = floatAt(time, start.alpha, end.alpha, startTime, endTime, easing)
        return Color4(r, g, b, a)
    }

}