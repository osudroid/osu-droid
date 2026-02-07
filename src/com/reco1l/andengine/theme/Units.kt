@file:Suppress("ConstPropertyName")

package com.reco1l.andengine.theme

import com.reco1l.andengine.UIEngine


/**
 * Indicates that this float value is in "rem" units (root em).
 * It will be converted to pixels by multiplying it with the root font size.
 */
val Float.rem: Float
    get() = this * UIEngine.Companion.current.rootFontSize

/**
 * Indicates that this int value is in "rem" units (root em).
 * It will be converted to pixels by multiplying it with the root font size.
 */
val Int.rem: Float
    get() = this.toFloat().rem

/**
 * Spaced rem units. Based on Tailwind CSS spacing used for margin, padding, gap, etc:
 * * gap-1 = 0.25rem
 * * gap-2 = 0.5rem
 * * gap-3 = 0.75rem
 * * gap-4 = 1rem
 *
 * [See documentation](https://tailwindcss.com/docs/theme#default-theme-variable-reference)
 */
val Float.srem: Float
    get() = this * 0.25f.rem

/**
 * Spaced rem units. Based on Tailwind CSS spacing used for margin, padding, gap, etc:
 * * gap-1 = 0.25rem
 * * gap-2 = 0.5rem
 * * gap-3 = 0.75rem
 * * gap-4 = 1rem
 *
 * [See documentation](https://tailwindcss.com/docs/theme#default-theme-variable-reference)
 */
val Int.srem: Float
    get() = this.toFloat().srem

/**
 * Indicates that this float value is in percentage units.
 * It will be converted to a value between -3 and -2.
 */
val Float.pct: Float
    get() = Size.relativeSizeRange.start + this.coerceAtLeast(0f).coerceAtMost(1f)

/**
 * Indicates that this float value is in "vw" units (viewport width).
 * It will be converted to pixels by multiplying it with the surface width.
 */
val Float.vw: Float
    get() = this * UIEngine.Companion.current.surfaceWidth

/**
 * Indicates that this float value is in "vh" units (viewport height).
 * It will be converted to pixels by multiplying it with the surface height.
 */
val Float.vh: Float
    get() = this * UIEngine.Companion.current.surfaceHeight


object Size {

    /**
     * Indicates that the size should be determined automatically.
     */
    const val Auto = -1f

    /**
     * Indicates that the size should take all the available space.
     */
    const val Full = -2f


    /**
     * The range for relative size units.
     */
    val relativeSizeRange = -3f..-2f

}