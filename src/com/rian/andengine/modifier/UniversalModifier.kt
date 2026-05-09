package com.rian.andengine.modifier

import androidx.core.util.Pools.Pool
import com.edlplan.framework.easing.Easing
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.rian.andengine.timing.IClock
import com.reco1l.andengine.component.UIComponent
import com.reco1l.framework.interpolate
import kotlin.math.max
import kotlin.math.min
import org.anddev.andengine.entity.modifier.IEntityModifier

/**
 * A modifier that can be used to apply different types of modifications to a [UIComponent]. This allows
 * instances of the class to be recycled in a pool.
 *
 * Unlike regular [IEntityModifier]s, which rely on relative timing from the frame it is registered, [UniversalModifier]
 * uses absolute timing from the [target]'s [IClock] to determine when it should run, effectively removing the need of
 * delay, parallel, and sequence modifiers.
 *
 * @see ModifierType
 * @author Reco1l, Rian8337
 */
class UniversalModifier @JvmOverloads constructor(private val pool: Pool<UniversalModifier>? = GlobalPool) :
    IPoolable, Comparable<UniversalModifier> {
    override var isRecycled = false

    /**
     * The identifier of this [UniversalModifier], used for comparison against other [UniversalModifier]s.
     */
    var id = -1L

    /**
     * The [UIComponent] to which this [UniversalModifier] is applied.
     */
    var target: UIComponent? = null

    /**
     * The type of this [UniversalModifier].
     */
    var type = ModifierType.None

    /**
     * The time at which this [UniversalModifier] should start applying, in seconds relative to [target]'s [IClock].
     */
    var startTime = 0f

    /**
     * The duration of this [UniversalModifier], in seconds relative to [startTime].
     */
    var duration = 0f
        set(value) {
            field = max(0f, value)
        }

    /**
     * The time at which this [UniversalModifier] ends, in seconds relative to [target]'s [IClock].
     */
    val endTime
        get() = startTime + duration

    /**
     * The initial values for this [UniversalModifier].
     */
    var initialValues = FloatArray(3)

    /**
     * The final values for this [UniversalModifier].
     */
    var finalValues = FloatArray(3)

    /**
     * Whether this [UniversalModifier] has initial values. If `false`, they will be retrieved when this
     * [UniversalModifier] is about to start being applied.
     */
    var hasInitialValues = false

    /**
     * Whether this [UniversalModifier] has been applied to [target].
     */
    @get:JvmName("isApplied")
    var applied = false
        private set

    /**
     * Whether this [UniversalModifier] has been applied to [target] until the end.
     */
    @get:JvmName("isAppliedToEnd")
    var appliedToEnd = false
        private set

    /**
     * Invoked when this [UniversalModifier] has finished and is about to be removed from a
     * [UniversalModifierTargetTracker].
     */
    var onFinished: OnModifierFinished? = null

    /**
     * The [Easing] function applied to this [UniversalModifier].
     */
    var easing = Easing.None

    /**
     * Sets the callback to be invoked when this [UniversalModifier] has finished and is about to be removed from a
     * [UniversalModifierTargetTracker].
     */
    fun after(onFinished: OnModifierFinished?): UniversalModifier {
        this.onFinished = onFinished

        return this
    }

    /**
     * Sets the [Easing] function applied to this [UniversalModifier].
     */
    fun eased(easing: Easing): UniversalModifier {
        this.easing = easing

        return this
    }

    /**
     * Applies this [UniversalModifier] to [target] based on the provided time.
     *
     * @param time The time.
     */
    fun apply(time: Float) {
        val target = target ?: return

        if (!hasInitialValues) {
            initialValues = type.getInitialValues(target, initialValues)
            hasInitialValues = true
        }

        applied = true

        val elapsed = min(time, endTime) - startTime
        val percentage = if (duration > 0) easing.interpolate(elapsed / duration).coerceIn(0f, 1f) else 1f

        type.setValues(target, initialValues, finalValues, percentage)

        appliedToEnd = time >= endTime
    }

    /**
     * Releases this [UniversalModifier] back to the pool.
     */
    fun release() {
        target = null
        type = ModifierType.None
        startTime = 0f
        duration = 0f
        easing = Easing.None
        hasInitialValues = false
        applied = false
        appliedToEnd = false
        onFinished = null

        pool?.release(this)
    }

    override fun compareTo(other: UniversalModifier) =
        startTime.compareTo(other.startTime).takeIf { it != 0 } ?: id.compareTo(other.id)

    companion object {
        val GlobalPool = SynchronizedPool<UniversalModifier>(200).apply {
            release(UniversalModifier(this))
        }
    }
}