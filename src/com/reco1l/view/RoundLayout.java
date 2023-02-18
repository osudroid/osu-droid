package com.reco1l.view;

// Written by Reco1l
// Corner radius implementation credits from here: https://stackoverflow.com/a/41098690

import static android.graphics.Path.*;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.core.math.MathUtils;

import com.reco1l.tables.ResourceTable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// CardView takes more time to render due to implementation so this is a good alternative i made
public class RoundLayout extends RelativeLayout implements ResourceTable {

    public static final int LINEAR = 0;
    public static final int RELATIVE = 1;

    // Namespaces
    protected final String
            appNS = "http://schemas.android.com/apk/res-auto",
            androidNS = "http://schemas.android.com/apk/res/android";

    // Initial values defined at creation of view, useful if you want to pass attrs to a child view
    protected final int
            defStyleAttr,
            defStyleRes;

    protected final AttributeSet attrs;

    protected Path mPath;

    private final ViewGroup mInternalLayout;
    private final int mLayoutType;

    private int mRadius = 12;

    private int
            mInitialWidth,
            mInitialHeight;

    private boolean
            mIsMaxRounded = false,
            mClampToSquare = false,
            mConstantInvalidate = false;

    //--------------------------------------------------------------------------------------------//

    @IntDef(value = {LINEAR, RELATIVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LayoutType {}

    //--------------------------------------------------------------------------------------------//

    public RoundLayout(@NonNull Context context) {
        this(context, null);
    }

    public RoundLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RoundLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        handleNativeAttributes(attrs);

        this.attrs = attrs;
        this.defStyleRes = defStyleRes;
        this.defStyleAttr = defStyleAttr;

        mLayoutType = getLayoutType();

        if (mLayoutType == LINEAR) {
            mInternalLayout = new LinearLayout(context, attrs, defStyleAttr, defStyleRes);
        } else {
            mInternalLayout = new RelativeLayout(context, attrs, defStyleAttr, defStyleRes);
        }
        super.addView(mInternalLayout, 0, getInitialLayoutParams());

        if (super.getBackground() != null) {
            super.setBackground(null);
        }
        super.setPadding(0, 0, 0, 0);

        onCreate();
        handleAttributes();

        super.post(() -> onLayoutChange(getLayoutParams()));
    }

    //--------------------------------------------------------------------------------------------//

    @SuppressLint("ResourceType")
    private void handleNativeAttributes(AttributeSet attrs) {
        int[] layoutAttrs = {
                android.R.attr.layout_width,
                android.R.attr.layout_height,
        };

        TypedArray a = getContext().obtainStyledAttributes(attrs, layoutAttrs);
        mInitialWidth = a.getLayoutDimension(0, WRAP_CONTENT);
        mInitialHeight = a.getLayoutDimension(1, WRAP_CONTENT);
        a.recycle();
    }

    // Return value defined in layout_width attribute
    protected final int getInitialWidth() {
        return mInitialWidth;
    }

    // Return value defined in layout_height attribute
    protected final int getInitialHeight() {
        return mInitialHeight;
    }

    protected final LayoutParams getInitialLayoutParams() {
        return new LayoutParams(mInitialWidth, mInitialHeight);
    }

    //--------------------------------------------------------------------------------------------//

    // Override and set this to handle attributes through a TypedArray in onManageAttributes()
    // You can pass a custom styleable resource or an array containing custom attributes but since
    // Android Studio sucks it'll not display properly on layout preview (it doesn't affect at runtime)
    protected @StyleableRes int[] getStyleable() {
        return null;
    }

    // Set the internal layout type: LinearLayout or RelativeLayout
    protected @LayoutType int getLayoutType() {
        if (attrs != null) {
            return attrs.getAttributeIntValue(appNS, "type", RELATIVE);
        }
        return RELATIVE;
    }

    //--------------------------------------------------------------------------------------------//

    protected void onCreate() {
    }

    // Override this instead of onDraw() method, don't make expensive operations here!
    protected void onManagedDraw(Canvas canvas) {
    }

    // It will be called if the view attributes are not null, if getStyleable() equals null then the
    // TypedArray parameter will be null.
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        // app:radius
        mRadius = sdp(a.getAttributeIntValue(appNS, "radius", mRadius));
    }

