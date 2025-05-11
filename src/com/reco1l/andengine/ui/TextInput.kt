package com.reco1l.andengine.ui

import android.view.*
import android.view.KeyEvent.*
import android.view.inputmethod.InputMethodManager
import androidx.core.content.*
import androidx.core.view.*
import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*
import kotlin.math.*
import kotlin.synchronized

class TextInput(initialValue: String) : Control<String>(initialValue), IFocusable, OnApplyWindowInsetsListener {

    override var applyTheme: ExtendedEntity.(Theme) -> Unit = { theme ->
        background?.color = theme.accentColor * 0.25f
        foreground?.color = if (isFocused) theme.accentColor else theme.accentColor * 0.4f
        textEntity.color = theme.accentColor
    }


    private val textEntity = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }

    private val caret = Box().apply {
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
     * The entity that will be used to translate the text input field when the keyboard overlaps it.
     */
    var translateTarget: ExtendedEntity = this

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

        background = Box().apply { cornerRadius = 12f }
        foreground = Box().apply {
            paintStyle = PaintStyle.Outline
            cornerRadius = 12f
        }
    }

    override fun onFocus() {
        setKeyboardVisibilty(true)
        caret.isVisible = true

        foreground?.clearModifiers(ModifierType.Color)
        foreground?.colorTo(Theme.current.accentColor, 0.1f)

        ViewCompat.setOnApplyWindowInsetsListener(ExtendedEngine.Current.context.window.decorView, this)
    }

    override fun onBlur() {
        setKeyboardVisibilty(false)
        caret.isVisible = false

        foreground?.clearModifiers(ModifierType.Color)
        foreground?.colorTo(Theme.current.accentColor * 0.4f, 0.1f)
    }

    @Suppress("DEPRECATION")
    private fun setKeyboardVisibilty(value: Boolean) {

        val imm = ExtendedEngine.Current.context.getSystemService<InputMethodManager>()
            ?: throw NullPointerException("InputMethodManager is null")

        val windowInsets = ViewCompat.getRootWindowInsets(ExtendedEngine.Current.context.window.decorView)
        val keyboardHeight = windowInsets!!.getInsets(WindowInsetsCompat.Type.ime()).bottom

        // Tricky prevention from openning the keyboard while it should be closed and vice versa.
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


    private fun appendCharacter(char: Char) {
        if (value.length == maxCharacters && maxCharacters != 0) {
            return
        }

        val currentText = value
        val currentCaretPosition = caretPosition

        value = currentText.substring(0, currentCaretPosition) + char + currentText.substring(currentCaretPosition)
        caretPosition++

        val text = value

        // Calculate the width of the text with a dummy character appended to it when we type with whitespaces at
        // the end of the line the function getStringWidth() will return the width of the text trimmed which will
        // cause the caret to be misplaced.
        val dummyCharWidth = font!!.getStringWidth("0")

        letterPositions = IntArray(text.length + 1) { i ->
            if (i > 0) font!!.getStringWidth(text.substring(0, i) + "0") - dummyCharWidth else 0
        }
    }


    override fun onValueChanged() {
        super.onValueChanged()
        textEntity.text = value
    }

    override fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean = synchronized(value) {

        if (keyCode == KEYCODE_BACK && isFocused) {
            if (event.action == ACTION_UP) {
                blur()
            }
            return true
        }

        if (event.action == ACTION_DOWN) {

            when (keyCode) {

                KEYCODE_DEL -> {
                    value = value.removeRange(max(0, caretPosition - 1), caretPosition)
                    caretPosition = max(0, caretPosition - 1)
                }

                KEYCODE_DPAD_LEFT -> caretPosition = max(0, caretPosition - 1)
                KEYCODE_DPAD_RIGHT -> caretPosition = min(value.length, caretPosition + 1)

                KEYCODE_ENTER -> {
                    if (confirmOnEnter) {
                        blur()
                    } else {
                        appendCharacter('\n')
                    }
                }

                else -> {
                    val char = event.unicodeChar.toChar()

                    if (char.isLetterOrDigit() || char.isWhitespace()) {
                        appendCharacter(char)
                    }
                }
            }
        }

        return true
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {

        if (isFocused) {
            val keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            val (_, sceneY) = convertLocalToSceneCoordinates(0f, height)
            val (_, screenY) = ExtendedEngine.Current.camera.getScreenSpaceCoordinates(0f, sceneY)

            translateTarget.translationY = if (screenY < keyboardHeight) -(keyboardHeight - screenY) else 0f
        } else {
            translateTarget.translationY = 0f
            ViewCompat.setOnApplyWindowInsetsListener(ExtendedEngine.Current.context.window.decorView, null)
        }

        return insets
    }
}