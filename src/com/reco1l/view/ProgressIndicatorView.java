package com.reco1l.view;

import static com.google.android.material.progressindicator.CircularProgressIndicator.*;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.framework.Maths;
import com.reco1l.framework.drawing.Dimension;

public class ProgressIndicatorView extends RoundLayout {

    private CircularProgressIndicator mIndicator;

    //--------------------------------------------------------------------------------------------//

    public ProgressIndicatorView(@NonNull Context context) {
        super(context);
    }

    public ProgressIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        mIndicator = new CircularProgressIndicator(getContext(), null, defStyleAttr);

        addView(mIndicator, getInitialLayoutParams());

        mIndicator.setIndicatorInset(0);
        mIndicator.setIndeterminate(true);
        mIndicator.setIndicatorDirection(INDICATOR_DIRECTION_COUNTERCLOCKWISE);

        setGravity(Gravity.CENTER);
        setClampToSquare(true);
        setRadius(0);
    }

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        mIndicator.setIndicatorColor(a.getAttributeIntValue(appNS, "color", 0xFF819DD4));
    }

    @Override
    protected void onDimensionChange(Dimension dimens) {
        super.onDimensionChange(dimens);
        matchSize(mIndicator);

        mIndicator.post(() -> {
            int size = Maths.pct(Math.min(getWidth(), getHeight()), 80);

            mIndicator.setIndicatorSize(size);

            int thick = Maths.pct(size, 12);

            mIndicator.setTrackThickness(thick);
            mIndicator.setTrackCornerRadius(Maths.half(thick));
        });
    }

    //--------------------------------------------------------------------------------------------//

    public void setMax(int max) {
        mIndicator.setMax(max);
    }

    public void setProgress(int progress) {
        mIndicator.setProgress(progress);
    }

    public void setIndeterminate(boolean indeterminate) {
        mIndicator.setIndeterminate(indeterminate);
    }
}
