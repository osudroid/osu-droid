package com.reco1l.osu

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.modifier.*
import com.reco1l.framework.Pool
import com.reco1l.andengine.modifier.ModifierType.*

/**
 * A collection of static methods to create different types of modifiers.
 * @see UniversalModifier
 * @see ModifierType
 * @author Reco1l
 */
@Deprecated(message = "Use ExtendedEntity integrated functions instead.")
object Modifiers {

    @JvmStatic
    val pool = Pool(50, ::UniversalModifier)


    @JvmStatic
    @JvmOverloads
    fun alpha(duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easeFunction: Easing = Easing.None) = pool.obtain().also {
        it.setToDefault()
        it.type = ALPHA
        it.duration = duration
        it.easing = easeFunction
        it.onFinished = listener
        it.setValues(from, to)
    }

    @JvmStatic
    @JvmOverloads
    fun fadeIn(duration: Float, listener: OnModifierFinished? = null, easeFunction: Easing = Easing.None) = alpha(duration, 0f, 1f, listener, easeFunction)

    @JvmStatic
    @JvmOverloads
    fun fadeOut(duration: Float, listener: OnModifierFinished? = null, easeFunction: Easing = Easing.None) = alpha(duration, 1f, 0f, listener, easeFunction)

    @JvmStatic
    @JvmOverloads
    fun scale(duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easeFunction: Easing = Easing.None) = pool.obtain().also {
        it.setToDefault()
        it.type = SCALE
        it.duration = duration
        it.easing = easeFunction
        it.onFinished = listener
        it.setValues(from, to)
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
        easeFunction: Easing = Easing.None
    ) = pool.obtain().also {
        it.setToDefault()
        it.type = COLOR
        it.duration = duration
        it.onFinished = listener
        it.easing = easeFunction
        it.setValues(fromRed, toRed, fromGreen, toGreen, fromBlue, toBlue)
    }

    @JvmStatic
    @JvmOverloads
    fun sequence(listener: OnModifierFinished? = null, vararg modifiers: UniversalModifier) = pool.obtain().also {
        it.setToDefault()
        it.type = SEQUENCE
        it.modifiers = arrayOf(*modifiers)
        it.onFinished = listener
    }

    @JvmStatic
    @JvmOverloads
    fun parallel(listener: OnModifierFinished? = null, vararg modifiers: UniversalModifier) = pool.obtain().also {
        it.setToDefault()
        it.type = PARALLEL
        it.modifiers = arrayOf(*modifiers)
        it.onFinished = listener
    }

    @JvmStatic
    @JvmOverloads
    fun delay(duration: Float, listener: OnModifierFinished? = null) = pool.obtain().also {
        it.setToDefault()
        it.type = NONE
        it.duration = duration
        it.onFinished = listener
    }

    @JvmStatic
    @JvmOverloads
    fun translateX(duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easeFunction: Easing = Easing.None) = pool.obtain().also {
        it.setToDefault()
        it.type = TRANSLATE_X
        it.duration = duration
        it.easing = easeFunction
        it.onFinished = listener
        it.setValues(from, to)
    }

    @JvmStatic
    @JvmOverloads
    fun translateY(duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easeFunction: Easing = Easing.None) = pool.obtain().also {
        it.setToDefault()
        it.type = TRANSLATE_Y
        it.duration = duration
        it.easing = easeFunction
        it.onFinished = listener
        it.setValues(from, to)

    }

    @JvmStatic
    @JvmOverloads
    fun move(duration: Float, fromX: Float, toX: Float, fromY: Float, toY: Float, listener: OnModifierFinished? = null, easeFunction: Easing = Easing.None) =
        pool.obtain().also {
            it.setToDefault()
            it.type = MOVE
            it.duration = duration
            it.easing = easeFunction
            it.onFinished = listener
            it.setValues(fromX, toX, fromY, toY)
        }

    @JvmStatic
    @JvmOverloads
    fun rotation(duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easeFunction: Easing = Easing.None) = pool.obtain().also {
        it.setToDefault()
        it.type = ROTATION
        it.duration = duration
        it.easing = easeFunction
        it.onFinished = listener
        it.setValues(from, to)

    }

    @JvmStatic
    @JvmOverloads
    fun shakeHorizontal(duration: Float, magnitude: Float, listener: OnModifierFinished? = null) = pool.obtain().also {

        // Based on osu!lazer's shake effect: https://github.com/ppy/osu/blob/5341a335a6165ceef4d91e910fa2ea5aecbfd025/osu.Game/Extensions/DrawableExtensions.cs#L19-L37

        it.setToDefault()
        it.type = SEQUENCE
        it.modifiers = arrayOf(
            translateX(duration / 8f, 0f, magnitude, easeFunction = Easing.OutSine),
            translateX(duration / 4f, magnitude, -magnitude, easeFunction = Easing.InOutSine),
            translateX(duration / 4f, -magnitude, magnitude, easeFunction = Easing.InOutSine),
            translateX(duration / 4f, magnitude, -magnitude, easeFunction = Easing.InOutSine),
            translateX(duration / 8f, -magnitude, 0f, easeFunction = Easing.InSine),
        )
        it.onFinished = listener


    }

}
