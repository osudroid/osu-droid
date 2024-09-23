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
        it.setToDefault()
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
        it.setToDefault()
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
        it.setToDefault()
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
        it.setToDefault()
        it.type = SEQUENCE
        it.modifiers = modifiers
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun parallel(listener: IModifierListener<IEntity>? = null, vararg modifiers: UniversalModifier) = pool.obtain().also {
        it.setToDefault()
        it.type = PARALLEL
        it.modifiers = modifiers
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun delay(duration: Float, listener: IModifierListener<IEntity>? = null) = pool.obtain().also {
        it.setToDefault()
        it.type = NONE
        it.duration = duration
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun translateX(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.setToDefault()
        it.type = TRANSLATE_X
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun translateY(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.setToDefault()
        it.type = TRANSLATE_Y
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun move(duration: Float, fromX: Float, toX: Float, fromY: Float, toY: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.setToDefault()
        it.type = MOVE
        it.duration = duration
        it.values = floatArrayOf(fromX, toX, fromY, toY)
        it.easeFunction = easeFunction
        it.listener = listener
    }

    @JvmStatic
    @JvmOverloads
    fun rotation(duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.setToDefault()
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

        it.setToDefault()
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
class UniversalModifier @JvmOverloads constructor(private val pool: Pool<UniversalModifier>? = null) : IModifier<IEntity>, IEntityModifier {


    @JvmOverloads
    constructor(type: ModifierType, duration: Float, from: Float, to: Float, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) : this(null) {
        this.type = type
        this.duration = duration
        this.values = floatArrayOf(from, to)
        this.listener = listener
        this.easeFunction = easeFunction
    }

    @JvmOverloads
    constructor(type: ModifierType, duration: Float, values: FloatArray, listener: IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = DefaultEaseFunction) : this(null) {
        this.type = type
        this.duration = duration
        this.values = values
        this.listener = listener
        this.easeFunction = easeFunction
    }

    @JvmOverloads
    constructor(type: ModifierType, listener: IModifierListener<IEntity>? = null, vararg modifiers: UniversalModifier) : this(null) {
        this.type = type
        this.listener = listener
        this.modifiers = modifiers
    }


    /**
     * The type of the modifier.
     * @see ModifierType
     */
    var type = NONE
        set(value) {
            if (field != value) {

                if (value != SEQUENCE && value != PARALLEL) {
                    clearNestedModifiers()
                }

                field = value

                values = null
                duration = getDuration()
            }
        }

    /**
     * The modifier listener.
     * @see IModifierListener
     */
    var listener: IModifierListener<IEntity>? = null

    /**
     * Inner modifiers for [SEQUENCE] or [PARALLEL] modifier types.
     */
    var modifiers: Array<out UniversalModifier>? = null
        set(value) {

            if (value != null && type != SEQUENCE && type != PARALLEL) {
                Log.w("UniversalModifier", "Inner modifiers can only be set for sequence or parallel modifiers, ignoring.")
                return
            }

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


    private var removeWhenFinished = true

    private var elapsedSec = 0f

    private var duration = 0f


    private fun clearNestedModifiers() {
        modifiers?.forEach { it.onUnregister() }
        modifiers = null
    }

    private fun getValueAt(dataIndex: Int, percentage: Float): Float {

        // 2 = Data span size.
        // 0 = "From" value in the span.
        // 1 = "To" value in the span.

        val from = values?.get(2 * dataIndex) ?: 0f
        val to = values?.get(2 * dataIndex + 1) ?: 0f

        return from + (to - from) * percentage
    }


    override fun onUpdate(deltaSec: Float, item: IEntity): Float {

        if (isFinished) {
            return 0f
        }

        var usedSec = 0f

        if (type == SEQUENCE || type == PARALLEL) {

            var remainingSec = deltaSec

            while (remainingSec > 0) {

                var isCurrentModifierFinished = false

                for (modifier in modifiers!!) {

                    if (modifier.isFinished) {
                        continue
                    }

                    if (type == SEQUENCE) {
                        remainingSec -= modifier.onUpdate(remainingSec, item)

                        isCurrentModifierFinished = modifier.isFinished()
                        break
                    }

                    usedSec = max(usedSec, modifier.onUpdate(deltaSec, item))
                }

                if (type == SEQUENCE && isCurrentModifierFinished) {
                    break
                }

                remainingSec -= usedSec
            }

            usedSec = deltaSec - remainingSec

        } else {
            usedSec = min(duration - elapsedSec, deltaSec)

            val percentage = easeFunction.getPercentage(elapsedSec + usedSec, duration)

            when (type) {

                ALPHA -> {
                    item.alpha = getValueAt(0, percentage)
                }

                SCALE -> {
                    val value = getValueAt(0, percentage)
                    item.scaleX = value
                    item.scaleY = value
                }

                MOVE -> {
                    item.setPosition(getValueAt(0, percentage), getValueAt(1, percentage))
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

                else -> Unit
            }
        }

        elapsedSec += usedSec

        if (isFinished) {
            elapsedSec = duration

            listener?.onModifierFinished(this, item)
        }

        return usedSec
    }

    override fun onUnregister() {
        setToDefault()
        pool?.free(this)
    }


    /**
     * Resets the modifier to its initial state.
     */
    override fun reset() {
        elapsedSec = 0f
        modifiers?.forEach { it.reset() }
    }

    /**
     * Sets the modifier to its default state.
     *
     * This will remove all the inner modifiers, the listener, reset the elapsed time, and set the type to [NONE].
     */
    fun setToDefault() {

        type = NONE
        values = null
        listener = null

        clearNestedModifiers()
        reset()
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
    override fun getDuration(): Float  = when(type) {

        SEQUENCE -> modifiers?.sumOf { it.duration } ?: 0f
        PARALLEL -> modifiers?.maxOf { it.duration } ?: 0f

        else -> duration
    }


    override fun getSecondsElapsed(): Float {
        return elapsedSec
    }


    override fun isFinished(): Boolean {

        if (type == SEQUENCE || type == PARALLEL) {

            if (modifiers == null) {
                return true
            }

            return modifiers!!.all { it.isFinished }
        }

        return elapsedSec >= duration
    }

    override fun isRemoveWhenFinished(): Boolean {
        return removeWhenFinished
    }

    override fun setRemoveWhenFinished(value: Boolean) {
        removeWhenFinished = value
    }


    override fun addModifierListener(listener: IModifierListener<IEntity>) {
        throw UnsupportedOperationException("Multiple entity modifiers are not allowed, consider using `setListener()` instead.")
    }

    override fun removeModifierListener(listener: IModifierListener<IEntity>): Boolean {
        throw UnsupportedOperationException("Multiple entity modifiers are not allowed, consider using `setListener()` instead.")
    }


    override fun deepCopy(): UniversalModifier = UniversalModifier(pool).also { modifier ->
        modifier.type = type
        modifier.duration = duration
        modifier.listener = listener
        modifier.easeFunction = easeFunction
        modifier.values = values?.copyOf()
        modifier.modifiers = modifiers?.map { it.deepCopy() }?.toTypedArray()
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
    MOVE,

    /**
     * Modifies the entity's translation in both axis.
     *
     * Note: This is only available for [ExtendedEntity] instances.
     */
    TRANSLATE,

    /**
     * Modifies the entity's X translation.
     *
     * Note: This is only available for [ExtendedEntity] instances.
     */
    TRANSLATE_X,

    /**
     * Modifies the entity's Y translation.
     *
     * Note: This is only available for [ExtendedEntity] instances.
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