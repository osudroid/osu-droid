@file:Suppress("ktPropBy", "LeakingThis")

package com.rian.osu.mods.settings

import com.rian.osu.mods.Mod
import kotlin.properties.*
import kotlin.reflect.*
import kotlinx.serialization.json.*
import org.json.JSONObject

/**
 * Represents a [Mod] specific setting.
 */
abstract class ModSetting<T>(

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
    val valueFormatter: (ModSetting<T>.(T) -> String)?,

    /**
     * The default value of this [ModSetting], which is also the initial value of this [ModSetting].
     */
    open var defaultValue: T,

    /**
     * The position of this [ModSetting] in the mod customization menu.
     */
    val orderPosition: Int? = null

) : ReadWriteProperty<Any?, T>, Comparable<ModSetting<*>> {

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
     * Loads the value of this [ModSetting] from a [JsonObject].
     *
     * @param json The [JsonObject] to load the value from.
     */
    abstract fun load(json: JsonObject)

    /**
     * Saves the value of this [ModSetting] to a [JsonObjectBuilder].
     *
     * @param builder The [JsonObjectBuilder] to save the value to.
     */
    abstract fun save(builder: JsonObjectBuilder)

    /**
     * Copies another [ModSetting] into this [ModSetting].
     *
     * @param other The other [ModSetting] to copy from.
     */
    open fun copyFrom(other: ModSetting<T>) {
        defaultValue = other.defaultValue
        value = other.value
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
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
 * An interface for [ModSetting]s whose value is constrained to a range of values.
 */
interface IRangeConstrainedModSetting<T : Comparable<T>> {
    /**
     * The minimum value of this [IRangeConstrainedModSetting].
     */
    var minValue: T

    /**
     * The maximum value of this [IRangeConstrainedModSetting].
     */
    var maxValue: T
}

/**
 * Represents a [Mod] specific setting whose value is constrained to a range of values.
 */
abstract class RangeConstrainedModSetting<T : Comparable<T>>(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<T>.(T) -> String = { it.toString() },
    defaultValue: T,

    /**
     * The minimum value of this [RangeConstrainedModSetting].
     */
    minValue: T,

    /**
     * The maximum value of this [RangeConstrainedModSetting].
     */
    maxValue: T,

    /**
     * The position of this [RangeConstrainedModSetting] in the mod customization menu.
     */
    orderPosition: Int? = null

) : ModSetting<T>(name, key, valueFormatter, defaultValue, orderPosition), IRangeConstrainedModSetting<T> {
    final override var defaultValue
        get() = super.defaultValue
        set(value) {
            require(value in minValue..maxValue) { "defaultValue must be between minValue and maxValue." }

            super.defaultValue = value
        }

    final override var minValue = minValue
        set(value) {
            if (field != value) {
                require(value <= maxValue) { "minValue cannot be greater maxValue." }

                field = value

                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    final override var maxValue = maxValue
        set(value) {
            if (field != value) {
                require(value >= minValue) { "maxValue cannot be less than minValue." }

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

    init {
        require(minValue <= maxValue) { "minValue must be less than or equal to maxValue." }
        require(defaultValue in minValue..maxValue) { "defaultValue must be between minValue and maxValue." }
    }

    /**
     * Processes [value] to ensure it meets constraints.
     *
     * @return The processed value.
     */
    protected open fun processValue(value: T) = value.coerceIn(minValue, maxValue)

    override fun copyFrom(other: ModSetting<T>) {
        if (other is RangeConstrainedModSetting<T>) {
            // When copying, we need to respect constraints. For example, assigning the other minValue first when the
            // current maxValue is smaller it would cause an exception.
            if (other.minValue > maxValue) {
                maxValue = other.maxValue
                minValue = other.minValue
            } else {
                minValue = other.minValue
                maxValue = other.maxValue
            }
        }

        super.copyFrom(other)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        super.setValue(thisRef, property, processValue(value))
    }
}

/**
 * Represents a [Mod] specific setting whose value is nullable and constrained to a range of values.
 */
abstract class NullableRangeConstrainedModSetting<T>(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<T?>.(T?) -> String = { it.toString() },
    defaultValue: T?,

    /**
     * The minimum value of this [NullableRangeConstrainedModSetting].
     */
    minValue: T,

    /**
     * The maximum value of this [NullableRangeConstrainedModSetting].
     */
    maxValue: T,

    /**
     * The position of this [NullableRangeConstrainedModSetting] in the mod customization menu.
     */
    orderPosition: Int? = null

) : ModSetting<T?>(name, key, valueFormatter, defaultValue, orderPosition), IRangeConstrainedModSetting<T> where T : Comparable<T> {
    final override var defaultValue
        get() = super.defaultValue
        set(value) {
            require(value == null || value in minValue..maxValue) {
                "defaultValue must be null or between minValue and maxValue."
            }

            super.defaultValue = value
        }

    final override var minValue = minValue
        set(value) {
            require(value <= maxValue) { "minValue cannot be greater maxValue." }

            if (field != value) {
                field = value

                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    final override var maxValue = maxValue
        set(value) {
            require(value >= minValue) { "maxValue cannot be less than minValue." }

            if (field != value) {
                field = value

                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    final override var value
        get() = super.value
        set(value) {
            super.value = processValue(value)
        }

    init {
        require(minValue <= maxValue) { "minValue must be less than or equal to maxValue." }

        require(defaultValue == null || defaultValue in minValue..maxValue) {
            "defaultValue must be null or between minValue and maxValue."
        }
    }

    /**
     * Processes [value] to ensure it meets constraints.
     */
    protected open fun processValue(value: T?) = value?.coerceIn(minValue, maxValue)

    override fun copyFrom(other: ModSetting<T?>) {
        if (other is NullableRangeConstrainedModSetting<T>) {
            minValue = other.minValue
            maxValue = other.maxValue
        }

        super.copyFrom(other)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        super.setValue(thisRef, property, processValue(value))
    }
}