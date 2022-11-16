package com.reco1l.utils;

import android.view.View;
import android.widget.TextView;

public class AnimationTable {

    public static void textChange(TextView view, String newText) {
        if (view == null)
            return;

        Animation fadeIn = new Animation(view);

        fadeIn.runOnStart(() -> view.setText(newText));
        fadeIn.duration(150);

        Animation fadeOut = new Animation(view);

        fadeOut.fade(1, 0);
        fadeOut.runOnEnd(fadeIn::play);
        fadeOut.play(150);
    }

    public static void fadeOutIn(View view, Runnable onStart) {
        if (view == null)
            return;

        Animation fadeIn = new Animation(view);

        fadeIn.runOnStart(onStart);
        fadeIn.duration(150);

        Animation fadeOut = new Animation(view);

        fadeOut.fade(1, 0);
        fadeOut.runOnEnd(fadeIn::play);
        fadeOut.play(150);
    }

    public static void fadeOutScaleOut(View view) {
        Animation anim = new Animation(view);

        anim.fade(0, 1);
        anim.scale(1, 0.8f);
        anim.play(200);
    }

    public static void fadeIn(View view) {
        Animation anim = new Animation(view);

        anim.fade(0, 1);
        anim.play(300);
    }
}
