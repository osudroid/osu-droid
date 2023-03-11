package com.reco1l.ui.elements;
// Created by Reco1l on 07/12/2022, 13:13

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.framework.drawing.Dimension;
import com.reco1l.management.Settings;
import com.reco1l.framework.Animation;
import com.reco1l.view.RoundLayout;
import com.reco1l.view.effects.StripsEffect;

import com.rimu.R;

public final class LogoView extends RoundLayout {

    private ImageView
            mLines,
            mBrand,
            mOverlay;

    private StripsEffect mEffect;
    private View mFlash;

    //--------------------------------------------------------------------------------------------//

    public LogoView(@NonNull Context context) {
        super(context);
    }

    public LogoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LogoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate() {
        setBackground(new ColorDrawable(0xFF21293B));
        setConstantInvalidation(true);
        setClampToSquare(true);
        setMaxRounded(true);

        // Strips effect
        mEffect = new StripsEffect(getContext());
        mEffect.setStripWidth(sdp(20));
        addView(mEffect, getInitialLayoutParams());

        // Flash effect
        mFlash = new View(getContext());
        mFlash.setBackground(new ColorDrawable(Color.WHITE));
        mFlash.setAlpha(0);
        addView(mFlash, getInitialLayoutParams());

        // Border overlay
        mOverlay = new ImageView(getContext());
        mOverlay.setImageResource(R.drawable.logo_overlay);
        addView(mOverlay, getInitialLayoutParams());

        // Lines overlay
        mLines = new ImageView(getContext());
        mLines.setImageResource(R.drawable.logo_lines);
        addView(mLines, getInitialLayoutParams());

        // rimu!
        mBrand = new ImageView(getContext());
        mBrand.setImageResource(R.drawable.logo_brand);
        addView(mBrand, getInitialLayoutParams());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDimensionChange(Dimension dimens) {
        super.onDimensionChange(dimens);

        matchSize(mEffect);
        matchSize(mFlash);
        matchSize(mBrand);
        matchSize(mLines);
        matchSize(mOverlay);
    }

    @Override
    protected void onManagedDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        if (Settings.<Boolean>get("menusEffects", true)) {
            if (Game.timingWrapper.isNextBeat()) {
                onNextBeat(Game.timingWrapper.getBeatLength());
            }
        }
        updatePeak();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    //--------------------------------------------------------------------------------------------//

    private void updatePeak() {
        if (!Game.musicManager.isPlaying()) {
            return;
        }
        assert Game.songService != null;
        float level = Game.songService.getLevel();

        float peak = Math.max(0.9f, 0.9f + level);
        setScaleX(peak);
        setScaleY(peak);
    }

    private void onNextBeat(float length) {
        boolean isKiai = Game.timingWrapper.isKiai();

        long in = (long) (length * 0.07);
        long out = (long) (length * 0.9);


        if (isKiai) {
            Animation.of(mFlash)
                     .toAlpha(0.05f)
                     .runOnEnd(() ->
                             Animation.of(mFlash)
                                      .toAlpha(0)
                                      .play(out)
                     ).play(in);
        }

        float delta = mLines.getRotation();
        delta += isKiai ? 25 : 10;

        Animation.of(mLines)
                 .toRotation(delta)
                 .play((long) length);
    }
}
