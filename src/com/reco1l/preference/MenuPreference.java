package com.reco1l.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import com.reco1l.ui.custom.ContextMenu;
import com.reco1l.ui.custom.ContextMenuBuilder;
import com.reco1l.framework.input.TouchHandler;
import com.reco1l.framework.input.TouchListener;
import com.reco1l.view.RoundLayout;

import java.util.Map;

import com.rimu.R;

public class MenuPreference extends Preference {

    private RoundLayout mButton;
    private TextView mSelectedText;

    private String mDefaultValue;
    private OnPreferenceChangeListener mListener;

    private Map<String, String> mEntries;

    private final SharedPreferences mPreferences;

    //--------------------------------------------------------------------------------------------//

    public MenuPreference(Context context) {
        this(context, null);
    }

    public MenuPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MenuPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MenuPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.custom_preference_menu);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
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

        mButton = (RoundLayout) holder.findViewById(R.id.pref_menu_button);
        mSelectedText = (TextView) holder.findViewById(R.id.pref_selected);

        TouchHandler.of(mButton, new TouchListener() {

            public void onPressUp() {
                if (mEntries == null || mEntries.isEmpty()) {
                    return;
                }

                ContextMenuBuilder builder = new ContextMenuBuilder(mButton);
                ContextMenu menu = new ContextMenu(builder);

                for (String key : mEntries.keySet()) {
                    builder.addItem(new ContextMenu.Item() {

                        public String getText() {
                            return key;
                        }

                        public void onClick(TextView view) {
                            setValue(key);
                        }
                    });
                }

                builder.setFixedWidth(mButton.getWidth());
                menu.show();
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

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

    public void setEntries(Map<String, String> entries) {
        mEntries = entries;
    }

    public void setText(String text) {
        if (mSelectedText != null) {
            mSelectedText.setText(text);
        }
    }

    public void setValue(String key) {
        if (mEntries == null || !mEntries.containsKey(key)) {
            return;
        }
        setText(key);

        if (hasKey()) {
            mPreferences.edit()
                    .putString(getKey(), mEntries.get(key))
                    .commit();
        }

        if (mListener != null) {
            mListener.onPreferenceChange(this, mEntries.get(key));
        }
    }

}
