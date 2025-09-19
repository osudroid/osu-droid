package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.math.*

class UITabSelector : UIFlexContainer() {


    /**
     * The currently selected tab index.
     */
    var selectedTab: Int?
        get() {
            return if (selectedButton == null) null else getChildIndex(selectedButton)
        }
        set(value) {
            selectedButton = if (value == null) null else getChild(value) as UITextButton
        }


    private val selectionIndicator: UIBox

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
        gap = 8f
        background = UIContainer().apply {
            padding = Vec4(4f)

            background = UIBox().apply {
                cornerRadius = 12f + 2f
                applyTheme = {
                    color = it.accentColor * 0.1f
                    alpha = 0.25f
                }
            }

            selectionIndicator = box {
                cornerRadius = 12f
                isVisible = false
                applyTheme = { color = it.accentColor * 0.2f }
            }
        }
    }


    fun addButton(name: String, onSelect: () -> Unit) {
        +UITextButton().apply {
            text = name
            background = null
            padding = Vec4(12f, 8f)
            alignment = Anchor.Center
            onActionUp = {
                selectedButton = this
                onSelect()
            }
            flexRules {
                grow = 1f
            }
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        val selectedButton = selectedButton
        if (selectedButton != null && !selectionIndicator.isAnimating) {
            selectionIndicator.setSize(selectedButton.width, selectedButton.height)
        }

        super.onManagedUpdate(deltaTimeSec)
    }


}