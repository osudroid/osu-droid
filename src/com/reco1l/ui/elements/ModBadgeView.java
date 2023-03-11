package com.reco1l.ui.elements;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.annotation.Size;
import com.reco1l.view.BadgeTextView;

public final class ModBadgeView extends BadgeTextView {

    //--------------------------------------------------------------------------------------------//

    public ModBadgeView(@NonNull Context context) {
        super(context);
    }

    public ModBadgeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ModBadgeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        super.onCreate();

        setSize(Size.XL);
        setBackground(new ColorDrawable(0xFF7CB1E5));

        TextView text = getTextView();

        text.setTextColor(Color.BLACK);
        text.setAllCaps(true);
        text.setAlpha(0.75f);
    }
}
