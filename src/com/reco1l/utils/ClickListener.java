package com.reco1l.utils;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

// Created by Reco1l on 23/6/22 20:44

public class ClickListener {

    private final View view;
    private boolean soundEffect = true;
    private boolean touchEffect = true;
    private boolean isOnlyOnce = false;
    private BassSoundProvider customSound;

    public ClickListener(View view) {
        this.view = view;
    }

    /**
     * @param bool true to play sound effect, false to disable.
     * @return
     * You can use {@link #soundEffect(BassSoundProvider)} to set a custom sound too.
     */
    public ClickListener soundEffect(boolean bool) {
        this.soundEffect = bool;
        return this;
    }

    public ClickListener soundEffect(BassSoundProvider customSound) {
        soundEffect = true;
        this.customSound = customSound;
        return this;
    }

    /**
     * @param bool true to play touch effect, false to disable.
     */
    public ClickListener touchEffect(Boolean bool) {
        touchEffect = bool;
        return this;
    }

    /**
     * Removes the listener once the view is clicked.
     */
    public ClickListener onlyOnce(Boolean bool) {
        isOnlyOnce = bool;
        return this;
    }

    public void simple(Runnable onUpAction) {simple(null, onUpAction); }

    /**
     * This method is used to add a simple (only ACTION_DOWN and ACTION_UP) click listener to the view.
     *
     * @param onDownAction action to be performed when the view is pressed.
     * @param onUpAction action to be performed when the view is released.
     */
    public void simple(Runnable onDownAction, Runnable onUpAction) {
        if(this.view == null)
            return;

        ClickEffect clickEffect = touchEffect ? new ClickEffect(view) : null;

        BassSoundProvider sound = customSound != null ?
                customSound : ResourceManager.getInstance().getSound("menuclick");

        this.view.setOnTouchListener((view, event) -> {

            if(touchEffect && clickEffect != null)
                clickEffect.play(event);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (onDownAction != null)
                    onDownAction.run();
                return true;
            }

            else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (onUpAction != null)
                    onUpAction.run();

                if (soundEffect && sound != null)
                    sound.play();

                if (isOnlyOnce)
                    view.setOnTouchListener(null);

                return true;
            }
            return false;
        });
    }

    /**
     * This method is used to add a gesture listener to the view.
     */
    public void gesture(GestureListener listener) {
        if(this.view == null)
            return;

        this.view.setOnTouchListener((view, event) -> {
            //For now, touchEffect doesn't work with gestures.
            //if (touchEffect) new ClickEffect(view, event);

            return new GestureDetector(view.getContext(), listener).onTouchEvent(event);
        });
    }

}
