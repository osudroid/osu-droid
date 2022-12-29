package com.reco1l.view;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.framework.math.FMath;

public class CarrouselRecyclerView extends RecyclerView {

    //--------------------------------------------------------------------------------------------//

    public CarrouselRecyclerView(@NonNull Context context) {
        super(context);
    }

    public CarrouselRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CarrouselRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onDraw(Canvas c) {
        updateTranslations();
        super.onDraw(c);
        invalidate();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
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
        int oy = (int) ((getHeight() - view.getHeight()) * 1f / 2);

        float fx = 1 - Math.abs(view.getY() - oy) / Math.abs(oy + view.getHeight() / 0.025f);
        float val = view.getWidth() - view.getWidth() * FMath.clamp(fx, 0f, 1f);

        return FMath.clamp(val, 0, view.getWidth());
    }
}
