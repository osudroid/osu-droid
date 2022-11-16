package com.reco1l.ui.fragments;

// Created by Reco1l on 13/11/2022, 21:13

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation2;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.BlurEffect;
import com.reco1l.utils.Resources;
import com.reco1l.view.KiaiView;
import com.reco1l.view.SpectrumView;

import ru.nsu.ccfit.zuev.osuplus.R;

public class Background extends UIFragment {

    public static Background instance;

    private KiaiView kiaiView;
    private SpectrumView spectrum;

    private ImageView image0, image1;

    private AsyncExec backgroundTask;

    private String imagePath;
    private Bitmap bitmap;

    private boolean isBlur = false;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "b";
    }

    @Override
    protected int getLayout() {
        return R.layout.background;
    }

    @Override
    protected Screens[] getParents() {
        return new Screens[] {Screens.MAIN, Screens.SONG_MENU};
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        image0 = find("image0");
        image1 = find("image1");

        spectrum = find("spectrum");
        spectrum.setLinesWidth(Resources.sdp(6));
        spectrum.setLineDistance(Resources.sdp(1));

        kiaiView = find("kiai");
        kiaiView.setPaintColor(Color.WHITE);

        changeFrom(imagePath);
    }

    @Override
    protected void onUpdate(float secondsElapsed) {

        float[] fft = Game.songService.getSpectrum();
        float level = Game.songService.getLevel() * 2;

        if (spectrum != null) {
            spectrum.setFft(fft);
        }

        if (kiaiView != null) {
            kiaiView.setAlpha(level);
        }
    }

    public void onBeatUpdate(float beatLength, int beat) {
        Game.mActivity.runOnUiThread(() -> {
            if (kiaiView != null) {
                kiaiView.onBeatUpdate(beatLength, beat);
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    public void setKiai(boolean bool) {
        if (kiaiView != null) {
            kiaiView.setKiai(bool);
        }
    }

    public void setBlur(boolean bool) {
        if (bool == isBlur) {
            return;
        }
        isBlur = bool;
        changeFrom(imagePath);
    }

    //--------------------------------------------------------------------------------------------//

    public void changeFrom(String path) {
        imagePath = path;

        if (backgroundTask != null) {
            backgroundTask.cancel(true);
        }

        backgroundTask = new AsyncExec() {
            Bitmap decoded;

            public void run() {
                if (path != null) {
                    decoded = BitmapFactory.decodeFile(imagePath);

                    if (isBlur) {
                        decoded = BlurEffect.applyTo(decoded, 25);
                    }
                }
            }

            public void onComplete() {
                handleChange(decoded);
                Game.resources.loadBackground(path);
            }
        };
        backgroundTask.execute();
    }

    private void handleChange(Bitmap newBitmap) {

        boolean cursor = image0.getVisibility() == View.VISIBLE;

        Game.mActivity.runOnUiThread(() -> {
            ImageView front = cursor ? image0 : image1;
            ImageView back = cursor ? image1 : image0;

            back.setImageBitmap(newBitmap);
            back.setVisibility(View.VISIBLE);
            back.setAlpha(1f);

            Animation2 anim = Animation2.of(front);

            anim.toAlpha = 0f;
            anim.runOnEnd =() -> {
                front.setImageBitmap(null);
                front.setVisibility(View.GONE);
                front.setElevation(0f);

                back.setElevation(1f);

                if (bitmap != null) {
                    bitmap.recycle();
                }
                bitmap = newBitmap;
            };

            anim.play(500);
        });
    }
}
