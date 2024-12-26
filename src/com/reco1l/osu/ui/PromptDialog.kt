package com.reco1l.osu.ui

import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import ru.nsu.ccfit.zuev.osuplus.R

/**
 * A dialog that prompts the user for input.
 */
open class PromptDialog : MessageDialog() {


    override val layoutID = R.layout.dialog_input_fragment


    override var message: CharSequence = ""
        set(value) {
            field = value
            super.message = value
            if (isCreated) {
                findViewById<TextView>(R.id.message)!!.isVisible = value.isNotBlank()
            }
        }


    /**
     * The text input by user.
     */
    var input: String? = null
        set(value) {
            field = value
            if (isCreated) {
                val inputView = findViewById<EditText>(R.id.input)!!
                if (inputView.text.toString() != value) {
                    inputView.setText(value)
                }
            }
        }

    /**
     * The text to display in the input hint.
     */
    var hint: String? = null
        set(value) {
            field = value
            if (isCreated) {
                findViewById<EditText>(R.id.input)?.hint = value
            }
        }

    /**
     * The function to call when the text input is changed.
     */
    var onTextChanged: ((PromptDialog) -> Unit)? = null

    /**
     * The input type of the EditText.
     */
    var inputType: Int = InputType.TYPE_NULL
        set(value) {
            field = value
            if (isCreated) {
                findViewById<EditText>(R.id.input)?.inputType = value
            }
        }


    override fun onLoadView() {
        super.onLoadView()

        hint = hint
        input = input
        message = message
        inputType = inputType

        findViewById<EditText>(R.id.input)!!.doOnTextChanged { text, _, _, _ ->
            input = text.toString()
            onTextChanged?.invoke(this@PromptDialog)
        }
    }


    /**
     * The text input by user.
     */
    fun setInput(text: String?): PromptDialog {
        input = text
        return this
    }

    /**
     * The text to display in the input hint.
     */
    fun setHint(text: String): PromptDialog {
        hint = text
        return this
    }

    /**
     * The function to call when the text input is changed.
     */
    fun setOnTextChanged(action: (PromptDialog) -> Unit): PromptDialog {
        onTextChanged = action
        return this
    }

    /**
     * The input type of the EditText.
     */
    fun setInputType(type: Int): PromptDialog {
        inputType = type
        return this
    }


    override fun addButton(text: String, tint: Int, clickListener: (MessageDialog) -> Unit): PromptDialog {
        return super.addButton(text, tint, clickListener) as PromptDialog
    }

}