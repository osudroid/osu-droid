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
}
