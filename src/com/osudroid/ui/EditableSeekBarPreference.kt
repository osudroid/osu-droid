package com.osudroid.ui

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.reco1l.osu.ui.PromptDialog
import ru.nsu.ccfit.zuev.osuplus.R

/**
 * A [SeekBarPreference] that allows for keyboard input.
 */
class EditableSeekBarPreference(context: Context, attrs: AttributeSet?) : SeekBarPreference(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.findViewById(R.id.seekbar_container)?.setOnClickListener {
            showEditDialog()
        }
    }

    private fun showEditDialog() {
        PromptDialog().apply {
            setTitle(this@EditableSeekBarPreference.title.toString())
            setMessage(summary.toString())
            setInput(value.toString())
            setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
            setHint("Enter a value between $min and $max")

            addButton("OK") { dialog ->
                dialog as PromptDialog

                val inputText = dialog.input

                if (!inputText.isNullOrEmpty()) {
                    val newValue = inputText.toIntOrNull() ?: value
                    value = newValue.coerceIn(min, max)
                }

                dismiss()
            }

            addButton("Cancel") { dialog ->
                dialog.dismiss()
            }

            show()
        }
    }
}