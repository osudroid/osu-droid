package com.reco1l.ui.scenes.selector.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.framework.math.FMath;

public class CarrouselRecyclerView extends RecyclerView {

    private View mWindow;

    private int mYOffset = 0;

    //--------------------------------------------------------------------------------------------//

    public CarrouselRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public CarrouselRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarrouselRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        updateTranslations();
        invalidate();
    }

    private void updateTranslations() {
        int i = 0;
        while (i < getChildCount()) {
            View child = getChildAt(i);

            if (child != null) {
                child.setTranslationX(computeTranslationX(child));
            }
            ++i;
        }
    }

    public float computeTranslationX(View view) {
        int w =  view.getWidth();
        int h = view.getHeight();

        int[] pos = new int[2];
        view.getLocationInWindow(pos);
        pos[1] += mYOffset;

        float diff = getWindowHeight() - h;
        float dx = 1 - Math.abs(pos[1] - diff) / Math.abs(diff + h / 0.01f);

        return w - w * FMath.clamp(dx, 0f, 1f);
    }

    private int getWindowHeight() {
        if (mWindow != null) {
            return mWindow.getHeight();
        }
        return getHeight();
    }

    public void setParentWindow(View window) {
        this.mWindow = window;
    }

    public void setYOffset(int yOffset) {
        this.mYOffset = yOffset;
    }
}
