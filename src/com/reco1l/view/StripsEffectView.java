package com.reco1l.view;
// Created by Reco1l on 06/12/2022, 23:23

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class StripsEffectView extends View {

    private StripsDrawable drawable;

    //--------------------------------------------------------------------------------------------//

    public StripsEffectView(Context context) {
        super(context);
        init();
    }

    public StripsEffectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        drawable = new StripsDrawable();
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setBackground(drawable);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        invalidate();
    }

    //--------------------------------------------------------------------------------------------//

    public void reset() {
        drawable.reset();
    }

    public void setStripColors(int[] colors) {
        drawable.setStripColors(colors);
    }

    public void setStripWidth(float width) {
        drawable.setStripWidth(width);
    }

    public void setStripSpeed(float speed) {
        drawable.setStripSpeed(speed);
    }

    public void setSpawnTime(int spawnTime) {
        drawable.setSpawnTime(spawnTime);
    }
}
