package com.rian.osu.math

/**
 * Represents a four-dimensional vector.
 */
data class Vector4(
    /**
     * The W component of this [Vector4].
     */
    @JvmField
    val w: Float,

    /**
     * The X component of this [Vector4].
     */
    @JvmField
    val x: Float,

    /**
     * The Y component of this [Vector4].
     */
    @JvmField
    val y: Float,

    /**
     * The Z component of this [Vector4].
     */
    @JvmField
    val z: Float,
) {
    /**
     * The Y coordinate of the top edge of this [Vector4].
     */
    val left
        get() = x

    /**
     * The Y coordinate of the top edge of this [Vector4].
     */
    val top
        get() = y

    /**
     * The X coordinate of the right edge of this [Vector4].
     */
    val right
        get() = z

    /**
     * The Y coordinate of the bottom edge of this [Vector4].
     */
    val bottom
        get() = w

    /**
     * The top left corner of this [Vector4].
     */
    val topLeft
        get() = Vector2(left, top)

    /**
     * The top right corner of this [Vector4].
     */
    val topRight
        get() = Vector2(right, top)

    /**
     * The bottom left corner of this [Vector4].
     */
    val bottomLeft
        get() = Vector2(left, bottom)

    /**
     * The bottom right corner of this [Vector4].
     */
    val bottomRight
        get() = Vector2(right, bottom)

    /**
     * The width of the rectangle defined by this [Vector4].
     */
    val width
        get() = right - left

    /**
     * The height of the rectangle defined by this [Vector4].
     */
    val height
        get() = bottom - top
}