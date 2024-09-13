package com.reco1l.osu.graphics

import com.reco1l.framework.Pool
import com.reco1l.osu.graphics.ModifierType.*
import com.reco1l.toolkt.kotlin.sumOf
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.util.modifier.IModifier
import org.anddev.andengine.util.modifier.ease.EaseSineInOut
import org.anddev.andengine.util.modifier.ease.EaseSineOut
import org.anddev.andengine.util.modifier.ease.IEaseFunction
import kotlin.math.max
import kotlin.math.min
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
    fun alpha(duration: Float, from: Float, to: Float, onFinished: OnModifierFinished? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.reset()
        it.type = ALPHA
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.onFinished = onFinished
    }

    @JvmStatic
    @JvmOverloads
    fun fadeIn(duration: Float, onFinished: OnModifierFinished? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = alpha(duration, 0f, 1f, onFinished, easeFunction)

    @JvmStatic
    @JvmOverloads
    fun fadeOut(duration: Float, onFinished: OnModifierFinished? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = alpha(duration, 1f, 0f, onFinished, easeFunction)

    @JvmStatic
    @JvmOverloads
    fun scale(duration: Float, from: Float, to: Float, onFinished: OnModifierFinished? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.reset()
        it.type = SCALE
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.onFinished = onFinished
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
        onFinished: OnModifierFinished? = null,
        easeFunction: IEaseFunction = DefaultEaseFunction
    ) = pool.obtain().also {
        it.reset()
        it.type = RGB
        it.duration = duration
        it.onFinished = onFinished
        it.easeFunction = easeFunction
        it.values = floatArrayOf(
            fromRed, toRed,
            fromGreen, toGreen,
            fromBlue, toBlue
        )
    }

    @JvmStatic
    @JvmOverloads
    fun sequence(onFinished: OnModifierFinished? = null, vararg modifiers: UniversalModifier) = pool.obtain().also {
        it.reset()
        it.type = SEQUENCE
        it.modifiers = modifiers
        it.onFinished = onFinished
    }

    @JvmStatic
    @JvmOverloads
    fun parallel(onFinished: OnModifierFinished? = null, vararg modifiers: UniversalModifier) = pool.obtain().also {
        it.reset()
        it.type = PARALLEL
        it.modifiers = modifiers
        it.onFinished = onFinished
    }

    @JvmStatic
    @JvmOverloads
    fun delay(duration: Float, onFinished: OnModifierFinished? = null) = pool.obtain().also {
        it.reset()
        it.type = NONE
        it.duration = duration
        it.onFinished = onFinished
    }

    @JvmStatic
    @JvmOverloads
    fun moveX(duration: Float, from: Float, to: Float, onFinished: OnModifierFinished? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.reset()
        it.type = MOVE_X
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.onFinished = onFinished
    }

    @JvmStatic
    @JvmOverloads
    fun moveY(duration: Float, from: Float, to: Float, onFinished: OnModifierFinished? = null, easeFunction: IEaseFunction = DefaultEaseFunction) = pool.obtain().also {
        it.reset()
        it.type = MOVE_Y
        it.duration = duration
        it.values = floatArrayOf(from, to)
        it.easeFunction = easeFunction
        it.onFinished = onFinished
    }

    @JvmStatic
    @JvmOverloads
    fun shakeHorizontal(duration: Float, originX: Float, magnitude: Float, onFinished: OnModifierFinished? = null) = pool.obtain().also {

        // Based on osu!lazer's shake effect: https://github.com/ppy/osu/blob/5341a335a6165ceef4d91e910fa2ea5aecbfd025/osu.Game/Extensions/DrawableExtensions.cs#L19-L37

        val boundLeft = originX - magnitude
        val boundRight = originX + magnitude

        it.reset()
        it.type = SEQUENCE
        it.modifiers = arrayOf(
            moveX(duration / 8f,     originX,    boundRight,  easeFunction = EaseSineOut.getInstance()),
            moveX(duration / 4f,     boundRight, boundLeft,   easeFunction = EaseSineInOut.getInstance()),
            moveX(duration / 4f,     boundLeft,  boundRight,  easeFunction = EaseSineInOut.getInstance()),
            moveX(duration / 4f,     boundRight, boundLeft,   easeFunction = EaseSineInOut.getInstance()),
            moveX(duration / 8f,     boundLeft,  originX,     easeFunction = EaseSineInOut.getInstance()),
        )
        it.onFinished = onFinished

    }

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
        }

    /**
     * Callback to be called when the modifier is finished.
     */
    var onFinished: OnModifierFinished? = null

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
     * * [MOVE] -> [xFrom, xTo, yFrom, yTo]
     * * [RGB] -> [redFrom, redTo, greenFrom, greenTo, blueFrom, blueTo]
     * * [SCALE] and [ALPHA] -> [scaleFrom, scaleTo]
     */
    var values: FloatArray? = null

    /**
     * Inner modifiers for [SEQUENCE] or [PARALLEL] modifier types.
     */
    var modifiers: Array<out UniversalModifier>? = null

    /**
     * Easing function to be used.
     */
    var easeFunction: IEaseFunction = DefaultEaseFunction


    private var elapsedSec = 0f

    private var duration = 0f


    private fun getValueAt(dataIndex: Int, percentage: Float): Float {

        val from = values?.get(strip * dataIndex + offsetFrom) ?: 0f
        val to = values?.get(strip * dataIndex + offsetTo) ?: 0f

        return from + (to - from) * percentage
    }

    override fun onUpdate(deltaSec: Float, item: IEntity): Float {

        if (isFinished) {
            return 0f
        }

        var usedSec = min(duration - elapsedSec, deltaSec)

        val percentage = easeFunction.getPercentage(elapsedSec + usedSec, duration)

        when (type) {

            ALPHA -> {
                item.alpha = getValueAt(0, percentage)
            }

            SCALE -> {
                item.setScale(getValueAt(0, percentage))
            }

            MOVE -> {
                item.setPosition(
                    getValueAt(0, percentage),
                    getValueAt(1, percentage)
                )
            }

            MOVE_X -> {
                item.setPosition(
                    getValueAt(0, percentage),
                    item.y
                )
            }

            MOVE_Y -> {
                item.setPosition(
                    item.x,
                    getValueAt(0, percentage)
                )
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

                while (remainingSec > 0 && modifiers != null) {

                    val modifier = modifiers!!.first()

                    remainingSec -= modifier.onUpdate(remainingSec, item)

                    if (modifier.isFinished) {
                        trimModifiers()
                        modifier.onUnregister()
                        break
                    }
                }

                usedSec = deltaSec - remainingSec
            }

            PARALLEL -> {
                var remainingSec = deltaSec

                while (remainingSec > 0 && modifiers != null) {

                    for (modifier in modifiers!!) {
                        usedSec = max(usedSec, modifier.onUpdate(remainingSec, item))

                        if (modifier.isFinished) {
                            trimModifiers()
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

            onFinished?.invoke(item)
            onFinished = null
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
        onFinished = null
        easeFunction = DefaultEaseFunction

        clearModifiers()
    }


    private fun clearModifiers() {
        modifiers?.forEach { it.onUnregister() }
        modifiers = null
    }

    private fun trimModifiers() {

        if (modifiers!!.size == 1) {
            modifiers = null
            return
        }

        modifiers = modifiers!!.copyOfRange(1, modifiers!!.size)
    }


    fun setDuration(value: Float) {
        duration = value
    }

    override fun getDuration(): Float {
        return duration
    }

    override fun getSecondsElapsed(): Float {
        return elapsedSec
    }

    override fun isFinished() = when (type) {

        // If the modifier is a sequence or parallel, it is finished when all the inner modifiers are finished.
        SEQUENCE, PARALLEL -> modifiers.isNullOrEmpty()

        // Otherwise, it is finished when the elapsed time is greater or equal to the duration.
        else -> elapsedSec >= duration
    }

    override fun isRemoveWhenFinished(): Boolean {
        // Always return true to recycle the modifier.
        return true
    }


    override fun deepCopy(): UniversalModifier = UniversalModifier(pool).also { modifier ->
        modifier.type = type
        modifier.duration = duration
        modifier.onFinished = onFinished
        modifier.easeFunction = easeFunction
        modifier.values = values?.copyOf()
        modifier.modifiers = modifiers?.map { it.deepCopy() }?.toTypedArray()
    }

    override fun setRemoveWhenFinished(value: Boolean) {
        throw UnsupportedOperationException("Cannot set remove when finished for UniversalModifier.")
    }

    override fun addModifierListener(listener: IModifier.IModifierListener<IEntity>) {
        throw UnsupportedOperationException("Use onFinished callback instead.")
    }

    override fun removeModifierListener(listener: IModifier.IModifierListener<IEntity>): Nothing {
        throw UnsupportedOperationException("Use onFinished callback instead.")
    }


    companion object {

        private const val offsetFrom = 0
        private const val offsetTo = 1
        private const val strip = 2

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
     * Modifies the entity's position in both axis.
     */
    MOVE,

    /**
     * Modifies the entity's X position.
     */
    MOVE_X,

    /**
     * Modifies the entity's Y position.
     */
    MOVE_Y,

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