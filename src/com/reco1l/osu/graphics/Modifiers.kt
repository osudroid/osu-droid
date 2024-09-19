package com.reco1l.osu.graphics

import android.util.*
import com.reco1l.framework.Pool
import com.reco1l.osu.graphics.ModifierType.*
import com.reco1l.toolkt.kotlin.sumOf
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.util.modifier.IModifier
import org.anddev.andengine.util.modifier.IModifier.*
import org.anddev.andengine.util.modifier.ease.EaseSineInOut
import org.anddev.andengine.util.modifier.ease.EaseSineOut
import org.anddev.andengine.util.modifier.ease.IEaseFunction
import kotlin.math.max
import kotlin.math.min
import org.anddev.andengine.util.modifier.ease.EaseSineIn
import org.anddev.andengine.util.modifier.ease.IEaseFunction.DEFAULT as DefaultEaseFunction

/**
 * A collection of static methods to create different types of modifiers.
 * @see UniversalModifier
 * @see ModifierType
 * @author Reco1l
 */
object Modifiers {

    private val pool = Pool(10, 50, ::UniversalModifier)


    @JvmStatic
    @JvmOverloads
    fun alpha(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.reset()
        it.type = ALPHA
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.listener = listener
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
        it.reset()
        it.type = SCALE
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.listener = listener
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
        it.reset()
        it.type = RGB
        it.duration = duration
        it.listener = listener
        it.easeFunction = easeFunction
        it.values = floatArrayOf(
            fromRed, toRed,
            fromGreen, toGreen,
            fromBlue, toBlue
        )
    }

