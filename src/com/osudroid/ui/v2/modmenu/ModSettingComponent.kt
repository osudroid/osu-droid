package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.container.Container
import com.reco1l.andengine.ui.Control
import com.reco1l.andengine.ui.form.FormControl
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModSetting

/**
 * Interface for a component that controls a [ModSetting].
 */
interface IModSettingComponent<V : Any?> {
    /**
     * The [ModSetting] that is controlled by this [IModSettingComponent].
     */
    val setting: ModSetting<V>

    /**
     * Updates this [IModSettingComponent] to reflect the current state of the [ModSetting].
     */
    fun update()
}

/**
 * A component that controls a [ModSetting].
 *
 * @param TSettingValue The type of the value in the [ModSetting].
 * @param TControlValue The type of the value in the [FormControl] that is used to display this [ModSettingComponent].
 */
sealed class ModSettingComponent<TSettingValue : Any?, TControlValue : Any>(
    val mod: Mod,
    override val setting: ModSetting<TSettingValue>
) : Container(), IModSettingComponent<TSettingValue> {
    /**
     * The [FormControl] that is used to display this [ModSettingComponent].
     */
    protected val control = createControl().apply {
        width = FillParent

        label = setting.name

        valueFormatter = {
            setting.valueFormatter?.invoke(setting, convertControlValue(it)) ?: it.toString()
        }

        onValueChanged = {
            setting.value = convertControlValue(it)
            ModMenu.queueModChange(mod)
        }
    }

    init {
        width = FillParent
        +control
    }

    override fun update() {
        control.apply {
            defaultValue = convertSettingValue(setting.defaultValue)
            value = convertSettingValue(setting.value)
        }
    }

    /**
     * Creates the [FormControl] that is used to display this [ModSettingComponent].
     */
    protected abstract fun createControl(): FormControl<TControlValue, Control<TControlValue>>

    /**
     * Converts a value from the [ModSetting] to a value that can be displayed in the [FormControl].
     *
     * @param value The value from the [ModSetting].
     * @return The value that can be displayed in the [FormControl].
     */
    protected abstract fun convertSettingValue(value: TSettingValue): TControlValue

    /**
     * Converts a value from the [FormControl] to a value that can be used in the [ModSetting].
     *
     * @param value The value from the [FormControl].
     * @return The value that can be used in the [ModSetting].
     */
    protected abstract fun convertControlValue(value: TControlValue): TSettingValue
}