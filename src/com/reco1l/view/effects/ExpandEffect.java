package com.reco1l.view.effects;
// Created by Reco1l on 18/11/2022, 23:01

import static android.graphics.Paint.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.reco1l.global.Game;
import com.reco1l.global.UI;
import com.reco1l.utils.Animation;
import com.reco1l.view.BaseView;
import com.reco1l.view.LogoView;

import java.util.ArrayList;
import java.util.Iterator;

public class ExpandEffect extends View implements BaseView {

    private LogoView logo;

    private float
            minRadius,
            strokeWidth;

    private int
            color,
            alpha;

    private ArrayList<Circle> circles;

    //--------------------------------------------------------------------------------------------//

    public ExpandEffect(Context context) {
        this(context, null);
    }

    public ExpandEffect(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        onCreate(attrs);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public View getView() {
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate(AttributeSet attrs) {
        circles = new ArrayList<>();

        strokeWidth = sdp(2);
        alpha = 80;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;

        if (logo != null) {
            minRadius = logo.getHeight() / 2f;

            centerX = (int) (logo.getX() + logo.getWidth() / 2);
            centerY = (int) (logo.getY() + logo.getHeight() / 2);
        }

        if (UI.background.isDark()) {
            setPaintColor(Color.WHITE);
        } else {
            setPaintColor(Color.BLACK);
        }

        if (Game.timingWrapper.isNextBeat()) {
            circles.add(new Circle());
        }

        Iterator<Circle> iterator = circles.iterator();
        while (iterator.hasNext()) {
            Circle circle = iterator.next();

            canvas.drawCircle(centerX, centerY, circle.radius, circle.paint);

            if (circle.paint.getAlpha() < 1 || circle.radius == getMaxRadius()) {
                iterator.remove();
            }
        }

        super.onDraw(canvas);
        invalidate();
    }

    //--------------------------------------------------------------------------------------------//

    public void attachToLogo(LogoView logo) {
        this.logo = logo;
    }

    private void setPaintColor(int color) {
        if (this.color == color) {
            return;
        }
        this.color = color;

        int i = 0;
        while (i < circles.size()) {
            circles.get(i).paint.setColor(color);
            ++i;
        }
    }

    //--------------------------------------------------------------------------------------------//

    private float getMaxRadius() {
        if (logo != null) {
            return logo.getHeight() * 0.75f;
        }

        if (getHeight() <= getWidth()) {
            return getHeight() / 2f;
        }
        return getWidth() / 2f;
    }

    private Paint createPaint() {
        Paint paint = new Paint();

        paint.setAlpha(alpha);
        paint.setColor(color);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
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
            radius = minRadius;

            long time = (long) Game.timingWrapper.getBeatLength();

            Animation.ofFloat(radius, getMaxRadius())
                    .runOnUpdate(value -> radius = (float) value)
                    .play(time * 3);

            Animation.ofInt(alpha, 0)
                    .runOnUpdate(value -> paint.setAlpha((int) value))
                    .play(time * 3);
        }
    }
}
