package com.reco1l.framework

import android.graphics.*

@JvmInline
value class ColorARGB(private val hex: Int) {

    @JvmOverloads
    constructor(red: Int, green: Int, blue: Int, alpha: Int = 255): this(Color.argb(alpha, red, green, blue))

    @JvmOverloads
    constructor(red: Float, green: Float, blue: Float, alpha: Float = 1f) : this(
        // Color.argb() for 0-1 range values requires API level 26 and above so we inline the function.
        ((alpha * 255f + 0.5f).toInt() shl 24) or ((red * 255f + 0.5f).toInt() shl 16) or ((green * 255f + 0.5f).toInt() shl 8) or (blue * 255f + 0.5f).toInt()
    )


    val alpha
        get() = Color.alpha(hex) / 255f

    val red
        get() = Color.red(hex) / 255f

    val green
        get() = Color.green(hex) / 255f

    val blue
        get() = Color.blue(hex) / 255f


    operator fun plus(other: ColorARGB) = ColorARGB(
        red = red + other.red,
        green = green + other.green,
        blue = blue + other.blue,
        alpha = alpha + other.alpha
    )

    operator fun minus(other: ColorARGB) = ColorARGB(
        red = red - other.red,
        green = green - other.green,
        blue = blue - other.blue,
        alpha = alpha - other.alpha
    )

    operator fun times(other: ColorARGB) = ColorARGB(
        red = red * other.red,
        green = green * other.green,
        blue = blue * other.blue,
        alpha = alpha * other.alpha
    )


    fun toInt() = hex


    companion object {

        val Black = ColorARGB(Color.BLACK)

        val White = ColorARGB(Color.WHITE)

        val Red = ColorARGB(Color.RED)

        val Green = ColorARGB(Color.GREEN)

        val Blue = ColorARGB(Color.BLUE)

        val Transparent = ColorARGB(Color.TRANSPARENT)

    }

}


fun Int.toColorARGB() = ColorARGB(this)
