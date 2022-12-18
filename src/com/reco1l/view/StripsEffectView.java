package com.reco1l.view;
// Created by Reco1l on 06/12/2022, 23:23

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.utils.Animation;

public class StripsEffectView extends View implements BaseView {

    private StripsDrawable drawable;

    private Animation
            speedIn,
            speedOut;

    private boolean
            _syncToBeat = true;

    //--------------------------------------------------------------------------------------------//

    public StripsEffectView(Context context) {
        super(context);
        init(null);
    }

    public StripsEffectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public StripsEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public StripsEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        drawable = new StripsDrawable();

        if (attrs != null) {
            _syncToBeat = attrs.getAttributeBooleanValue(NS, "beatSync", true);

            drawable.stripWidth = attrs.getAttributeIntValue(NS, "stripWidth", 80);
            drawable.speed = attrs.getAttributeFloatValue(NS, "stripSpeed", 9f);
            drawable.spawnTime = attrs.getAttributeIntValue(NS, "spawnTime", 200);
            drawable.limit = attrs.getAttributeIntValue(NS, "spawnLimit", 60);
        }

        setLayerType(LAYER_TYPE_HARDWARE, null);
        setBackground(drawable);

        if (_syncToBeat) {
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

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isInEditMode()) {
            if (Game.timingWrapper.isNextBeat()) {
                onBeatUpdate();
            }
        }
        invalidate();
    }

    private void onBeatUpdate() {
        setSpawnTime(Game.timingWrapper.isKiai() ? 200 : 400);

        float beatLength = Game.timingWrapper.getBeatLength();

        long in = (long) (beatLength * 0.07f);
        long out = (long) (beatLength * 0.9f);

        speedOut.delay(in).play(out);
        speedIn.play(in);
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
        _syncToBeat = bool;
    }
}
