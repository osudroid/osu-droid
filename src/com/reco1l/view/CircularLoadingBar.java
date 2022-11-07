package com.reco1l.view;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.utils.Resources;
import com.reco1l.utils.ViewUtils;

import ru.nsu.ccfit.zuev.osuplus.R;

public class CircularLoadingBar extends LinearLayout {

    private CircularProgressIndicator indicator;

    //--------------------------------------------------------------------------------------------//

    public CircularLoadingBar(Context context) {
        super(context);
        create(context);
    }

    public CircularLoadingBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        create(context);
    }

    public CircularLoadingBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create(context);
    }

    //--------------------------------------------------------------------------------------------//

    private void create(Context context) {
        setGravity(Gravity.CENTER);

        LinearLayout layout = new LinearLayout(context);

        // Background
        Drawable background = Resources.drw(R.drawable.round_shape).mutate();
        background.setTint(Resources.color(R.color.circularLoadingBarBackground));
        layout.setBackground(background);

        // Indicator
        indicator = new CircularProgressIndicator(context);

        int size = (int) Resources.dimen(R.dimen.circularLoadingBarSize);
        LinearLayout.LayoutParams indicatorParams = new LayoutParams(size, size);
        indicator.setLayoutParams(indicatorParams);

        indicator.setIndicatorColor(Resources.color(R.color.circularLoadingBarIndicator));
        indicator.setIndeterminate(true);

        int xs = (int) Resources.dimen(R.dimen.XS);
        ViewUtils.margins(indicator).all(xs);

        int cornerRadius = (int) Resources.dimen(R.dimen.circularLoadingBarTrackCornerRadius);
        indicator.setTrackCornerRadius(cornerRadius);

        layout.addView(indicator);
        addView(layout, new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
    }
}