    @JvmStatic
    @JvmOverloads
    fun sequence(listener: IModifierListener<IEntity>? = null, vararg modifiers: UniversalModifier) = pool.obtain().also {
        it.reset()
        it.type = SEQUENCE
        it.modifiers = arrayOf(*modifiers)
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun parallel(listener: IModifierListener<IEntity>? = null, vararg modifiers: UniversalModifier) = pool.obtain().also {
        it.reset()
        it.type = PARALLEL
        it.modifiers = arrayOf(*modifiers)
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun delay(duration: Float, listener: IModifierListener<IEntity>? = null) = pool.obtain().also {
        it.reset()
        it.type = NONE
        it.duration = duration
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun translateX(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.reset()
        it.type = TRANSLATE_X
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun translateY(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.reset()
        it.type = TRANSLATE_Y
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun rotation(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.reset()
        it.type = ROTATION
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun shakeHorizontal(duration: Float, magnitude: Float, listener: IModifierListener<IEntity>? = null) = pool.obtain().also {

        // Based on osu!lazer's shake effect: https://github.com/ppy/osu/blob/5341a335a6165ceef4d91e910fa2ea5aecbfd025/osu.Game/Extensions/DrawableExtensions.cs#L19-L37

        it.reset()
        it.type = SEQUENCE
        it.modifiers = arrayOf(
            translateX(duration / 8f,  0f,          magnitude,   easeFunction = EaseSineOut.getInstance()),
            translateX(duration / 4f,  magnitude,   -magnitude,  easeFunction = EaseSineInOut.getInstance()),
            translateX(duration / 4f,  -magnitude,  magnitude,   easeFunction = EaseSineInOut.getInstance()),
            translateX(duration / 4f,  magnitude,   -magnitude,  easeFunction = EaseSineInOut.getInstance()),
            translateX(duration / 8f,  -magnitude,  0f,          easeFunction = EaseSineIn.getInstance()),
        )
        it.listener = listener

    }

    @JvmStatic
    fun free(modifier: UniversalModifier) = pool.free(modifier)

    @JvmStatic
    fun clearPool() = pool.clear()

}


/**
 * A universal modifier is a modifier that can be used to apply different types of modifications to
 * an entity. The main reason for this class is to be able to recycle it by using a single pool and
 * just changing the type.
 *
 * @see ModifierType
 * @author Reco1l
 */
class UniversalModifier(private val pool: Pool<UniversalModifier>? = null) : IModifier<IEntity>, IEntityModifier {

    /**
     * The type of the modifier.
     * @see ModifierType
     */
    var type = NONE
        set(value) {
            if (field == value) {
                return
            }

            if (value != SEQUENCE && value != PARALLEL) {
                clearModifiers()
            }

            if (type == SEQUENCE || type == PARALLEL || type == NONE) {
                values = null
            }

            field = value
            duration = getDuration()
        }

    /**
     * The modifier listener.
     * @see IModifierListener
     */
    var listener: IModifierListener<IEntity>? = null

    /**
     * Inner modifiers for [SEQUENCE] or [PARALLEL] modifier types.
     */
    var modifiers: Array<UniversalModifier?>? = null
        set(value) {
            field = value
            duration = getDuration()
        }

    /**
     * Easing function to be used.
     */
    var easeFunction: IEaseFunction = DefaultEaseFunction

    /**
     * An array of values to be used in the modifier.
     *
     * The values are stored in an array of spans of 2 elements where the first element is the `from` value and the second is the
     * `to` values, the amount of spans needed depends on the type of the modifier.
     *
     * As an example, the `MOVE` modifier needs 2 value spans, one for the X axis and one for the Y axis. Meanwhile, the `RGB`
     * modifier needs 3 value spans, one per each color channel.
     *
     * Disposition of the values:
     * * [TRANSLATE] -> [xFrom, xTo, yFrom, yTo]
     * * [RGB] -> [redFrom, redTo, greenFrom, greenTo, blueFrom, blueTo]
     * * [SCALE] and [ALPHA] -> [scaleFrom, scaleTo]
     */
    var values: FloatArray? = null


    private var elapsedSec = 0f

    private var duration = 0f


    private fun getValueAt(dataIndex: Int, percentage: Float): Float {

        // 2 = Data span size.
        // 0 = "From" value in the span.
        // 1 = "To" value in the span.

        val from = values?.get(2 * dataIndex) ?: 0f
        val to = values?.get(2 * dataIndex + 1) ?: 0f

        return from + (to - from) * percentage
    }

    private fun clearModifiers() {
        modifiers?.forEach { it?.onUnregister() }
        modifiers = null
    }


    override fun onUpdate(deltaSec: Float, item: IEntity): Float {

        if (isFinished) {
            return 0f
        }

        var usedSec = min(duration - elapsedSec, deltaSec)

        val percentage = if (duration > 0) easeFunction.getPercentage(elapsedSec + usedSec, duration) else 1f

        when (type) {

            ALPHA -> {
                item.alpha = getValueAt(0, percentage)
            }

            SCALE -> {
                item.setScale(getValueAt(0, percentage))
            }

            TRANSLATE -> {
                if (item is ExtendedEntity) {
                    item.translationX = getValueAt(0, percentage)
                    item.translationY = getValueAt(1, percentage)
                }
            }

            TRANSLATE_X -> {
                if (item is ExtendedEntity) {
                    item.translationX = getValueAt(0, percentage)
                }
            }

            TRANSLATE_Y -> {
                if (item is ExtendedEntity) {
                    item.translationY = getValueAt(0, percentage)
                }
            }

            ROTATION -> {
                item.rotation = getValueAt(0, percentage)
            }

            RGB -> {
                item.setColor(
                    getValueAt(0, percentage),
                    getValueAt(1, percentage),
                    getValueAt(2, percentage)
                )
            }

            SEQUENCE -> {
                var remainingSec = deltaSec

                while (remainingSec > 0) {

                    var isCurrentModifierFinished = false

                    for (i in 0 until modifiers!!.size) {

                        val modifier = modifiers!![i] ?: continue

                        remainingSec -= modifier.onUpdate(remainingSec, item)

                        if (modifier.isFinished) {
                            modifier.onUnregister()
                            modifiers!![i] = null
                            isCurrentModifierFinished = true
                        }
                        break
                    }

                    if (isCurrentModifierFinished) {
                        break
                    }
                }

                usedSec = deltaSec - remainingSec
            }

            PARALLEL -> {
                var remainingSec = deltaSec

                while (remainingSec > 0) {

                    for (i in 0 until modifiers!!.size) {

                        val modifier = modifiers!![i] ?: continue

                        usedSec = max(usedSec, modifier.onUpdate(deltaSec, item))

                        if (modifier.isFinished) {
                            modifiers!![i] = null
                            modifier.onUnregister()
                        }
                    }

                    remainingSec -= usedSec
                }

                usedSec = deltaSec - remainingSec
            }

            NONE -> {
                // Works as delay modifier that can be inserted in a sequence modifier.
            }
        }

        elapsedSec += usedSec

        if (isFinished) {
            elapsedSec = duration

            listener?.onModifierFinished(this, item)
            listener = null
        }

        return usedSec
    }

    override fun onUnregister() {
        reset()
        pool?.free(this)
    }

    override fun reset() {

        type = NONE
        values = null
        duration = 0f
        elapsedSec = 0f
        listener = null
        easeFunction = DefaultEaseFunction

        clearModifiers()
    }


    /**
     * Sets the duration of the modifier.
     *
     * Note: If the modifier is a [SEQUENCE] or [PARALLEL] modifier, this method will do nothing.
     */
    fun setDuration(value: Float) {

        if (type == SEQUENCE || type == PARALLEL) {
            Log.w("UniversalModifier", "Cannot set duration for sequence or parallel modifiers, ignoring.")
            return
        }

        duration = value
    }

    /**
     * Returns the duration of the modifier.
     *
     * When the modifier is a [SEQUENCE] modifier, the duration is the sum of the inner modifiers' durations, meanwhile, when it is a [PARALLEL] modifier, it is the maximum duration of the
     * inner modifiers. Otherwise, it is the duration of the modifier itself.
     */
    override fun getDuration() = when(type) {

        SEQUENCE -> modifiers?.sumOf { it?.duration ?: 0f } ?: 0f

        PARALLEL -> modifiers?.maxOf { it?.duration ?: 0f } ?: 0f

        else -> duration
    }

    override fun getSecondsElapsed(): Float {
        return elapsedSec
    }

    /**
     * Returns whether the modifier is finished or not.
     *
     * When the modifier is a [SEQUENCE] or [PARALLEL] modifier, it is finished when all the inner modifiers are finished.
     */
    override fun isFinished() = when (type) {

        SEQUENCE, PARALLEL -> modifiers?.all { it == null } ?: true

        else -> elapsedSec >= duration
    }


    override fun isRemoveWhenFinished(): Boolean {
        return true
    }

    override fun setRemoveWhenFinished(value: Boolean) {
        throw UnsupportedOperationException("Cannot set remove when finished for UniversalModifier.")
    }


    override fun addModifierListener(listener: IModifierListener<IEntity>) {
        Log.w("UniversalModifier", "Multiple entity modifiers are not allowed, overwriting previous listener.")
        this.listener = listener
    }

    override fun removeModifierListener(listener: IModifierListener<IEntity>): Boolean {

        if (listener == this.listener) {
            this.listener = null
            return true
        }

        Log.w("UniversalModifier", "The listener to remove is not the current listener.")
        return false
    }


    override fun deepCopy(): UniversalModifier = UniversalModifier(pool).also { modifier ->
        modifier.type = type
        modifier.duration = duration
        modifier.listener = listener
        modifier.easeFunction = easeFunction
        modifier.values = values?.copyOf()
        modifier.modifiers = modifiers?.mapNotNull { it?.deepCopy() }?.toTypedArray()
    }
}


/**
 * The type of the modifier.
 * @see Modifiers
 */
enum class ModifierType {

    /**
     * Modifies the entity's alpha value.
     */
    ALPHA,

    /**
     * Modifies the entity's scale values for both axis.
     */
    SCALE,

    /**
     * Modifies the entity's color values.
     */
    RGB,

    /**
     * Modifies the entity's translation in both axis.
     */
    TRANSLATE,

    /**
     * Modifies the entity's X translation.
     */
    TRANSLATE_X,

    /**
     * Modifies the entity's Y translation.
     */
    TRANSLATE_Y,

    /**
     * Modifies the entity's rotation.
     */
    ROTATION,

    /**
     * Modifies the entity's with inner modifiers in sequence.
     */
    SEQUENCE,

    /**
     * Modifies the entity's with inner modifiers in parallel.
     */
    PARALLEL,

    /**
     * Does nothing, used as a delay modifier.
     */
    NONE
}