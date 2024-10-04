package com.reco1l.andengine.modifier

import android.util.*
import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.ModifierType.*
import com.reco1l.framework.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.util.modifier.*
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
    constructor(type: ModifierType, duration: Float, from: Float, to: Float, listener: OnModifierFinished? = null, easeFunction: Easing = Easing.None) : this(null) {
        this.type = type
        this.duration = duration
        this.values = floatArrayOf(from, to)
        this.onFinished = listener
        this.easing = easeFunction
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

                values.fill(0f)
                calculateDuration()
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
            calculateDuration()
        }

    /**
     * Easing function to be used.
     */
    var easing: Easing = Easing.None


    private var values = FloatArray(2)

    private var removeWhenFinished = true

    private var elapsedSec = -1f

    private var duration = 0f


    private fun clearNestedModifiers() {
        modifiers?.forEach { it.onUnregister() }
        modifiers = null
    }

    private fun calculateDuration() {
        duration = when(type) {
            SEQUENCE -> modifiers?.sumOf { it.duration } ?: 0f
            PARALLEL -> modifiers?.maxOf { it.duration } ?: 0f
            else -> duration
        }
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

                // In parallel modifiers the consumed time is equal to the maximum consumed
                // time of the inner modifiers so we need to reset it to 0, at this point
                // remainingTimeSec should have the previous consumed time subtracted.
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
                        // In a sequence the delta time is subtracted with the consumed time of the first
                        // non-finished modifier until it reaches 0.
                        // We break the loop because only the first non-finished modifier should be updated.
                        remainingTimeSec -= modifier.onUpdate(remainingTimeSec, entity)
                        break
                    } else {
                        // In parallel the consumed time is the maximum consumed time of all inner modifiers.
                        consumedTimeSec = max(consumedTimeSec, modifier.onUpdate(remainingTimeSec, entity))
                    }
                }

                // In a parallel modifier since the consumed time is the maximum consumed time
                // between all inner modifiers, we subtract it after all modifiers are updated.
                if (type == PARALLEL) {
                    remainingTimeSec -= consumedTimeSec
                }
            }

            consumedTimeSec = deltaTimeSec - remainingTimeSec
            elapsedSec += consumedTimeSec

        } else {

            consumedTimeSec = min(duration - elapsedSec, deltaTimeSec)
            elapsedSec += consumedTimeSec

            // The consumed time is already fully calculated here, if the duration is 0
            // we have to assume the percentage is 1 to avoid division by zero.
            val percentage = if (duration > 0) easing.interpolate(elapsedSec / duration) else 1f

            type.onApply?.invoke(entity, values, percentage)
        }

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
        modifiers?.forEach { it.reset() }
    }


    override fun applyModifier(block: (UniversalModifier) -> Unit): UniversalModifier {

        // When this happens it means that this was called from a chained call.
        // If the type of modifier is not a sequence or parallel, we should apply
        // the modifier to the target directly.
        if (type != SEQUENCE && type != PARALLEL) {

            // If the type is delay, we should convert it to a sequence modifier with the delay first,
            // so the next chained modifiers will be applied after the delay.
            if (type == NONE) {

                // We preserve the duration because it will be reset when changing the type.
                val delay = duration

                // Changing type to sequence, at this point shouldn't be needed to
                // call setToDefault() since this is supposed to be a delay modifier.
                type = SEQUENCE

                applyModifier { it.duration = delay }
                applyModifier(block)
                return this
            }

            // If it's not a sequence or parallel modifier, we should apply the modifier directly
            // to the target entity assuming it's not null.
            if (entity == null) {
                throw IllegalStateException("The target entity of an UniversalModifier cannot be null.")
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
     * Sets the easing function to be used.
     */
    fun eased(easing: Easing): UniversalModifier {
        this.easing = easing
        return this
    }

    /**
     * Sets the callback to be called when the modifier finishes.
     */
    fun then(block: OnModifierFinished): UniversalModifier {
        onFinished = block
        return this
    }

    /**
     * Delays the next modifier with the duration of this modifier.
     */
    fun then(): UniversalModifier {

        if (type == SEQUENCE) {
            return this
        }

        if (entity == null) {
            throw IllegalStateException("The target entity of an UniversalModifier cannot be null.")
        }

        return entity!!.applyModifier {
            it.type = NONE
            it.duration = duration
        }
    }


    /**
     * Sets the modifier to its default state.
     *
     * This will remove all the inner modifiers, the listener, reset the elapsed time, and set the type to [NONE].
     */
    fun setToDefault() {

        type = NONE
        entity = null
        duration = 0f
        onFinished = null
        easing = Easing.None

        values.fill(0f)

        clearNestedModifiers()
        reset()
    }


    /**
     * Sets the transformation values for the modifier.
     *
     * The values are stored in an array of spans of 2 elements where the first element is the `from` value and the second is the
     * `to` values, the amount of spans needed depends on the type of the modifier.
     */
    fun setValues(vararg newValues: Float) {

        if (values.size < newValues.size) {
            values = values.copyOf(newValues.size)
        }

        for (i in newValues.indices) {
            values[i] = newValues[i]
        }
    }

    /**
     * Seeks the modifier to a specific time.
     */
    @JvmOverloads
    fun setTime(seconds: Float, target: IEntity? = entity) {
        onUpdate(seconds - elapsedSec, target ?: return)
    }

    /**
     * Sets the progress of the modifier.
     */
    @JvmOverloads
    fun setProgress(progress: Float, target: IEntity? = entity) {
        onUpdate(progress * duration, target ?: return)
    }


    /**
     * Sets the duration of the modifier.
     *
     * If the modifier is a [SEQUENCE] or [PARALLEL] modifier, this method will do nothing.
     */
    fun setDuration(value: Float) {

        if (type == SEQUENCE || type == PARALLEL) {
            Log.w("UniversalModifier", "Cannot set duration for sequence or parallel modifiers, ignoring.")
            return
        }

        duration = value
    }


    override fun getDuration(): Float {
        return duration
    }

    override fun getSecondsElapsed(): Float {
        return elapsedSec
    }


    override fun isFinished(): Boolean {
        if (type == SEQUENCE || type == PARALLEL) {
            for (modifier in modifiers ?: return true) {
                if (!modifier.isFinished) {
                    return false
                }
            }
            return true
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
        modifier.easing = easing
        modifier.values = values.copyOf()
        modifier.duration = duration
        modifier.modifiers = modifiers?.map { it.deepCopy() }?.toTypedArray()
        modifier.onFinished = onFinished
    }


}

/**
 * A function that is called when a modifier finishes.
 */
fun interface OnModifierFinished {
    operator fun invoke(entity: IEntity)
}