package com.reco1l.andengine.ui

import com.osudroid.utils.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.input.touch.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*

/**
 * A dropdown menu that allows the user to select an option from a list.
 */
@Suppress("LeakingThis")
open class UISelect<T : Any>(initialValues: List<T> = emptyList()) : UIControl<List<T>>(initialValues) {


    private val optionsContainer: UILinearContainer

    private val menuWrapper: UIContainer


    /**
     * Whether the dropdown menu is currently expanded or not.
     */
    val isExpanded: Boolean
        get() = menuWrapper.hasParent()

    /**
     * The menu that contains the options.
     */
    val menu = UIScrollableContainer().apply menu@{
        width = MatchContent
        scrollAxes = Axes.Y
        clipToBounds = true
        background = UIBox().apply {
            cornerRadius = 14f
            applyTheme = { color = it.accentColor * 0.175f }
        }

        optionsContainer = linearContainer {
            orientation = Orientation.Vertical
            spacing = 4f
            padding = Vec4(4f)
        }

        menuWrapper = object : UIContainer() {

            init {
                width = FillParent
                height = FillParent
                attachChild(this@menu)
            }

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (!super.onAreaTouched(event, localX, localY)) {
                    if (event.isActionUp) {
                        collapse()
                    }
                }
                return true
            }
        }
    }

    /**
     * The button that toggles the dropdown menu.
     */
    val button = object : UITextButton() {

        init {
            width = FillParent
            content.textEntity.clipToBounds = true
            onActionUp = {
                if (isExpanded) {
                    collapse()
                } else {
                    expand()
                }
            }

            trailingIcon = UIContainer().apply {
                padding = Vec4(12f, 0f)

                triangle {
                    width = 14f
                    height = 8f
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    color = Color4.White
                    alpha = 0.25f
                    rotationCenter = Anchor.Center
                    rotation = 180f
                }
            }
        }

        override fun onThemeChanged(theme: Theme) {
            super.onThemeChanged(theme)
            background?.color = theme.accentColor * 0.25f
        }
    }


    /**
     * The text displayed on the button when no option is selected.
     */
    var placeholder = "Choose an option"
        set(value) {
            if (field != value) {
                field = value
                if (value.isEmpty()) {
                    button.text = value
                }
            }
        }

    /**
     * The mode of selection for the dropdown menu.
     *
     * @see SelectionMode
     */
    var selectionMode = SelectionMode.Single

    /**
     * The options available in the dropdown menu.
     */
    var options = listOf<Option<T>>()
        set(value) {
            if (field != value) {
                field = value
                listChanged = true
            }
        }


    private var buttons = mapOf<Option<T>, UITextButton>()

    private var listChanged = true


    init {
        clipToBounds = true

        +button.apply {
            text = placeholder
        }

        menu.apply {
            width = MatchContent
            height = MatchContent
            scaleCenter = Anchor.Center
            alpha = 0f
            scale = Vec2(0.9f)
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (listChanged) {
            listChanged = false
            onOptionsChanged()
        }

        if (isExpanded) {
            optionsContainer.minWidth = max(buttons.values.maxOfOrNull { it.contentWidth + it.padding.horizontal } ?: 0f, button.width)
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        button.content.textEntity.width = width - button.padding.horizontal - button.trailingIcon!!.width

        if (isExpanded) {
            val (sceneSpaceX, sceneSpaceY) = convertLocalToSceneCoordinates(0f, height)

            menu.x = sceneSpaceX
            menu.y = sceneSpaceY
            menu.maxHeight = menuWrapper.parent.getHeight() - sceneSpaceY
        }

        super.onManagedDraw(gl, camera)
    }


    protected open fun onOptionsChanged() {
        optionsContainer.detachChildren()

        options.fastForEach { option ->

            val button = object : UITextButton() {

                override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
                    color = theme.accentColor
                }

                init {
                    width = FillParent
                    text = option.text
                    color = option.color
                    leadingIcon = option.leadingIcon
                    trailingIcon = option.trailingIcon
                    onActionUp = { onOptionPress(option) }
                    background = null
                    foreground = UIBox().apply {
                        cornerRadius = 12f
                        applyTheme = {
                            color = it.accentColor
                            alpha = 0f
                        }
                    }
                    isSelected = option.value in value
                }

                override fun onSelectionChange() {
                    foreground!!.clearModifiers(ModifierType.Alpha)
                    foreground!!.fadeTo(if (isSelected) 0.25f else 0f, 0.2f)
                }
            }

            optionsContainer += button
            buttons += option to button
        }
    }

    protected open fun onOptionPress(option: Option<T>) {

        val button = buttons[option]

        when (selectionMode) {

            SelectionMode.Single -> {
                value = listOf(option.value)
                buttons.values.forEach { it.isSelected = it == button }
                collapse()
            }

            SelectionMode.Multiple -> {
                value = if (option.value in value) {
                    value - option.value
                } else {
                    value + option.value
                }
            }
        }
    }


    override fun onValueChanged() {
        super.onValueChanged()

        if (value.isEmpty()) {
            button.text = placeholder
            buttons.values.forEach { it.isSelected = false }
            return
        }

        when (selectionMode) {

            SelectionMode.Single -> {
                val selectedOption = options.firstOrNull { it.value == value.firstOrNull() }
                button.text = selectedOption?.text ?: placeholder
            }

            SelectionMode.Multiple -> {
                button.text = options.filter { o -> o.value in value }.joinToString { it.text }
            }
        }

        buttons.forEach { (option, button) ->
            button.isSelected = option.value in value
        }
    }


    fun expand() {
        if (!isExpanded) {
            menu.clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            menu.fadeTo(1f, 0.2f)
            menu.scaleTo(1f, 0.2f)

            menuWrapper.detachSelf()
            getParentScene()?.attachChild(menuWrapper)
        }
    }

    fun collapse() {
        if (isExpanded) {
            menu.clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            menu.scaleTo(0.9f, 0.2f)
            menu.fadeTo(0f, 0.2f).after {
                updateThread {
                    menuWrapper.detachSelf()
                }
            }
        }
    }


    data class Option<T : Any>(
        val value: T,
        val text: String,
        val color: Color4 = Theme.current.accentColor,
        val leadingIcon: UIComponent? = null,
        val trailingIcon: UIComponent? = null,
    )

}

enum class SelectionMode {

    /**
     * Only one option can be selected at a time.
     */
    Single,

    /**
     * Multiple options can be selected at a time.
     */
    Multiple
}