    // Called when layout params has been changed
    protected void onLayoutChange(ViewGroup.LayoutParams params) {
        matchSize(mInternalLayout);
    }

    // Override only to make special paths
    protected void onPathCreated(Path path, float radius, int width, int height) {
        path.addRoundRect(new RectF(0, 0, width, height), radius, radius, Direction.CW);
    }

    //--------------------------------------------------------------------------------------------//

    private void handleAttributes() {
        if (attrs == null) {
            return;
        }

        if (getStyleable() == null) {
            onManageAttributes(null, attrs);
            return;
        }
        TypedArray t = getContext().obtainStyledAttributes(attrs, getStyleable(), defStyleAttr, defStyleRes);
        onManageAttributes(t, attrs);
        t.recycle();
    }

    //--------------------------------------------------------------------------------------------//

    // Enabling this will make the view constantly call onDraw()
    public final void setConstantInvalidation(boolean bool) {
        mConstantInvalidate = bool;
        invalidate();
    }

    // Setting true will clamp width and height between them making the view to a square shape
    public final void setClampToSquare(boolean bool) {
        mClampToSquare = bool;
        invalidate();
    }

    public final void setMaxRounded(boolean bool) {
        mIsMaxRounded = bool;
        invalidate();
    }

    public final void setRadius(int radius) {
        mRadius = radius;
        invalidate();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        int size = Math.min(width, height);

        if (mClampToSquare) {
            width = size;
            height = size;

            getLayoutParams().width = size;
            getLayoutParams().height = size;
            requestLayout();
        }

        float radius = MathUtils.clamp(mRadius, 0, size / 2f);
        if (mIsMaxRounded) {
            radius = size / 2f;
        }

        mPath = new Path();
        onPathCreated(mPath, radius, width, height);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        onManagedDraw(canvas);

        if (mPath != null) {
            canvas.clipPath(mPath);
        }
        super.dispatchDraw(canvas);

        if (mConstantInvalidate) {
            invalidate();
        }
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        onLayoutChange(params);
    }

    // Match parent layout params useful when you want a child view match width and height from parent,
    // it's recommended to be used inside onResize() method.
    protected final void matchSize(View view) {
        if (view == null) {
            return;
        }
        view.getLayoutParams().width = getLayoutParams().width;
        view.getLayoutParams().height = getLayoutParams().height;
        view.requestLayout();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        mInternalLayout.addView(child, index, params);
    }

    @Override
    public void removeView(View view) {
        mInternalLayout.removeView(view);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void setBackground(Drawable background) {
        if (mInternalLayout != null) {
            mInternalLayout.setBackground(background);
        }
    }

    @Override
    public void setForeground(Drawable foreground) {
        if (mInternalLayout != null) {
            mInternalLayout.setForeground(foreground);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (mInternalLayout != null) {
            mInternalLayout.setVisibility(visibility);
        }
    }

    @Override
    public void setGravity(int gravity) {
        super.setGravity(gravity);

        if (mInternalLayout != null) {
            switch (mLayoutType) {
                case LINEAR:
                    ((LinearLayout) mInternalLayout).setGravity(gravity);
                    break;
                case RELATIVE:
                    ((RelativeLayout) mInternalLayout).setGravity(gravity);
                    break;
            }
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (mInternalLayout != null) {
            mInternalLayout.setPadding(left, top, right, bottom);
        }
    }

    @Override
    public boolean post(Runnable action) {
        if (mInternalLayout != null) {
            return mInternalLayout.post(action);
        }
        return super.post(action);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public Drawable getBackground() {
        return mInternalLayout.getBackground();
    }

    @Override
    public Drawable getForeground() {
        return mInternalLayout.getForeground();
    }

    //--------------------------------------------------------------------------------------------//

    public final ViewGroup getInternalLayout() {
        return mInternalLayout;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public Context context() {
        return getContext();
    }

    //--------------------------------------------------------------------------------------------//

    public final int sdp(int dp) {
        if (isInEditMode()) {
            return (int) (dp * resources().getDisplayMetrics().density);
        }
        return ResourceTable.super.sdp(dp);
    }

    public final int ssp(int sp) {
        if (isInEditMode()) {
            return (int) (sp * resources().getDisplayMetrics().density);
        }
        return ResourceTable.super.ssp(sp);
    }
}