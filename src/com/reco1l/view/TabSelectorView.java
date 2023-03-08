package com.reco1l.view;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.tools.Views;
import com.reco1l.framework.drawing.Dimension;

public class TabSelectorView extends RoundLayout {

    private View mIndicator;
    private LinearLayout mLayout;

    private TextView[] mTabs;

    //--------------------------------------------------------------------------------------------//

    public TabSelectorView(@NonNull Context context) {
        super(context);
    }

    public TabSelectorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TabSelectorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setRadius(0);

        mLayout = new LinearLayout(getContext());
        mLayout.setGravity(Gravity.CENTER);
        addView(mLayout, getInitialLayoutParams());

        mIndicator = new View(getContext());
        mIndicator.setBackground(new ColorDrawable(Color.WHITE));
        addView(mIndicator);
        Views.height(mIndicator, sdp(2));
    }

    @Override
    protected void onDimensionChange(Dimension dimens) {
        super.onDimensionChange(dimens);
        matchSize(mLayout);

        mLayout.post(() -> {
            if (mTabs != null && mTabs.length > 0) {
                int w = getWidth() / mTabs.length;

                for (TextView tab : mTabs) {
                    tab.post(() -> Views.size(tab, w, dimens.height));
                }

                mIndicator.setY(getHeight() - mIndicator.getHeight());
                Views.width(mIndicator, w);
            }
        });
    }

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {

        String tabs = a.getAttributeValue(appNS, "tabs");
        String[] names = tabs.split("\\|");

        mTabs = new TextView[names.length];

        int i = 0;
        while (i < names.length) {
            TextView text = Views.styledText(this, null);

            text.setTextSize(COMPLEX_UNIT_PX, sdp(12));
            text.setGravity(Gravity.CENTER);
            text.setText(names[i]);

            mLayout.addView(text);
            mTabs[i] = text;
            i++;
        }
    }
}
