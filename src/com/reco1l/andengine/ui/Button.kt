package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.ResourceManager


data class ButtonTheme(
    val backgroundColor: Long = 0xFF222234,
    val textColor: Long = 0xFFFFFFFF,
    val selectedBackgroundColor: Long = 0xFFF27272,
    val selectedTextColor: Long = 0xFF222234,
    val withBezelEffect: Boolean = true,
    val cornerRadius: Float = 14f,
    val iconSize: Float = 28f,
    val textFont: Font = ResourceManager.getInstance().getFont("smallFont")
) : ITheme


@Suppress("LeakingThis")
open class Button : LinearContainer(), IWithTheme<ButtonTheme> {


    override var theme = DefaultTheme
        set(value) {
            if (field != value) {
                field = value
                onThemeChanged()
            }
        }


    /**
     * The text of the button.
     */
    var text
        get() = textEntity.text
        set(value) { textEntity.text = value }


    //region State

    /**
     * Whether the button is being pressed or not.
     */
    var isPressed = false
        private set

    /**
     * Whether the button is enabled or not. If disabled, the button will not process any
     * touch events.
     */
    open var isEnabled = true
        set(value) {
            if (field != value) {
                field = value
                onEnableStateChange()
            }
        }

    /**
     * Whether the button is selected or not.
     */
    open var isSelected = false
        set(value) {
            if (field != value) {
                field = value
                onSelectionChange()
            }
        }

    //endregion

    //region Icons

    /**
     * The leading icon.
     */
    var leadingIcon: ExtendedEntity? = null
        set(value) {
            if (field != value) {
                field?.detachSelf()
                field = value

                if (value != null) {
                    onIconChange(value)
                    attachChild(value, 0)
                }
            }
        }

    /**
     * The trailing icon.
     */
    var trailingIcon: ExtendedEntity? = null
        set(value) {
            if (field != value) {
                field?.detachSelf()
                field = value

                if (value != null) {
                    onIconChange(value)
                    attachChild(value)
                }
            }
        }

    //endregion

    //region Actions

    /**
     * The action to perform when the button is pressed.
     */
    var onActionDown: (() -> Unit)? = null

    /**
     * The action to perform when the button is released.
     */
    var onActionUp: (() -> Unit)? = null

    //endregion


    private val textEntity = ExtendedText().apply {
        height = 60f
        alignment = Anchor.Center
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }


    init {
        orientation = Orientation.Horizontal
        padding = Vec4(16f, 0f, 18f, 0f)
        spacing = 8f
        scaleCenter = Anchor.Center

        attachChild(textEntity)
        onThemeChanged()
    }


    override fun onThemeChanged() {
        foreground = if (theme.withBezelEffect) BezelOutline(theme.cornerRadius) else null
        background = RoundedBox().apply {
            color = ColorARGB(theme.backgroundColor)
            cornerRadius = theme.cornerRadius
        }
        onSelectionChange()
        onEnableStateChange()
        trailingIcon?.let(::onIconChange)
        leadingIcon?.let(::onIconChange)
        textEntity.font = theme.textFont
        textEntity.color = ColorARGB(theme.textColor)
    }


    //region Callbacks

    /**
     * Called when the enable state of the button changes.
     */
    open fun onEnableStateChange() {
        clearModifiers(ModifierType.Alpha)

        if (isEnabled) {
            fadeTo(1f, 0.2f)
        } else {
            fadeTo(0.25f, 0.2f)
        }
    }

    /**
     * Called when the selection state of the button changes.
     */
    open fun onSelectionChange() {

        background?.clearModifiers(ModifierType.Color)
        textEntity.clearModifiers(ModifierType.Color)

        if (isSelected) {
            background?.colorTo(theme.selectedBackgroundColor, 0.2f)
            textEntity.colorTo(theme.selectedTextColor, 0.2f)
        } else {
            background?.colorTo(theme.backgroundColor)
            textEntity.colorTo(theme.textColor, 0.2f)
        }
    }

    /**
     * Called when an icon is added or changed.
     */
    open fun onIconChange(icon: ExtendedEntity) {
        icon.height = theme.iconSize
        icon.width = theme.iconSize
        icon.anchor = Anchor.CenterLeft
        icon.origin = Anchor.CenterLeft
        icon.color = theme.textColor.toColorARGB()
    }

    //endregion

    open fun processTouchFeedback(event: TouchEvent) {

        if (event.isActionDown) {
            clearModifiers(ModifierType.ScaleXY)
            scaleTo(0.9f, 0.2f)
        }

        if ((event.isActionUp || event.isActionCancel) && scaleX != 1f) {
            clearModifiers(ModifierType.ScaleXY)
            scaleTo(1f, 0.2f)
        }
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (!isEnabled) {
            return true
        }
        processTouchFeedback(event)

        when {
            event.isActionDown -> {
                isPressed = true
                onActionDown?.invoke()
            }

            event.isActionUp -> {
                if (localX <= width && localY <= height && isPressed) {
                    onActionUp?.invoke()
                }
                isPressed = false
            }

            !event.isActionMove -> isPressed = false
        }

        return true
    }


    companion object {
        val DefaultTheme = ButtonTheme()
    }
}