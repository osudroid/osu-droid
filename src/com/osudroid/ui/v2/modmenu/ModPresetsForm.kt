package com.osudroid.ui.v2.modmenu

import com.osudroid.ui.v2.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.math.*

class ModPresetsForm(section: ModMenuPresetsSection) : UIDialog<UILinearContainer>(innerContent = UILinearContainer().apply {
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

            +ModsIndicator().apply {
                mods = ModMenu.enabledMods.serializeMods()
                padding = Vec4(24f, 24f, 24f, 12f)
            }
        }

        addButton(UITextButton().apply {
            text = "Save"
            isSelected = true
            onActionUp = {
                section.saveModPreset(nameInput.value)
                hide()
            }
        })

        addButton(UITextButton().apply {
            text = "Cancel"
            onActionUp = {
                hide()
            }
        })
    }
}