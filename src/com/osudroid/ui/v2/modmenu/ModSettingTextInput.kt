package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.ui.UIControl
import com.reco1l.andengine.ui.form.FloatFormInput
import com.reco1l.andengine.ui.form.FormControl
import com.reco1l.andengine.ui.form.IntegerFormInput
import com.rian.osu.mods.Mod
import com.rian.osu.mods.settings.ModSetting
import com.rian.osu.mods.settings.RangeConstrainedModSetting

sealed class ModSettingTextInput<V : Any?>(mod: Mod, setting: ModSetting<V>) :
    ModSettingComponent<V, String>(mod, setting) {

    final override fun convertSettingValue(value: V) = value?.toString() ?: setting.defaultValue?.toString() ?: ""
}

class IntegerModSettingTextInput(mod: Mod, setting: ModSetting<Int>) : ModSettingTextInput<Int>(mod, setting) {

    @Suppress("UNCHECKED_CAST")
    override fun createControl() = IntegerFormInput(
        setting.initialValue,
        (setting as? RangeConstrainedModSetting<Int>)?.minValue,
        (setting as? RangeConstrainedModSetting<Int>)?.maxValue
    ) as FormControl<String, UIControl<String>>

    override fun convertControlValue(value: String) = value.toInt()
}

class NullableIntegerModSettingTextInput(mod: Mod, setting: ModSetting<Int?>) : ModSettingTextInput<Int?>(mod, setting) {

    @Suppress("UNCHECKED_CAST")
    override fun createControl() = IntegerFormInput(
        setting.initialValue,
        (setting as? RangeConstrainedModSetting<Int?>)?.minValue,
        (setting as? RangeConstrainedModSetting<Int?>)?.maxValue
    ) as FormControl<String, UIControl<String>>

    override fun convertControlValue(value: String) = value.toIntOrNull()
}

class FloatModSettingTextInput(mod: Mod, setting: ModSetting<Float>) : ModSettingTextInput<Float>(mod, setting) {
    @Suppress("UNCHECKED_CAST")
    override fun createControl() = FloatFormInput(
        setting.initialValue,
        (setting as? RangeConstrainedModSetting<Float>)?.minValue,
        (setting as? RangeConstrainedModSetting<Float>)?.maxValue
    ) as FormControl<String, UIControl<String>>

    override fun convertControlValue(value: String) = value.toFloat()
}

class NullableFloatModSettingTextInput(mod: Mod, setting: ModSetting<Float?>) : ModSettingTextInput<Float?>(mod, setting) {
    @Suppress("UNCHECKED_CAST")
    override fun createControl() = FloatFormInput(
        setting.initialValue,
        (setting as? RangeConstrainedModSetting<Float?>)?.minValue,
        (setting as? RangeConstrainedModSetting<Float?>)?.maxValue
    ) as FormControl<String, UIControl<String>>

    override fun convertControlValue(value: String) = value.toFloatOrNull()
}