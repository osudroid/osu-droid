package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.Size
import com.reco1l.framework.math.*

class UITabSelector : UIFillContainer() {


    /**
     * The currently selected tab index.
     */
    var selectedTab: Int?
        get() {
            return if (selectedButton == null) null else buttonsContainer.getChildIndex(selectedButton)
        }
        set(value) {
            selectedButton = if (value == null) null else buttonsContainer.getChild(value) as UITextButton
        }


    private val selectionIndicator: UIBox
    private val buttonsContainer: UILinearContainer

    private var selectedButton: UITextButton? = null
        set(value) {
            if (field != value) {
                field = value

                selectionIndicator.clearModifiers(ModifierType.MoveXY, ModifierType.SizeXY)

                if (value != null) {
                    if (!selectionIndicator.isVisible) {
                        selectionIndicator.setSize(value.width, value.height)
                    } else {
                        selectionIndicator.sizeTo(value.width, value.height, 0.1f)
                    }

                    selectionIndicator.isVisible = true
                    selectionIndicator.moveTo(value.x, value.y, 0.1f)
                } else {
                    selectionIndicator.isVisible = false
                }
            }
        }


    init {
        padding = Vec4(4f)
        style = {
            backgroundColor = (it.accentColor * 0.1f).copy(alpha = 0.25f)
        }

        buttonsContainer = linearContainer {
            spacing = 8f
        }

        selectionIndicator = box {
            cornerRadius = 12f
            isVisible = false
            style = { color = it.accentColor * 0.2f }
        }
    }


    fun addButton(name: String, onSelect: () -> Unit) {
        buttonsContainer.attachChild(UITextButton().apply {
            width = Size.Full
            text = name
            padding = Vec4(12f, 8f)
            alignment = Anchor.Center
            colorVariant = ColorVariant.Tertiary
            onActionUp = {
                selectedButton = this
                onSelect()
            }
        })
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        val selectedButton = selectedButton
        if (selectedButton != null) {
            selectionIndicator.setSize(selectedButton.width, selectedButton.height)
        }

        super.onManagedUpdate(deltaTimeSec)
    }


}