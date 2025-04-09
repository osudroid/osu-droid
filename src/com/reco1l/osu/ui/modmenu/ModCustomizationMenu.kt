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
        anchor = Anchor.TopLeft
        origin = Anchor.TopLeft
        scaleCenter = Anchor.Center
        width = 0.60f
        height = 0.75f
        x = 60f
        y = 90f
        clipChildren = true

        +LinearContainer().apply {
            width = FitParent
            orientation = Orientation.Vertical
        }
    }

) {

    private val modSettings: LinearContainer = content[0]


    init {
        theme = ModalTheme(backgroundColor = 0xFF181828)
    }


    fun onModAdded(mod: Mod) {
        val settings = mod.settings

        if (settings.isNotEmpty()) {
            modSettings.attachChild(ModSettingsSection(mod, settings))
        }
    }

    fun onModRemoved(mod: Mod) {
        modSettings.detachChild { it is ModSettingsSection && it.mod == mod }
    }

    fun isSelectorEmpty(): Boolean {
        return modSettings.findChild { it is ModSettingsSection } == null
    }


    private fun createComponent(mod: Mod, setting: ModSetting<*>) = when (setting) {

        is FloatModSetting -> FormSlider().apply {
            label = setting.name
            value = setting.value
            defaultValue = setting.defaultValue
            control.min = setting.minValue
            control.max = setting.maxValue
            control.step = setting.step
            onValueChanged = {
                setting.value = it
                ModMenuV2.onModsChanged(mod)
            }
            valueFormatter = setting.valueFormatter!!
        }

        is NullableFloatModSetting -> FormSlider().apply {
            label = setting.name
            value = setting.value ?: 0f
            defaultValue = setting.defaultValue ?: 0f
            control.isEnabled = setting.value != null
            control.min = setting.minValue!!
            control.max = setting.maxValue!!
            control.step = setting.step!!
            onValueChanged = {
                setting.value = it
                control.isEnabled = it != setting.defaultValue
                ModMenuV2.onModsChanged(mod)
            }
            valueFormatter = setting.valueFormatter!!
        }

        is BooleanModSetting -> FormCheckbox().apply {
            label = setting.name
            value = setting.value
            defaultValue = setting.defaultValue
            onValueChanged = {
                setting.value = it
                ModMenuV2.onModsChanged(mod)
            }
        }

        else -> throw IllegalArgumentException("Unsupported setting type: ${setting::class}")
    }


    private inner class ModSettingsSection(val mod: Mod, settings: List<ModSetting<*>>) : LinearContainer() {

        init {
            orientation = Orientation.Vertical
            width = FitParent

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

            settings.fastForEach {
                +createComponent(mod, it)
            }
        }

    }

}