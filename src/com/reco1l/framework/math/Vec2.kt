package com.reco1l.framework.math

data class Vec2(

    val x: Float,

    val y: Float,

) {

    constructor(value: Float = 0f) : this(value, value)


    val total
        get() = x + y

    val vertical
        get() = y

    val horizontal
        get() = x


    operator fun plus(other: Vec2) = Vec2(
        x + other.x,
        y + other.y
    )

    operator fun minus(other: Vec2) = Vec2(
        x - other.x,
        y - other.y
    )

    operator fun times(scalar: Float) = Vec2(
        x * scalar,
        y * scalar,
    )

    operator fun div(scalar: Float) = Vec2(
        x / scalar,
        y / scalar
    )

    operator fun unaryMinus() = Vec2(
        -x,
        -y
    )

    override fun toString() = "Vector2($x, $y)"


    companion object {
        val Zero = Vec2()
        val One = Vec2(1f, 1f)
    }

}