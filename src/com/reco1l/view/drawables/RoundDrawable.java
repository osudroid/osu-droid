package com.reco1l.view.drawables;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.math.MathUtils;

import com.reco1l.tables.ResourceTable;

public abstract class RoundDrawable extends Drawable implements ResourceTable {

    private final Context mContext;

    private Path mPath;

    private float mRadius;

    private boolean
            mIsMaxRounded,
            mIsInEditMode;

    //--------------------------------------------------------------------------------------------//

    public RoundDrawable(@NonNull Context context) {
        super();
        mContext = context;
        mIsMaxRounded = true;
    }

    public RoundDrawable(@NonNull Context context, float radius) {
        super();
        mContext = context;
        mRadius = radius;
    }

    @Override
    public Context context() {
        return mContext;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mPath != null && mRadius > 0) {
            canvas.clipPath(mPath);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (bounds != null) {
            int min = Math.min(bounds.height(), bounds.width());

            float radius = MathUtils.clamp(mRadius, 0, min / 2f);

            if (mIsMaxRounded) {
                radius = min / 2f;
            }

            mPath = new Path();
            mPath.addRoundRect(new RectF(0, 0, bounds.width(), bounds.height()), radius, radius, Path.Direction.CW);
        }
        super.onBoundsChange(bounds);
    }

    //--------------------------------------------------------------------------------------------//

    public final void setRadius(float radius) {
        mRadius = radius;
        onBoundsChange(getBounds());
    }

    public final void setMaxRounded(boolean bool) {
        mIsMaxRounded = bool;
        onBoundsChange(getBounds());
    }

    // Development usage only! use this if your drawable require context resources.
    public final void setEditMode(boolean bool) {
        mIsInEditMode = bool;
    }

    //--------------------------------------------------------------------------------------------//

    protected final boolean isIsInEditMode() {
        return mIsInEditMode;
    }
}
