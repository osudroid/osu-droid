package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.container.Container
import com.reco1l.andengine.ui.Control
import com.reco1l.andengine.ui.form.FormControl
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModSetting

interface IModSettingComponent<V : Any?> {
    val setting: ModSetting<V>
    fun update()
}

sealed class ModSettingComponent<TSettingValue : Any?, TControlValue : Any>(
    val mod: Mod,
    override val setting: ModSetting<TSettingValue>
) : Container(), IModSettingComponent<TSettingValue> {

    val control = createControl().apply {
        width = FillParent
    }

    init {
        width = FillParent
        +control
    }

    override fun update() {
        control.apply {
            label = setting.name
            defaultValue = convertSettingValue(setting.defaultValue)
            value = convertSettingValue(setting.value)
            valueFormatter = {
                setting.valueFormatter!!.invoke(convertControlValue(it) ?: setting.defaultValue)
            }

            onValueChanged = {
                setting.value = convertControlValue(it) ?: setting.defaultValue
                ModMenu.queueModChange(mod)
            }
        }
    }

    abstract fun createControl(): FormControl<TControlValue, Control<TControlValue>>
    abstract fun convertSettingValue(value: TSettingValue): TControlValue
    abstract fun convertControlValue(value: TControlValue): TSettingValue?
}