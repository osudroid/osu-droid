package com.reco1l.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.annotation.StyleableRes;
import androidx.cardview.widget.CardView;

import com.reco1l.tables.Res;

import ru.nsu.ccfit.zuev.osuplus.R;

public abstract class BaseRoundedView extends CardView {

    private boolean
            isCircle = false,
            isClampToSquare = false,
            isConstantInvalidate = false;

    private float radius;

    //--------------------------------------------------------------------------------------------//

    public BaseRoundedView(@NonNull Context context) {
        this(context, null);
    }

    public BaseRoundedView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseRoundedView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onCreate();
        handleAttributes(attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    protected @StyleableRes int[] getStyleable() {
        return R.styleable.BaseRoundedView;
    }

    protected @StyleRes int getDefaultStyle() {
        return 0;
    }

    //--------------------------------------------------------------------------------------------//

    protected abstract void onCreate();

    protected void onUpdate(Canvas canvas) {
    }

    protected void onManageAttributes(TypedArray a) {
    }

    //--------------------------------------------------------------------------------------------//

    private void handleBaseAttributes(AttributeSet attrs, int defStyleAttr) {
        if (defStyleAttr == 0 && getDefaultStyle() == 0) {
            return;
        }

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BaseRoundedView, defStyleAttr, getDefaultStyle());

        isCircle = a.getBoolean(R.styleable.BaseRoundedView_maxRounded, false);
        isClampToSquare = a.getBoolean(R.styleable.BaseRoundedView_clampToSquare, false);

        a.recycle();
    }

    private void handleAttributes(AttributeSet attrs, int defStyleAttr) {
        //handleBaseAttributes(attrs, defStyleAttr);

        TypedArray a = getContext().obtainStyledAttributes(attrs, getStyleable(), defStyleAttr, getDefaultStyle());
        onManageAttributes(a);
        a.recycle();
    }

    //--------------------------------------------------------------------------------------------//

    public final void setConstantInvalidation(boolean bool) {
        isConstantInvalidate = bool;
        invalidate();
    }

    public final void setClampToSquare(boolean bool) {
        isClampToSquare = bool;
        invalidate();
    }

    public final void setCircleShape(boolean bool) {
        isCircle = bool;
        invalidate();
    }

    @Override
    public void setRadius(float radius) {
        this.radius = radius;
        super.setRadius(radius);
    }

    //--------------------------------------------------------------------------------------------//

    private void fixRadius() {
        if (isCircle) {
            setRadius(Math.min(getWidth(), getHeight()) / 2f);
        }
        if (radius <= 0) {
            return;
        }
        // Fix for old APIs where radius can break shape if radius is grater than it's size
        if (getWidth() <= getHeight()) {
            super.setRadius(Math.min(radius, getWidth() / 2f));
        } else {
            super.setRadius(Math.min(radius, getHeight() / 2f));
        }
    }

    private void fixSize() {
        int size = Math.max(getWidth(), getHeight());

        getLayoutParams().width = size;
        getLayoutParams().height = size;

        requestLayout();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        fixRadius();
        if (isClampToSquare) {
            fixSize();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        fixRadius();
        if (isClampToSquare) {
            fixSize();
        }
        super.onDraw(canvas);

        if (isConstantInvalidate) {
            onUpdate(canvas);
            invalidate();
        }
    }

    //--------------------------------------------------------------------------------------------//

    protected final int sdp(int dp) {
        if (isInEditMode()) {
            return (int) (dp * getContext().getResources().getDisplayMetrics().density);
        }
        return Res.sdp(dp);
    }

    protected final int ssp(int sp) {
        if (isInEditMode()) {
            return (int) (sp * getContext().getResources().getDisplayMetrics().density);
        }
        return Res.ssp(sp);
    }
}
