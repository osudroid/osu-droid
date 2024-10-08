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
            if (field != value) {

                if (value != Sequence && value != Parallel) {
                    clearNestedModifiers()
                }

                field = value
            }
        }

    /**
     * Callback to be called when the modifier finishes.
     */
    var onFinished: OnModifierFinished? = null

    /**
     * Inner modifiers for [Sequence] or [Parallel] modifier types.
     */
    var modifiers: Array<UniversalModifier>? = null
        set(value) {

            when (type) {

                Parallel -> duration = value?.maxOf { it.duration } ?: 0f
                Sequence -> duration = value?.sumOf { it.duration } ?: 0f

                else -> Unit
            }

            field = value
        }

    /**
     * Easing function to be used.
     */
    var easing = Easing.None

    /**
     * The final values for the modifier.
     */
    var finalValue = 0f


    private var duration = 0f

    private var elapsedSec = -1f

    private var initialValue = 0f

    private var parent: UniversalModifier? = null


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

            initialValue = type.getCurrentValue(entity)
        }

        var consumedTimeSec = 0f

        if (type.usesNestedModifiers) {

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
                        consumedTimeSec = max(consumedTimeSec, modifier.onUpdate(remainingTimeSec, entity))
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

            type.setValue(entity, initialValue + (finalValue - initialValue) * percentage)
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

        // Nested modified can only be applied to sequence or parallel modifiers so in case this modifier
        // it's not one of them then we create a new sequence modifier and add the current modifier to it.
        // If the parent is set and it's a sequence or parallel modifier we can just add the nested
        // modifier to it.
        if (!type.usesNestedModifiers && (parent == null || !parent!!.type.usesNestedModifiers)) {
            return then().applyModifier(block)
        }

        val nestedModifier = pool?.obtain() ?: UniversalModifier()
        nestedModifier.setToDefault()
        nestedModifier.parent = this
        block(nestedModifier)

        modifiers = modifiers?.plus(nestedModifier) ?: arrayOf(nestedModifier)
        return nestedModifier
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

        // If the parent is a sequence we can just add the following modifiers to it.
        if (parent?.type == Sequence) {
            return parent!!
        }

        // A "new" sequence will be created containing this modifier as well the following modifiers in the chain will be added to it.
        if (type != Sequence) {

            val thisCopy = pool?.obtain() ?: UniversalModifier()
            thisCopy.setToDefault()
            thisCopy.setFrom(this)
            thisCopy.parent = this

            // Setting modifiers to null so setToDefault() doesn't pool them back.
            modifiers = null

            setToDefault()

            type = Sequence
            modifiers = arrayOf(thisCopy)
        }

        return this
    }


    /**
     * Sets the modifier to its default state.
     *
     * This will remove all the inner modifiers, the listener, reset the elapsed time, and set the type to [Delay].
     */
    fun setToDefault() {

        type = Delay

        duration = 0f
        finalValue = 0f
        initialValue = 0f

        parent = null
        easing = Easing.None
        onFinished = null

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

        if (type == Sequence || type == Parallel) {
            Log.w("UniversalModifier", "Cannot manually set duration for sequence or parallel modifiers, ignoring.")
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
        if (type == Sequence || type == Parallel) {
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
        modifier.finalValue = finalValue
        modifier.initialValue = initialValue
        modifier.type = type
        modifier.easing = easing
        modifier.duration = duration
        modifier.modifiers = modifiers?.map { it.deepCopy() }?.toTypedArray()
        modifier.onFinished = onFinished
    }

    fun setFrom(other: UniversalModifier) {
        type = other.type
        easing = other.easing
        duration = other.duration
        modifiers = other.modifiers
        onFinished = other.onFinished
        finalValue = other.finalValue
        initialValue = other.initialValue
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