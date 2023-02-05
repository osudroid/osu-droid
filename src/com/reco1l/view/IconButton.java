package com.reco1l.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
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

import com.reco1l.interfaces.fields.Identifiers;
import com.reco1l.utils.Animation;
import com.reco1l.utils.TouchHandler;
import com.reco1l.utils.Views;

import ru.nsu.ccfit.zuev.osuplus.R;

public class IconButton extends RelativeLayout implements BaseView {

    private View indicator;
    private ImageView icon;

    private LinearLayout widget;

    private boolean
            isActivated = false;

     //--------------------------------------------------------------------------------------------//

    public enum IndicatorAnchor {
        BOTTOM,
        TOP
    }

    //--------------------------------------------------------------------------------------------//

    public IconButton(Context context) {
        this(context, null);
    }

    public IconButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        onCreate(attrs);
    }

    public IconButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onCreate(attrs);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public View getView() {
        return this;
    }

    @Override
    public int[] getStyleable() {
        return R.styleable.IconButton;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    @SuppressLint("ResourceType")
    public void onCreate(AttributeSet attrs) {
        icon = new ImageView(getContext());
        icon.setId(Identifiers.IconButton_Icon);
        icon.setScaleType(ScaleType.CENTER);
        addView(icon);

        indicator = new View(getContext());
        indicator.setBackground(new ColorDrawable(Color.WHITE));
        indicator.setAlpha(0);
        addView(indicator);

        widget = new LinearLayout(getContext());
        widget.setGravity(Gravity.CENTER);
        widget.setId(Identifiers.IconButton_Widget);
        addView(widget);

        Views.rule(widget)
                .add(RIGHT_OF)
                .apply(icon.getId());

        Views.rule(indicator)
                .add(ALIGN_END, widget.getId())
                .apply();

        indicator.getLayoutParams().height = sdp(2);

        int w = sdp(62);
        int h = sdp(40);

        Views.size(icon, w, h);
        Views.height(widget, h);

        handleAttributes(attrs);
    }

    @Override
    public void onManageAttributes(TypedArray a) {
        Drawable drw = a.getDrawable(R.styleable.IconButton_iconSrc);
        icon.setImageDrawable(drw);

        int index = a.getInt(R.styleable.IconButton_indicatorPosition, 0);
        IndicatorAnchor anchor = IndicatorAnchor.values()[index];

        Views.RuleUtils rule = Views.rule(indicator);
        if (anchor == IndicatorAnchor.TOP) {
            rule.add(ALIGN_TOP);
        } else {
            rule.add(ALIGN_BOTTOM);
        }
        rule.apply(icon.getId());

        boolean bool = a.getBoolean(R.styleable.IconButton_activated, false);
        setActivated(bool);
    }

    //--------------------------------------------------------------------------------------------//

    public void setActivated(boolean bool) {
        if (bool == isActivated) {
            return;
        }

        if (isInEditMode()) {
            indicator.setAlpha(bool ? 1 : 0);
            isActivated = bool;
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

    public void setTouchListener(Runnable task) {
        new TouchHandler(task).apply(this);
    }

    //--------------------------------------------------------------------------------------------//

    public boolean isActivated() {
        return isActivated;
    }

    public LinearLayout getWidget() {
        return widget;
    }
}
