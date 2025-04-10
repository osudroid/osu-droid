package com.reco1l.framework.math

import kotlin.math.*

data class Vec2(val x: Float, val y: Float) {

    constructor(value: Float = 0f) : this(value, value)


    operator fun plus(other: Vec2) = Vec2(
        x + other.x,
        y + other.y
    )

    operator fun minus(other: Vec2) = Vec2(
        x - other.x,
        y - other.y
    )

    operator fun times(other: Vec2) = Vec2(
        x * other.x,
        y * other.y
    )

    operator fun times(scalar: Float) = Vec2(
        x * scalar,
        y * scalar
    )

    operator fun div(other: Vec2) = Vec2(
        x / other.x,
        y / other.y
    )

    operator fun div(scalar: Float) = Vec2(
        x / scalar,
        y / scalar
    )

    operator fun unaryMinus() = Vec2(
        -x,
        -y
    )


    fun distance(other: Vec2) = hypot(x - other.x, y - other.y)


    override fun equals(other: Any?) = this === other || other is Vec2
        && x == other.x
        && y == other.y

    override fun hashCode() = javaClass.hashCode()


    companion object {

        val Zero = Vec2()
        val One = Vec2(1f)

    }
}