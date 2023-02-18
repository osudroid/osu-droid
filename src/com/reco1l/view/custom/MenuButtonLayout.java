package com.reco1l.view.custom;

import static androidx.appcompat.widget.LinearLayoutCompat.VERTICAL;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.utils.Views;
import com.reco1l.view.RoundLayout;

public class MenuButtonLayout extends RoundLayout {

    //--------------------------------------------------------------------------------------------//

    public MenuButtonLayout(@NonNull Context context) {
        super(context);
    }

    public MenuButtonLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MenuButtonLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayoutType() {
        return LINEAR;
    }

    //--------------------------------------------------------------------------------------------//
    @Override
    protected void onCreate() {
        setBackground(new ColorDrawable(0xFF242E40));
        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);
        setRadius(0);

        ImageView image = new ImageView(getContext(), attrs);
        addView(image);

        Views.margins(image).bottom(sdp(6));

        TextView text = new TextView(getContext(), attrs);
        Views.styleText(text, this);
        addView(text);
    }

    @Override
    protected void onLayoutChange(ViewGroup.LayoutParams params) {
        Views.size(getInternalLayout(), sdp(120), sdp(90));
    }
}
