package com.reco1l.framework.input;

import android.graphics.PointF;

// Created by Reco1l on 23/6/22 20:44

public class TouchListener {

    private PointF mLastPosition;

    //--------------------------------------------------------------------------------------------//

    public String getPressDownSound() {
        return null;
    }

    public String getPressUpSound() {
        return "menuclick";
    }

    public String getLongPressSound() {
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
