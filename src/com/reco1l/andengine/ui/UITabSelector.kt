package com.reco1l.andengine.ui

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.*

class UITabSelector : UIContainer() {


    /**
     * The currently selected tab index.
     */
    var selectedTab: Int?
        get() = if (selectedButton == null) null else buttonsContainer.getChildIndex(selectedButton)
        set(value) {
            selectedButton = if (value == null) null else buttonsContainer.getChild(value) as UITextButton
        }


    private val selectionIndicator: UIBox
    private val buttonsContainer: UILinearContainer

    private var selectedButton: UITextButton? = null


    init {
        style = {
            padding = Vec4(1f.srem)
            backgroundColor = (it.accentColor * 0.1f).copy(alpha = 0.25f)
            radius = Radius.LG
        }

        selectionIndicator = box {
            alpha = 0f
            height = Size.Full
            style = {
                color = it.accentColor * 0.2f
                radius = Radius.LG
            }
        }

        buttonsContainer = fillContainer {
            width = Size.Full
            style = {
                spacing = 1f.srem
            }
        }

    }


    fun addButton(name: String, onSelect: () -> Unit) {
        buttonsContainer.attachChild(UITextButton().apply {
            width = Size.Full
            text = name
            alignment = Anchor.Center
            sizeVariant = SizeVariant.Small
            colorVariant = ColorVariant.Tertiary
            onActionUp = {
                selectedButton = this
                onSelect()
            }
        })
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        selectionIndicator.apply {
            val selectedButton = selectedButton

            if (selectedButton != null) {

                alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), alpha, 1f, 0f, 0.1f)
                width = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), width, selectedButton.width, 0f, 0.1f)
                x = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.2f), x, selectedButton.x, 0f, 0.2f, Easing.OutQuint)

            } else {
                alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), alpha, 0f, 0f, 0.1f)
            }
        }



        super.onManagedUpdate(deltaTimeSec)
    }


}