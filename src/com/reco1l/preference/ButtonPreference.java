package com.reco1l.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.reco1l.tables.Res;
import com.reco1l.view.ButtonView;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ButtonPreference extends Preference {

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
        ButtonView view = holder.itemView.findViewById(R.id.pref_button);

        if (view != null) {
            view.getTextView().setId(android.R.id.title);

            TypedValue out = Res.attr(android.R.attr.selectableItemBackground, true);
            view.setForeground(Res.drw(out.resourceId));
        }
        super.onBindViewHolder(holder);
    }
}
