package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.rian.osu.mods.Mod
import com.rian.osu.mods.settings.*

class ModSettingEnum(mod: Mod, setting: EnumModSetting<Enum<*>>) :
    ModSettingComponent<Enum<*>, List<Enum<*>>>(mod, setting),
    IModSettingComponent<Enum<*>> {

    @Suppress("UNCHECKED_CAST")
    override fun createControl() = FormSelect(listOf(setting.initialValue)) as FormControl<List<Enum<*>>, Control<List<Enum<*>>>>

    override fun update() {
        setting as EnumModSetting<*>

        (control.control as Select).apply {
            options = setting.entries.map {
                Select.Option(
                    value = it,
                    text = setting.valueFormatter?.invoke(setting, it) ?: it.toString()
                )
            }
        }

        super.update()
    }

    // We return the first value because the mod setting only allows single selection.
    override fun convertControlValue(value: List<Enum<*>>) = value[0]
    override fun convertSettingValue(value: Enum<*>) = listOf(value)

}
