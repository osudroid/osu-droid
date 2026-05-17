package com.reco1l.framework

import com.edlplan.framework.easing.*

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


}