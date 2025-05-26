package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.TouchEvent

@Suppress("LeakingThis")
open class Button : LinearContainer() {

    override var applyTheme: ExtendedEntity.(Theme) -> Unit = { theme ->
        background?.color = if (isSelected) theme.accentColor else theme.accentColor * 0.175f
        color = if (isSelected) theme.accentColor * 0.1f else theme.accentColor

        onSelectionChange()
        onEnableStateChange()
    }


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


    private var pressStartTime = 0L

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

    /**
     * The action to perform when the button is cancelled.
     */
    var onActionCancel: (() -> Unit)? = null

    /**
     * The action to perform when the button is long pressed.
     */
    var onActionLongPress: (() -> Unit)? = null

    //endregion


    init {
        padding = Vec4(12f, 16f)
        scaleCenter = Anchor.Center
        background = Box().apply { cornerRadius = 12f }
    }


    //region Callbacks

    /**
     * Called when the enable state of the button changes.
     */
    open fun onEnableStateChange() {
        clearModifiers(ModifierType.Alpha)
        fadeTo(if (isEnabled) 1f else 0.25f, 0.2f)
    }

    /**
     * Called when the selection state of the button changes.
     */
    open fun onSelectionChange() {
        clearModifiers(ModifierType.Color)
        colorTo(if (isSelected) Theme.current.accentColor * 0.1f else Theme.current.accentColor, 0.2f)

        background?.apply {
            clearModifiers(ModifierType.Color)
            colorTo(if (isSelected) Theme.current.accentColor else Theme.current.accentColor * 0.175f)
        }
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
                pressStartTime = System.currentTimeMillis()
            }

            event.isActionUp -> {
                if (localX <= width && localY <= height && isPressed) {
                    onActionUp?.invoke()
                } else {
                    onActionCancel?.invoke()
                }
                isPressed = false
            }

            event.isActionOutside || event.isActionCancel -> {
                onActionCancel?.invoke()
                isPressed = false
            }

            !event.isActionMove -> isPressed = false
        }

        return true
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (onActionLongPress != null) {
            if (isPressed && System.currentTimeMillis() - pressStartTime >= 500L) {
                onActionLongPress?.invoke()
                propagateTouchEvent(TouchEvent.ACTION_CANCEL)
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }

}


/**
 * A button that displays a text.
 */
open class TextButton : Button() {

    /**
     * The compound text of the button.
     */
    val content = CompoundText().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        spacing = 8f
    }


    /**
     * The text of the button.
     */
    var text by content::text

    /**
     * The font of the button.
     */
    var font by content::font

    /**
     * The leading icon.
     */
    var leadingIcon by content::leadingIcon

    /**
     * The trailing icon.
     */
    var trailingIcon by content::trailingIcon

    /**
     * The icon change callback.
     */
    var onIconChange by content::onIconChange


    init {
        +content
    }

    override fun onEnableStateChange() {
        clearModifiers(ModifierType.Alpha)
        fadeTo(if (isEnabled) 1f else 0.5f, 0.2f)
    }
}

open class IconButton : Button() {

    protected val sprite = sprite {
        scaleType = ScaleType.Fit
        anchor = Anchor.Center
        origin = Anchor.Center
    }

    /**
     * The icon to be displayed on the button.
     */
    var icon by sprite::textureRegion

}

