package com.reco1l.andengine.ui

import android.text.InputType
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.getSystemService
import androidx.core.text.isDigitsOnly
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.edlplan.framework.easing.Easing
import com.osudroid.utils.mainThread
import com.reco1l.andengine.*
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.component.innerWidth
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.andengine.input.touch.*
import com.rian.andengine.modifier.ModifierType
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.math.*
import kotlin.text.substring

open class UITextInput(initialValue: String) : UIControl<String>(initialValue), IFocusable {

    override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
        background?.color = theme.accentColor * 0.25f
        foreground?.color = if (isFocused) theme.accentColor else theme.accentColor * 0.4f
        textEntity.color = theme.accentColor
        placeholderEntity.color = theme.accentColor * 0.6f
    }

    private val placeholderEntity = UIText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
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

    private val selectionBox = UIBox().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft

        applyTheme = {
            color = it.accentColor.copy(alpha = 0.35f)
        }
    }


    /**
     * Whether to confirm the input on enter key press.
     */
    var confirmOnEnter = true

    /**
     * A callback that is invoked when the input is confirmed by pressing the enter key.
     */
    var onConfirm: (() -> Unit)? = null

    /**
     * The maximum number of characters allowed in the text field. If set to 0, there is no limit.
     */
    var maxCharacters = 0

    /**
     * The font used to render the text.
     */
    val font by textEntity::font

    /**
     * The placeholder text displayed when the input field is empty.
     */
    var placeholder by placeholderEntity::text


    private var caretFading = false
    private var elapsedTimeSec = 0f
    private var targetSelectionBoxWidth = 0f
    private var targetCursorPosition = 0f

    private var cursorPositions = intArrayOf(0)

    /**
     * Horizontal text scroll, see [updateTextScroll].
     */
    private var textScrollOffset = 0f

    /**
     * Start of the current press in milliseconds, or -1 when not pressed.
     */
    private var pressStartTime = -1L

    /**
     * Whether the current press turned into a selection, dragging then extends it and the release must not
     * move the cursor.
     */
    private var isSelecting = false

    /**
     * The x position of the current press, relative to the start of the text.
     */
    private var pressTextX = 0f

    /**
     * Bounds of the word selected by the long press, the selection always contains it while dragging.
     */
    private var selectionWordStart = 0
    private var selectionWordEnd = 0

    /**
     * The width of the entered text, without padding.
     */
    val textWidth: Float
        get() = cursorPositions.lastOrNull()?.toFloat() ?: 0f

    /**
     * Hides the outline while the input is neither focused nor holds text.
     */
    var hideOutlineWhenIdle = false

    // Use a hidden EditText to properly handle soft keyboard input (e.g., UTF-16 characters, selection and clipboard operations).
    private val internalEditText = AppCompatEditText(UIEngine.current.context).apply {
        layoutParams = ViewGroup.LayoutParams(1, 1)
        alpha = 0f

        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_EXTRACT_UI

        isClickable = false

        addTextChangedListener(
            onTextChanged = { text: CharSequence?, start: Int, _: Int, count: Int ->
                val newText = text?.toString() ?: ""

                if (newText == value) {
                    return@addTextChangedListener
                }

                fun revertWithError() {
                    setText(value, TextView.BufferType.EDITABLE)
                    setSelection(value.length.coerceAtMost(start))
                    notifyInputError()
                }

                if (maxCharacters > 0 && newText.codePointCount(0, newText.length) > maxCharacters) {
                    revertWithError()
                    return@addTextChangedListener
                }

                if (count > 0) {
                    val addedText = newText.substring(start, start + count)
                    var codePointIndex = 0

                    while (codePointIndex < addedText.length) {
                        val codePoint = addedText.codePointAt(codePointIndex)
                        val charCount = Character.charCount(codePoint)
                        val character = addedText.substring(codePointIndex, codePointIndex + charCount)

                        if (!isCharacterAllowed(character)) {
                            revertWithError()
                            return@addTextChangedListener
                        }

                        codePointIndex += charCount
                    }
                }

                if (!isTextValid(newText)) {
                    revertWithError()
                    return@addTextChangedListener
                }

                value = newText
            }
        )

        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (confirmOnEnter) {
                    onConfirm?.invoke()
                    blur()
                } else {
                    // New line insertion
                    text?.insert(selectionEnd, "\n")
                }

                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

    }


    init {
        height = 48f
        padding = Vec4(12f, 0f)
        // Clipped viewport for scrolling text, see updateTextScroll().
        +UIContainer().apply {
            width = FillParent
            height = FillParent
            clipToBounds = true

            +placeholderEntity
            // Behind the text, so the selected characters stay readable.
            +selectionBox
            +textEntity
            +caret
        }

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
    }

    private fun setKeyboardVisibility(value: Boolean) = mainThread {
        val root = UIEngine.current.context.window.decorView as ViewGroup

        val inputMethodManager = UIEngine.current.context.getSystemService<InputMethodManager>()!!

        val windowInsets = ViewCompat.getRootWindowInsets(UIEngine.current.context.window.decorView)
        val keyboardHeight = windowInsets!!.getInsets(WindowInsetsCompat.Type.ime()).bottom

        // Tricky prevention from opening the keyboard while it should be closed and vice versa.
        if (value && keyboardHeight > 0) {
            return@mainThread
        }

        internalEditText.apply {
            if (value) {
                if (parent == null) {
                    root.addView(this)
                }

                requestFocus()
                setText(this@UITextInput.value, TextView.BufferType.EDITABLE)

                post {
                    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
            } else {
                inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
                clearFocus()

                if (parent != null) {
                    root.removeView(this)
                }
            }
        }
    }


    /**
     * Scrolls the text so the caret stays visible.
     */
    private fun updateTextScroll() {
        val visibleWidth = (innerWidth - caret.width).coerceAtLeast(0f)
        val caretPosition = if (caret.isVisible) targetCursorPosition else textWidth

        var offset = textScrollOffset
        if (caretPosition - offset > visibleWidth) {
            offset = caretPosition - visibleWidth
        }
        if (caretPosition - offset < 0f) {
            offset = caretPosition
        }
        // Scroll back when the text got shorter.
        offset = offset.coerceAtMost((textWidth - visibleWidth).coerceAtLeast(0f)).coerceAtLeast(0f)

        textScrollOffset = offset
        textEntity.x = -offset
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        // A long press selects the word under the finger, dragging afterward extends the selection.
        if (pressStartTime > 0L && System.currentTimeMillis() - pressStartTime >= LONG_PRESS_DURATION) {
            pressStartTime = -1L
            isSelecting = true

            if (!isFocused) {
                focus()
            }
            selectWordAt(getPositionAt(pressTextX))
        }

        // The cursor position first, the scroll depends on it and the caret on the scroll.
        if (caret.isVisible) {
            targetCursorPosition = cursorPositions.getOrNull(internalEditText.selectionEnd)?.toFloat() ?: textWidth
        }
        updateTextScroll()

        if (hideOutlineWhenIdle) {
            val isIdle = !isFocused && value.isEmpty()
            foreground?.let { it.alpha = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.15f, it.alpha, if (isIdle) 0f else 1f, 0.02f) }
        }

        selectionBox.width = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.3f), selectionBox.width, targetSelectionBoxWidth, 0f, 0.3f, Easing.OutExpo)
        caret.x = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.3f), caret.x, textEntity.x + targetCursorPosition, 0f, 0.3f, Easing.OutExpo)
        selectionBox.x = caret.x

        if (caret.isVisible) {
            caret.y = textEntity.y
            caret.height = textEntity.height

            if (elapsedTimeSec >= 0.5f) {
                caretFading = !caretFading
                elapsedTimeSec = 0f
            }

            val alphaFactor = elapsedTimeSec / 0.5f
            caret.alpha = if (caretFading) 1f - alphaFactor else alphaFactor

            elapsedTimeSec += deltaTimeSec

            if (internalEditText.selectionStart != internalEditText.selectionEnd) {
                val startX = cursorPositions.getOrNull(internalEditText.selectionStart)?.toFloat() ?: 0f
                val endX = cursorPositions.getOrNull(internalEditText.selectionEnd)?.toFloat() ?: 0f

                selectionBox.origin = if (startX >= endX) Anchor.CenterLeft else Anchor.CenterRight
                selectionBox.height = textEntity.height
                targetSelectionBoxWidth = abs(endX - startX)
            } else {
                targetSelectionBoxWidth = 0f
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        pressTextX = localX - padding.left + textScrollOffset

        when {
            event.isActionDown -> {
                pressStartTime = System.currentTimeMillis()
                isSelecting = false
            }

            event.isActionMove -> {
                if (isSelecting) {
                    updateSelection(getPositionAt(pressTextX))
                }
            }

            event.isActionUp -> {
                pressStartTime = -1L

                if (isSelecting) {
                    isSelecting = false
                } else if (!isFocused) {
                    focus()
                } else {
                    val position = getPositionAt(pressTextX)
                    mainThread { internalEditText.setSelection(position) }
                }
            }
        }
        return true
    }

    /**
     * Returns the cursor position closest to [x], relative to the start of the text.
     */
    private fun getPositionAt(x: Float): Int {
        var distance = 0f
        var position = 0

        for (i in cursorPositions.indices) {
            val newDistance = abs(cursorPositions[i].toFloat() - x)

            if (newDistance < distance || i == 0) {
                distance = newDistance
                position = i
            }
        }

        return position
    }

    /**
     * Selects the word around [position], words are separated by whitespace.
     */
    private fun selectWordAt(position: Int) {
        selectionWordStart = position
        selectionWordEnd = position

        while (selectionWordStart > 0 && !value[selectionWordStart - 1].isWhitespace()) {
            selectionWordStart--
        }

        while (selectionWordEnd < value.length && !value[selectionWordEnd].isWhitespace()) {
            selectionWordEnd++
        }

        updateSelection(position)
    }

    /**
     * Selects from the long pressed word to [position], in either direction.
     */
    private fun updateSelection(position: Int) {
        val start = minOf(selectionWordStart, position)
        val end = maxOf(selectionWordEnd, position)

        mainThread {
            val length = internalEditText.text?.length ?: 0
            internalEditText.setSelection(start.coerceIn(0, length), end.coerceIn(0, length))
        }
    }


    override fun onProcessValue(value: String): String {

        val codePointCount = value.codePointCount(0, value.length)
        cursorPositions = IntArray(codePointCount + 1) { cursorIndex ->

            // First position is always the beginning of the line.
            if (cursorIndex <= 0) {
                return@IntArray 0
            }

            var totalAdvance = 0
            var stringIndex = 0
            var codePointIndex = 0

            // Iterate through code points until the cursor position is reached.
            while (codePointIndex < cursorIndex && stringIndex < value.length) {
                val codePoint = value.codePointAt(stringIndex)
                val charCount = Character.charCount(codePoint)

                // Extract the complete character as a string, including surrogate pairs.
                val characterString = value.substring(stringIndex, stringIndex + charCount)
                totalAdvance += (font?.getLetter(characterString[0])?.mAdvance ?: 0f).toInt()

                stringIndex += charCount
                codePointIndex++
            }

            totalAdvance
        }

        return super.onProcessValue(value)
    }

    /**
     * Checks if a [Char] is allowed to be appended to this [UITextInput].
     *
     * @param char The [Char] to check.
     * @return `true` if [char] is allowed to be appended to this [UITextInput], `false` otherwise.
     */
    protected open fun isCharacterAllowed(char: String) = true

    /**
     * Checks whether a text is valid as a [value] for this [UITextInput].
     *
     * @param text The text to check.
     * @return `true` if [text] is valid as a [value] for this [UITextInput], `false` otherwise.
     */
    protected open fun isTextValid(text: String) = true

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
        placeholderEntity.isVisible = value.isEmpty()
    }

    override fun onValueChanged() {
        super.onValueChanged()
        updateVisuals()

        // A value set from outside while focused, the hidden EditText would otherwise restore its old text.
        if (isFocused) {
            mainThread {
                if (internalEditText.text?.toString() != value) {
                    internalEditText.setText(value, TextView.BufferType.EDITABLE)
                    internalEditText.setSelection(value.length)
                }
            }
        }
    }


    companion object {
        /**
         * How long a press has to last to select the word under it, the same as the long press of buttons.
         */
        private const val LONG_PRESS_DURATION = 500L
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
    val maxValue: T? = null,
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
    maxValue: Int? = Int.MAX_VALUE,
) : RangeConstrainedTextInput<Int>(initialValue, minValue, maxValue) {
    override fun isCharacterAllowed(char: String) = super.isCharacterAllowed(char) && (char.isDigitsOnly() || char == "-")

    override fun isTextValid(text: String) =
        // Check for underflow/overflow
        super.isTextValid(text) && (text.isEmpty() || text.toIntOrNull() != null)

    override fun convertValue(value: String) = value.toIntOrNull()
}

/**
 * A [UITextInput] that only allows [Float]s to be entered.
 */
class FloatTextInput(
    initialValue: Float?,
    minValue: Float? = -Float.MAX_VALUE,
    maxValue: Float? = Float.MAX_VALUE,
) : RangeConstrainedTextInput<Float>(initialValue, minValue, maxValue) {
    override fun isCharacterAllowed(char: String) =
        super.isCharacterAllowed(char) && (char.isDigitsOnly() || char == "." || char == "-")

    override fun isTextValid(text: String) =
        // Check for underflow/overflow
        super.isTextValid(text) && (text.isEmpty() || text.toFloatOrNull() != null)

    override fun convertValue(value: String) = value.toFloatOrNull()
}
