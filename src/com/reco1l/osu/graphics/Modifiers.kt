package com.reco1l.osu.graphics

import com.reco1l.framework.Pool
import com.reco1l.osu.graphics.ModifierType.*
import com.reco1l.toolkt.kotlin.sumOf
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.util.modifier.IModifier
import org.anddev.andengine.util.modifier.ease.IEaseFunction
import kotlin.math.max
import kotlin.math.min

/**
 * A collection of static methods to create different types of modifiers.
 * @see UniversalModifier
 * @see ModifierType
 * @author Reco1l
 */
object Modifiers {

    private val pool = Pool(::UniversalModifier)


    @JvmStatic
    fun alpha(duration: Float, from: Float, to: Float) = pool.obtain()
        .also { it.reset() }
        .setType(ALPHA)
        .setDuration(duration)
        .setValueSpan(from, to)

    @JvmStatic
    fun fadeIn(duration: Float) = alpha(duration, 0f, 1f)

    @JvmStatic
    fun fadeOut(duration: Float) = alpha(duration, 1f, 0f)

    @JvmStatic
    fun scale(duration: Float, from: Float, to: Float) = pool.obtain()
        .also { it.reset() }
        .setType(SCALE)
        .setDuration(duration)
        .setValueSpan(from, to)

    @JvmStatic
    fun color(duration: Float, fromRed: Float, toRed: Float, fromGreen: Float, toGreen: Float, fromBlue: Float, toBlue: Float) = pool.obtain()
        .also { it.reset() }
        .setType(RGB)
        .setDuration(duration)
        .setRGBSpan(fromRed, toRed, fromGreen, toGreen, fromBlue, toBlue)

    @JvmStatic
    fun sequence(vararg modifiers: UniversalModifier) = pool.obtain()
        .also { it.reset() }
        .setType(SEQUENCE)
        .setInnerModifiers(*modifiers)

    @JvmStatic
    fun parallel(vararg modifiers: UniversalModifier) = pool.obtain()
        .also { it.reset() }
        .setType(PARALLEL)
        .setInnerModifiers(*modifiers)

    @JvmStatic
    fun delay(duration: Float) = pool.obtain()
        .also { it.reset() }
        .setType(NONE)
        .setDuration(duration)


    @JvmStatic
    fun free(modifier: UniversalModifier) = pool.free(modifier)

    @JvmStatic
    fun clearPool() = pool.clear()

}


/**
 * Callback to be called when the modifier is finished.
 * @author Reco1l
 */
