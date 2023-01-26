package com.reco1l.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import ru.nsu.ccfit.zuev.osuplus.R;

public class GameEditTextPreference extends Preference {

    private EditText mEditText;
    private OnPreferenceChangeListener mListener;

    private TextWatcher mTextWatcher;
    private CharSequence mDefaultValue;

    private SharedPreferences mPreferences;

    private Runnable mOnFocusLost;

    //--------------------------------------------------------------------------------------------//

    public GameEditTextPreference(Context context) {
        this(context, null);
    }

    public GameEditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GameEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.custom_preference_edittext);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        mTextWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (hasKey()) {
                    mPreferences.edit()
                            .putString(getKey(), s.toString())
                            .commit();
                }

                if (mListener != null) {
                    mListener.onPreferenceChange(GameEditTextPreference.this, s.toString());
                }
            }
        };
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void setOnPreferenceChangeListener(OnPreferenceChangeListener onPreferenceChangeListener) {
        mListener = onPreferenceChangeListener;
        super.setOnPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mEditText = (EditText) holder.findViewById(R.id.edit_text);
        mEditText.removeTextChangedListener(mTextWatcher);

        CharSequence text = mDefaultValue;
        if (hasKey()) {
            text = mPreferences.getString(getKey(), mDefaultValue != null ? mDefaultValue.toString() : "");
        }
        mEditText.setText(text);
        mEditText.addTextChangedListener(mTextWatcher);

        mEditText.setOnFocusChangeListener((v, hasFocus) -> {
            mEditText.setSelected(hasFocus);

            if (!hasFocus && mOnFocusLost != null) {
                mOnFocusLost.run();
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    public void setOnFocusLostListener(Runnable task) {
        mOnFocusLost = task;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        Object value = a.getString(index);

        if (value != null) {
            mDefaultValue = value.toString();
        }
        return value;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue != null) {
            mDefaultValue = defaultValue.toString();
        }
        super.setDefaultValue(defaultValue);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        if (defaultValue != null) {
            mDefaultValue = defaultValue.toString();
        }
        super.onSetInitialValue(defaultValue);
    }

    //--------------------------------------------------------------------------------------------//

    public void setText(String text) {
        mEditText.setText(text);
    }

    public String getText() {
        return mEditText.getText().toString();
    }
}
