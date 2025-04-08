package com.reco1l.osu.ui.modmenu

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.mods.*
import ru.nsu.ccfit.zuev.osu.ResourceManager

class ModCustomizationMenu : Modal() {

    private val modSelector = LinearContainer().apply {
        orientation = Orientation.Vertical
        width = FitParent
        spacing = 8f
        padding = Vec4(12f)
    }

    private val modSettings: Container

    private val modButtons = mutableListOf<ModButton>()


    init {
        anchor = Anchor.TopCenter
        origin = Anchor.TopCenter
        relativeSizeAxes = Axes.Both
        width = 0.9f
        height = 0.75f
        y = 90f
        theme = ModalTheme(backgroundColor = 0xFF181828)

        attachChild(LinearContainer().apply {
            height = FitParent
            width = FitParent
            orientation = Orientation.Horizontal

            attachChild(ScrollableContainer().apply {
                scrollAxes = Axes.Y
                relativeSizeAxes = Axes.X
                width = 0.25f
                height = FitParent

                attachChild(modSelector)
            })

            modSettings = ScrollableContainer().apply {
                scrollAxes = Axes.Y
                relativeSizeAxes = Axes.X
                width = 0.75f
                height = FitParent

                background = RoundedBox().apply {
                    color = ColorARGB(0xFF161622)
                    cornerRadius = 16f
                }

                attachChild(EmptyAlert())
            }
            attachChild(modSettings)
        })
    }


    private fun EmptyAlert() = LinearContainer().apply {
        orientation = Orientation.Vertical
        width = FitParent
        anchor = Anchor.Center
        origin = Anchor.Center
        spacing = 10f

        attachChild(ExtendedSprite().apply {
            textureRegion = ResourceManager.getInstance().getTexture("search")
            anchor = Anchor.TopCenter
            origin = Anchor.TopCenter
        })

        attachChild(ExtendedText().apply {
            text = "Select one mod to customize"
            font = ResourceManager.getInstance().getFont("smallFont")
            anchor = Anchor.TopCenter
            origin = Anchor.TopCenter
        })
    }


    fun onModAdded(mod: Mod) {
        val settings = mod.settings

        if (settings.isNotEmpty() && modButtons.none { it.mod == mod }) {
            val button = ModButton(mod, settings)
            modButtons.add(button)
            modSelector.attachChild(button)
        }
    }

    fun onModRemoved(mod: Mod) {

        for (button in modButtons) {
            if (button.mod == mod) {
                if (button.isSelected) {
                    modSettings.detachChildren()
                    modSettings.attachChild(EmptyAlert())
                }
                modButtons.remove(button)
                break
            }
        }

        modSelector.detachChild { it is ModButton && it.mod == mod }
    }

    fun isSelectorEmpty(): Boolean {
        return modButtons.isEmpty()
    }

    private fun createComponent(setting: ModSetting<*>) = when (setting) {

        is FloatModSetting -> FormSlider().apply {
            label = setting.name
            value = setting.value
            defaultValue = setting.defaultValue
            control.min = setting.min
            control.max = setting.max
            control.step = setting.step
            onValueChanged = { setting.value = it }
            valueFormatter = setting.valueFormatter!!
        }

        is NullableFloatModSetting -> FormSlider().apply {
            label = setting.name
            value = setting.value ?: 0f
            defaultValue = setting.defaultValue ?: 0f
            control.isEnabled = setting.value != null
            control.min = setting.min!!
            control.max = setting.max!!
            control.step = setting.step!!
            onValueChanged = {
                setting.value = it
                control.isEnabled = it != setting.defaultValue
            }
            valueFormatter = setting.valueFormatter!!
        }

        is BooleanModSetting -> FormCheckbox().apply {
            label = setting.name
            value = setting.value
            defaultValue = setting.defaultValue
            onValueChanged = { setting.value = it }
        }

        else -> throw IllegalArgumentException("Unsupported setting type: ${setting::class}")
    }


    fun onModSelected(settings: List<ModSetting<*>>) {
        modSettings.detachChildren()
        modSettings.attachChild(LinearContainer().apply {
            orientation = Orientation.Vertical
            width = FitParent
            alpha = 0f

            fadeIn(0.2f)

            settings.fastForEach { setting -> attachChild(createComponent(setting)) }
        })
    }


    private inner class ModButton(val mod: Mod, val settings: List<ModSetting<*>>) : Button() {

        init {
            width = FitParent
            theme = MOD_BUTTON_THEME
            leadingIcon = ModIcon(mod)
            text = mod.name

            onActionUp = {
                if (!isSelected) {
                    isSelected = true

                    modButtons.fastForEach { button ->
                        if (button != this) {
                            button.isSelected = false
                        }
                    }
                    onModSelected(settings)
                }
            }
        }

    }

    companion object {
        private val MOD_BUTTON_THEME = ButtonTheme(
            iconSize = 40f,
            withBezelEffect = false,
            backgroundColor = 0xFF181828,
            selectedBackgroundColor = 0xFF202036,
            selectedTextColor = 0xFFFFFFFF,
        )
    }
}