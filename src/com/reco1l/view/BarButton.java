package com.reco1l.view;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;

import ru.nsu.ccfit.zuev.osuplus.R;

public final class BarButton extends RelativeLayout {

    private View indicator;
    private ImageView icon;

    private boolean
            isToggle = false,
            isActivated = false;

    private Runnable onTouchListener;

    //--------------------------------------------------------------------------------------------//

    public BarButton(Context context) {
        super(context);
        create(context);
    }

    public BarButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        create(context);
    }

    private void create(Context context) {
        setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

        int width = (int) Resources.dimen(R.dimen.topBarButtonWidth);
        int height = (int) Resources.dimen(R.dimen.topBarButtonHeight);

        // Icon
        icon = new ImageView(context);

        icon.setLayoutParams(new LayoutParams(width, height));
        icon.setScaleType(ScaleType.CENTER);
        addView(icon);

        // Indicator
        indicator = new View(context);

        indicator.setLayoutParams(new LayoutParams(width, (int) Resources.sdp(2)) {{
            addRule(ALIGN_PARENT_BOTTOM);
        }});
        indicator.setBackground(new ColorDrawable(Color.WHITE));
        indicator.setAlpha(0f);
        addView(indicator);

        onTouchListener = () -> {
            if (indicator != null) {
                setActivated(!isActivated);
            }
        };
    }

    //--------------------------------------------------------------------------------------------//

    public void setActivated(boolean bool) {
        if (!isToggle) {
            return;
        }

        if (bool != isActivated) {
            isActivated = bool;

            Animation anim = new Animation(indicator);

            if (bool) {
                anim.fade(0, 1);
            } else {
                anim.fade(1, 0);
            }
            anim.play(200);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void setIcon(Drawable drawable) {
        if (icon != null) {
            icon.setImageDrawable(drawable);
        }
    }

    public void setAsToggle(boolean bool) {
        isToggle = bool;
    }

    public void runOnTouch(Runnable task) {
        onTouchListener = () -> {
            if (indicator != null) {
                setActivated(!isActivated);
            }
            if (task != null) {
                task.run();
            }
        };
    }

    public Runnable getTouchListener() {
        return onTouchListener;
    }

    public boolean isActivated() {
        return isActivated;
    }
}
