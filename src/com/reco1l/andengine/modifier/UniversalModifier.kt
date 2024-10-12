package com.reco1l.andengine.modifier

import android.util.*
import com.edlplan.framework.easing.Easing
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
class UniversalModifier @JvmOverloads constructor(private val pool: Pool<UniversalModifier>? = GlobalPool) : IEntityModifier, IModifierChain {

    /**
     * The type of the modifier.
     * @see ModifierType
     */
    var type = Delay
        set(value) {
            field = value

            if (!value.isCompoundModifier) {
                clearNestedModifiers()
            }
        }

    /**
     * Inner modifiers for [Sequence] or [Parallel] modifier types.
     */
    var modifiers: Array<UniversalModifier>? = null

    /**
     * The initial values for the modifier.
     */
    var initialValues: FloatArray? = null

    /**
     * The final values for the modifier.
     */
    var finalValues: FloatArray? = null

    /**
     * Callback to be called when the modifier finishes.
     */
    var onFinished: OnModifierFinished? = null

    /**
     * Whether to allow nested modifiers.
     *
     * Used to prevent chained calls of modifiers from being
     * applied to this compound modifier, only modifiers inside the block should be applied.
     */
    var allowNesting = true


    private var parent: UniversalModifier? = null

    private var easing = Easing.None

    private var duration = 0f

    private var elapsedSec = -1f


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

            if (initialValues == null) {
                initialValues = type.getInitialValues(entity)
            }
        }

        var consumedTimeSec = 0f

        if (type.isCompoundModifier) {

            var remainingTimeSec = deltaTimeSec
            var isAllModifiersFinished = false

            while (remainingTimeSec > 0 && !isAllModifiersFinished) {

                // In parallel modifiers the consumed time is equal to the maximum consumed
                // time of the inner modifiers so we need to reset it to 0, at this point
                // remainingTimeSec should have the previous consumed time subtracted.
                if (type == Parallel) {
                    consumedTimeSec = 0f
                }

                // Assuming all modifiers are finished until proven otherwise.
                isAllModifiersFinished = true

                for (modifier in modifiers!!) {

                    if (modifier.isFinished) {
                        continue
                    }
                    isAllModifiersFinished = false

                    if (type == Sequence) {
                        // In a sequence the delta time is subtracted with the consumed time of the first
                        // non-finished modifier until it reaches 0.
                        // We break the loop because only the first non-finished modifier should be updated.
                        remainingTimeSec -= modifier.onUpdate(remainingTimeSec, entity)
                        break
                    } else {
                        // In parallel the consumed time is the maximum consumed time of all inner modifiers.
                        consumedTimeSec = max(consumedTimeSec, modifier.onUpdate(deltaTimeSec, entity))
                    }
                }

                // In a parallel modifier since the consumed time is the maximum consumed time
                // between all inner modifiers, we subtract it after all modifiers are updated.
                if (type == Parallel) {
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

            if (initialValues != null && finalValues != null) {
                type.setValues(entity, initialValues!!, finalValues!!, percentage)
            }
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


    override fun applyModifier(block: UniversalModifier.() -> Unit): UniversalModifier {

        if (type.isCompoundModifier && allowNesting) {

            val nested = pool?.obtain() ?: UniversalModifier()
            nested.setToDefault()
            nested.parent = this
            nested.block()

            modifiers = modifiers?.plus(nested) ?: arrayOf(nested)
            return nested
        }

        if (parent?.type == Sequence) {
            return parent!!.applyModifier(block)
        }

        if (type != Sequence || !allowNesting) {

            val copy = pool?.obtain() ?: UniversalModifier()
            copy.setToDefault()
            copy.parent = this

            copy.type = type
            copy.easing = easing
            copy.duration = duration
            copy.modifiers = modifiers
            copy.onFinished = onFinished
            copy.finalValues = finalValues
            copy.initialValues = initialValues

            modifiers = null // Preventing modifiers of the copy from being pooled.
            setToDefault()

            type = Sequence
            modifiers = arrayOf(copy)
        }

        return applyModifier(block)
    }


    /**
     * Sets the easing function to be used.
     *
     * If the modifier is a [Sequence] or [Parallel] modifier, this method will set the easing for all the nested modifiers.
     */
    fun eased(value: Easing): UniversalModifier {

        if (type.isCompoundModifier) {
            modifiers?.fastForEach { it.eased(value) }
        } else {
            easing = value
        }

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
     * Sets the modifier to its default state.
     *
     * This will remove all the inner modifiers, the listener, reset the elapsed time, and set the type to [Delay].
     */
    fun setToDefault() {

        type = Delay
        easing = Easing.None

        duration = 0f

        parent = null
        onFinished = null
        finalValues = null
        allowNesting = true
        initialValues = null

        clearNestedModifiers()
        reset()
    }


    /**
     * Seeks the modifier to a specific time.
     */
    fun setTime(seconds: Float, target: IEntity) {
        onUpdate(seconds - elapsedSec, target)
    }

    /**
     * Sets the progress of the modifier.
     */
    fun setProgress(progress: Float, target: IEntity) {
        onUpdate(progress * duration, target)
    }

    /**
     * Sets the duration of the modifier.
     *
     * If the modifier is a [Sequence] or [Parallel] modifier, this method will do nothing.
     */
    fun setDuration(value: Float) {

        if (type.isCompoundModifier) {
            Log.w("UniversalModifier", "Cannot manually set duration for sequence or parallel modifiers, ignoring.")
            return
        }

        duration = value
    }


    override fun getDuration(): Float = when (type) {

        Sequence -> modifiers?.sumOf { it.getDuration() } ?: 0f
        Parallel -> modifiers?.maxOf { it.getDuration() } ?: 0f

        else -> duration
    }

    override fun getSecondsElapsed(): Float {
        return elapsedSec
    }


    override fun isFinished(): Boolean {
        if (type.isCompoundModifier) {
            for (modifier in modifiers ?: return true) {
                if (!modifier.isFinished) {
                    return false
                }
            }
        }
        return elapsedSec >= duration
    }


    override fun isRemoveWhenFinished(): Boolean {
        return true
    }

    override fun setRemoveWhenFinished(value: Boolean) {
        Log.w("UniversalModifier", "Remove when finished is always true for UniversalModifier, ignoring.")
    }


    override fun addModifierListener(listener: IModifier.IModifierListener<IEntity>) {
        throw UnsupportedOperationException("Multiple entity modifiers are not allowed, consider using `setListener()` instead.")
    }

    override fun removeModifierListener(listener: IModifier.IModifierListener<IEntity>): Boolean {
        throw UnsupportedOperationException("Multiple entity modifiers are not allowed, consider using `setListener()` instead.")
    }


    override fun deepCopy(): UniversalModifier = UniversalModifier(pool).also { modifier ->
        modifier.finalValues = finalValues?.copyOf()
        modifier.initialValues = initialValues?.copyOf()
        modifier.type = type
        modifier.easing = easing
        modifier.duration = duration
        modifier.modifiers = modifiers?.map { it.deepCopy() }?.toTypedArray()
        modifier.onFinished = onFinished
    }


    companion object {

        /**
         * The global pool for universal modifiers.
         */
        @JvmField
        val GlobalPool = Pool { UniversalModifier(it) }

    }

}

/**
 * A function that is called when a modifier finishes.
 */
fun interface OnModifierFinished {
    operator fun invoke(entity: IEntity)
}