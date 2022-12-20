package com.reco1l.view;
// Created by Reco1l on 07/12/2022, 13:13

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.reco1l.Game;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Res;
import com.reco1l.utils.ViewUtils;

import ru.nsu.ccfit.zuev.osuplus.R;

public class LogoView extends CardView implements BaseView {

    private ImageView overlay;

    private Animation
            lightIn,
            lightOut,
            rotateIn,
            rotateOut;

    private boolean
            _syncToBeat = true;

    //--------------------------------------------------------------------------------------------//

    public LogoView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public LogoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LogoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            _syncToBeat = attrs.getAttributeBooleanValue(NS, "beatSync", true);
        }

        ViewGroup.LayoutParams params = ViewUtils.match_parent;

        setCardBackgroundColor(0xFF1E1E1E);
        if (!isInEditMode()) {
            setCardElevation(Res.sdp(20));
        }

        StripsEffectView stripsEffect = new StripsEffectView(context);
        if (!isInEditMode()) {
            stripsEffect.setStripWidth(Res.sdp(20));
        }
        addView(stripsEffect, params);

        View highlight = new View(context);
        highlight.setBackground(new ColorDrawable(Color.WHITE));
        highlight.setAlpha(0);
        addView(highlight, params);

        overlay = new ImageView(context);
        overlay.setImageResource(R.drawable.logo_overlay);
        overlay.setScaleType(ScaleType.FIT_XY);
        addView(overlay, params);

        ImageView brand = new ImageView(context);
        brand.setImageResource(R.drawable.logo_brand);
        brand.setScaleType(ScaleType.FIT_XY);
        addView(brand, params);

        lightIn = Animation.of(highlight).toAlpha(0.1f);
        lightOut = Animation.of(highlight).toAlpha(0);

        rotateIn = Animation.of(overlay);
        rotateOut = Animation.of(overlay);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        invalidate();
        setRadius(canvas.getHeight() / 2f);

        if (!isInEditMode()) {
            if (_syncToBeat && Game.timingWrapper.isNextBeat()) {
                onBeatUpdate();
            }
            updatePeak();
        }
    }

    private void onBeatUpdate() {
        float beatLength = Game.timingWrapper.getBeatLength();

        long in = (long) (beatLength * 0.07f);
        long out = (long) (beatLength * 0.9f);

        if (Game.musicManager.isPlaying()) {
            if (Game.timingWrapper.isKiai()) {
                lightOut.duration(out);
                lightIn.runOnEnd(lightOut::play).play(in);
            }
        }

        float increment = overlay.getRotation();
        if (Game.timingWrapper.isKiai()) {
            increment += 25;
        } else {
            increment += 10;
        }

        rotateOut.toRotation(increment).delay(in).play(out);
        rotateIn.toRotation(increment).play(in);
    }

    private void updatePeak() {
        if (!Game.musicManager.isPlaying()) {
            return;
        }
        float level = Game.songService.getLevel();

        float peak = Math.max(0.9f, 0.9f + level);
        setScaleX(peak);
        setScaleY(peak);
    }

    //--------------------------------------------------------------------------------------------//

    public void setBeatSyncing(boolean bool) {
        _syncToBeat = bool;
    }
}
