package com.reco1l.framework.math

data class Vec4(

    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 0f,

) {

    val left: Float
        get() = x

    val top: Float
        get() = y

    val right: Float
        get() = z

    val bottom: Float
        get() = w


    val total
        get() = x + y + z + w

    val vertical
        get() = y + w

    val horizontal
        get() = x + z


    operator fun plus(other: Vec4) = Vec4(
        x + other.x,
        y + other.y,
        z + other.z,
        w + other.w
    )

    operator fun minus(other: Vec4) = Vec4(
        x - other.x,
        y - other.y,
        z - other.z,
        w - other.w
    )

    operator fun times(scalar: Float) = Vec4(
        x * scalar,
        y * scalar,
        z * scalar,
        w * scalar
    )

    operator fun div(scalar: Float) = Vec4(
        x / scalar,
        y / scalar,
        z / scalar,
        w / scalar
    )

    operator fun unaryMinus() = Vec4(
        -x,
        -y,
        -z,
        -w
    )

    override fun toString() = "Vector4($x, $y, $z, $w)"


    companion object {
        val Zero = Vec4()
        val One = Vec4(1f, 1f, 1f, 1f)
    }

}