fun interface OnModifierFinished {
    operator fun invoke(item: IEntity)
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


    private var elapsedSec = 0f

    private var data = FloatArray(2)

    private var _type = NONE

    private var _duration = 0f

    private var _modifiers: Array<out UniversalModifier>? = null

    private var _onFinished: OnModifierFinished? = null

    private var _easeFunction = IEaseFunction.DEFAULT


    override fun isFinished() = elapsedSec == _duration

    override fun getDuration() = _duration

    override fun getSecondsElapsed() = elapsedSec


    private fun getValueAt(dataIndex: Int, percentage: Float): Float {

        val from = data[DATA_SIZE * dataIndex + FROM_INDEX]
        val to = data[DATA_SIZE * dataIndex + TO_INDEX]

        return from + (to - from) * percentage
    }


    override fun onUpdate(deltaSec: Float, item: IEntity): Float {

        if (elapsedSec == _duration) {
            return 0f
        }

        val percentage = _easeFunction.getPercentage(elapsedSec, _duration)

        var usedSec = min(_duration - elapsedSec, deltaSec)

        when (_type) {

            ALPHA -> {
                item.alpha = getValueAt(0, percentage)
            }

            SCALE -> {
                item.setScale(getValueAt(0, percentage))
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

                while (remainingSec > 0 && _modifiers != null) {

                    remainingSec -= _modifiers!![0].onUpdate(remainingSec, item)

                    if (_modifiers!![0].isFinished) {
                        _modifiers = if (_modifiers!!.size > 1) _modifiers!!.copyOfRange(1, _modifiers!!.size) else null
                        break
                    }
                }

                usedSec = deltaSec - remainingSec
            }

            PARALLEL -> {
                var remainingSec = deltaSec

                while (remainingSec > 0 && _modifiers != null) {

                    val modifiers = _modifiers!!

                    for (modifier in modifiers) {
                        usedSec = max(usedSec, modifier.onUpdate(deltaSec, item))

                        if (modifier.isFinished) {
                            _modifiers = if (_modifiers!!.size > 1) _modifiers!!.copyOfRange(1, _modifiers!!.size) else null
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

        if (elapsedSec >= _duration) {
            elapsedSec = _duration

            _modifiers = null

            _onFinished?.invoke(item)
            _onFinished = null

            pool?.free(this)
        }

        return usedSec
    }

    override fun onUnregister() {
        pool?.free(this)
    }


    /**
     * Set inner modifiers for sequence or parallel modifier types.
     */
    fun setInnerModifiers(vararg modifiers: UniversalModifier): UniversalModifier {

        if (modifiers.isEmpty()) {
            throw IllegalArgumentException("Modifiers list must not be empty")
        }

        _modifiers = modifiers

        if (_type == PARALLEL) {
            _modifiers!!.sortBy { it._duration }
        }

        setDuration(_duration)
        return this
    }

    /**
     * The type of the modifier.
     * @see ModifierType
     */
    fun setType(type: ModifierType): UniversalModifier {

        if (type != SEQUENCE && type != PARALLEL) {
            _modifiers = null
        }

        _type = type
        setDuration(_duration)
        return this
    }

    /**
     * Set the span values.
     */
    fun setValueSpan(from: Float, to: Float): UniversalModifier {
        data[FROM_INDEX] = from
        data[TO_INDEX] = to
        return this
    }

    /**
     * Set the RGB span values.
     */
    fun setRGBSpan(
        fromRed: Float,
        toRed: Float,
        fromGreen: Float,
        toGreen: Float,
        fromBlue: Float,
        toBlue: Float,
    ): UniversalModifier {

        if (data.size < 6) {
            data = data.copyOf(6)
        }

        data[DATA_SIZE * 0 + FROM_INDEX] = fromRed
        data[DATA_SIZE * 0 + TO_INDEX] = toRed

        data[DATA_SIZE * 1 + FROM_INDEX] = fromGreen
        data[DATA_SIZE * 1 + TO_INDEX] = toGreen

        data[DATA_SIZE * 2 + FROM_INDEX] = fromBlue
        data[DATA_SIZE * 2 + TO_INDEX] = toBlue

        return this
    }

    /**
     * Duration of the modifier in seconds.
     */
    fun setDuration(duration: Float): UniversalModifier {

        _duration = when (_type) {
            SEQUENCE -> _modifiers?.sumOf { it._duration } ?: 0f
            PARALLEL -> _modifiers?.maxOf { it._duration } ?: 0f
            else -> duration
        }

        return this
    }

    /**
     * Callback to be called when the modifier is finished.
     */
    fun setOnFinished(onFinished: OnModifierFinished?): UniversalModifier {
        _onFinished = onFinished
        return this
    }

    /**
     * Easing function to be used.
     */
    fun setEaseFunction(easeFunction: IEaseFunction): UniversalModifier {
        _easeFunction = easeFunction
        return this
    }


    override fun isRemoveWhenFinished() = true


    override fun deepCopy() = throw NotImplementedError()
    override fun setRemoveWhenFinished(value: Boolean) = throw NotImplementedError()
    override fun addModifierListener(listener: IModifier.IModifierListener<IEntity>) = throw NotImplementedError()
    override fun removeModifierListener(listener: IModifier.IModifierListener<IEntity>) = throw NotImplementedError()


    override fun reset() {

        data.fill(0f)

        _type = NONE
        _duration = 0f
        _onFinished = null
        _easeFunction = IEaseFunction.DEFAULT

        _modifiers?.forEach {
            it.reset()
            pool?.free(it)
        }
        _modifiers = null

        elapsedSec = 0f
    }


    companion object {

        private const val FROM_INDEX = 0

        private const val TO_INDEX = 1

        private const val DATA_SIZE = 2

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