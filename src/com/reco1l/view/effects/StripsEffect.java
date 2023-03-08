package com.reco1l.view.effects;
// Created by Reco1l on 06/12/2022, 23:23

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.management.Settings;
import com.reco1l.framework.Animation;
import com.reco1l.view.RoundLayout;
import com.reco1l.view.drawables.StripsDrawable;

public class StripsEffect extends RoundLayout {

    private StripsDrawable mDrawable;

    private Animation
            mSpeedIn,
            mSpeedOut;

    private boolean mSyncToBeat = true;

    //--------------------------------------------------------------------------------------------//

    public StripsEffect(@NonNull Context context) {
        super(context);
    }

    public StripsEffect(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StripsEffect(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate() {
        mDrawable = new StripsDrawable();

        ViewGroup internal = getInternalLayout();
        internal.setLayerType(LAYER_TYPE_HARDWARE, null);
        internal.setBackground(mDrawable);

        setRadius(0);
        setConstantInvalidation(true);
    }

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        mSyncToBeat = a.getAttributeBooleanValue(appNS, "beatSync", true);

        if (mSyncToBeat) {
            Animation.UpdateListener onUpdate = value -> {
                float speed = (float) value;

                if (Game.timingWrapper.isKiai()) {
                    speed *= 2f;
                }
                setStripSpeed(speed);
            };

            mSpeedIn = Animation.ofFloat(1f, 12f).runOnUpdate(onUpdate);
            mSpeedOut = Animation.ofFloat(12f, 1f).runOnUpdate(onUpdate);
        }
    }

    //--------------------------------------------------------------------------------------------//


    @Override
    protected void onManagedDraw(Canvas canvas) {
        if (isInEditMode() || !Settings.<Boolean>get("menusEffects", true)) {
            return;
        }

        if (mSyncToBeat && Game.timingWrapper.isNextBeat()) {
            onNextBeat();
        }
    }

    private void onNextBeat() {
        setSpawnTime(Game.timingWrapper.isKiai() ? 200 : 400);

        float beatLength = Game.timingWrapper.getBeatLength();

        long in = (long) (beatLength * 0.07f);
        long out = (long) (beatLength * 0.9f);

        if (mSpeedOut != null && mSpeedIn != null) {
            mSpeedOut.delay(in).play(out);
            mSpeedIn.play(in);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void reset() {
        mDrawable.reset();
    }

    public void setStripColor(@ColorInt int color) {
        mDrawable.setStripColor(color);
        reset();
    }

    public void setStripWidth(float width) {
        mDrawable.setStripWidth(width);
        reset();
    }

    public void setStripSpeed(float speed) {
        mDrawable.setStripSpeed(speed);
    }

    public void setSpawnTime(int spawnTime) {
        mDrawable.setSpawnTime(spawnTime);
    }

    public void setSpawnLimit(int spawnLimit) {
        mDrawable.setSpawnLimit(spawnLimit);
    }

    public void setSyncToBeat(boolean bool) {
        mSyncToBeat = bool;
    }
}
