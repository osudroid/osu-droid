package com.reco1l.osu.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.widget.EditText
import androidx.core.content.res.TypedArrayUtils
import androidx.core.widget.doAfterTextChanged
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.R
import com.reco1l.toolkt.android.hideKeyboard


@SuppressLint("RestrictedApi")
open class SelectPreference(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): Preference(context, attrs, defStyleAttr, defStyleRes) {


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle, android.R.attr.dialogPreferenceStyle))


    /**
     * The selected value.
     */
    var value: String? = null
        set(value) {

            // Always persist/notify the first time.
            val changed = !field.contentEquals(value)

            if (changed || !wasValueSet) {
                field = value
                dialog.setSelected(value)
                wasValueSet = true

                persistString(value)
                if (changed) {
                    notifyChanged()
                }
            }
        }

    /**
     * The options to be displayed in the dialog.
     */
    var options: MutableList<Option>
        get() = dialog.options
        set(value) {
            dialog.setOptions(value)
        }


    private var wasValueSet = false


    private val dialog = SelectDialog()


    init {

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ListPreference, defStyleAttr, defStyleRes)

        val entries = TypedArrayUtils.getTextArray(attributes, R.styleable.ListPreference_entries, R.styleable.ListPreference_android_entries)
        val values = TypedArrayUtils.getTextArray(attributes, R.styleable.ListPreference_entryValues, R.styleable.ListPreference_android_entryValues)

        attributes.recycle()

        if (entries != null && values != null) {
            dialog.setOptions(MutableList(entries.size) {
                Option(entries[it].toString(), values[it])
            })
        }

        dialog.setTitle(title.toString())
        dialog.setOnSelectListener { value = it as String }
    }


    override fun onClick() = dialog.show()


    override fun onGetDefaultValue(a: TypedArray, index: Int) = a.getString(index)

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedString(defaultValue as String?)
    }



    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return if (isPersistent) superState else SavedState(superState).also { it.value = value }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {

        if (state == null || state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        value = state.value
    }


    private class SavedState : BaseSavedState {

        constructor(superState: Parcelable?) : super(superState)

        constructor(source: Parcel) : super(source) {
            value = source.readString()
        }

        var value: String? = null

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(value)
        }

    }
}


@SuppressLint("RestrictedApi")
class InputPreference(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): Preference(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.editTextPreferenceStyle, android.R.attr.editTextPreferenceStyle))


    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, defStyleRes)

        val layout = TypedArrayUtils.getResourceId(
            attributes,
            R.styleable.Preference_layout,
            R.styleable.Preference_android_layout,
            ru.nsu.ccfit.zuev.osuplus.R.layout.settings_preference_input
        )

        attributes.recycle()

        if (layout != ru.nsu.ccfit.zuev.osuplus.R.layout.settings_preference_input &&
            layout != ru.nsu.ccfit.zuev.osuplus.R.layout.settings_preference_input_bottom) {
            layoutResource = ru.nsu.ccfit.zuev.osuplus.R.layout.settings_preference_input
        }
    }


    private lateinit var input: EditText


    private var onTextInputBind: EditText.() -> Unit = {}

    private var value: String? = null


    /**
     * Set the function to be called when the text input is bound.
     */
    fun setOnTextInputBind(listener: EditText.() -> Unit) {
        onTextInputBind = listener
    }

    /**
     * Set the text to be displayed in the input.
     */
    fun setText(value: String?) {

        if (!::input.isInitialized) {
            this.value = value
            return
        }

        input.setText(value)
        input.post { onValueChange(value) }
    }

    /**
     * Returns the text from the input.
     */
    fun getText() = value


    private fun onValueChange(value: String?) {

        val wasBlocking = shouldDisableDependents()

        this.value = value
        persistString(value)

        if (::input.isInitialized) {
            input.setText(value)
        }

        val isBlocking = shouldDisableDependents()
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking)
        }

        notifyChanged()
    }


    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        input = holder.findViewById(ru.nsu.ccfit.zuev.osuplus.R.id.input)!! as EditText
        input.setText(value)
        input.onTextInputBind()
        input.imeOptions = IME_ACTION_DONE

        input.setOnEditorActionListener { view, action, _ ->

            // Only if user press done it'll save the value.
            if (action == IME_ACTION_DONE) {
                input.hideKeyboard()
                view.post { onValueChange(value) }
                return@setOnEditorActionListener true
            }
            false
        }

        // We ensure if the focus was lost we restore the value.
        input.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                input.setText(value)
            }
        }

        input.doAfterTextChanged { value = it.toString() }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int) = a.getString(index)

    override fun onSetInitialValue(defaultValue: Any?) {
        setText(getPersistedString(defaultValue as String?))
    }


    override fun shouldDisableDependents() = value.isNullOrEmpty() || super.shouldDisableDependents()


    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return if (isPersistent) superState else SavedState(superState).also { it.text = value }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {

        if (state == null || state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        value = state.text
    }


    private class SavedState : BaseSavedState {

        constructor(superState: Parcelable?) : super(superState)

        constructor(source: Parcel) : super(source) {
            text = source.readString()
        }

        var text: String? = null

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(text)
        }

    }

}