@file:Suppress("ktPropBy", "LeakingThis")

package com.rian.osu.mods.settings

import com.rian.osu.mods.Mod
import kotlin.properties.*
import kotlin.reflect.*

/**
 * Represents a [Mod] specific setting.
 */
open class ModSetting<V>(

    /**
     * The legible name of this [ModSetting].
     */
    val name: String,

    /**
     * The value formatter of this [ModSetting].
     *
     * This is used to format the value of this [ModSetting] when displaying it.
     */
    val valueFormatter: (ModSetting<V>.(V) -> String)?,

    /**
     * The default value of this [ModSetting], which is also the initial value of this [ModSetting].
     */
    open var defaultValue: V,

    /**
     * The position of this [ModSetting] in the mod customization menu.
     */
    val orderPosition: Int? = null

) : ReadWriteProperty<Any?, V>, Comparable<ModSetting<*>> {

    /**
     * The initial value.
     */
    val initialValue = defaultValue

    /**
     * The value itself.
     */
    open var value = defaultValue

    /**
     * Whether this [ModSetting] is set to its default value.
     */
    val isDefault
        get() = value == defaultValue

    /**
     * Resets this [ModSetting] to its initial state.
     */
    fun reset() {
        defaultValue = initialValue
        value = initialValue
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }

    override fun compareTo(other: ModSetting<*>) = when {
        orderPosition == other.orderPosition -> 0
        // Unordered settings come last (are greater than any ordered settings).
        orderPosition == null -> 1
        other.orderPosition == null -> -1
        // Ordered settings are sorted by their order position.
        else -> orderPosition.compareTo(other.orderPosition)
    }
}

/**
 * Represents a [Mod] specific setting whose value is constrained to a range of values.
 */
abstract class RangeConstrainedModSetting<V>(
    name: String,
    valueFormatter: ModSetting<V>.(V) -> String = { it.toString() },
    defaultValue: V,

    /**
     * The minimum value of this [RangeConstrainedModSetting].
     */
    minValue: V & Any,

    /**
     * The maximum value of this [RangeConstrainedModSetting].
     */
    maxValue: V & Any,

    /**
     * The step size for the value of this [RangeConstrainedModSetting].
     */
    step: V & Any,

    /**
     * The position of this [RangeConstrainedModSetting] in the mod customization menu.
     */
    orderPosition: Int? = null

) : ModSetting<V>(name, valueFormatter, defaultValue, orderPosition) {
    /**
     * The minimum value of this [RangeConstrainedModSetting].
     */
    open var minValue = minValue
        set(value) {
            if (field != value) {
                field = value

                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    /**
     * The maximum value of this [RangeConstrainedModSetting].
     */
    open var maxValue = maxValue
        set(value) {
            if (field != value) {
                field = value

                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    /**
     * The step size for the value of this [RangeConstrainedModSetting].
     */
    open var step = step
        set(value) {
            if (field != value) {
                field = value

                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    override var value
        get() = super.value
        set(value) {
            super.value = processValue(value)
        }

    protected abstract fun processValue(value: V): V

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        super.setValue(thisRef, property, processValue(value))
    }
}