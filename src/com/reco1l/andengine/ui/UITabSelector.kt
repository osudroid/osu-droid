package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.srem
import com.reco1l.framework.math.*
import com.rian.andengine.modifier.ModifierType

/**
 * A horizontal row of buttons where a single one is highlighted as selected.
 *
 * The selected tab uses the regular [UIButton.isSelected] styling (accent background, dark text),
 * the other tabs are rendered flat so the selection stands out.
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

    /**
     * Called after the selected tab changed, with the new tab index.
     */
    var onTabSelected: ((Int?) -> Unit)? = null


    private val buttonsContainer: UIFlexContainer

    private var selectedButton: UITextButton? = null
        set(value) {
            if (field != value) {
                field = value

                for (i in 0 until buttonsContainer.childCount) {
                    val button = buttonsContainer.getChildByIndex(i) as? UITextButton ?: continue
                    button.isSelected = button == value
                }

                onTabSelected?.invoke(selectedTab)
            }
        }


    override var applyTheme: ThemeApplier = { theme ->
        (background as? UIBox)?.color = (theme.accentColor * 0.1f).copy(alpha = 0.7f)
    }


    init {
        padding = Vec4(1f.srem)
        background = UIBox().apply {
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
        val button = TabButton().apply {
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


    /**
     * A text button whose unselected state is flat (transparent background) so only the selected tab is filled.
     */
    private class TabButton : UITextButton() {

        private companion object {
            /**
             * The selected tab is filled with a slightly darkened accent color so it does not glare.
             */
            const val SELECTED_FILL_BRIGHTNESS = 0.8f
        }

        override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
            background?.color = theme.accentColor * SELECTED_FILL_BRIGHTNESS
            background?.alpha = if (isSelected) 1f else 0f
            color = if (isSelected) theme.accentColor * 0.1f else theme.accentColor
            alpha = if (isEnabled) 1f else 0.5f
        }

        override fun onSelectionChange() {
            clearModifiers(ModifierType.Color)
            colorTo(if (isSelected) Theme.current.accentColor * 0.1f else Theme.current.accentColor, 0.2f)

            // Color modifiers do not touch alpha, so the background fades in and out separately.
            background?.apply {
                clearModifiers(ModifierType.Alpha)
                fadeTo(if (isSelected) 1f else 0f, 0.2f)
            }
        }
    }

}
