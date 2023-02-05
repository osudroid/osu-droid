package com.reco1l.view.custom;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.view.BadgeTextView;

public final class ModBadge extends BadgeTextView {

    //--------------------------------------------------------------------------------------------//

    public ModBadge(@NonNull Context context) {
        super(context);
    }

    public ModBadge(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ModBadge(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        super.onCreate();

        setSize(XL);
        setBackground(new ColorDrawable(0xFF7CB1E5));
        getTextView().setTextColor(Color.BLACK);
        getTextView().setAlpha(0.75f);
    }
}
