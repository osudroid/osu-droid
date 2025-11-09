@file:Suppress("ktPropBy", "LeakingThis")

package com.rian.osu.mods.settings

import com.rian.osu.mods.Mod
import kotlin.properties.*
import kotlin.reflect.*
import org.json.JSONObject

/**
 * Represents a [Mod] specific setting.
 */
open class ModSetting<V>(

    /**
     * The legible name of this [ModSetting].
     */
    val name: String,

    /**
     * The key used to load and save this [ModSetting] from a [JSONObject].
     *
     * If set to `null`, this [ModSetting] will not be loaded or saved.
     */
    val key: String? = null,

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

    /**
     * Loads the value of this [ModSetting] from a [JSONObject].
     *
     * @param json The [JSONObject] to load the value from.
     */
    open fun load(json: JSONObject) {
        if (key != null) {
            @Suppress("UNCHECKED_CAST")
            value = json.opt(key) as? V ?: defaultValue
        }
    }

    /**
     * Saves the value of this [ModSetting] to a [JSONObject].
     *
     * @param json The [JSONObject] to save the value to.
     */
    open fun save(json: JSONObject) {
        json.putOpt(key, value)
    }

    /**
     * Copies another [ModSetting] into this [ModSetting].
     *
     * @param other The other [ModSetting] to copy from.
     */
    open fun copyFrom(other: ModSetting<V>) {
        defaultValue = other.defaultValue
        value = other.value
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
    key: String? = null,
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

) : ModSetting<V>(name, key, valueFormatter, defaultValue, orderPosition) {
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

    override fun copyFrom(other: ModSetting<V>) {
        super.copyFrom(other)

        if (other is RangeConstrainedModSetting<V>) {
            minValue = other.minValue
            maxValue = other.maxValue
            step = other.step
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        super.setValue(thisRef, property, processValue(value))
    }
}