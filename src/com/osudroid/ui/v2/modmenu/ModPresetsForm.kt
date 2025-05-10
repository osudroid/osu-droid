package com.osudroid.ui.v2.modmenu

import com.osudroid.ui.v2.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.math.*

class ModPresetsForm(section: ModMenuPresetsSection) : Dialog<LinearContainer>(innerContent = LinearContainer().apply {
    orientation = Orientation.Vertical
    width = FillParent
}) {

    init {
        title = "New mod preset"
        staticBackdrop = true

        val nameInput = FormInput().apply {
            label = "Name"
            width = FillParent
            showResetButton = false
        }

        innerContent.apply {
            +nameInput

            +ModsIndicator(ModMenu.enabledMods).apply {
                isExpanded = false
                padding = Vec4(24f, 24f, 24f, 12f)
            }
        }

        addButton(TextButton().apply {
            text = "Save"
            isSelected = true
            onActionUp = {
                section.saveModPreset(nameInput.value)
                hide()
            }
        })

        addButton(TextButton().apply {
            text = "Cancel"
            onActionUp = {
                hide()
            }
        })

        show()
    }
}