package com.reco1l.osu.ui.modmenu

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

    content = ScrollableContainer().apply {
        scrollAxes = Axes.Y
        relativeSizeAxes = Axes.Both
        width = 0.60f
        height = 0.75f
        x = 60f
        y = 90f
        scaleCenter = Anchor.TopCenter
        clipChildren = true

        +LinearContainer().apply {
            width = FitParent
            orientation = Orientation.Vertical
        }
    }

) {

    private val modSettings: LinearContainer = content[0]

    private val modSettingComponents = mutableListOf<IModSettingComponent<*>>()


    init {
        ResourceManager.getInstance().loadHighQualityAsset("reset", "reset.png")

        theme = ModalTheme(backgroundColor = 0xFF181828)
    }


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
            is FloatModSetting -> ModSettingSlider(mod, setting)
            is NullableFloatModSetting -> NullableModSettingSlider(mod, setting)
            is BooleanModSetting -> ModSettingCheckbox(mod, setting)

            else -> throw IllegalArgumentException("Unsupported setting type or component not defined for: ${setting::class}")
        }
        modSettingComponents.add(component)
        component.update()
        return component
    }

    //endregion


    private inner class ModSettingsSection(val mod: Mod, settings: List<ModSetting<*>>) : LinearContainer() {

        init {
            orientation = Orientation.Vertical
            width = FitParent
            padding = Vec4(0f, 0f, 0f, 16f)

            +LinearContainer().apply {
                orientation = Orientation.Horizontal
                width = FitParent
                padding = Vec4(20f, 14f)
                spacing = 12f
                background = RoundedBox().apply {
                    color = ColorARGB(0xFF1A1A2B)
                    cornerRadius = 16f
                }

                +ModIcon(mod).apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    width = 36f
                    height = 36f
                }

                +ExtendedText().apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = mod.name.uppercase()
                    color = ColorARGB(0xFF8282A8)
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

private class ModSettingSlider(val mod: Mod, override val setting: ModSetting<Float>) :
    FormSlider(setting.initialValue),
    IModSettingComponent<Float> {

    override fun update() {
        label = setting.name

        if (setting is RangeConstrainedModSetting<Float>) {
            control.min = setting.minValue
            control.max = setting.maxValue
            control.step = setting.step
        }

        defaultValue = setting.defaultValue
        value = setting.value
        valueFormatter = setting.valueFormatter!!
        onValueChanged = {
            setting.value = it
            ModMenuV2.onModsChanged(mod)
        }
    }
}

private class NullableModSettingSlider(val mod: Mod, override val setting: ModSetting<Float?>) :
    FormSlider(setting.initialValue ?: 0f),
    IModSettingComponent<Float?> {

    override fun update() {
        label = setting.name

        if (setting is RangeConstrainedModSetting<Float?>) {
            control.min = setting.minValue!!
            control.max = setting.maxValue!!
            control.step = setting.step!!
        }

        defaultValue = setting.defaultValue ?: 0f
        value = setting.value ?: setting.defaultValue ?: 0f
        valueFormatter = setting.valueFormatter!!
        onValueChanged = { value ->
            setting.value = if (value == setting.defaultValue) null else value
            ModMenuV2.onModsChanged(mod)
        }
    }
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
            ModMenuV2.onModsChanged(mod)
        }
    }
}

//endregion