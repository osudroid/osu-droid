package com.rian.osu.mods.settings

/**
 * A [ModSetting] that represents a numeric value with range constraints and step increments.
 */
abstract class NumberModSetting<T>(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<T>.(T) -> String = { it.toString() },
    defaultValue: T,
    minValue: T,
    maxValue: T,
    step: T,
    orderPosition: Int? = null,

    /**
     * Whether to allow the user to input the value of this [NumberModSetting] manually.
     */
    @get:JvmName("isUseManualInput")
    val useManualInput: Boolean = false

) : RangeConstrainedModSetting<T>(
    name,
    key,
    valueFormatter,
    defaultValue,
    minValue,
    maxValue,
    orderPosition
) where T : Number, T : Comparable<T> {
    /**
     * The step increment for this [NumberModSetting].
     *
     * If set to 0, no stepping is applied.
     */
    var step = step
        set(value) {
            if (field != value) {
                if (value.toDouble() < 0) {
                    throw IllegalArgumentException("step must be non-negative.")
                }

                field = value

                // Re-apply the current value to enforce the new step.
                this.value = this.value
            }
        }

    init {
        require(step.toDouble() >= 0) { "step must be non-negative." }
    }

    override fun processValue(value: T): T {
        var processedValue = super.processValue(value)

        if (step.toDouble() > 0) {
            processedValue = snapToStep(processedValue)
        }

        return processedValue
    }

    override fun copyFrom(other: ModSetting<T>) {
        if (other is NumberModSetting<T>) {
            step = other.step
        }

        super.copyFrom(other)
    }

    /**
     * Snaps [value] to the nearest step increment. When this is called, [step] is guaranteed to be greater than 0.
     */
    protected abstract fun snapToStep(value: T): T
}

/**
 * A [ModSetting] that represents a nullable numeric value with range constraints and step increments.
 */
abstract class NullableNumberModSetting<T>(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<T?>.(T?) -> String = { it.toString() },
    defaultValue: T?,
    minValue: T,
    maxValue: T,
    step: T,
    orderPosition: Int? = null,

    /**
     * Whether to allow the user to input the value of this [NullableNumberModSetting] manually.
     */
    val useManualInput: Boolean = false

) : NullableRangeConstrainedModSetting<T>(
    name,
    key,
    valueFormatter,
    defaultValue,
    minValue,
    maxValue,
    orderPosition
) where T : Number, T : Comparable<T> {
    /**
     * The step increment for this [NullableNumberModSetting].
     *
     * If set to 0, no stepping is applied.
     */
    var step = step
        set(value) {
            if (field != value) {
                if (value.toDouble() < 0) {
                    throw IllegalArgumentException("step must be non-negative.")
                }

                field = value

                // Re-apply the current value to enforce the new step.
                this.value = this.value
            }
        }

    init {
        require(step.toDouble() >= 0) { "step must be non-negative." }
    }

    override fun processValue(value: T?): T? {
        var processedValue = super.processValue(value)

        if (processedValue != null && step.toDouble() > 0) {
            processedValue = snapToStep(processedValue)
        }

        return processedValue
    }

    override fun copyFrom(other: ModSetting<T?>) {
        if (other is NullableNumberModSetting<T>) {
            step = other.step
        }

        super.copyFrom(other)
    }

    /**
     * Snaps [value] to the nearest step increment. When this is called, [step] is guaranteed to be greater than 0.
     */
    protected abstract fun snapToStep(value: T): T
}