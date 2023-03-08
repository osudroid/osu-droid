package com.reco1l.ui.elements;

import static androidx.appcompat.widget.LinearLayoutCompat.VERTICAL;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.tools.Views;
import com.reco1l.framework.drawing.Dimension;
import com.reco1l.view.RoundLayout;

public class MenuButtonVIew extends RoundLayout {

    //--------------------------------------------------------------------------------------------//

    public MenuButtonVIew(@NonNull Context context) {
        super(context);
    }

    public MenuButtonVIew(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MenuButtonVIew(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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

        TextView text = Views.styledText(this, attrs);
        addView(text);
    }

    @Override
    protected void onDimensionChange(Dimension dimens) {
        dimens.width = sdp(120);
        dimens.height = sdp(90);

        super.onDimensionChange(dimens);
    }
}
