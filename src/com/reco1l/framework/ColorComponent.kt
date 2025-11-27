package com.reco1l.framework

import android.graphics.Color
import kotlin.math.*

data class Color4(private val hex: Long) {

    constructor() : this(0xFF000000)
    constructor(hex: Int): this(hex.toLong())

    @JvmOverloads
    constructor(red: Int, green: Int, blue: Int, alpha: Int = 255): this((alpha shl 24) or (red shl 16) or (green shl 8) or blue)

    @JvmOverloads
    constructor(red: Float, green: Float, blue: Float, alpha: Float = 1f) : this(
        // Color.argb() for 0-1 range values requires API level 26 and above so we inline the function.
        ((alpha * 255f + 0.5f).toInt() shl 24) or ((red * 255f + 0.5f).toInt() shl 16) or ((green * 255f + 0.5f).toInt() shl 8) or (blue * 255f + 0.5f).toInt()
    )

    @Throws(NumberFormatException::class)
    constructor(value: String, composition: HexComposition = HexComposition.RRGGBB) : this(when (composition) {
        HexComposition.AARRGGBB -> value.removePrefix("#").toLong(16).toInt()
        HexComposition.RRGGBBAA -> value.removePrefix("#").toLong(16).shl(8).toInt()
        HexComposition.RRGGBB -> value.removePrefix("#").toLong(16).or(0xFF000000).toInt()
    })


    val alphaInt
        get() = ((hex ushr 24) and 0xFF).toInt()

    val redInt
        get() = ((hex shr 16) and 0xFF).toInt()

    val greenInt
        get() = ((hex shr 8) and 0xFF).toInt()

    val blueInt
        get() = (hex and 0xFF).toInt()


    val alpha
        get() = alphaInt / 255f

    val red
        get() = redInt / 255f

    val green
        get() = greenInt / 255f

    val blue
        get() = blueInt / 255f

    /**
     * Brigthens or darkens the color by multiplying each RGB component by the given
     * scalar value. Alpha remains unchanged.
     */
    operator fun times(scalar: Float) = Color4(
        red = red * scalar,
        green = green * scalar,
        blue = blue * scalar,
        alpha = alpha
    )

    /**
     * Replaces the color alpha with the given scalar value.
     * Similar to [Tailwind CSS opacity utilities](https://tailwindcss.com/docs/opacity).
     */
    operator fun div(scalar: Float) = Color4(
        red = red,
        green = green,
        blue = blue,
        alpha = scalar
    )


    /**
     * Lightens the color by the given factor.
     */
    fun lighteen(factor: Float): Color4 {
        val factor = max(1f, 1f + factor)
        return Color4(
            (red * factor).coerceIn(0f, 1f),
            (green * factor).coerceIn(0f, 1f),
            (blue * factor).coerceIn(0f, 1f),
            alpha
        )
    }

    /**
     * Darkens the color by the given factor.
     */
    fun darken(factor: Float): Color4 {
        val factor = max(1f, 1f + factor)
        return Color4(
            (red / factor).coerceIn(0f, 1f),
            (green / factor).coerceIn(0f, 1f),
            (blue / factor).coerceIn(0f, 1f),
            alpha
        )
    }


    fun copy(
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue,
        alpha: Float = this.alpha,
    ) = Color4(red, green, blue, alpha)


    fun toInt() = hex.toInt()


    companion object {
        val Black = Color4(Color.BLACK)
        val White = Color4(Color.WHITE)
        val Red = Color4(Color.RED)
        val Green = Color4(Color.GREEN)
        val Blue = Color4(Color.BLUE)
        val Transparent = Color4(Color.TRANSPARENT)
    }
}

enum class HexComposition {
    AARRGGBB,
    RRGGBBAA,
    RRGGBB,
}

fun Int.toColor4() = Color4(this)

fun Long.toColor4() = Color4(this)
