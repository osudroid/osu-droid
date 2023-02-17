package com.reco1l.view;

import static com.google.android.material.progressindicator.CircularProgressIndicator.*;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.utils.Maths;

public class ProgressIndicator extends RoundLayout {

    private CircularProgressIndicator mIndicator;

    //--------------------------------------------------------------------------------------------//

    public ProgressIndicator(@NonNull Context context) {
        super(context);
    }

    public ProgressIndicator(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressIndicator(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        mIndicator = new CircularProgressIndicator(getContext(), null, defStyleAttr);

        addView(mIndicator, getInitialLayoutParams());

        mIndicator.setIndicatorInset(0);
        mIndicator.setIndeterminate(true);
        mIndicator.setIndicatorColor(0xFF819DD4);
        mIndicator.setIndicatorDirection(INDICATOR_DIRECTION_COUNTERCLOCKWISE);

        setGravity(Gravity.CENTER);
        setClampToSquare(true);
    }

    @Override
    protected void onLayoutChange(ViewGroup.LayoutParams params) {
        super.onLayoutChange(params);
        matchSize(mIndicator);

        mIndicator.post(() -> {
            int size = Maths.pct(Math.min(mIndicator.getWidth(), mIndicator.getHeight()), 80);

            setRadius(Maths.pct(size, 15));
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
