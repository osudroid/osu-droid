package com.reco1l.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
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
import com.reco1l.utils.Res;
import com.reco1l.utils.ViewUtils;

public final class BarButton extends RelativeLayout {

    private View indicator;
    private ImageView icon;

    private LinearLayout widget;

    private boolean
            isActivated = false;

    private Runnable onTouchListener;

    //--------------------------------------------------------------------------------------------//

    public BarButton(Context context) {
        super(context);
        init(context);
    }

    public BarButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @SuppressLint("ResourceType")
    private void init(Context context) {
        icon = new ImageView(context);
        icon.setId(0x11);
        icon.setScaleType(ScaleType.CENTER);
        addView(icon);

        // Indicator
        indicator = new View(context);
        indicator.setBackground(new ColorDrawable(Color.WHITE));
        indicator.setAlpha(0);
        addView(indicator);

        widget = new LinearLayout(context);
        widget.setGravity(Gravity.CENTER);
        widget.setId(0x12);
        addView(widget);

        ViewUtils.rule(widget)
                .add(RIGHT_OF, icon.getId())
                .apply();

        ViewUtils.rule(indicator)
                .add(ALIGN_BOTTOM, icon.getId())
                .add(ALIGN_END, widget.getId())
                .apply();

        indicator.getLayoutParams().height = 4;

        int w = 62 * 3;
        int h = 40 * 3;

        if (!isInEditMode()) {
            w = Res.sdp(62);
            h = Res.sdp(40);

            indicator.getLayoutParams().height = Res.sdp(2);
        }
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
