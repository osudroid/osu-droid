package com.reco1l.utils;

import android.view.GestureDetector;
import android.view.MotionEvent;

// Created by Reco1l on 23/6/22 20:44

public class GestureListener implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    @Override
    public boolean onDown(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent event) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent event0, MotionEvent event1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent event) {

    }

    @Override
    public boolean onFling(MotionEvent event0, MotionEvent event1, float v, float v1) {
        return false;
    }
}
