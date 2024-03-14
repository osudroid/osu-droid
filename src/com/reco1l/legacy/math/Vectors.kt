package com.reco1l.legacy.math

import com.edlplan.framework.math.Vec2
import kotlin.math.atan2
import kotlin.math.sqrt


object Vectors
{
    fun getOrthogonalDirection(start: Vec2, end: Vec2, recycled: Vec2 = Vec2()): Vec2
    {
        var x = end.x - start.x
        var y = end.y - start.y

        val length = sqrt(x * x + y * y)

        x /= length
        y /= length

        recycled.x = -y
        recycled.y = x

        return recycled
    }

    fun getTheta(start: Vec2, end: Vec2) = atan2(end.y - start.y, end.x - start.x)
}