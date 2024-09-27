package com.reco1l.andengine.modifier

import android.util.*
import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.ModifierType.*
import com.reco1l.framework.*
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
class UniversalModifier @JvmOverloads constructor(private val pool: Pool<UniversalModifier>? = null) : IEntityModifier, IModifierChain {

    @JvmOverloads
    constructor(type: ModifierType, duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easeFunction: IEaseFunction = IEaseFunction.DEFAULT) : this(null) {
        this.type = type
        this.duration = duration
        this.values = floatArrayOf(from, to)
        this.onFinished = listener
        this.easeFunction = easeFunction
    }

    @JvmOverloads
    constructor(type: ModifierType, listener: OnModifierFinished? = null, vararg modifiers: UniversalModifier) : this(null) {
        this.type = type
        this.onFinished = listener
        this.modifiers = arrayOf(*modifiers)
    }


    var entity: ExtendedEntity? = null


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
     * Callback to be called when the modifier finishes.
     */
    var onFinished: OnModifierFinished? = null

    /**
     * Inner modifiers for [SEQUENCE] or [PARALLEL] modifier types.
     */
    var modifiers: Array<UniversalModifier>? = null
        set(value) {

            if (value != null && type != SEQUENCE && type != PARALLEL) {
                Log.w("UniversalModifier", "Inner modifiers can only be set for sequence or parallel modifiers, ignoring.")
                return
            }

            if (type == PARALLEL) {
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
     * * [COLOR] -> [redFrom, redTo, greenFrom, greenTo, blueFrom, blueTo]
     * * [SCALE] and [ALPHA] -> [scaleFrom, scaleTo]
     */
    var values: FloatArray? = null


    private var removeWhenFinished = true

    private var elapsedSec = -1f

    private var percentage = 0f

    private var duration = 0f


    private fun clearNestedModifiers() {
        modifiers?.forEach { it.onUnregister() }
        modifiers = null
    }


    override fun onUpdate(deltaTimeSec: Float, entity: IEntity): Float {

        if (isFinished) {
            return 0f
        }

        // We use negative elapsed time to know whether the modifier has started or not yet.
        if (elapsedSec < 0) {
            elapsedSec = 0f
        }

        var consumedTimeSec = 0f

        if (type == SEQUENCE || type == PARALLEL) {

            var remainingTimeSec = deltaTimeSec
            var isAllModifiersFinished = false

            while (remainingTimeSec > 0 && !isAllModifiersFinished) {

                var isCurrentModifierFinished = false

                // In parallel modifiers, the consumed time is equal to the maximum consumed time of the inner modifiers.
                if (type == PARALLEL) {
                    consumedTimeSec = 0f
                }

                // Assuming all modifiers are finished until proven otherwise.
                isAllModifiersFinished = true

                for (modifier in modifiers!!) {

                    if (modifier.isFinished) {
                        continue
                    }
                    isAllModifiersFinished = false

                    if (type == SEQUENCE) {
                        remainingTimeSec -= modifier.onUpdate(remainingTimeSec, entity)
                    } else {
                        consumedTimeSec = max(consumedTimeSec, modifier.onUpdate(deltaTimeSec, entity))
                    }

                    isCurrentModifierFinished = modifier.isFinished

                    if (type == SEQUENCE) {
                        break
                    }
                }

                if (type == PARALLEL) {
                    remainingTimeSec -= consumedTimeSec
                } else if (isCurrentModifierFinished) {
                    break
                }

            }

            consumedTimeSec = deltaTimeSec - remainingTimeSec

            // Not really necessary but we want to report the current percentage.
            // Modifiers with nested modifiers will always use linear easing.
            percentage = if (duration > 0) (elapsedSec + consumedTimeSec) / duration else 1f

        } else {

            consumedTimeSec = min(duration - elapsedSec, deltaTimeSec)

            // In this case the consumed time is already fully calculated here, if the duration is 0
            // we have to assume the percentage is 1 to avoid division by zero.
            percentage = if (duration > 0) easeFunction.getPercentage(elapsedSec + consumedTimeSec, duration) else 1f

            if (values != null) {
                type.onApply?.invoke(entity, values!!, percentage)
            }
        }

        elapsedSec += consumedTimeSec

        if (isFinished) {
            elapsedSec = duration
            onFinished?.invoke(entity)
        }

        return consumedTimeSec
    }

    override fun onUnregister() {
        setToDefault()
        pool?.free(this)
    }


    /**
     * Resets the modifier to its initial state.
     */
    override fun reset() {
        elapsedSec = -1f
        percentage = 0f
        modifiers?.forEach { it.reset() }
    }


    override fun applyModifier(block: (UniversalModifier) -> Unit): UniversalModifier {

        if (entity == null) {
            throw IllegalStateException("Modifier target is not set in this UniversalModifier cannot apply modifier.")
        }

        // When this happens it means that this was called from a chained call.
        // If the type of modifier is not a sequence or parallel, we should apply
        // the modifier to the target directly.
        if (type != SEQUENCE && type != PARALLEL) {

            // If the type is delay, we should convert it to a sequence modifier with the delay first,
            // so the next chained modifiers will be applied after the delay.
            if (type == NONE) {
                val delay = duration
                val target = entity!!

                setToDefault()
                type = SEQUENCE
                entity = target

                modifiers = arrayOf(
                    target.applyModifier { it.duration = delay },
                    target.applyModifier(block)
                )
            }

            return entity!!.applyModifier(block)
        }

        val modifier = pool?.obtain() ?: UniversalModifier()
        modifier.setToDefault()
        modifier.entity = entity
        block(modifier)

        modifiers = modifiers?.plus(modifier) ?: arrayOf(modifier)
        return modifier
    }


    /**
     * Sets the modifier to its default state.
     *
     * This will remove all the inner modifiers, the listener, reset the elapsed time, and set the type to [NONE].
     */
    fun setToDefault() {

        type = NONE
        values = null
        entity = null
        duration = 0f
        onFinished = null
        easeFunction = IEaseFunction.DEFAULT

        clearNestedModifiers()
        reset()
    }

    /**
     * Seeks the modifier to a specific time.
     */
    @JvmOverloads
    fun seekTo(seconds: Float, target: IEntity? = entity!!) {
        onUpdate(seconds - elapsedSec, target ?: return)
    }

    /**
     * Sets the progress of the modifier.
     */
    @JvmOverloads
    fun setProgress(progress: Float, target: IEntity? = entity!!) {
        onUpdate(progress * duration, target ?: return)
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


    override fun addModifierListener(listener: IModifier.IModifierListener<IEntity>) {
        throw UnsupportedOperationException("Multiple entity modifiers are not allowed, consider using `setListener()` instead.")
    }

    override fun removeModifierListener(listener: IModifier.IModifierListener<IEntity>): Boolean {
        throw UnsupportedOperationException("Multiple entity modifiers are not allowed, consider using `setListener()` instead.")
    }


    override fun deepCopy(): UniversalModifier = UniversalModifier(pool).also { modifier ->
        modifier.type = type
        modifier.duration = duration
        modifier.onFinished = onFinished
        modifier.easeFunction = easeFunction
        modifier.values = values?.copyOf()
        modifier.modifiers = modifiers?.map { it.deepCopy() }?.toTypedArray()
    }


}

/**
 * A function that is called when a modifier finishes.
 */
fun interface OnModifierFinished {
    operator fun invoke(entity: IEntity)
}