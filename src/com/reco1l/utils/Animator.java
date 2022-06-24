package com.reco1l.utils;

import android.view.View;
import android.view.ViewPropertyAnimator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import ru.nsu.ccfit.zuev.osu.GlobalManager;

// Created by Reco1l on 23/6/22 20:44

public class Animator {
    //Simplifies the usage of ViewPropertyAnimator class.

    private final View view;
    private Float fromX, toX;
    private Float fromY, toY;
    private Float fromZ, toZ;
    private Float fromScale, toScale;
    private Float fromScaleX, toScaleX;
    private Float fromScaleY, toScaleY;
    private Float pivotX, pivotY;
    private Float fromAlpha, toAlpha;
    private Easing interpolator = Easing.InOutQuad;
    private Runnable onStart, onEnd;
    private long delay = 0;

    /**
     * Don't forget to call {@link #play(long)} to start the animation
     *
     * @param view View to animate.
     */
    public Animator(View view) {
        this.view = view;
    }

    public Animator delay(long delay){
        this.delay = delay;
        return this;
    }

    public Animator pivot(Float pivotX, Float pivotY) {
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        return this;
    }

    public Animator scaleY(float fromScaleY, float toScaleY) {
        this.fromScaleY = fromScaleY;
        this.toScaleY = toScaleY;
        return this;
    }

    public Animator scaleX(float fromScaleX, float toScaleX) {
        this.fromScaleX = fromScaleX;
        this.toScaleX = toScaleX;
        return this;
    }

    public Animator scale(float fromScale, float toScale) {
        this.fromScale = fromScale;
        this.toScale = toScale;
        return this;
    }

    public Animator moveX(float fromX, float toX) {
        this.fromX = fromX;
        this.toX = toX;
        return this;
    }

    public Animator moveY(float fromY, float toY) {
        this.fromY = fromY;
        this.toY = toY;
        return this;
    }

    public Animator moveZ(float fromZ, float toZ) {
        this.fromZ = fromZ;
        this.toZ = toZ;
        return this;
    }

    /**
     * @param interpolator The interpolator to use.
     */
    public Animator interpolator(Easing interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public Animator fade(float fromAlpha, float toAlpha){
        this.fromAlpha = fromAlpha;
        this.toAlpha = toAlpha;
        return this;
    }

    /**
     * @param onStart The runnable to run when the animation starts.
     */
    public Animator runOnStart(Runnable onStart) {
        this.onStart = onStart;
        return this;
    }

    /**
     * @param onEnd Runnable to run when animation is finished
     */
    public Animator runOnEnd(Runnable onEnd) {
        this.onEnd = onEnd;
        return this;
    }


    /**
     * @param duration duration of animation in milliseconds.
     */
    public void play(long duration) {
        //hasWindowFocus() prevents crashes when the activity is paused.
        if(view == null || duration <= 0 || !GlobalManager.getInstance().getMainActivity().hasWindowFocus())
          return;
        
        if(fromX != null) view.setTranslationX(fromX);
        if(fromY != null) view.setTranslationY(fromY);
        if(fromZ != null) view.setTranslationZ(fromZ);
        if(pivotX != null) view.setPivotX(pivotX);
        if(pivotY != null) view.setPivotY(pivotY);

        if(fromScale != null || fromScaleX != null) 
            view.setScaleX(fromScale != null ? fromScale : fromScaleX);

        if(fromScale != null || fromScaleY != null)
            view.setScaleY(fromScale != null ? fromScale : fromScaleY);

        if(fromAlpha != null) view.setAlpha(fromAlpha);

        view.animate().cancel();

        ViewPropertyAnimator anim = view.animate();

        if(toX != null) anim.translationX(toX);
        if(toY != null) anim.translationY(toY);
        if(toZ != null) anim.translationZ(toZ);

        if(toScale != null || toScaleX != null)
            anim.scaleX(toScale != null ? toScale : toScaleX);

        if(toScale != null || toScaleY != null)
            anim.scaleY(toScale != null ? toScale : toScaleY);

        if(toAlpha != null) anim.alpha(toAlpha);

        if(interpolator != null) 
            anim.setInterpolator(EasingHelper.asInterpolator(interpolator));

        anim.setDuration(duration);
        anim.setListener(new BaseAnimationListener(){

            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                if(onStart != null)
                    onStart.run();
                super.onAnimationStart(animation);
            }
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if(onEnd != null)
                    onEnd.run();
                super.onAnimationEnd(animation);
            }
        });

        //Prevents crashes if the view isn't visible.
        anim.setUpdateListener(animation -> {
            if(!view.isShown()) {
                anim.cancel();
                return;
            }
            view.invalidate();
        });

        view.postDelayed(anim::start, delay);
    }

}
