package com.reco1l.osu

import com.reco1l.andengine.modifier.*
import com.reco1l.framework.Pool
import com.reco1l.andengine.modifier.ModifierType.*
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.util.modifier.IModifier.*
import org.anddev.andengine.util.modifier.ease.EaseSineInOut
import org.anddev.andengine.util.modifier.ease.EaseSineOut
import org.anddev.andengine.util.modifier.ease.IEaseFunction
import org.anddev.andengine.util.modifier.ease.EaseSineIn
import org.anddev.andengine.util.modifier.ease.IEaseFunction.DEFAULT as DefaultEaseFunction

/**
 * A collection of static methods to create different types of modifiers.
 * @see UniversalModifier
 * @see ModifierType
 * @author Reco1l
 */
object Modifiers {

    @JvmStatic
    val pool = Pool(50, ::UniversalModifier)


    @JvmStatic
    @JvmOverloads
    fun alpha(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.setToDefault()
        it.type = ALPHA
        it.duration = duration
        it.values = SpanArray(1).apply { this[from, to] = 0 }
        it.easeFunction = easeFunction
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }
    }

    @JvmStatic
    @JvmOverloads
    fun fadeIn(duration: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = alpha(duration, 0f, 1f, listener, easeFunction)

    @JvmStatic
    @JvmOverloads
    fun fadeOut(duration: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = alpha(duration, 1f, 0f, listener, easeFunction)

    @JvmStatic
    @JvmOverloads
    fun scale(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.setToDefault()
        it.type = SCALE
        it.duration = duration
        it.values = SpanArray(2).apply {
            this[from, to] = 0
            this[from, to] = 1
        }
        it.easeFunction = easeFunction
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }
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
        listener: IModifierListener<IEntity>? = null,
        easeFunction: IEaseFunction = DefaultEaseFunction
    ) = pool.obtain().also {
        it.setToDefault()
        it.type = RGB
        it.duration = duration
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }

        it.easeFunction = easeFunction
        it.values = SpanArray(3).apply {
            this[fromRed, toRed] = 0
            this[fromGreen, toGreen] = 1
            this[fromBlue, toBlue] = 2
        }
    }

    @JvmStatic
    @JvmOverloads
    fun sequence(listener: IModifierListener<IEntity>? = null, vararg modifiers: UniversalModifier) = pool.obtain().also {
        it.setToDefault()
        it.type = SEQUENCE
        it.modifiers = arrayOf(*modifiers)
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }

    }

    @JvmStatic
    @JvmOverloads
    fun parallel(listener: IModifierListener<IEntity>? = null, vararg modifiers: UniversalModifier) = pool.obtain().also {
        it.setToDefault()
        it.type = PARALLEL
        it.modifiers = arrayOf(*modifiers)
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }

    }

    @JvmStatic
    @JvmOverloads
    fun delay(duration: Float, listener: IModifierListener<IEntity>? = null) = pool.obtain().also {
        it.setToDefault()
        it.type = NONE
        it.duration = duration
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }

    }

    @JvmStatic
    @JvmOverloads
    fun translateX(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.setToDefault()
        it.type = TRANSLATE_X
        it.duration = duration
        it.values = SpanArray(1).apply { this[from, to] = 0 }
        it.easeFunction = easeFunction
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }

    }

    @JvmStatic
    @JvmOverloads
    fun translateY(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.setToDefault()
        it.type = TRANSLATE_Y
        it.duration = duration
        it.values = SpanArray(1).apply { this[from, to] = 0 }
        it.easeFunction = easeFunction
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }

    }

    @JvmStatic
    @JvmOverloads
    fun move(duration: Float, fromX: Float, toX: Float, fromY: Float, toY: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) =
        pool.obtain().also {
            it.setToDefault()
            it.type = MOVE
            it.duration = duration
            it.values = SpanArray(2).apply {
                this[fromX, toX] = 0
                this[fromY, toY] = 1
            }
            it.easeFunction = easeFunction
            it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }

        }

    @JvmStatic
    @JvmOverloads
    fun rotation(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.setToDefault()
        it.type = ROTATION
        it.duration = duration
        it.values = SpanArray(1).apply { this[from, to] = 0 }
        it.easeFunction = easeFunction
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }

    }

    @JvmStatic
    @JvmOverloads
    fun shakeHorizontal(duration: Float, magnitude: Float, listener: IModifierListener<IEntity>? = null) = pool.obtain().also {

        // Based on osu!lazer's shake effect: https://github.com/ppy/osu/blob/5341a335a6165ceef4d91e910fa2ea5aecbfd025/osu.Game/Extensions/DrawableExtensions.cs#L19-L37

        it.setToDefault()
        it.type = SEQUENCE
        it.modifiers = arrayOf(
            translateX(duration / 8f, 0f, magnitude, easeFunction = EaseSineOut.getInstance()),
            translateX(duration / 4f, magnitude, -magnitude, easeFunction = EaseSineInOut.getInstance()),
            translateX(duration / 4f, -magnitude, magnitude, easeFunction = EaseSineInOut.getInstance()),
            translateX(duration / 4f, magnitude, -magnitude, easeFunction = EaseSineInOut.getInstance()),
            translateX(duration / 8f, -magnitude, 0f, easeFunction = EaseSineIn.getInstance()),
        )
        it.onFinished = OnModifierFinished { e -> listener?.onModifierFinished(null, e) }


    }

}
