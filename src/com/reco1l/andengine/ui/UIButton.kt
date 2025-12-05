package com.reco1l.andengine.ui

import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.framework.Color4
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.TouchEvent

@Suppress("LeakingThis")
open class UIButton : UIClickableContainer(), ISizeVariable, IColorVariable {


    override var sizeVariant = SizeVariant.Medium
        set(value) {
            if (field != value) {
                field = value
                applyStyle()
            }
        }

    override var colorVariant = ColorVariant.Secondary
        set(value) {
            if (field != value) {
                field = value
                applyStyle()
            }
        }


    //region State

    /**
     * Whether the button is enabled or not. If disabled, the button will not process any
     * touch events.
     */
    var isEnabled = true
        set(value) {
            if (field != value) {
                field = value
                onEnableStateChange()
            }
        }

    /**
     * Whether the button is selected or not.
     */
    @Deprecated("Use colorVariant instead", ReplaceWith("colorVariant"))
    var isSelected
        get() = colorVariant == ColorVariant.Primary
        set(value) {
            colorVariant = if (value) ColorVariant.Primary else ColorVariant.Secondary
        }


    //endregion


    init {
        scaleCenter = Anchor.Center
        shrink = false
        clipToBounds = true

        style = {

            when (colorVariant) {
                ColorVariant.Primary -> {
                    backgroundColor = it.accentColor
                    color = it.accentColor * 0.1f
                }
                ColorVariant.Secondary -> {
                    backgroundColor = it.accentColor * 0.175f
                    color = it.accentColor
                }
                ColorVariant.Tertiary -> {
                    backgroundColor = Color4.Transparent
                    color = it.accentColor
                }
            }

            when (sizeVariant) {
                SizeVariant.Small -> {
                    height = 1f.rem
                    padding = Vec4(1.25f.srem, 0f)
                    radius = Radius.MD
                }
                SizeVariant.Medium -> {
                    height = 2.5f.rem
                    padding = Vec4(2.5f.srem, 0f)
                    radius = Radius.LG
                }
                SizeVariant.Large -> {
                    height = 3f.rem
                    padding = Vec4(3f.srem, 0f)
                    radius = Radius.LG
                }
            }

            alpha = if (isEnabled) 1f else 0.5f
        }
    }


    //region Callbacks

    /**
     * Called when the enable state of the button changes.
     */
    fun onEnableStateChange() {
        clearModifiers(ModifierType.Alpha)
        fadeTo(if (isEnabled) 1f else 0.5f, 0.2f)
    }

    //endregion

    //region Touch

    open fun processTouchFeedback(event: TouchEvent) {
        if (event.isActionDown) {
            clearModifiers(ModifierType.ScaleXY)
            scaleTo(0.9f, 0.3f).eased(Easing.Out)
        }

        if ((event.isActionUp || event.isActionCancel) && scaleX != 1f) {
            clearModifiers(ModifierType.ScaleXY)
            scaleTo(1f, 0.4f).eased(Easing.OutElastic)
        }
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (!isEnabled) {
            return true
        }
        processTouchFeedback(event)
        return super.onAreaTouched(event, localX, localY)
    }
}


/**
 * A button that displays a text.
 */
open class UITextButton : UIButton() {

    private val textComponent = CompoundText().apply {
        width = Size.Full
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        style = {
            when (sizeVariant) {
                SizeVariant.Small -> {
                    fontSize = FontSize.XS
                    spacing = 0.5f.srem
                }
                SizeVariant.Medium -> {
                    fontSize = FontSize.MD
                    spacing = 2f.srem
                }
                SizeVariant.Large -> {
                    fontSize = FontSize.LG
                    spacing = 2.5f.srem
                }
            }
        }
        alignment = Anchor.Center
        shrink = false
    }


    //region Shortcuts

    var text by textComponent::text
    var fontFamily by textComponent::fontFamily
    var fontSize by textComponent::fontSize
    var leadingIcon by textComponent::leadingIcon
    var trailingIcon by textComponent::trailingIcon
    var alignment by textComponent::alignment

    //endregion

    init {
        +textComponent
    }

}

open class UIIconButton : UIButton() {

    /**
     * The icon of the button.
     */
    var icon: UIComponent? = null
        set(value) {
            if (field != value) {
                field = value

                detachChildren()
                if (value != null) {
                    value.anchor = Anchor.Center
                    value.origin = Anchor.Center
                    applyStyle()
                    attachChild(value)
                }
            }
        }

    init {
        style += {
            width = height

            when (sizeVariant) {
                SizeVariant.Small -> {
                    icon?.width = FontSize.SM
                    icon?.height = FontSize.SM
                }
                SizeVariant.Medium -> {
                    icon?.width = FontSize.MD
                    icon?.height = FontSize.MD
                }
                SizeVariant.Large -> {
                    icon?.width = FontSize.LG
                    icon?.height = FontSize.LG
                }
            }
        }
    }

}

