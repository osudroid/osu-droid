package com.reco1l.andengine.ui

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.srem
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.*

/**
 * A horizontal row of buttons where a single one is highlighted as selected.
 */
class UITabSelector : UIContainer() {


    /**
     * The currently selected tab index.
     */
    var selectedTab: Int?
        get() {
            val button = selectedButton ?: return null
            for (i in 0 until buttonsContainer.childCount) {
                if (buttonsContainer.getChildByIndex(i) == button) {
                    return i
                }
            }
            return null
        }
        set(value) {
            selectedButton = if (value == null) null else buttonsContainer.getChildByIndex(value) as UITextButton
        }


    private val selectionIndicator: UIBox
    private val buttonsContainer: UIFlexContainer

    private var selectedButton: UITextButton? = null


    override var applyTheme: ThemeApplier = { theme ->
        (background as? UIBox)?.color = (theme.accentColor * 0.1f).copy(alpha = 0.25f)
        selectionIndicator.color = theme.accentColor * 0.2f
    }


    init {
        padding = Vec4(1f.srem)
        background = UIBox().apply {
            cornerRadius = Radius.LG
        }

        selectionIndicator = box {
            alpha = 0f
            height = FillParent
            cornerRadius = Radius.LG
        }

        buttonsContainer = flexContainer {
            width = FillParent
            gap = 1f.srem
        }
    }


    /**
     * Adds a tab button. [onSelect] is also invoked when the already selected tab is tapped again,
     * which lets callers implement secondary actions on the active tab.
     */
    fun addButton(name: String, onSelect: () -> Unit): UITextButton {
        val button = UITextButton().apply {
            text = name
            alignment = Anchor.Center
            onActionUp = {
                selectedButton = this
                onSelect()
            }
        }

        buttonsContainer.apply {
            +button
            button.flexRules {
                grow = 1f
                basis = 0f
            }
        }

        return button
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        selectionIndicator.apply {
            val selectedButton = selectedButton

            if (selectedButton != null) {
                alpha = Interpolation.floatLerp(deltaTimeSec, 0.1f, alpha, 1f)
                width = Interpolation.floatLerp(deltaTimeSec, 0.1f, width, selectedButton.width)
                x = Interpolation.floatLerp(deltaTimeSec, 0.2f, x, selectedButton.x, Easing.OutQuint)
            } else {
                alpha = Interpolation.floatLerp(deltaTimeSec, 0.1f, alpha, 0f)
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }

}
