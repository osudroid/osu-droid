@file:Suppress("ConstPropertyName")

package com.reco1l.andengine.theme

/**
 * The root font size in scene units. Sizes expressed in [rem] and [srem] are multiples of this value.
 *
 * The GLES branch lays out the UI in fixed scene coordinates, so this is a constant rather than a
 * density-dependent value.
 */
const val ROOT_FONT_SIZE = 16f

/**
 * Indicates that this float value is in "rem" units (root em).
 * It will be converted to scene units by multiplying it with [ROOT_FONT_SIZE].
 */
val Float.rem: Float
    get() = this * ROOT_FONT_SIZE

/**
 * Indicates that this int value is in "rem" units (root em).
 * It will be converted to scene units by multiplying it with [ROOT_FONT_SIZE].
 */
val Int.rem: Float
    get() = this.toFloat().rem

/**
 * Spaced rem units. Based on Tailwind CSS spacing used for margin, padding, gap, etc:
 * * gap-1 = 0.25rem
 * * gap-2 = 0.5rem
 * * gap-3 = 0.75rem
 * * gap-4 = 1rem
 */
val Float.srem: Float
    get() = this * 0.25f.rem

/**
 * Spaced rem units, see [Float.srem].
 */
val Int.srem: Float
    get() = this.toFloat().srem


object Radius {
    val XS
        get() = 0.425f.rem

    val SM
        get() = 0.45f.rem

    val MD
        get() = 0.475f.rem

    val LG
        get() = 0.5f.rem

    val XL
        get() = 0.525f.rem

    const val Full = Float.MAX_VALUE
}
