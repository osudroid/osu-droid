package com.reco1l.framework.math

@JvmInline
value class Vector4(private val values: FloatArray = FloatArray(4)) {


    constructor(x: Float, y: Float, z: Float, w: Float) : this(floatArrayOf(x, y, z, w))


    val x: Float
        get() = values[0]

    val y: Float
        get() = values[1]

    val z: Float
        get() = values[2]

    val w: Float
        get() = values[3]


    val left: Float
        get() = values[0]

    val top: Float
        get() = values[1]

    val right: Float
        get() = values[2]

    val bottom: Float
        get() = values[3]


    val total
        get() = x + y + z + w

    val vertical
        get() = y + w

    val horizontal
        get() = x + z


    operator fun plus(other: Vector4) = Vector4(
        x + other.x,
        y + other.y,
        z + other.z,
        w + other.w
    )

    operator fun minus(other: Vector4) = Vector4(
        x - other.x,
        y - other.y,
        z - other.z,
        w - other.w
    )

    operator fun times(scalar: Float) = Vector4(
        x * scalar,
        y * scalar,
        z * scalar,
        w * scalar
    )

    operator fun div(scalar: Float) = Vector4(
        x / scalar,
        y / scalar,
        z / scalar,
        w / scalar
    )

    operator fun unaryMinus() = Vector4(
        -x,
        -y,
        -z,
        -w
    )

    override fun toString() = "Vector4($x, $y, $z, $w)"


    companion object {
        val Zero = Vector4()
        val One = Vector4(1f, 1f, 1f, 1f)
    }

}