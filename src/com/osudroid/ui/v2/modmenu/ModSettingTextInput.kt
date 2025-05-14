package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.ui.Control
import com.reco1l.andengine.ui.form.FloatFormInput
import com.reco1l.andengine.ui.form.FormControl
import com.reco1l.andengine.ui.form.IntegerFormInput
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModSetting
import com.rian.osu.mods.RangeConstrainedModSetting

sealed class ModSettingTextInput<V : Any?>(mod: Mod, setting: ModSetting<V>) :
    ModSettingComponent<V, String>(mod, setting)

class IntegerModSettingTextInput(mod: Mod, setting: ModSetting<Int>) : ModSettingTextInput<Int>(mod, setting) {

    @Suppress("UNCHECKED_CAST")
    override fun createControl() = IntegerFormInput(
        setting.initialValue,
        (setting as? RangeConstrainedModSetting<Int>)?.minValue,
        (setting as? RangeConstrainedModSetting<Int>)?.maxValue
    ) as FormControl<String, Control<String>>

    override fun convertSettingValue(value: Int) = value.toString()
    override fun convertControlValue(value: String) = value.toIntOrNull()
}

class NullableIntegerModSettingTextInput(mod: Mod, setting: ModSetting<Int?>) : ModSettingTextInput<Int?>(mod, setting) {

    @Suppress("UNCHECKED_CAST")
    override fun createControl() = IntegerFormInput(
        setting.initialValue,
        (setting as? RangeConstrainedModSetting<Int?>)?.minValue,
        (setting as? RangeConstrainedModSetting<Int?>)?.maxValue
    ) as FormControl<String, Control<String>>

    override fun convertSettingValue(value: Int?) = value?.toString() ?: control.defaultValue
    override fun convertControlValue(value: String) = value.toIntOrNull()
}

class FloatModSettingTextInput(mod: Mod, setting: ModSetting<Float>) : ModSettingTextInput<Float>(mod, setting) {
    @Suppress("UNCHECKED_CAST")
    override fun createControl() = FloatFormInput(
        setting.initialValue,
        (setting as? RangeConstrainedModSetting<Float>)?.minValue,
        (setting as? RangeConstrainedModSetting<Float>)?.maxValue
    ) as FormControl<String, Control<String>>

    override fun convertSettingValue(value: Float) = value.toString()
    override fun convertControlValue(value: String) = value.toFloatOrNull()
}

class NullableFloatModSettingTextInput(mod: Mod, setting: ModSetting<Float?>) : ModSettingTextInput<Float?>(mod, setting) {
    @Suppress("UNCHECKED_CAST")
    override fun createControl() = FloatFormInput(
        setting.initialValue,
        (setting as? RangeConstrainedModSetting<Float?>)?.minValue,
        (setting as? RangeConstrainedModSetting<Float?>)?.maxValue
    ) as FormControl<String, Control<String>>

    override fun convertSettingValue(value: Float?) = value?.toString() ?: control.defaultValue
    override fun convertControlValue(value: String) = value.toFloatOrNull()
}