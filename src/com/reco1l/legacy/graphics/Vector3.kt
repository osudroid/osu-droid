package com.reco1l.legacy.graphics

import com.edlplan.framework.math.Vec2
import kotlin.math.sqrt


/**
 * Basic class to represent 3D vectors.
 */
data class Vector3(

    @JvmField var x: Float,

    @JvmField var y: Float,

    @JvmField var z: Float
)
{

    val length
        get() = sqrt(x * x + y * y + z * z)


    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)

    operator fun times(other: Float) = Vector3(x * other, y * other, z * other)

    operator fun div(other: Float) = Vector3(x / other, y / other, z / other)

}


/**
 * Convert [Vec2] to [Vector3].
 */
fun Vec2.toVector3(z: Float = 0f) = Vector3(x, y, z)