package com.reco1l.andengine.ui

import android.view.*
import android.view.KeyEvent.*
import android.view.inputmethod.InputMethodManager
import androidx.core.content.*
import androidx.core.view.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*
import kotlin.math.*
import kotlin.synchronized
import kotlin.text.substring

open class UITextInput(initialValue: String) : UIControl<String>(initialValue), IFocusable {

    override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
        background?.color = theme.accentColor * 0.25f
        foreground?.color = if (isFocused) theme.accentColor else theme.accentColor * 0.4f
        textEntity.color = theme.accentColor
    }


    private val textEntity = UIText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }

    private val caret = UIBox().apply {
        width = 2f
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        isVisible = false
    }


    /**
     * Whether to confirm the input on enter key press.
     */
    var confirmOnEnter = true

    /**
     * The maximum number of characters allowed in the text field. If set to 0, there is no limit.
     */
    var maxCharacters = 0

    /**
     * The font used to render the text.
     */
    var font by textEntity::font


    private var caretFading = false

    private var caretPosition = 0

    private var elapsedTimeSec = 0f

    private var letterPositions = intArrayOf(0)


    init {
        height = 48f
        padding = Vec4(12f, 0f)

        +textEntity
        +caret

        background = UIBox().apply { cornerRadius = 12f }
        foreground = UIBox().apply {
            paintStyle = PaintStyle.Outline
            cornerRadius = 12f
        }

        updateVisuals()
    }

    override fun onFocus() {
        setKeyboardVisibility(true)
        caret.isVisible = true

        foreground?.clearModifiers(ModifierType.Color)
        foreground?.colorTo(Theme.current.accentColor, 0.1f)

    }

    override fun onBlur() {
        setKeyboardVisibility(false)
        caret.isVisible = false

        foreground?.clearModifiers(ModifierType.Color)
        foreground?.colorTo(Theme.current.accentColor * 0.4f, 0.1f)

        ViewCompat.setOnApplyWindowInsetsListener(ExtendedEngine.Current.context.window.decorView, null)
    }

    @Suppress("DEPRECATION")
    private fun setKeyboardVisibility(value: Boolean) {

        val imm = ExtendedEngine.Current.context.getSystemService<InputMethodManager>()
            ?: throw NullPointerException("InputMethodManager is null")

        val windowInsets = ViewCompat.getRootWindowInsets(ExtendedEngine.Current.context.window.decorView)
        val keyboardHeight = windowInsets!!.getInsets(WindowInsetsCompat.Type.ime()).bottom

        // Tricky prevention from opening the keyboard while it should be closed and vice versa.
        if (value == (keyboardHeight > 0) || !value == (keyboardHeight == 0)) {
            return
        }

        imm.toggleSoftInput(if (value) InputMethodManager.SHOW_FORCED else InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (caret.isVisible) {
            caret.x = letterPositions.getOrNull(caretPosition)?.toFloat() ?: (textEntity.x + textEntity.width)
            caret.y = textEntity.y
            caret.height = textEntity.height

            if (elapsedTimeSec >= 0.5f) {
                caretFading = !caretFading
                elapsedTimeSec = 0f
            }

            val alphaFactor = elapsedTimeSec / 0.5f
            caret.alpha = if (caretFading) 1f - alphaFactor else alphaFactor

            elapsedTimeSec += deltaTimeSec
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionUp) {
            if (!isFocused) {
                focus()
            } else {
                val x = localX - padding.left

                // Find the closest letter position to the touch
                var distance = 0f
                var position = 0

                for (i in letterPositions.indices) {
                    val letterX = letterPositions[i].toFloat()
                    val newDistance = abs(letterX - x)

                    if (newDistance < distance || i == 0) {
                        distance = newDistance
                        position = i
                    }
                }

                caretPosition = position
            }
        }
        return true
    }


    override fun onProcessValue(value: String): String {

        // Calculate the width of the text with a dummy character appended to it when we type with whitespaces at
        // the end of the line the function getStringWidth() will return the width of the text trimmed which will
        // cause the caret to be misplaced.
        val dummyCharWidth = font!!.getStringWidth("0")

        letterPositions = IntArray(value.length + 1) { i ->
            if (i > 0) font!!.getStringWidth(value.substring(0, i) + "0") - dummyCharWidth else 0
        }

        return super.onProcessValue(value)
    }

    /**
     * Checks if a [Char] is allowed to be appended to this [UITextInput].
     *
     * @param char The [Char] to check.
     * @return `true` if [char] is allowed to be appended to this [UITextInput], `false` otherwise.
     */
    protected open fun isCharacterAllowed(char: Char) = true

    /**
     * Checks whether a text is valid as a [value] for this [UITextInput].
     *
     * @param text The text to check.
     * @return `true` if [text] is valid as a [value] for this [UITextInput], `false` otherwise.
     */
    protected open fun isTextValid(text: String) = true

    private fun appendCharacter(char: Char) {

        if (value.length == maxCharacters && maxCharacters != 0) {
            return
        }

        if (!isCharacterAllowed(char)) {
            notifyInputError()
            return
        }

        val currentText = value
        val currentCaretPosition = caretPosition
        val newText =
            currentText.substring(0, currentCaretPosition) + char + currentText.substring(currentCaretPosition)

        if (newText.isNotEmpty() && !isTextValid(newText)) {
            notifyInputError()
            return
        }

        value = newText
        caretPosition++
    }

    private fun deleteCharacterAt(position: Int) {
        if (value.isEmpty()) {
            return
        }

        val currentText = value
        val currentCaretPosition = caretPosition

        val newText =
            if (position > 0) currentText.substring(0, position - 1) + currentText.substring(position)
            else currentText.substring(1)

        if (newText.isNotEmpty() && !isTextValid(newText)) {
            notifyInputError()
            return
        }

        value = newText

        // Move the caret to the left if it's located after the deleted character
        if (position < currentCaretPosition) {
            caretPosition = max(0, currentCaretPosition - 1)
        }
    }

    private fun notifyInputError() {
        textEntity.apply {
            clearModifiers(ModifierType.Color)
            color = Color4.Red
            colorTo(Theme.current.accentColor, 0.2f)
        }

        foreground?.apply {
            clearModifiers(ModifierType.Color)
            color = Color4.Red
            colorTo(Theme.current.accentColor, 0.2f)
        }
    }

    private fun updateVisuals() {
        textEntity.text = value
        caretPosition = min(caretPosition, value.length)
    }

    override fun onValueChanged() {
        super.onValueChanged()
        updateVisuals()
    }

    override fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean = synchronized(value) {

        if (!isFocused) {
            return false
        }

        if (keyCode == KEYCODE_BACK && isFocused) {
            if (event.action == ACTION_UP) {
                blur()
            }
            return true
        }

        if (event.action == ACTION_DOWN) {

            when (keyCode) {

                KEYCODE_DEL -> deleteCharacterAt(caretPosition)

                KEYCODE_ENTER -> {
                    if (confirmOnEnter) {
                        blur()
                    } else {
                        appendCharacter('\n')
                    }
                }

                KEYCODE_DPAD_LEFT -> caretPosition = max(0, caretPosition - 1)
                KEYCODE_DPAD_RIGHT -> caretPosition = min(value.length, caretPosition + 1)

                else -> appendCharacter(event.unicodeChar.toChar())
            }
        }

        return true
    }

}

