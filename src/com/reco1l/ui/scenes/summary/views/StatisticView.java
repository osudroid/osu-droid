package com.reco1l.ui.scenes.summary.views;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.annotation.Size;
import com.reco1l.tools.Views;
import com.reco1l.view.BadgeTextView;
import com.reco1l.view.RoundLayout;

public final class StatisticView extends RoundLayout {

    private LinearLayout mLayout;
    private BadgeTextView mBadge;
    private TextView mText;

    //--------------------------------------------------------------------------------------------//

    public StatisticView(@NonNull Context context) {
        super(context);
    }

    public StatisticView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StatisticView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayoutType() {
        return LINEAR;
    }

    @Override
    protected void onCreate() {
        mLayout = (LinearLayout) getInternalLayout();
        mLayout.setOrientation(LinearLayout.VERTICAL);
        mLayout.setGravity(Gravity.CENTER);

        // Badge
        mBadge = new BadgeTextView(getContext());
        mBadge.getBackground().setTint(0x80000000);
        mBadge.setSize(Size.L);
        mBadge.getTextView().setAllCaps(true);
        addView(mBadge, new LayoutParams(getInitialWidth(), WRAP_CONTENT));

        mText = new TextView(getContext(), attrs);
        mText.setTextColor(Color.WHITE);
        addView(mText);

        Views.margins(mText).top(sdp(4));
    }

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        mBadge.setText(a.getAttributeValue(appNS, "badgeText"));
        mBadge.setTextColor(a.getAttributeIntValue(appNS, "badgeTextColor", Color.WHITE));
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onManagedDraw(Canvas canvas) {
        mBadge.getLayoutParams().width = getLayoutParams().width;
    }

    //--------------------------------------------------------------------------------------------//

    public void setText(CharSequence charSequence) {
        mText.setText(charSequence);
    }
}
