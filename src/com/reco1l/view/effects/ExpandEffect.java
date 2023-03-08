package com.reco1l.view.effects;
// Created by Reco1l on 18/11/2022, 23:01

import static android.graphics.Paint.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.management.Settings;
import com.reco1l.framework.Animation;
import com.reco1l.view.RoundLayout;

import java.util.ArrayList;
import java.util.Iterator;

public class ExpandEffect extends RoundLayout {

    private View mAttachedView;

    private float
            mMinRadius,
            mStrokeWidth;

    private int mAlpha;

    private ArrayList<Circle> mCircles;

    //--------------------------------------------------------------------------------------------//

    public ExpandEffect(@NonNull Context context) {
        super(context);
    }

    public ExpandEffect(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExpandEffect(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setConstantInvalidation(true);

        mCircles = new ArrayList<>();

        mStrokeWidth = sdp(3);
        mAlpha = 100;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onManagedDraw(Canvas canvas) {
        if (isInEditMode() || !Settings.<Boolean>get("menusEffects", true)) {
            return;
        }

        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;

        if (mAttachedView != null) {
            mMinRadius = mAttachedView.getHeight() / 2f;

            centerX = (int) (mAttachedView.getX() + mAttachedView.getWidth() / 2);
            centerY = (int) (mAttachedView.getY() + mAttachedView.getHeight() / 2);
        }

        if (Game.timingWrapper.isNextBeat()) {
            mCircles.add(new Circle());
        }

        Iterator<Circle> iterator = mCircles.iterator();
        while (iterator.hasNext()) {
            Circle circle = iterator.next();

            canvas.drawCircle(centerX, centerY, circle.radius, circle.paint);

            if (circle.paint.getAlpha() < 1 || circle.radius == getMaxRadius()) {
                iterator.remove();
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void attachTo(View logo) {
        this.mAttachedView = logo;
    }

    //--------------------------------------------------------------------------------------------//

    private float getMaxRadius() {
        if (mAttachedView != null) {
            return mAttachedView.getHeight() * 0.75f;
        }

        if (getHeight() <= getWidth()) {
            return getHeight() / 2f;
        }
        return getWidth() / 2f;
    }

    private Paint createPaint() {
        Paint paint = new Paint();

        paint.setAlpha(mAlpha);
        paint.setColor(Color.WHITE);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(mStrokeWidth);
        paint.setAntiAlias(true);

        return paint;
    }

    //--------------------------------------------------------------------------------------------//

    private class Circle {

        private final Paint paint;

        private float radius;

        //----------------------------------------------------------------------------------------//

        private Circle() {
            paint = createPaint();
            radius = mMinRadius;

            long time = (long) Game.timingWrapper.getBeatLength();

            Animation.ofFloat(radius, getMaxRadius())
                    .runOnUpdate(value -> radius = (float) value)
                    .play(time * 3);

            Animation.ofInt(mAlpha, 0)
                    .runOnUpdate(value -> paint.setAlpha((int) value))
                    .play(time * 3);
        }
    }
}
