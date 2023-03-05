package com.reco1l.ui.elements;
// Created by Reco1l on 07/12/2022, 13:13

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Views;
import com.reco1l.view.RoundLayout;
import com.reco1l.view.effects.StripsEffect;

import ru.nsu.ccfit.zuev.osuplus.R;

public final class LogoView extends RoundLayout {

    private ImageView mLines;
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
        setBackground(new ColorDrawable(0xFF1E1E1E));
        setConstantInvalidation(true);
        setClampToSquare(true);
        setMaxRounded(true);

        ViewGroup.LayoutParams params = Views.match_parent;

        // Strips effect
        StripsEffect effect = new StripsEffect(getContext());
        effect.setStripWidth(sdp(20));
        addView(effect, params);

        // Flash effect
        mFlash = new View(getContext());
        mFlash.setBackground(new ColorDrawable(Color.WHITE));
        mFlash.setAlpha(0);
        addView(mFlash, params);

        // Border overlay
        ImageView overlay = new ImageView(getContext());
        overlay.setImageResource(R.drawable.logo_overlay);
        addView(overlay, params);

        // Lines overlay
        mLines = new ImageView(getContext());
        mLines.setImageResource(R.drawable.logo_lines);
        addView(mLines, params);

        // rimu!
        ImageView brand = new ImageView(getContext());
        brand.setImageResource(R.drawable.logo_brand);
        addView(brand, params);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onManagedDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        if (Game.timingWrapper.isNextBeat()) {
            onNextBeat(Game.timingWrapper.getBeatLength());
        }
        updatePeak();
    }

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
