package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.math.*

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
        content.apply {

            val nameInput = FormInput().apply {
                label = "Name"
                width = FillParent
            }
            +nameInput

            linearContainer {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                padding = Vec4(12f)
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