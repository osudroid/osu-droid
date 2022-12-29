package com.reco1l.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.reco1l.utils.Animation;
import com.reco1l.utils.ViewUtils;

public class IconButton extends RelativeLayout implements BaseView {

    private View indicator;
    private ImageView icon;

    private LinearLayout widget;

    private boolean
            isActivated = false;

    private Runnable onTouchListener;

    //--------------------------------------------------------------------------------------------//

    public IconButton(Context context) {
        this(context, null);
    }

    public IconButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        onCreate(attrs);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public View getView() {
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    @SuppressLint("ResourceType")
    public void onCreate(AttributeSet attrs) {
        icon = new ImageView(getContext());
        icon.setId(0x11);
        icon.setScaleType(ScaleType.CENTER);
        addView(icon);

        indicator = new View(getContext());
        indicator.setBackground(new ColorDrawable(Color.WHITE));
        indicator.setAlpha(0);
        addView(indicator);

        widget = new LinearLayout(getContext());
        widget.setGravity(Gravity.CENTER);
        widget.setId(0x12);
        addView(widget);

        ViewUtils.rule(widget)
                .add(RIGHT_OF)
                .apply(icon.getId());

        ViewUtils.rule(indicator)
                .add(ALIGN_BOTTOM, icon.getId())
                .add(ALIGN_END, widget.getId())
                .apply();

        indicator.getLayoutParams().height = sdp(2);

        int w = sdp(62);
        int h = sdp(40);

        ViewUtils.size(icon, w, h);
        ViewUtils.height(widget, h);
    }

    //--------------------------------------------------------------------------------------------//

    public void setActivated(boolean bool) {
        if (bool == isActivated) {
            return;
        }

        Animation.of(indicator)
                .toAlpha(bool ? 1 : 0)
                .runOnEnd(() -> isActivated = bool)
                .play(200);
    }

    //--------------------------------------------------------------------------------------------//

    public void setIcon(Drawable drawable) {
        icon.setImageDrawable(drawable);
    }

    public void setIcon(@DrawableRes int resource) {
        icon.setImageResource(resource);
    }

    public void runOnTouch(Runnable task) {
        onTouchListener = task;
    }

    //--------------------------------------------------------------------------------------------//

    public Runnable getTouchListener() {
        return onTouchListener;
    }

    public boolean isActivated() {
        return isActivated;
    }

    public LinearLayout getWidget() {
        return widget;
    }
}
