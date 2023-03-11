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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.management.Settings;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class StripsDrawable extends Drawable {

    private int
            mColor,
            mWidth,
            mHeight,
            mSpawnTime,
            mSpawnClock,
            mSpawnLimit;

    private float
            mSpeed,
            mStripWidth;

    private final LinkedList<Strip> mStrips;
    private final Random mRandomizer;

    private long mTime = -1;

    //--------------------------------------------------------------------------------------------//

    public StripsDrawable() {
        mStrips = new LinkedList<>();
        mRandomizer = new Random();

        mColor = 0xFF292929;
        mSpeed = 9f;

        mStripWidth = 80;
        mSpawnTime = 200;
        mSpawnLimit = 60;
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

    public void reset() {
        mTime = -1;
        mStrips.clear();
    }

    public void setStripColor(@ColorInt int color) {
        mColor = color;
    }

    public void setStripWidth(float width) {
        mStripWidth = width;
    }

    public void setStripSpeed(float speed) {
        mSpeed = speed;
    }

    public void setSpawnTime(int spawnTime) {
        mSpawnTime = spawnTime;
    }

    public void setSpawnLimit(int spawnLimit) {
        mSpawnLimit = spawnLimit;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (!Settings.<Boolean>get("menusEffects", true)) {
            return;
        }

        mWidth = getBounds().width();
        mHeight = getBounds().height();

        if (mTime == -1) {
            mTime = System.currentTimeMillis();
            int i = 0;
            while (i < 200) {
                update(40);
                ++i;
            }
            return;
        }

        int dt = (int) (System.currentTimeMillis() - mTime);
        mTime += dt;
        update(dt * 2);

        float radius = mStripWidth / 2;
        for (Strip strip : mStrips) {
            canvas.save();
            canvas.rotate(30);
            canvas.drawRoundRect(strip.mRect, radius, radius, strip.mBackPaint);
            canvas.drawRoundRect(strip.mRect, radius, radius, strip.mPaint);
            canvas.restore();
        }
    }

    //--------------------------------------------------------------------------------------------//

    private int nextAlpha() {
        // TODO [StripsDrawable] Do maxAlpha & minAlpha customizable
        int max = 120;
        int min = 40;

        return mRandomizer.nextInt(max - min) + min;
    }

    private PointF nextPosition() {
        return new PointF(mWidth, mRandomizer.nextInt(Math.max(1, mWidth + mHeight + mHeight / 3)) - mWidth);
    }

    private float nextLength() {
        return mRandomizer.nextInt(Math.max(1, (int) (mWidth - mStripWidth))) + mStripWidth;
    }

    //--------------------------------------------------------------------------------------------//

    private void spawnNewStrips(int dt) {
        mSpawnClock += dt;

        while (mSpawnClock > mSpawnTime && mStrips.size() < mSpawnLimit) {
            float length = nextLength();

            if (length < mStripWidth) {
                continue;
            }
            mStrips.add(new Strip(length));
            mSpawnClock -= mSpawnTime;
        }
    }

    private void update(int dt) {
        spawnNewStrips(dt);

        Iterator<Strip> iterator = mStrips.iterator();
        while (iterator.hasNext()) {
            Strip strip = iterator.next();

            strip.update(dt);
            if (strip.mCenter.x + strip.mLength / 2 < 0) {
                iterator.remove();
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    private class Strip {

        private final Paint
                mPaint,
                mBackPaint;

        private final RectF mRect;
        private final PointF mCenter;

        private final float mLength;

        //----------------------------------------------------------------------------------------//

        private Strip(float length) {
            mLength = length;

            mRect = new RectF();

            mPaint = new Paint();
            mPaint.setColor(mColor);
            mPaint.setAlpha(nextAlpha());

            mBackPaint = new Paint();
            mBackPaint.setColor(Color.TRANSPARENT);
            mBackPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));

            mCenter = nextPosition();

            mRect.top = mCenter.y + mStripWidth / 2;
            mRect.bottom = mCenter.y - mStripWidth / 2;

            mCenter.x += length;
        }

        //----------------------------------------------------------------------------------------//

        private void update(int dt) {
            mCenter.x -= dt * (mSpeed * 20) / 1000;

            mRect.left = mCenter.x - mLength / 2;
            mRect.right = mCenter.x + mLength / 2;
        }
    }
}
