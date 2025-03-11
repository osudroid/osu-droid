package com.reco1l.andengine

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.modifier.ModifierType.*
import com.reco1l.andengine.modifier.UniversalModifier.Companion.GlobalPool

/**
 * A collection of static methods to create different types of modifiers.
 * @see UniversalModifier
 * @see ModifierType
 * @author Reco1l
 */
@Deprecated(message = "Use ExtendedEntity integrated functions instead.")
object Modifiers {

    @JvmStatic
    @JvmOverloads
    fun alpha(duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easing: Easing = Easing.None) = GlobalPool.obtain().also {
        it.setToDefault()
        it.type = Alpha
        it.duration = duration
        it.initialValues = floatArrayOf(from)
        it.finalValues = floatArrayOf(to)
        it.onFinished = listener
        it.eased(easing)
    }

    @JvmStatic
    @JvmOverloads
    fun fadeIn(duration: Float, listener: OnModifierFinished? = null, easing: Easing = Easing.None) = alpha(duration, 0f, 1f, listener, easing)

    @JvmStatic
    @JvmOverloads
    fun fadeOut(duration: Float, listener: OnModifierFinished? = null, easing: Easing = Easing.None) = alpha(duration, 1f, 0f, listener, easing)

    @JvmStatic
    @JvmOverloads
    fun scale(duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easing: Easing = Easing.None) = GlobalPool.obtain().also {
        it.setToDefault()
        it.type = ScaleXY
        it.duration = duration
        it.initialValues = floatArrayOf(from)
        it.finalValues = floatArrayOf(to)
        it.onFinished = listener
        it.eased(easing)
    }

    @JvmStatic
    @JvmOverloads
    fun color(
        duration: Float,
        fromRed: Float,
        toRed: Float,
        fromGreen: Float,
        toGreen: Float,
        fromBlue: Float,
        toBlue: Float,
        listener: OnModifierFinished? = null,
        easing: Easing = Easing.None
    ) = GlobalPool.obtain().also {
        it.setToDefault()
        it.type = Color
        it.duration = duration
        it.onFinished = listener
        it.initialValues = floatArrayOf(fromRed, fromGreen, fromBlue)
        it.finalValues = floatArrayOf(toRed, toGreen, toBlue)
        it.eased(easing)
    }

    @JvmStatic
    @JvmOverloads
    fun sequence(listener: OnModifierFinished? = null, vararg modifiers: UniversalModifier) = GlobalPool.obtain().also {
        it.setToDefault()
        it.type = Sequence
        it.modifiers = arrayOf(*modifiers)
        it.onFinished = listener
    }

    @JvmStatic
    @JvmOverloads
    fun parallel(listener: OnModifierFinished? = null, vararg modifiers: UniversalModifier) = GlobalPool.obtain().also {
        it.setToDefault()
        it.type = Parallel
        it.modifiers = arrayOf(*modifiers)
        it.onFinished = listener
    }

    @JvmStatic
    @JvmOverloads
    fun delay(duration: Float, listener: OnModifierFinished? = null) = GlobalPool.obtain().also {
        it.setToDefault()
        it.type = Delay
        it.duration = duration
        it.onFinished = listener
    }

    @JvmStatic
    @JvmOverloads
    fun translateY(duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easing: Easing = Easing.None) = GlobalPool.obtain().also {
        it.setToDefault()
        it.type = TranslateY
        it.duration = duration
        it.initialValues = floatArrayOf(from)
        it.finalValues = floatArrayOf(to)
        it.onFinished = listener
        it.eased(easing)
    }

    @JvmStatic
    @JvmOverloads
    fun move(duration: Float, fromX: Float, toX: Float, fromY: Float, toY: Float, listener: OnModifierFinished? = null, easing: Easing = Easing.None) =
        GlobalPool.obtain().also {
            it.setToDefault()
            it.type = MoveXY
            it.duration = duration
            it.initialValues = floatArrayOf(fromX, fromY)
            it.finalValues = floatArrayOf(toX, toY)
            it.onFinished = listener
            it.eased(easing)
        }

    @JvmStatic
    @JvmOverloads
    fun rotation(duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easing: Easing = Easing.None) = GlobalPool.obtain().also {
        it.setToDefault()
        it.type = Rotation
        it.duration = duration
        it.initialValues = floatArrayOf(from)
        it.finalValues = floatArrayOf(to)
        it.onFinished = listener
        it.eased(easing)
    }

}
