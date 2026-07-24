package com.reco1l.framework.android

import android.content.res.Resources
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewOutlineProvider
import android.widget.TextView
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import kotlin.math.min


/**
 * Ensures that the view will always have layout params, if not a new one with [WRAP_CONTENT] as
 * both width and height dimensions is set.
 */
fun View.ensureLayoutParams() {
    if (layoutParams == null) {
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }
}


/**
 * Changes the view width.
 */
var View.layoutWidth
    get() = width
    set(value) {
        ensureLayoutParams()
        updateLayoutParams { width = value }
    }

/**
 * Changes the view height.
 */
var View.layoutHeight
    get() = height
    set(value) {
        ensureLayoutParams()
        updateLayoutParams { height = value }
    }



/**
 * Converts a [Float] value to pixels using [TypedValue.applyDimension].
 *
 * @see COMPLEX_UNIT_DIP
 */
val Float.dp
    get() = TypedValue.applyDimension(COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)

/**
 * Converts an [Int] value to pixels using [TypedValue.applyDimension].
 *
 * @see COMPLEX_UNIT_DIP
 */
val Int.dp
    get() = TypedValue.applyDimension(COMPLEX_UNIT_DIP, toFloat(), Resources.getSystem().displayMetrics).toInt()



/**
 * Returns the [MarginLayoutParams] of this view. If the view does not have a [MarginLayoutParams]
 * a new one is created (inherithing dimensions from the current layout params) and set to this view.
 */
val View.marginLayoutParams
    get() = layoutParams as? MarginLayoutParams ?: MarginLayoutParams(layoutParams)


var View.topMargin
    get() = marginTop
    set(value) {
        marginLayoutParams.topMargin = value
    }

var View.bottomMargin
    get() = marginBottom
    set(value) {
        marginLayoutParams.bottomMargin = value
    }

var View.leftMargin
    get() = marginLeft
    set(value) {
        marginLayoutParams.leftMargin = value
    }

var View.rightMargin
    get() = marginRight
    set(value) {
        marginLayoutParams.rightMargin = value
    }


var TextView.drawableLeft: Drawable?
    get() = compoundDrawables[0]
    set(value) {
        setCompoundDrawablesWithIntrinsicBounds(value, compoundDrawables[1], compoundDrawables[2], compoundDrawables[3])
    }

var TextView.drawableTop: Drawable?
    get() = compoundDrawables[1]
    set(value) {
        setCompoundDrawablesWithIntrinsicBounds(compoundDrawables[0], value, compoundDrawables[2], compoundDrawables[3])
    }

var TextView.drawableRight: Drawable?
    get() = compoundDrawables[2]
    set(value) {
        setCompoundDrawablesWithIntrinsicBounds(compoundDrawables[0], compoundDrawables[1], value, compoundDrawables[3])
    }

var TextView.drawableBottom: Drawable?
    get() = compoundDrawables[3]
    set(value) {
        setCompoundDrawablesWithIntrinsicBounds(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2], value)
    }


/**
 * The view corner radius.
 * Internally uses a custom [ViewOutlineProvider] and it'll replace any previously set.
 *
 * @see RoundOutlineProvider
 */
var View.cornerRadius: Float
    get() = (outlineProvider as? RoundOutlineProvider)?.radius ?: 0f
    set(value) {
        val provider = outlineProvider as? RoundOutlineProvider ?: RoundOutlineProvider().also { outlineProvider = it }
        provider.radius = value
        invalidateOutline()
    }


/**
 * Applies a round outline to the view.
 */
class RoundOutlineProvider : ViewOutlineProvider() {

    /**
     * The corner radius.
     *
     * Note: You must [invalidate][View.invalidateOutline] the view outline in order to take effect on this.
     */
    var radius = 0f


    override fun getOutline(view: View, outline: Outline) {

        // Clipping view to outline, without this the rounding will not take effect.
        view.clipToOutline = true

        // This is a workaround for older devices without Skia support where if the radius is greater
        // than any of the view bounds it'll cause an unexpected visual.
        val radius = radius.coerceIn(0f, min(view.width, view.height) / 2f)

        // Applying corner radius to outline.
        outline.setRoundRect(0, 0, view.width, view.height, radius)
    }

}
