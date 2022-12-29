package com.reco1l.view;
// Created by Reco1l on 06/12/2022, 23:23

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.utils.Animation;

import ru.nsu.ccfit.zuev.osuplus.R;

public class StripsEffectView extends View implements BaseView {

    private StripsDrawable drawable;

    private Animation
            speedIn,
            speedOut;

    private boolean syncToBeat = true;

    //--------------------------------------------------------------------------------------------//

    public StripsEffectView(Context context) {
        this(context, null);
    }

    public StripsEffectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        onCreate(attrs);
    }

    public StripsEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onCreate(attrs);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public View getView() {
        return this;
    }

    @Override
    public int[] getStyleable() {
        return R.styleable.StripsEffectView;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate(AttributeSet attrs) {
        drawable = new StripsDrawable();

        setLayerType(LAYER_TYPE_HARDWARE, null);
        setBackground(drawable);

        handleAttributes(attrs);

        if (syncToBeat) {
            Animation.UpdateListener onUpdate = value -> {
                float speed = (float) value;

                if (Game.timingWrapper.isKiai()) {
                    speed *= 2f;
                }
                setStripSpeed(speed);
            };

            speedIn = Animation.ofFloat(1f, 12f).runOnUpdate(onUpdate);
            speedOut = Animation.ofFloat(12f, 1f).runOnUpdate(onUpdate);
        }
    }

    @Override
    public void onManageAttributes(TypedArray a) {
        syncToBeat = a.getBoolean(R.styleable.StripsEffectView_beatSync, true);

        drawable.stripWidth = a.getDimension(R.styleable.StripsEffectView_stripWidth, 80);
        drawable.speed = a.getFloat(R.styleable.StripsEffectView_stripSpeed, 2f);
        drawable.spawnTime = a.getInt(R.styleable.StripsEffectView_spawnTime, 200);
        drawable.limit = a.getInt(R.styleable.StripsEffectView_spawnLimit, 60);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isInEditMode()) {
            if (syncToBeat && Game.timingWrapper.isNextBeat()) {
                onNextBeat();
            }
        }

        super.onDraw(canvas);
        invalidate();
    }

    private void onNextBeat() {
        setSpawnTime(Game.timingWrapper.isKiai() ? 200 : 400);

        float beatLength = Game.timingWrapper.getBeatLength();

        long in = (long) (beatLength * 0.07f);
        long out = (long) (beatLength * 0.9f);

        if (speedOut != null && speedIn != null) {
            speedOut.delay(in).play(out);
            speedIn.play(in);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void reset() {
        drawable.reset();
    }

    public void setStripColors(int... colors) {
        drawable.colors = colors;
        reset();
    }

    public void setStripWidth(float width) {
        drawable.stripWidth = width;
        reset();
    }

    public void setStripSpeed(float speed) {
        drawable.speed = speed;
    }

    public void setSpawnTime(int spawnTime) {
        drawable.spawnTime = spawnTime;
    }

    public void setStripLimit(int limit) {
        drawable.limit = limit;
    }

    public void setBeatSyncing(boolean bool) {
        syncToBeat = bool;
    }
}