/**
 * A [UITextInput] whose [value] is constrained to a range of values.
 */
sealed class RangeConstrainedTextInput<T : Comparable<T>?>(
    initialValue: T?,

    /**
     * The minimum value that can be entered in this [RangeConstrainedTextInput].
     *
     * If set to `null`, there is no minimum value.
     */
    val minValue: T? = null,

    /**
     * The maximum value that can be entered in this [RangeConstrainedTextInput].
     *
     * If set to `null`, there is no maximum value.
     */
    val maxValue: T? = null
) : UITextInput(initialValue?.toString() ?: "") {
    override fun isTextValid(text: String): Boolean {
        // Avoid calling convertValue whenever necessary, in case it is expensive
        if (!super.isTextValid(text)) {
            return false
        }

        val minValue = minValue
        val maxValue = maxValue

        if (minValue == null && maxValue == null) {
            return true
        }

        val value = convertValue(text)

        return value == null || (value >= minValue!! && value <= maxValue!!)
    }

    /**
     * Converts a value from a [String] to a [T].
     *
     * @param value The value to convert.
     * @return The converted value. If `null`, [value] is always considered valid.
     */
    protected abstract fun convertValue(value: String): T?
}

/**
 * A [UITextInput] that only allows [Int]s to be entered.
 */
class IntegerTextInput(
    initialValue: Int?,
    minValue: Int? = -Int.MAX_VALUE,
    maxValue: Int? = Int.MAX_VALUE
) : RangeConstrainedTextInput<Int>(initialValue, minValue, maxValue) {
    override fun isCharacterAllowed(char: Char) = super.isCharacterAllowed(char) && (char.isDigit() || char == '-')

    override fun isTextValid(text: String) =
        // Check for underflow/overflow
        super.isTextValid(text) && text.toIntOrNull() != null

    override fun convertValue(value: String) = value.toIntOrNull()
}

/**
 * A [UITextInput] that only allows [Float]s to be entered.
 */
class FloatTextInput(
    initialValue: Float?,
    minValue: Float? = -Float.MAX_VALUE,
    maxValue: Float? = Float.MAX_VALUE
) : RangeConstrainedTextInput<Float>(initialValue, minValue, maxValue) {
    override fun isCharacterAllowed(char: Char) =
        super.isCharacterAllowed(char) && (char.isDigit() || char == '.' || char == '-')

    override fun isTextValid(text: String) =
        // Check for underflow/overflow
        super.isTextValid(text) && text.toFloatOrNull() != null

    override fun convertValue(value: String) = value.toFloatOrNull()
}
