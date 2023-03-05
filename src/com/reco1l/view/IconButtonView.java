package com.reco1l.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.utils.TouchHandler;
import com.reco1l.utils.Views;

public class IconButtonView extends RoundLayout {

    private ImageView mIcon;
    private LinearLayout mWidget;

    //--------------------------------------------------------------------------------------------//

    public IconButtonView(@NonNull Context context) {
        super(context);
    }

    public IconButtonView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public IconButtonView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayoutType() {
        return LINEAR;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate() {
        mIcon = new ImageView(getContext(), attrs);
        mIcon.setScaleType(ScaleType.CENTER);
        addView(mIcon);

        mWidget = new LinearLayout(getContext());
        mWidget.setGravity(Gravity.CENTER);
        addView(mWidget);

        int w = sdp(62);
        int h = sdp(40);

        Views.size(mIcon, w, h);
        Views.height(mWidget, h);
    }

    //--------------------------------------------------------------------------------------------//

    public void setIcon(Drawable drawable) {
        mIcon.setImageDrawable(drawable);
    }

    public void setIcon(@DrawableRes int resource) {
        mIcon.setImageResource(resource);
    }

    public void setTouchListener(Runnable task) {
        new TouchHandler(task).apply(this);
    }

    //--------------------------------------------------------------------------------------------//

    public LinearLayout getWidget() {
        return mWidget;
    }
}
