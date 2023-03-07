package com.reco1l.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import com.reco1l.framework.Animation;

import com.rimu.R;

public class SliderPreference extends SeekBarPreference {

    private OnValueFormat formatter;
    private LinearLayout resetButton;
    private TextView valueText;

    private int defaultValue;
    private boolean isResetButtonShown = false;

    //--------------------------------------------------------------------------------------------//

    public SliderPreference(Context context) {
        this(context, null);
    }

    public SliderPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SliderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SliderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setLayoutResource(R.layout.custom_preference_seekbar);
        setShowSeekBarValue(true);
        setUpdatesContinuously(true);
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface OnValueFormat {
        String applyFormat(int value);
    }

    public void setValueFormatter(OnValueFormat formatter) {
        this.formatter = formatter;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        resetButton = holder.itemView.findViewById(R.id.pref_reset);

        if (resetButton != null) {
            resetButton.setOnClickListener(v -> setValue(defaultValue));
        }

        valueText = holder.itemView.findViewById(R.id.seekbar_value);

        if (valueText != null) {
            valueText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (formatter != null) {
                        valueText.removeTextChangedListener(this);
                        valueText.setText(formatter.applyFormat(getValue()));
                        valueText.addTextChangedListener(this);
                    }
                }
            });
        }
        super.onBindViewHolder(holder);
        isResetButtonShown = false;
        handleResetButton(getValue());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        Object value = super.onGetDefaultValue(a, index);

        if (value != null) {
            defaultValue = (int) value;
        }
        return value;
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue != null) {
            this.defaultValue = (int) defaultValue;
        }
        super.onSetInitialValue(defaultValue);
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue != null) {
            this.defaultValue = (int) defaultValue;
        }
        super.setDefaultValue(defaultValue);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public boolean callChangeListener(Object newValue) {
        handleResetButton((int) newValue);
        return super.callChangeListener(newValue);
    }

    @Override
    public void setValue(int seekBarValue) {
        callChangeListener(seekBarValue);
        super.setValue(seekBarValue);
    }

    //--------------------------------------------------------------------------------------------//

    private void handleResetButton(int newValue) {
        boolean isDefaultValue = newValue == defaultValue;

        if (!isDefaultValue && !isResetButtonShown) {
            isResetButtonShown = true;
            Animation.of(resetButton)
                    .toAlpha(1)
                    .play(200);
        }
        if (isDefaultValue && isResetButtonShown) {
            isResetButtonShown = false;
            Animation.of(resetButton)
                    .toAlpha(0)
                    .play(200);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public TextView getValueTextView() {
        return valueText;
    }
}
