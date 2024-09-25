package com.reco1l.andengine

import android.util.*
import com.reco1l.framework.*
import com.reco1l.osu.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.util.modifier.*
import org.anddev.andengine.util.modifier.ease.*
import kotlin.math.*

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
    constructor(type: ModifierType, duration: Float, from: Float, to: Float, listener: IModifier.IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = IEaseFunction.DEFAULT) : this(null) {
        this.type = type
        this.duration = duration
        this.values = floatArrayOf(from, to)
        this.listener = listener
        this.easeFunction = easeFunction
    }

    @JvmOverloads
    constructor(type: ModifierType, duration: Float, values: FloatArray, listener: IModifier.IModifierListener<IEntity>? = null, easeFunction: IEaseFunction = IEaseFunction.DEFAULT) : this(null) {
        this.type = type
        this.duration = duration
        this.values = values
        this.listener = listener
        this.easeFunction = easeFunction
    }

    @JvmOverloads
    constructor(type: ModifierType, listener: IModifier.IModifierListener<IEntity>? = null, vararg modifiers: UniversalModifier) : this(null) {
        this.type = type
        this.listener = listener
        this.modifiers = modifiers
    }


    /**
     * The type of the modifier.
     * @see ModifierType
     */
    var type = ModifierType.NONE
        set(value) {
            if (field != value) {

                if (value != ModifierType.SEQUENCE && value != ModifierType.PARALLEL) {
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
    var listener: IModifier.IModifierListener<IEntity>? = null

    /**
     * Inner modifiers for [SEQUENCE] or [PARALLEL] modifier types.
     */
    var modifiers: Array<out UniversalModifier>? = null
        set(value) {

            if (value != null && type != ModifierType.SEQUENCE && type != ModifierType.PARALLEL) {
                Log.w("UniversalModifier", "Inner modifiers can only be set for sequence or parallel modifiers, ignoring.")
                return
            }

            if (type == ModifierType.PARALLEL) {
                // Sorting to reduce iterations, obviously sequential modifiers cannot be sorted.
                value?.sortBy { it.duration }
            }

            field = value
            duration = getDuration()
        }

    /**
     * Easing function to be used.
     */
    var easeFunction: IEaseFunction = IEaseFunction.DEFAULT

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

        if (type == ModifierType.SEQUENCE || type == ModifierType.PARALLEL) {

            var remainingSec = deltaSec
            var isAllModifiersFinished = false

            while (remainingSec > 0 && !isAllModifiersFinished) {

                var isCurrentModifierFinished = false

                if (type == ModifierType.PARALLEL) {
                    usedSec = 0f
                }

                // Assuming all modifiers are finished until proven otherwise in the loop below.
                isAllModifiersFinished = true

                for (modifier in modifiers!!) {

                    if (modifier.isFinished) {
                        continue
                    }
                    isAllModifiersFinished = false

                    if (type == ModifierType.SEQUENCE) {
                        remainingSec -= modifier.onUpdate(remainingSec, item)
                    } else {
                        usedSec = max(usedSec, modifier.onUpdate(deltaSec, item))
                    }

                    isCurrentModifierFinished = modifier.isFinished

                    if (type == ModifierType.SEQUENCE) {
                        break
                    }
                }

                if (type == ModifierType.PARALLEL) {
                    remainingSec -= usedSec
                } else if (isCurrentModifierFinished) {
                    break
                }

            }

            usedSec = deltaSec - remainingSec

        } else {
            usedSec = min(duration - elapsedSec, deltaSec)

            val percentage = easeFunction.getPercentage(elapsedSec + usedSec, duration)

            when (type) {

                ModifierType.ALPHA -> {
                    item.alpha = getValueAt(0, percentage)
                }

                ModifierType.SCALE -> {
                    val value = getValueAt(0, percentage)
                    item.scaleX = value
                    item.scaleY = value
                }

                ModifierType.MOVE -> {
                    item.setPosition(getValueAt(0, percentage), getValueAt(1, percentage))
                }

                ModifierType.TRANSLATE -> {
                    if (item is ExtendedEntity) {
                        item.translationX = getValueAt(0, percentage)
                        item.translationY = getValueAt(1, percentage)
                    }
                }

                ModifierType.TRANSLATE_X -> {
                    if (item is ExtendedEntity) {
                        item.translationX = getValueAt(0, percentage)
                    }
                }

                ModifierType.TRANSLATE_Y -> {
                    if (item is ExtendedEntity) {
                        item.translationY = getValueAt(0, percentage)
                    }
                }

                ModifierType.ROTATION -> {
                    item.rotation = getValueAt(0, percentage)
                }

                ModifierType.RGB -> {
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

        type = ModifierType.NONE
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

        if (type == ModifierType.SEQUENCE || type == ModifierType.PARALLEL) {
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

        ModifierType.SEQUENCE -> modifiers?.sumOf { it.duration } ?: 0f
        ModifierType.PARALLEL -> modifiers?.maxOf { it.duration } ?: 0f

        else -> duration
    }


    override fun getSecondsElapsed(): Float {
        return elapsedSec
    }


    override fun isFinished(): Boolean {

        if (type == ModifierType.SEQUENCE || type == ModifierType.PARALLEL) {

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


    override fun addModifierListener(listener: IModifier.IModifierListener<IEntity>) {
        throw UnsupportedOperationException("Multiple entity modifiers are not allowed, consider using `setListener()` instead.")
    }

    override fun removeModifierListener(listener: IModifier.IModifierListener<IEntity>): Boolean {
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
     * Modifies the entity's position in both axis.
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