package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.ui.UIControl
import com.reco1l.andengine.ui.form.FormControl
import com.reco1l.andengine.ui.form.FormSlider
import com.rian.osu.mods.Mod
import com.rian.osu.mods.settings.ModSetting
import com.rian.osu.mods.settings.RangeConstrainedModSetting

sealed class ModSettingSlider<V : Number?>(mod: Mod, setting: ModSetting<V>) :
    ModSettingComponent<V, Float>(mod, setting) {

    final override fun update() {
        val slider = (control as FormSlider).control

        if (setting is RangeConstrainedModSetting<V>) {
            val setting = setting as RangeConstrainedModSetting<V>

            // Assigning the min, max and step values to the slider may unexpectedly change the value of the setting
            // due to their boundaries. Since the base update call will update the value of the control, we do not want
            // to trigger its onValueChanged() here (avoid calling it repetitively).
            val valueChanged = control.onValueChanged
            control.onValueChanged = null

            slider.min = convertSettingValue(setting.minValue)
            slider.max = convertSettingValue(setting.maxValue)
            slider.step = convertSettingValue(setting.step)

            control.onValueChanged = valueChanged
        }

        super.update()
    }

    @Suppress("UNCHECKED_CAST")
    final override fun createControl() =
        FormSlider(convertSettingValue(setting.initialValue)) as FormControl<Float, UIControl<Float>>

    final override fun convertSettingValue(value: V) = value?.toFloat() ?: setting.defaultValue?.toFloat() ?: 0f
}

class IntegerModSettingSlider(mod: Mod, setting: ModSetting<Int>) : ModSettingSlider<Int>(mod, setting) {
    override fun convertControlValue(value: Float) = value.toInt()
}

class NullableIntegerModSettingSlider(mod: Mod, setting: ModSetting<Int?>) :
    ModSettingSlider<Int?>(mod, setting) {
    override fun convertControlValue(value: Float): Int? {
        val converted = value.toInt()

        return if (converted == setting.defaultValue) null else converted
    }
}

class FloatModSettingSlider(mod: Mod, setting: ModSetting<Float>) : ModSettingSlider<Float>(mod, setting) {
    override fun convertControlValue(value: Float) = value
}

class NullableFloatModSettingSlider(
    mod: Mod,
    setting: ModSetting<Float?>
) : ModSettingSlider<Float?>(mod, setting) {
    override fun convertControlValue(value: Float) = if (value == setting.defaultValue) null else value
}