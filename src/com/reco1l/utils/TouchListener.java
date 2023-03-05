package com.reco1l.utils;

import android.graphics.PointF;

// Created by Reco1l on 23/6/22 20:44

public class TouchListener {

    private PointF mLastPosition;

    //--------------------------------------------------------------------------------------------//

    public BassSoundProvider getPressDownSound() {
        return null;
    }

    public BassSoundProvider getPressUpSound() {
        return Game.resourcesManager.getSound("menuclick");
    }

    public BassSoundProvider getLongPressSound() {
        return null;
    }

    public boolean useOnlyOnce() {
        return false;
    }

    public boolean useTouchEffect() {
        return true;
    }

    public boolean useBorderlessEffect() {
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    void setPosition(float x, float y) {
        mLastPosition = new PointF(x, y);
    }

    public PointF getTouchPosition() {
        return mLastPosition;
    }

    //--------------------------------------------------------------------------------------------//

    public void onPressDown() {
        // Do something
    }

    public void onPressUp() {
        // Do something
    }

    public void onLongPress() {
        // Do something
    }
}
