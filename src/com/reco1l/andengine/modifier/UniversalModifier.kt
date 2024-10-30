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
        set(value) {
            if (value != null && !type.isCompoundModifier) {
                Log.w("UniversalModifier", "Cannot set inner modifiers for non-compound modifiers.")
                return
            }

            var totalDuration = 0f

            value?.fastForEach {
                it.parent = this

                if (type == Sequence) {
                    totalDuration += it.duration
                } else {
                    totalDuration = max(totalDuration, it.duration)
                }
            }

            field = value
            duration = totalDuration
        }

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
        modifiers?.fastForEach { it.onUnregister() }
        modifiers = null
    }

    override fun onUpdate(deltaSec: Float, entity: IEntity): Float {

        if (elapsedSec >= duration || deltaSec == 0f) {
            return 0f
        }

        // We use negative elapsed time to know whether the modifier has started or not yet.
        if (elapsedSec < 0) {
            elapsedSec = 0f

            if (!type.isCompoundModifier && initialValues == null) {
                initialValues = type.getInitialValues(entity)
            }
        }

        val consumedDeltaSec: Float

        if (type.isCompoundModifier) {

            var remainingDeltaSec = deltaSec

            while (remainingDeltaSec > 0f && modifiers != null && !isFinished) {

                var allModifiersFinished = true

                if (type == Parallel) {

                    var maxConsumedDeltaSec = 0f

                    for (modifier in modifiers!!) {
                        if (!modifier.isFinished) {
                            allModifiersFinished = false
                            maxConsumedDeltaSec = max(maxConsumedDeltaSec, modifier.onUpdate(remainingDeltaSec, entity))
                        }
                    }

                    remainingDeltaSec -= maxConsumedDeltaSec
                    elapsedSec += maxConsumedDeltaSec

                } else if (type == Sequence) {

                    var currentConsumedDeltaSec = 0f

                    for (modifier in modifiers!!) {
                        if (!modifier.isFinished) {
                            allModifiersFinished = false
                            currentConsumedDeltaSec = modifier.onUpdate(remainingDeltaSec, entity)
                            break
                        }
                    }

                    remainingDeltaSec -= currentConsumedDeltaSec
                    elapsedSec += currentConsumedDeltaSec

                }

                // This is a workaround for an issue that seems to be caused by floating point precision where in some cases the
                // elapsed time never reaches the duration even if all nested modifiers are finished causing a deadlock.
                // The "while" loop (which is based on "remainingDeltaSec") will loop infinitely because the remaining time will
                // never reach 0 due to all modifiers being finished and the consumed time they'll give is always 0.
                // In order to prevent this, we'll manually set the elapsed time to the duration if all modifiers are finished,
                // as well the consumed time will be set to the difference between the duration and the elapsed time.
                if (allModifiersFinished && !isFinished) {
                    remainingDeltaSec -= duration - elapsedSec
                    elapsedSec = duration
                }
            }

            consumedDeltaSec = deltaSec - max(0f, remainingDeltaSec)

        } else {

            consumedDeltaSec = min(duration - elapsedSec, deltaSec)
            elapsedSec += consumedDeltaSec

            if (type != Delay) {

                // Assuming the percentage is 1 if the duration is 0 to prevent division by zero.
                val percentage = if (duration > 0f) easing.interpolate(elapsedSec / duration) else 1f

                if (initialValues != null && finalValues != null) {
                    type.setValues(entity, initialValues!!, finalValues!!, percentage)
                }
            }
        }

        if (elapsedSec >= duration) {
            elapsedSec = duration
            onFinished?.invoke(entity)
        }

        return max(0f, consumedDeltaSec)
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
        modifiers?.fastForEach { it.reset() }
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

        duration = max(0f, value)
    }


    override fun getDuration(): Float {
        return duration
    }

    override fun getSecondsElapsed(): Float {
        return elapsedSec
    }


    override fun isFinished(): Boolean {
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