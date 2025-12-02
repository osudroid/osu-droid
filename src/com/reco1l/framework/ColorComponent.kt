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

/**
 * Converts the cylindrical form of an [Oklab](https://bottosson.github.io/posts/oklab/) color space to the sRGB color space.
 *
 * @param l The perceived lightness of the color (0 to 1).
 * @param c The chroma of the color (0 to ~0.4).
 * @param h The hue angle in degrees (0 to 360).
 * @param alpha The alpha component of the color (0 to 1). Defaults to 1.
 * @return A [Color4] representing the color in sRGB.
 */
fun oklch(l: Float, c: Float, h: Float, alpha: Float = 1f): Color4 {
    // oklab
    val hRad = h * PI / 180.0
    val a = c * cos(hRad)
    val b = c * sin(hRad)

    // linear rgb
    val lm = l + 0.3963377774 * a + 0.2158037573 * b
    val mm = l - 0.1055613458 * a - 0.0638541728 * b
    val sm = l - 0.0894841775 * a - 1.2914855480 * b

    val l3 = lm * lm * lm
    val m3 = mm * mm * mm
    val s3 = sm * sm * sm

    val lr = +4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3
    val lg = -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3
    val lb = -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3

    // rgb to srgb
    fun linearToSrgb(c: Double): Float {
        return if (c <= 0.0031308) {
            (12.92 * c).toFloat()
        } else {
            (1.055 * c.pow(1.0 / 2.4) - 0.055).toFloat()
        }
    }

    val red = linearToSrgb(lr).coerceIn(0f, 1f)
    val green = linearToSrgb(lg).coerceIn(0f, 1f)
    val blue = linearToSrgb(lb).coerceIn(0f, 1f)

    return Color4(red, green, blue, alpha.coerceIn(0f, 1f))
}

fun rgb(r: Int, g: Int, b: Int) = Color4(r, g, b)
fun rgb(r: Float, g: Float, b: Float) = Color4(r, g, b)
fun rgb(r: Int, g: Int, b: Int, a: Int) = Color4(r, g, b, a)
fun rgb(r: Float, g: Float, b: Float, a: Float) = Color4(r, g, b, a)

