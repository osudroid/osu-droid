package com.reco1l.view;
// Created by Reco1l on 07/12/2022, 13:13

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
import com.reco1l.utils.Animation.UpdateListener;
import com.reco1l.utils.Resources;
import com.reco1l.utils.ViewUtils;

import ru.nsu.ccfit.zuev.osuplus.R;

public class LogoView extends CardView {


    private StripsEffectView stripsEffect;
    private ImageView overlay;
    private View highlight;

    private Animation
            lightIn,
            lightOut,
            speedIn,
            speedOut,
            rotateIn,
            rotateOut;

    private boolean isKiai = false;

    //--------------------------------------------------------------------------------------------//

    public LogoView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public LogoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setCardBackgroundColor(0xFF1E1E1E);
        if (!isInEditMode()) {
            setCardElevation(Resources.sdp(20));
        }

        ViewGroup.LayoutParams params = ViewUtils.match_parent();

        stripsEffect = new StripsEffectView(context);
        if (!isInEditMode()) {
            stripsEffect.setStripWidth(Resources.sdp(20));
        }
        stripsEffect.setRotation(30);

        highlight = new View(context);
        highlight.setBackground(new ColorDrawable(Color.WHITE));
        highlight.setAlpha(0);

        overlay = new ImageView(context);
        overlay.setImageResource(R.drawable.logo_overlay);
        overlay.setScaleType(ScaleType.FIT_XY);

        ImageView brand = new ImageView(context);
        brand.setImageResource(R.drawable.logo_brand);
        brand.setScaleType(ScaleType.FIT_XY);

        addView(stripsEffect, params);
        addView(highlight, params);
        addView(overlay, params);
        addView(brand, params);

        createAnimations();
    }

    private void createAnimations() {
        lightIn = Animation.of(highlight).toAlpha(0.1f);
        lightOut = Animation.of(highlight).toAlpha(0);

        UpdateListener onUpdate = value -> {
            float speed = (float) value;

            if (isKiai) {
                speed *= 2f;
            }
            stripsEffect.setStripSpeed(speed);
        };

        speedIn = Animation.ofFloat(1f, 12f).runOnUpdate(onUpdate);
        speedOut = Animation.ofFloat(12f, 1f).runOnUpdate(onUpdate);

        rotateIn = Animation.of(overlay);
        rotateOut = Animation.of(overlay);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setRadius(canvas.getHeight() / 2f);
        if (!isInEditMode()) {
            updatePeak();
            invalidate();
        }
    }

    public void onBeatUpdate(float beatLength) {
        long in = (long) (beatLength * 0.07f);
        long out = (long) (beatLength * 0.9f);

        if (Game.musicManager.isPlaying()) {
            if (isKiai) {
                lightOut.duration(out);
                lightIn.runOnEnd(lightOut::play).play(in);
            }
        }

        float increment = overlay.getRotation();
        if (isKiai) {
            increment += 25;
        } else {
            increment += 10;
        }

        rotateOut.toRotation(increment).delay(in).play(out);
        rotateIn.toRotation(increment).play(in);

        speedOut.delay(in).play(out);
        speedIn.play(in);
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

    public void setKiai(boolean isKiai) {
        this.isKiai = isKiai;
        this.stripsEffect.setSpawnTime(isKiai ? 300 : 600);
    }
}
