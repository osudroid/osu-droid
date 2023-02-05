package com.reco1l.view.drawables;
// Created by Reco1l on 06/12/2022, 14:28

import static android.graphics.PorterDuff.*;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import ru.nsu.ccfit.zuev.osu.Config;

public class StripsDrawable extends Drawable {

    public int
            spawnTime = 200,
            limit = 60;

    public float
            speed = 9f,
            stripWidth = 80;

    public int color = 0xFF292929;

    private final LinkedList<Strip> strips;
    private final Random random;

    private int width, height, spawnClock;
    private long time = -1;

    //--------------------------------------------------------------------------------------------//

    public StripsDrawable() {
        strips = new LinkedList<>();
        random = new Random();
    }

    //--------------------------------------------------------------------------------------------//

    public void reset() {
        time = -1;
        strips.clear();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }


    //--------------------------------------------------------------------------------------------//

    @Override
    public void draw(@NonNull Canvas canvas) {
        width = getBounds().width();
        height = getBounds().height();

        if (time == -1) {
            time = System.currentTimeMillis();
            int i = 0;
            while (i < 200) {
                update(40);
                ++i;
            }
            return;
        }

        int dt = (int) (System.currentTimeMillis() - time);
        time += dt;
        update(dt * 2);

        float radius = stripWidth / 2;
        for (Strip strip : strips) {
            canvas.save();
            canvas.rotate(30);
            canvas.drawRoundRect(strip.rectF, radius, radius, strip.backPaint);
            canvas.drawRoundRect(strip.rectF, radius, radius, strip.paint);
            canvas.restore();
        }
    }

    //--------------------------------------------------------------------------------------------//

    private int nextAlpha() {
        // TODO [StripsDrawable] Do maxAlpha & minAlpha customizable
        int max = 120;
        int min = 40;

        return random.nextInt(max - min) + min;
    }

    private PointF nextPosition() {
        return new PointF(width, random.nextInt(width + height + height / 3) - width);
    }

    private float nextLength() {
        return random.nextInt((int) (width - stripWidth)) + stripWidth;
    }

    //--------------------------------------------------------------------------------------------//

    private Paint getPaint() {
        Paint paint = new Paint();
        paint.setDither(Config.isUseDither());
        return paint;
    }

    //--------------------------------------------------------------------------------------------//

    private void spawnNewStrips(int dt) {
        spawnClock += dt;

        while (spawnClock > spawnTime && strips.size() < limit) {
            float length = nextLength();

            if (length < stripWidth) {
                return;
            }
            strips.add(new Strip(length));
            spawnClock -= spawnTime;
        }
    }

    private void update(int dt) {
        spawnNewStrips(dt);

        Iterator<Strip> iterator = strips.iterator();
        while (iterator.hasNext()) {
            Strip strip = iterator.next();

            strip.update(dt);
            if (strip.center.x + strip.length / 2 < 0) {
                iterator.remove();
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    private class Strip {

        private final Paint paint, backPaint;
        private final RectF rectF;
        private final PointF center;

        private final float length;

        //----------------------------------------------------------------------------------------//

        private Strip(float length) {
            this.length = length;

            rectF = new RectF();

            paint = getPaint();
            paint.setColor(color);
            paint.setAlpha(nextAlpha());

            backPaint = getPaint();
            backPaint.setColor(Color.TRANSPARENT);
            backPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));

            center = nextPosition();

            rectF.top = center.y + stripWidth / 2;
            rectF.bottom = center.y - stripWidth / 2;

            center.x += length;
        }

        //----------------------------------------------------------------------------------------//

        private void update(int dt) {
            center.x -= dt * (speed * 20) / 1000;

            rectF.left = center.x - length / 2;
            rectF.right = center.x + length / 2;
        }
    }
}
