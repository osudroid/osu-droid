package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.mods.*
import ru.nsu.ccfit.zuev.osu.ResourceManager

class ModCustomizationMenu : Modal(

    card = ScrollableContainer().apply {
        scrollAxes = Axes.Y
        relativeSizeAxes = Axes.Both
        width = 0.475f
        height = 0.75f
        x = 60f
        y = 90f
        scaleCenter = Anchor.TopCenter
        clipToBounds = true

        +LinearContainer().apply {
            width = FillParent
            orientation = Orientation.Vertical
        }
    }

) {

    private val modSettings: LinearContainer = card[0]
    private val modSettingComponents = mutableListOf<IModSettingComponent<*>>()


    //region Mods

    fun onModAdded(mod: Mod) {
        val settings = mod.settings

        if (settings.isNotEmpty()) {
            modSettings.attachChild(ModSettingsSection(mod, settings))
        }
    }

    fun onModRemoved(mod: Mod) {
        modSettings.detachChild { it is ModSettingsSection && it.mod == mod }
    }

    fun isEmpty(): Boolean {
        return modSettings.findChild { it is ModSettingsSection } == null
    }

    //endregion

    //region Component lifecycle

    fun updateComponents() {
        modSettingComponents.fastForEach { it.update() }
    }

    private fun ModSettingComponent(mod: Mod, setting: ModSetting<*>): FormControl<*, *> {

        val component = when (setting) {
            is FloatModSetting -> FloatModSettingSlider(mod, setting)
            is IntegerModSetting -> IntegerModSettingSlider(mod, setting)
            is NullableFloatModSetting -> NullableModSettingSlider(mod, setting)
            is BooleanModSetting -> ModSettingCheckbox(mod, setting)
        }
        modSettingComponents.add(component)
        component.update()
        return component
    }

    //endregion


    private inner class ModSettingsSection(val mod: Mod, settings: List<ModSetting<*>>) : LinearContainer() {

        init {
            orientation = Orientation.Vertical
            width = FillParent
            padding = Vec4(0f, 0f, 0f, 16f)

            +LinearContainer().apply {
                orientation = Orientation.Horizontal
                width = FillParent
                padding = Vec4(20f, 14f)
                spacing = 12f
                background = Box().apply {
                    color = ColorARGB.Black
                    alpha = 0.05f
                    cornerRadius = 12f
                }

                +ModIcon(mod).apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    width = 34f
                    height = 34f
                }

                +ExtendedText().apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = mod.name.uppercase()
                    applyTheme = {
                        color = it.accentColor
                        alpha = 0.9f
                    }
                }
            }

            settings.fastForEach { +ModSettingComponent(mod, it) }
        }
    }
}


//region Components

private interface IModSettingComponent<V : Any?> {
    val setting: ModSetting<V>
    fun update()
}

private sealed class ModSettingSlider<V : Number?>(val mod: Mod, override val setting: ModSetting<V>) :
    FormSlider(setting.initialValue?.toFloat() ?: 0f),
    IModSettingComponent<V> {

    final override fun update() {
        label = setting.name

        if (setting is RangeConstrainedModSetting<V>) {
            val setting = setting as RangeConstrainedModSetting<V>
            control.min = setting.minValue!!.toFloat()
            control.max = setting.maxValue!!.toFloat()
            control.step = setting.step!!.toFloat()
        }

        defaultValue = setting.defaultValue?.toFloat() ?: 0f
        value = setting.value?.toFloat() ?: 0f
        valueFormatter = { setting.valueFormatter!!.invoke(convertValue(it)) }
        onValueChanged = {
            setting.value = convertValue(it)
            ModMenu.queueModChange(mod)
        }
    }

    abstract fun convertValue(value: Float): V
}

private class IntegerModSettingSlider(mod: Mod, setting: ModSetting<Int>) : ModSettingSlider<Int>(mod, setting) {
    override fun convertValue(value: Float) = value.toInt()
}

private class FloatModSettingSlider(mod: Mod, setting: ModSetting<Float>) : ModSettingSlider<Float>(mod, setting) {
    override fun convertValue(value: Float) = value
}

private class NullableModSettingSlider(mod: Mod, setting: ModSetting<Float?>) : ModSettingSlider<Float?>(mod, setting) {
    override fun convertValue(value: Float) = if (value == setting.defaultValue) null else value
}

private class ModSettingCheckbox(val mod: Mod, override val setting: ModSetting<Boolean>) :
    FormCheckbox(setting.initialValue),
    IModSettingComponent<Boolean> {

    override fun update() {
        label = setting.name
        defaultValue = setting.defaultValue
        value = setting.value
        onValueChanged = {
            setting.value = it
            ModMenu.queueModChange(mod)
        }
    }
}

//endregion