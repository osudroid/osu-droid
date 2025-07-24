package com.reco1l.andengine.ui

import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.TouchEvent

@Suppress("LeakingThis")
open class UIButton : UILinearContainer() {

    override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
        background?.color = if (isSelected) theme.accentColor else theme.accentColor * 0.175f
        color = if (isSelected) theme.accentColor * 0.1f else theme.accentColor
        alpha = if (isEnabled) 1f else 0.5f
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
        background = UIBox().apply { cornerRadius = 12f }
    }


    //region Callbacks

    /**
     * Called when the enable state of the button changes.
     */
    open fun onEnableStateChange() {
        clearModifiers(ModifierType.Alpha)
        fadeTo(if (isEnabled) 1f else 0.5f, 0.2f)
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
open class UITextButton : UIButton() {

    /**
     * The compound text of the button.
     */
    val content = CompoundText().apply {
        width = FillParent
        height = FillParent
        alignment = Anchor.Center
        spacing = 8f
    }


    var text by content::text

    var font by content::font

    var leadingIcon by content::leadingIcon

    var trailingIcon by content::trailingIcon

    var autoSizeTrailingIcon by content::autoSizeTrailingIcon

    var autoSizeLeadingIcon by content::autoSizeLeadingIcon

    var onIconChange by content::onIconChange

    var alignment by content::alignment


    override fun onContentChanged() {
        // We don't use direct reference of `content` because it may not be initialized yet when this method is called.
        contentWidth = get<CompoundText>(0)?.contentWidth ?: 0f
        contentHeight = get<CompoundText>(0)?.contentHeight ?: 0f
    }


    init {
        +content
    }

}

open class UIIconButton : UIButton() {

    protected val sprite = sprite {
        scaleType = ScaleType.Fit
        anchor = Anchor.Center
        origin = Anchor.Center
        width = 28f
        height = 28f
    }

    /**
     * The icon to be displayed on the button.
     */
    var icon by sprite::textureRegion

}

