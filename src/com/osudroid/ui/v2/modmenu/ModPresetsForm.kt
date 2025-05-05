package com.osudroid.ui.v2.modmenu

import com.osudroid.ui.v2.*
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.osu.*

class ModPresetsForm : Modal(

    content = LinearContainer().apply {
        orientation = Orientation.Vertical
        relativeSizeAxes = Axes.X
        width = 0.5f
        anchor = Anchor.Center
        origin = Anchor.Center
        clipToBounds = true
    }

) {

    init {
        staticBackdrop = true

        content.apply {

            text {
                width = FillParent
                text = "New mod preset"
                font = ResourceManager.getInstance().getFont("smallFont")
                alignment = Anchor.Center
                padding = Vec4(0f, 12f)
            }

            box {
                width = FillParent
                height = 1f
                applyTheme = {
                    color = it.accentColor
                    alpha = 0.1f
                }
            }

            val nameInput = FormInput().apply {
                label = "Name"
                width = FillParent
                showResetButton = false
            }
            +nameInput

            +ModsIndicator(ModMenu.enabledMods).apply {
                isExpanded = false
                padding = Vec4(24f, 24f, 24f, 12f)
            }

            linearContainer {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                padding = Vec4(24f)
                spacing = 12f

                textButton {
                    text = "Save"
                    isSelected = true
                    onActionUp = {
                        ModMenu.saveModPreset(nameInput.value)
                        hide()
                    }
                }

                textButton {
                    text = "Cancel"
                    onActionUp = {
                        hide()
                    }
                }
            }
        }
        show()
    }
}