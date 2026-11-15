package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4

/**
 * A dialog that lists its options as full width buttons below each other.
 */
abstract class OptionsDialog : UIDialog<UILinearContainer>(innerContent = UILinearContainer().apply {
    orientation = Orientation.Vertical
    width = FillParent
}) {

    protected fun optionIcon(icon: Int) = FontAwesomeIcon(icon).apply { iconSize = ICON_SIZE }

    /**
     * A full width button with an icon in front of its text.
     */
    protected fun optionButton(icon: FontAwesomeIcon, block: UITextButton.() -> Unit) = UITextButton().apply {
        width = FillParent
        alignment = Anchor.CenterLeft
        leadingIcon = icon
        block()
    }

    /**
     * An [optionButton] in red, for an option that deletes something.
     */
    protected fun deleteOptionButton(block: UITextButton.() -> Unit) = optionButton(optionIcon(Icon.Trash)) {
        applyTheme = { theme ->
            background?.color = theme.accentColor * 0.175f
            color = DELETE_COLOR
        }
        block()
    }

    /**
     * Wraps a button in the horizontal margin of the dialog's other content.
     */
    protected fun optionRow(button: UITextButton) = UIContainer().apply {
        width = FillParent
        padding = Vec4(24f, 6f)
        +button
    }


    companion object {
        const val ICON_SIZE = 20f

        private val DELETE_COLOR = Color4(255, 191, 191)
    }

}
