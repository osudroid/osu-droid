package com.reco1l.framework

import android.graphics.*

data class ColorARGB(private val hex: Int) {

    constructor() : this(Color.BLACK)

    constructor(hex: Long): this(hex.toInt())

    constructor(other: ColorARGB) : this(other.hex)

    @JvmOverloads
    constructor(red: Int, green: Int, blue: Int, alpha: Int = 255): this(Color.argb(alpha, red, green, blue))

    @JvmOverloads
    constructor(red: Float, green: Float, blue: Float, alpha: Float = 1f) : this(
        // Color.argb() for 0-1 range values requires API level 26 and above so we inline the function.
        ((alpha * 255f + 0.5f).toInt() shl 24) or ((red * 255f + 0.5f).toInt() shl 16) or ((green * 255f + 0.5f).toInt() shl 8) or (blue * 255f + 0.5f).toInt()
    )

    constructor(value: String, composition: HexComposition = HexComposition.RRGGBB) : this(when (composition) {
        HexComposition.AARRGGBB -> value.removePrefix("#").toLong(16).toInt()
        HexComposition.RRGGBBAA -> value.removePrefix("#").toLong(16).shl(8).toInt()
        HexComposition.RRGGBB -> value.removePrefix("#").toLong(16).or(0xFF000000).toInt()
    })


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

    operator fun times(scalar: Float) = ColorARGB(
        red = red * scalar,
        green = green * scalar,
        blue = blue * scalar,
        alpha = alpha
    )


    fun copy(
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue,
        alpha: Float = this.alpha
    ) = ColorARGB(red, green, blue, alpha)


    fun toInt() = hex

    fun colorEquals(other: ColorARGB) = red == other.red && green == other.green && blue == other.blue


    companion object {

        val Black = ColorARGB(Color.BLACK)

        val White = ColorARGB(Color.WHITE)

        val Red = ColorARGB(Color.RED)

        val Green = ColorARGB(Color.GREEN)

        val Blue = ColorARGB(Color.BLUE)

        val Transparent = ColorARGB(Color.TRANSPARENT)

    }

}

enum class HexComposition {
    AARRGGBB,
    RRGGBBAA,
    RRGGBB,
}
fun Int.toColorARGB() = ColorARGB(this)

fun Long.toColorARGB() = ColorARGB(this)
