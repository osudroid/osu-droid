package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.ResourceManager

sealed class ButtonTheme(
    open val backgroundColor: Long = 0xFF222234,
    open val selectedBackgroundColor: Long = 0xFFF27272,
    open val withBezelEffect: Boolean = true,
    open val cornerRadius: Float = 14f,
) : ITheme


@Suppress("LeakingThis")
sealed class Button<T : ButtonTheme> : LinearContainer(), IWithTheme<T> {

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

    //endregion

    init {
        orientation = Orientation.Horizontal
        padding = Vec4(16f, 0f)
        spacing = 8f
        scaleCenter = Anchor.Center
    }

    //region Callbacks

    override fun onThemeChanged() {
        foreground = if (theme.withBezelEffect) BezelOutline(theme.cornerRadius) else null
        background = Box().apply {
            color = ColorARGB(theme.backgroundColor)
            cornerRadius = theme.cornerRadius
        }
        onSelectionChange()
        onEnableStateChange()
    }

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
        background?.clearModifiers(ModifierType.Color)
        background?.colorTo(if (isSelected) theme.selectedBackgroundColor else theme.backgroundColor, 0.2f)
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

}


//region TextButton

data class TextButtonTheme(
    override val backgroundColor: Long = 0xFF222234,
    override val selectedBackgroundColor: Long = 0xFFF27272,
    override val withBezelEffect: Boolean = true,
    override val cornerRadius: Float = 14f,
    val selectedTextColor: Long = 0xFF222234,
    val textColor: Long = 0xFFFFFFFF,
    val textFont: Font = ResourceManager.getInstance().getFont("smallFont"),
    val iconSize: Float = 28f,
) : ButtonTheme()

@Suppress("LeakingThis")
open class TextButton : Button<TextButtonTheme>() {

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

    private val textEntity = ExtendedText().apply {
        height = 60f
        alignment = Anchor.Center
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }


    init {
        +textEntity
        onThemeChanged()
    }


    //region Callbacks

    override fun onThemeChanged() {
        super.onThemeChanged()
        trailingIcon?.let(::onIconChange)
        leadingIcon?.let(::onIconChange)
        textEntity.font = theme.textFont
        textEntity.color = ColorARGB(if (isSelected) theme.selectedTextColor else theme.textColor)
    }

    override fun onSelectionChange() {
        super.onSelectionChange()
        textEntity.clearModifiers(ModifierType.Color)
        textEntity.colorTo(if (isSelected) theme.selectedTextColor else theme.textColor, 0.2f)
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

    companion object {
        val DefaultTheme = TextButtonTheme()
    }
}

//endregion

//region IconButton

data class IconButtonTheme(
    override val backgroundColor: Long = 0xFF222234,
    override val selectedBackgroundColor: Long = 0xFFF27272,
    override val withBezelEffect: Boolean = true,
    override val cornerRadius: Float = 14f,
    val iconColor: Long = 0xFFFFFFFF,
    val selectedIconColor: Long = 0xFF222234,
    val iconSize: Float = 32f,
) : ButtonTheme()

@Suppress("LeakingThis")
open class IconButton : Button<IconButtonTheme>() {

    override var theme = DefaultTheme
        set(value) {
            if (field != value) {
                field = value
                onThemeChanged()
            }
        }

    /**
     * The icon of the button.
     */
    var icon
        get() = image.textureRegion
        set(value) { image.textureRegion = value }


    private val image = sprite {
        scaleType = ScaleType.Fit
        anchor = Anchor.Center
        origin = Anchor.Center
    }


    init {
        onThemeChanged()
    }


    //region Callbacks

    override fun onThemeChanged() {
        super.onThemeChanged()
        image.color = ColorARGB(if (isSelected) theme.selectedIconColor else theme.iconColor)
        image.height = theme.iconSize
        image.width = theme.iconSize
    }

    override fun onSelectionChange() {
        super.onSelectionChange()
        image.clearModifiers(ModifierType.Color)
        image.colorTo(if (isSelected) theme.selectedIconColor else theme.iconColor, 0.2f)
    }

    //endregion

    companion object {
        val DefaultTheme = IconButtonTheme()
    }
}

