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
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.reco1l.Game;
import com.reco1l.utils.Animation;
import com.reco1l.utils.ViewUtils;
import com.reco1l.view.effects.StripsEffect;

import ru.nsu.ccfit.zuev.osuplus.R;

public class LogoView extends CardView implements BaseView {

    private ImageView lines;
    private RelativeLayout layout;
    private StripsEffect effect;

    private Animation
            lightOut,
            lightIn,
            rotate;

    //--------------------------------------------------------------------------------------------//

    public LogoView(@NonNull Context context) {
        this(context, null);
    }

    public LogoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        onCreate(attrs);
    }

    public LogoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onCreate(attrs);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public View getView() {
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate(AttributeSet attrs) {
        layout = new RelativeLayout(getContext());
        addView(layout);

        setCardBackgroundColor(0xFF1E1E1E);
        setCardElevation(sdp(20));

        ViewGroup.LayoutParams p = ViewUtils.match_parent;

        effect = new StripsEffect(getContext());
        effect.setStripWidth(sdp(20));
        layout.addView(effect, p);

        View flash = new View(getContext());
        flash.setBackground(new ColorDrawable(Color.WHITE));
        flash.setAlpha(0);
        layout.addView(flash, p);

        ImageView overlay = new ImageView(getContext());
        overlay.setImageResource(R.drawable.logo_overlay);
        layout.addView(overlay, p);

        lines = new ImageView(getContext());
        lines.setImageResource(R.drawable.logo_lines);
        layout.addView(lines, p);

        ImageView brand = new ImageView(getContext());
        brand.setImageResource(R.drawable.logo_brand);
        layout.addView(brand, p);

        lightIn = Animation.of(flash).toAlpha(0.1f);
        lightOut = Animation.of(flash).toAlpha(0);

        rotate = Animation.of(lines);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onResize(int w, int h) {
        int size = Math.max(w, h);

        layout.getLayoutParams().width = size;
        layout.getLayoutParams().height = size;
        layout.requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        setRadius(canvas.getHeight() / 2f);

        if (!isInEditMode()) {
            if (Game.timingWrapper.isNextBeat()) {
                onNextBeat();
            }
            updatePeak();
        }

        super.onDraw(canvas);
        invalidate();
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

    private void onNextBeat() {
        float beatLength = Game.timingWrapper.getBeatLength();

        long in = (long) (beatLength * 0.07f);
        long out = (long) (beatLength * 0.9f);

        if (Game.musicManager.isPlaying()) {
            if (Game.timingWrapper.isKiai()) {
                lightOut.duration(out);
                lightIn.runOnEnd(lightOut::play).play(in);
            }
        }

        float increment = lines.getRotation();
        if (Game.timingWrapper.isKiai()) {
            increment += 25;
        } else {
            increment += 10;
        }

        rotate.toRotation(increment).play((long) beatLength);
    }

    //--------------------------------------------------------------------------------------------//

    public StripsEffect getStripsEffect() {
        return effect;
    }
}
