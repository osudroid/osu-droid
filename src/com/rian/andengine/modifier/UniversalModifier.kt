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
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.util.modifier.IModifier

/**
 * An [IEntityModifier] that can be used to apply different types of modifications to a [UIComponent]. This allows
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
    IEntityModifier, IPoolable {
    override var isRecycled = false

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
    private var duration = 0f
        set(value) {
            field = max(0f, value)
        }

    /**
     * The time at which this [UniversalModifier] ends, in seconds relative to [target]'s [IClock].
     */
    val endTime: Float
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
    var applied = false
        private set

    /**
     * The [Easing] function applied to this [UniversalModifier].
     */
    private var easing = Easing.None

    /**
     * Invoked when this [UniversalModifier] finishes.
     */
    private var onFinished: OnModifierFinished? = null

    /**
     * Sets the callback to be invoked when this [UniversalModifier] finishes.
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

    private var lastUpdateTime = 0f

    override fun onUpdate(pSecondsElapsed: Float, pItem: IEntity?): Float {
        if (pSecondsElapsed == 0f) {
            return 0f
        }

        val target = target ?: return 0f
        val time = target.time ?: return 0f

        if (time.current < startTime) {
            return 0f
        }

        if (!hasInitialValues) {
            initialValues = type.getInitialValues(target, initialValues)
            hasInitialValues = true
        }

        if (!applied) {
            lastUpdateTime = startTime
            applied = true
        }

        val elapsed = min(time.current, endTime) - startTime
        val percentage = if (duration > 0) easing.interpolate(elapsed / duration).coerceIn(0f, 1f) else 1f

        type.setValues(target, initialValues, finalValues, percentage)

        val deltaSec = time.current - lastUpdateTime
        lastUpdateTime += deltaSec

        if (isFinished) {
            lastUpdateTime = endTime
            onFinished?.invoke(target)
        }

        return max(0f, deltaSec)
    }

    override fun onUnregister() {
        reset()

        pool?.release(this)
    }

    override fun reset() {
        target = null
        type = ModifierType.None
        startTime = 0f
        duration = 0f
        easing = Easing.None
        hasInitialValues = false
        applied = false
        onFinished = null
        lastUpdateTime = 0f
    }

    override fun isFinished() = lastUpdateTime >= endTime
    override fun isRemoveWhenFinished() = true
    override fun setRemoveWhenFinished(pRemoveWhenFinished: Boolean) = Unit

    override fun getSecondsElapsed() = max(0f, lastUpdateTime - startTime)
    override fun getDuration() = duration

    /**
     * Sets the duration of this [UniversalModifier].
     *
     * @param duration The duration of this [UniversalModifier], in seconds relative to [startTime].
     */
    fun setDuration(duration: Float): UniversalModifier {
        this.duration = duration

        return this
    }

    override fun addModifierListener(pModifierListener: IModifier.IModifierListener<IEntity?>?) = Unit
    override fun removeModifierListener(pModifierListener: IModifier.IModifierListener<IEntity?>?) = false

    override fun deepCopy(): UniversalModifier = UniversalModifier(pool).also { modifier ->
        modifier.target = target
        modifier.type = type
        modifier.startTime = startTime
        modifier.duration = duration
        modifier.easing = easing
        modifier.hasInitialValues = hasInitialValues
        modifier.onFinished = onFinished
        modifier.lastUpdateTime = lastUpdateTime
        modifier.initialValues = initialValues.copyOf()
        modifier.finalValues = finalValues.copyOf()
    }

    companion object {
        val GlobalPool = SynchronizedPool<UniversalModifier>(200).apply {
            release(UniversalModifier(this))
        }
    }
}

fun interface OnModifierFinished {
    operator fun invoke(entity: IEntity)
}