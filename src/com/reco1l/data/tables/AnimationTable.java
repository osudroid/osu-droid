package com.reco1l.data.tables;

import android.view.View;
import android.widget.TextView;

import com.reco1l.utils.Animation;
import com.reco1l.utils.AnimationOld;

public class AnimationTable {

    public static void textChange(TextView view, String newText) {
        if (view == null)
            return;

        AnimationOld fadeIn = new AnimationOld(view);

        fadeIn.runOnStart(() -> view.setText(newText));
        fadeIn.duration(150);

        AnimationOld fadeOut = new AnimationOld(view);

        fadeOut.fade(1, 0);
        fadeOut.runOnEnd(fadeIn::play);
        fadeOut.play(150);
    }

    public static void fadeOutIn(View view, Runnable onStart) {
        if (view == null)
            return;

        AnimationOld fadeIn = new AnimationOld(view);

        fadeIn.runOnStart(onStart);
        fadeIn.duration(150);

        AnimationOld fadeOut = new AnimationOld(view);

        fadeOut.fade(1, 0);
        fadeOut.runOnEnd(fadeIn::play);
        fadeOut.play(150);
    }

    public static void fadeOutScaleOut(View view) {
        AnimationOld anim = new AnimationOld(view);

        anim.fade(0, 1);
        anim.scale(1, 0.8f);
        anim.play(200);
    }

    public static Animation moveY(View view, float to) {
        return Animation.of(view).toY(to);
    }

    public static Animation fadeIn(View... views) {
        return Animation.of(views).toAlpha(1);
    }

    public static Animation fadeOut(View... views) {
        return Animation.of(views).toAlpha(0);
    }
}
