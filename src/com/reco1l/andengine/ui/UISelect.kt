package com.reco1l.andengine.ui

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.modifier.ModifierType
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.framework.*
import org.anddev.andengine.engine.camera.*
import javax.microedition.khronos.opengles.*

/**
 * A dropdown menu that allows the user to select an option from a list.
 */
@Suppress("LeakingThis")
open class UISelect<T : Any>(initialValues: List<T> = emptyList()) : UIControl<List<T>>(initialValues) {

    private val dropdown = UIDropdown(this)

    /**
     * The button that toggles the dropdown menu.
     */
    val button = UITextButton().apply {
        width = Size.Full
        alignment = Anchor.CenterLeft
        onActionUp = {
            if (dropdown.isExpanded) {
                dropdown.hide()
            } else {
                dropdown.show()
            }
        }
        style += {
            backgroundColor = it.accentColor * 0.25f
        }

        trailingIcon = FontAwesomeIcon(Icon.ChevronDown).apply {
            rotation = 0f
            alpha = 0.5f
        }
        trailingIcon?.rotationCenter = Anchor.Center
    }

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


    /**
     * The mode of selection for the dropdown menu.
     *
     * @see SelectionMode
     */
    var selectionMode = SelectionMode.Single

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


    private var buttons = mapOf<Option<T>, UITextButton>()

    private var listChanged = true


    init {
        clipToBounds = true

        +button.apply {
            text = placeholder
        }

        val chevronIcon = button.trailingIcon as FontAwesomeIcon

        dropdown.onExpand = {
            chevronIcon.clearModifiers(ModifierType.Rotation)
            chevronIcon.rotateTo(180f, 0.4f).eased(Easing.OutBounce)
        }

        dropdown.onCollapse = {
            chevronIcon.clearModifiers(ModifierType.Rotation)
            chevronIcon.rotateTo(0f, 0.4f).eased(Easing.OutBounce)
        }
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        if (listChanged) {
            listChanged = false
            onOptionsChanged()
        }

        super.onManagedDraw(gl, camera)
    }

    open fun onOptionsChanged() {
        dropdown.clearButtons()
        buttons = mapOf()

        options.forEach { option ->

            dropdown.addButton {
                text = option.text
                color = option.color
                leadingIcon = option.leadingIcon
                trailingIcon = option.trailingIcon
                isSelected = option.value in value
                buttons += option to this

                onActionUp = {
                    when (selectionMode) {

                        SelectionMode.Single -> {
                            value = listOf(option.value)

                            dropdown.forEachButton { it.isSelected = it == this }
                            dropdown.hide()
                        }

                        SelectionMode.Multiple -> {
                            value = if (option.value in value) value - option.value else value + option.value
                        }
                    }
                }

            }

        }
    }

    override fun onValueChanged() {
        super.onValueChanged()

        if (value.isEmpty()) {
            button.text = placeholder
            dropdown.forEachButton { it.isSelected = false }
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

    /**
     * Represents an option in the dropdown menu.
     */
    data class Option<T : Any>(

        /**
         * The value associated with this option.
         */
        val value: T,

        /**
         * The text displayed for this option.
         */
        val text: String,

        /**
         * The color of the option text and icons.
         */
        val color: Color4 = Theme.current.accentColor,

        /**
         * An optional icon displayed before the text.
         */
        val leadingIcon: UIComponent? = null,

        /**
         * An optional icon displayed after the text.
         */
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

