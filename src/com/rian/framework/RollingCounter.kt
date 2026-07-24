package com.rian.framework

import com.edlplan.framework.easing.Easing
import com.reco1l.framework.interpolate
import org.anddev.andengine.engine.handler.IUpdateHandler

/**
 * A counter that keeps track of its value over a rolling window of time.
 *
 * @param initialValue The initial value of this [RollingCounter].
 * @param T The type of the value being tracked by this [RollingCounter].
 */
abstract class RollingCounter<T>(initialValue: T) : IUpdateHandler {
    /**
     * The duration of the rolling effect, in seconds.
     *
     * If set to 0, the rolling effect will be disabled and [currentValue] will immediately change to [targetValue].
     */
    open var rollingDuration = 0f

    /**
     * The [Easing] for the rolling effect.
     */
    open var rollingEasing = Easing.OutQuad

    /**
     * The current value of this [RollingCounter]. This is the value that is being rolled towards [targetValue].
     */
    var currentValue = initialValue
        private set

    /**
     * The target value of this [RollingCounter]. This is the value that [currentValue] is rolling towards.
     */
    var targetValue = initialValue
        set(value) {
            if (field != value) {
                rollingStartValue = currentValue
                field = value
                rollingTime = 0f
            }
        }

    /**
     * Whether this [RollingCounter]'s [currentValue] is rolling towards [targetValue].
     */
    val isRolling
        get() = currentValue != targetValue

    private var rollingStartValue = initialValue
    private var rollingTime = 0f

    override fun onUpdate(pSecondsElapsed: Float) {
        if (isRolling) {
            if (rollingDuration <= 0f) {
                currentValue = targetValue
                return
            }

            rollingTime = (rollingTime + pSecondsElapsed).coerceAtMost(rollingDuration)

            val progress = rollingEasing.interpolate(rollingTime / rollingDuration)

            currentValue = interpolate(rollingStartValue, targetValue, progress)
        } else {
            currentValue = targetValue
        }
    }

    /**
     * Sets the value of this [RollingCounter] immediately, bypassing the rolling effect.
     *
     * @param value The value to set immediately.
     */
    fun setValueWithoutRolling(value: T) {
        rollingStartValue = value
        targetValue = value
        currentValue = value
        rollingTime = 0f
    }

    override fun reset() {
        currentValue = targetValue
        rollingStartValue = targetValue
        rollingTime = 0f
    }

    /**
     * Interpolates between two values of type [T] based on the given progress.
     *
     * This is used to calculate the current value during the rolling effect.
     *
     * @param startValue The starting value.
     * @param endValue The target value.
     * @param progress The progress of the rolling effect, between 0 and 1.
     * @return The interpolated value based on the progress.
     */
    protected abstract fun interpolate(startValue: T, endValue: T, progress: Float): T
}
