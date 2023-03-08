package com.reco1l.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.reco1l.management.resources.ResourceTable;
import com.reco1l.view.ButtonView;

import com.rimu.R;

public class ButtonPreference extends Preference implements ResourceTable {

    public ButtonPreference(Context context) {
        this(context, null);
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.custom_preference_button);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        if (!(holder.itemView.findViewById(R.id.pref_button) instanceof ButtonView)) {
            return;
        }

        ButtonView view = holder.itemView.findViewById(R.id.pref_button);

        if (view != null) {
            view.getTextView().setId(android.R.id.title);

            TypedValue out = attr(android.R.attr.selectableItemBackground, true);
            view.setForeground(drw(out.resourceId));
        }
        super.onBindViewHolder(holder);
    }
}
